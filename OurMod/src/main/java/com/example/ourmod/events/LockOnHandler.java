package com.example.ourmod.events;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class LockOnHandler {
    private static final Map<UUID, BlockPos> lockedTargets = new HashMap<>();
    private static final float SMOOTH_FACTOR = 0.01f; // Higher = faster ease-in/out
    private static final int TNT_FUSE_TICKS = 40;

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("lockon")
                .then(Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("y", IntegerArgumentType.integer())
                .then(Commands.argument("z", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        int x = IntegerArgumentType.getInteger(ctx, "x");
                        int y = IntegerArgumentType.getInteger(ctx, "y");
                        int z = IntegerArgumentType.getInteger(ctx, "z");

                        lockedTargets.put(player.getUUID(), new BlockPos(x, y, z));
                        ctx.getSource().sendSuccess(() -> Component.literal("Locked on!"), false);
                        return 1;
                    }))))
        );

        event.getDispatcher().register(
            Commands.literal("unlock")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    lockedTargets.remove(player.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("Unlocked."), false);
                    return 1;
                })
        );

        event.getDispatcher().register(
            Commands.literal("tnt")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    var level = player.level();

                    Vec3 look = player.getLookAngle();
                    Vec3 spawn = player.position().add(look.scale(1.5));

                    PrimedTnt tnt = new PrimedTnt(level, spawn.x, spawn.y, spawn.z, player);
                    tnt.setFuse(TNT_FUSE_TICKS); // 2 seconds

                    level.addFreshEntity(tnt);
                    ctx.getSource().sendSuccess(() -> Component.literal("BOOM incoming!"), false);
                    return 1;
                })
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        UUID uuid = event.player.getUUID();
        BlockPos target = lockedTargets.get(uuid);
        if (target == null) return;

        Vec3 eye = event.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(target);
        Vec3 dir = center.subtract(eye);

        float targetYaw = (float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90);
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z))));

        float currentYaw = event.player.getYRot();
        float currentPitch = event.player.getXRot();

        float newYaw = smoothDampAngle(currentYaw, targetYaw, SMOOTH_FACTOR);
        float newPitch = smoothDamp(currentPitch, targetPitch, SMOOTH_FACTOR);

        event.player.setYRot(newYaw);
        event.player.setXRot(newPitch);

        if (event.player.level().isClientSide) {
            event.player.yRotO = newYaw;
            event.player.xRotO = newPitch;
            event.player.yHeadRot = newYaw;
            event.player.yBodyRot = newYaw;
        }
    }

    // Better easing than lerp: exponential smooth damp
    private static float smoothDamp(float current, float target, float factor) {
        return current + (target - current) * (1 - (float) Math.pow(1 - factor, 3));
    }

    // Smooth damp that wraps correctly around angles
    private static float smoothDampAngle(float current, float target, float factor) {
        float delta = wrapDegrees(target - current);
        return current + delta * (1 - (float) Math.pow(1 - factor, 3));
    }

    private static float wrapDegrees(float degrees) {
        degrees %= 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }
}
