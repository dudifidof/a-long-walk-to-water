package com.example.ourmod;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TNTCommand {
    private static final int TNT_FUSE_TICKS = 40;
    private static final int COUNT = 4;
    private static final double RADIUS = 3.0;

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("tnt")
                .requires(cs -> cs.hasPermission(0)) // No permission required
                .executes(ctx -> spawnTntRing(ctx.getSource()))
        );
    }

    private static int spawnTntRing(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Vec3 pos = player.position();

        double radius = RADIUS;
        int count = COUNT;

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = pos.x + radius * Math.cos(angle);
            double z = pos.z + radius * Math.sin(angle);
            double y = pos.y;

            PrimedTnt tnt = new PrimedTnt(EntityType.TNT, player.level());
            tnt.setPos(x, y, z);
            tnt.setFuse(TNT_FUSE_TICKS); // 2 seconds (20 ticks per second)
            player.level().addFreshEntity(tnt);
        }

        return 1;
    }
}
