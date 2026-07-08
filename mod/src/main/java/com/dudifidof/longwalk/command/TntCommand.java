package com.dudifidof.longwalk.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Registers `/tnt`, which spawns a ring of primed TNT around the player by
 * sending vanilla `/execute ... summon` commands. Spawning entities is
 * server-authoritative and this mod ships no server-side component, so the
 * player needs operator permission (or cheats enabled in singleplayer) for
 * this to work.
 */
public final class TntCommand {

    private static final int RING_RADIUS = 3;
    private static final int RING_COUNT = 8;

    private TntCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("tnt").executes(TntCommand::spawnRing)));
    }

    private static int spawnRing(CommandContext<FabricClientCommandSource> ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            ctx.getSource().sendError(Text.literal("No player loaded."));
            return 0;
        }

        BlockPos center = client.player.getBlockPos();
        for (int i = 0; i < RING_COUNT; i++) {
            double angle = 2 * Math.PI * i / RING_COUNT;
            int x = center.getX() + (int) Math.round(RING_RADIUS * Math.cos(angle));
            int z = center.getZ() + (int) Math.round(RING_RADIUS * Math.sin(angle));
            String command = String.format(
                    "execute positioned %d %d %d run summon minecraft:tnt ~ ~ ~ {Fuse:80}",
                    x, center.getY(), z);
            client.player.networkHandler.sendChatCommand(command);
        }

        ctx.getSource().sendFeedback(Text.literal("Spawned a ring of TNT."));
        return 1;
    }
}
