package com.example.ourmod;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class WebSocketCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("websocket")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.literal("start")
                    .executes(ctx -> {
                        if (!Config.enableWebSocket) {
                            ctx.getSource().sendFailure(Component.literal("WebSocket disabled in config."));
                            return 0;
                        }

                        if (OurMod.getInstance().isWebSocketRunning()) {
                            int p = OurMod.getInstance().getRunningWebSocketPort();
                            ctx.getSource().sendFailure(Component.literal("WebSocket server already running on port " + p));
                            return 0;
                        }

                        CommandSourceStack source = ctx.getSource();
                        new Thread(() -> {
                            boolean ok = OurMod.getInstance().startWebSocket();
                            if (ok) {
                                int p = OurMod.getInstance().getRunningWebSocketPort();
                                source.sendSuccess(() -> Component.literal("WebSocket server started on port " + p), false);
                            } else {
                                source.sendFailure(Component.literal("Failed to start WebSocket server"));
                            }
                        }, "WebSocketStartCommand").start();

                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("stop")
                    .executes(ctx -> {
                        boolean ok = OurMod.getInstance().stopWebSocket();
                        if (ok) {
                            ctx.getSource().sendSuccess(() -> Component.literal("WebSocket server stopped."), false);
                            return Command.SINGLE_SUCCESS;
                        } else {
                            ctx.getSource().sendFailure(Component.literal("WebSocket server was not running."));
                            return 0;
                        }
                    }))
        );
    }
}
