package com.dudifidof.longwalk.command;

import com.dudifidof.longwalk.navigation.ClientWorldView;
import com.dudifidof.longwalk.navigation.NavigationState;
import com.dudifidof.longwalk.pathfinding.BlockPos3;
import com.dudifidof.longwalk.pathfinding.PathStep;
import com.dudifidof.longwalk.pathfinding.PathfindingEngine;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** Registers `/run <x> <y> <z>` and `/run stop`. */
public final class RunCommand {

    private RunCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("run")
                        .then(literal("stop").executes(RunCommand::stop))
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .executes(RunCommand::start))))));
    }

    private static int start(CommandContext<FabricClientCommandSource> ctx) {
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int y = IntegerArgumentType.getInteger(ctx, "y");
        int z = IntegerArgumentType.getInteger(ctx, "z");

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            ctx.getSource().sendError(Text.literal("No player/world loaded."));
            return 0;
        }

        BlockPos3 start = new BlockPos3(
                (int) Math.floor(client.player.getX()),
                (int) Math.floor(client.player.getY()),
                (int) Math.floor(client.player.getZ()));
        BlockPos3 goal = new BlockPos3(x, y, z);

        PathfindingEngine engine = new PathfindingEngine(new ClientWorldView(client));
        Optional<List<PathStep>> path = engine.findPath(start, goal);

        if (path.isEmpty()) {
            ctx.getSource().sendError(Text.literal("No path found to " + x + " " + y + " " + z + "."));
            return 0;
        }

        NavigationState.start(goal, path.get());
        ctx.getSource().sendFeedback(
                Text.literal("Running to " + x + " " + y + " " + z + " (" + path.get().size() + " steps)."));
        return 1;
    }

    private static int stop(CommandContext<FabricClientCommandSource> ctx) {
        boolean wasActive = NavigationState.isActive();
        NavigationState.requestCancel();
        ctx.getSource().sendFeedback(Text.literal(wasActive ? "Stopping..." : "Nothing to stop."));
        return 1;
    }
}
