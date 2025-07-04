package com.example.ourmod;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public class WebSocketTNTListener extends WebSocketServer {
    private static final Logger LOGGER = LogUtils.getLogger();

    public WebSocketTNTListener(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("WebSocket connection opened: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("WebSocket connection closed: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        LOGGER.info("WebSocket message: {}", message);
        if (message.equalsIgnoreCase("!tnt")) {
            // Run Minecraft command from the server thread
            OurMod.runCommandFromServerThread("tnt");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("WebSocket error", ex);
    }

    @Override
    public void onStart() {
        LOGGER.info("WebSocket server started");
    }
}
