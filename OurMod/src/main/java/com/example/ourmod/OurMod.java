package com.example.ourmod;

import com.example.ourmod.events.LockOnHandler;
import com.example.ourmod.WebSocketTNTListener;
import com.example.ourmod.WebSocketCommand;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.example.ourmod.Config;

import net.minecraftforge.event.server.ServerStoppingEvent;
import com.example.ourmod.Config;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;


@Mod(OurMod.MODID)
public class OurMod {
    public static final String MODID = "ourmod";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static OurMod INSTANCE;
    private static final int DEFAULT_WEBSOCKET_PORT = 9001;
    private static WebSocketTNTListener webSocketServer;
    private static volatile boolean webSocketRunning;
    private static int actualWebSocketPort = -1;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

@Mod(OurMod.MODID)
public class OurMod {
    public static final String MODID = "ourmod";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static OurMod INSTANCE;
    private static final int DEFAULT_WEBSOCKET_PORT = 9001;
    private static WebSocketTNTListener webSocketServer;
    private static volatile boolean webSocketRunning;
    private static int actualWebSocketPort = -1;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));

    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () ->
            new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () ->
            new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .alwaysEat().nutrition(1).saturationMod(2f).build())));

    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () ->
            CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(EXAMPLE_ITEM.get());
                    }).build());

    public OurMod() {
        this(FMLJavaModLoadingContext.get());
    }

    public OurMod(FMLJavaModLoadingContext context) {
        INSTANCE = this;
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);


        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(LockOnHandler.class);
        MinecraftForge.EVENT_BUS.register(TNTCommand.class);
        MinecraftForge.EVENT_BUS.register(WebSocketCommand.class);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /**
     * Starts the WebSocket server if it isn't already active. The underlying
     * Java-WebSocket library does not expose a direct `isRunning` check, so we
     * simply track whether our server instance is null.
     */
    public synchronized boolean startWebSocket() {
        if (webSocketRunning) {
            return false;
        }

        int configuredPort = Config.webSocketPort > 0 ? Config.webSocketPort : DEFAULT_WEBSOCKET_PORT;
        LOGGER.info("Attempting WebSocket connection to ws://localhost:{}", configuredPort);

        try {
            webSocketServer = new WebSocketTNTListener(configuredPort);
            Thread t = new Thread(() -> {
                try {
                    webSocketServer.start();
                } catch (Exception e) {
                    LOGGER.error("WebSocket server thread failed", e);
                }
            }, "WebSocketServer");
            t.start();
            actualWebSocketPort = webSocketServer.getPort();
            webSocketRunning = true;
            LOGGER.info("WebSocket server started on ws://localhost:{}", actualWebSocketPort);
            LOGGER.info("WEBSOCKET SERVER STARTED");
            webSocketServer.broadcast("Server started");
            broadcastToPlayers(Component.literal("WebSocket server listening on port " + actualWebSocketPort));
            return true;
        } catch (Throwable e) {
            LOGGER.error("WebSocket server failed to start", e);
            webSocketServer = null;
            actualWebSocketPort = -1;
            webSocketRunning = false;
            return false;
        }
    }

    /** Stops the WebSocket server if running. */
    public synchronized boolean stopWebSocket() {
        if (!webSocketRunning || webSocketServer == null) {
            return false;
        }
        try {
            LOGGER.info("Stopping WebSocket server");
            webSocketServer.stop(1000);
            webSocketServer = null;
            actualWebSocketPort = -1;
            webSocketRunning = false;
            broadcastToPlayers(Component.literal("WebSocket server stopped"));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while stopping WebSocket server", e);
            webSocketServer = null;
            actualWebSocketPort = -1;
            webSocketRunning = false;
            return false;
        } catch (Exception e) {
            LOGGER.error("Error stopping WebSocket server", e);
            webSocketServer = null;
            actualWebSocketPort = -1;
            webSocketRunning = false;
            return false;
        }
    }

    /**
     * Returns the current mod instance.
     */
    public static OurMod getInstance() {
        return INSTANCE;
    }

    /**
     * Provides access to the mod logger for other classes.
     */
    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Returns the port the WebSocket server is bound to, or -1 if not running.
     */
    public synchronized int getRunningWebSocketPort() {
        return actualWebSocketPort;
    }

    /**
     * Indicates whether the WebSocket server is currently active.
     */
    public synchronized boolean isWebSocketRunning() {
        return webSocketRunning;
    }

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(LockOnHandler.class);
        MinecraftForge.EVENT_BUS.register(TNTCommand.class);
        MinecraftForge.EVENT_BUS.register(WebSocketCommand.class);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /**
     * Starts the WebSocket server if it isn't already active. The underlying
     * Java-WebSocket library does not expose a direct `isRunning` check, so we
     * simply track whether our server instance is null.
     */
    public synchronized boolean startWebSocket() {
        if (webSocketRunning) {
            return false;
        }

        int configuredPort = Config.webSocketPort > 0 ? Config.webSocketPort : DEFAULT_WEBSOCKET_PORT;
        LOGGER.info("Attempting WebSocket connection to ws://localhost:{}", configuredPort);

        try {
            webSocketServer = new WebSocketTNTListener(configuredPort);
            Thread t = new Thread(() -> {
                try {
                    webSocketServer.start();
                } catch (Exception e) {
                    LOGGER.error("WebSocket server thread failed", e);
                }
            }, "WebSocketServer");
            t.start();

            actualWebSocketPort = webSocketServer.getPort();

            actualWebSocketPort = configuredPort;

            webSocketRunning = true;
            LOGGER.info("WebSocket server started on ws://localhost:{}", actualWebSocketPort);
            webSocketServer.broadcast("Server started");
            broadcastToPlayers(Component.literal("WebSocket server listening on port " + actualWebSocketPort));
            return true;
        } catch (Throwable e) {
            LOGGER.error("WebSocket server failed to start", e);
            webSocketServer = null;
            actualWebSocketPort = -1;
            webSocketRunning = false;
            return false;
        }
    }

    /** Stops the WebSocket server if running. */
    public synchronized boolean stopWebSocket() {
        if (!webSocketRunning || webSocketServer == null) {
            return false;
        }
        try {
            LOGGER.info("Stopping WebSocket server");
            webSocketServer.stop(1000);
            webSocketServer = null;
            actualWebSocketPort = -1;
            webSocketRunning = false;
            broadcastToPlayers(Component.literal("WebSocket server stopped"));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while stopping WebSocket server", e);
            webSocketServer = null;
            actualWebSocketPort = -1;
            webSocketRunning = false;
            return false;
        } catch (Exception e) {
            LOGGER.error("Error stopping WebSocket server", e);
            webSocketServer = null;
            actualWebSocketPort = -1;
            webSocketRunning = false;
            return false;
        }
    }

    /**
     * Returns the current mod instance.
     */
    public static OurMod getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the port the WebSocket server is bound to, or -1 if not running.
     */
    public synchronized int getRunningWebSocketPort() {
        return actualWebSocketPort;
    }

    /**
     * Indicates whether the WebSocket server is currently active.
     */
    public synchronized boolean isWebSocketRunning() {
        return webSocketRunning;
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("OurMod v2.0.0 loaded successfully.");

        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        }

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item.toString()));


        // WebSocket server can be started later via the /websocket command
    }

        // WebSocket server can be started later via the /websocket command
    }


    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }


    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        stopWebSocket();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        event.getEntity().sendSystemMessage(Component.literal("Welcome to Big Ev's world"));
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        stopWebSocket();
    }


    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    /**
     * Executes a chat command from the main server thread to avoid concurrency issues.
     */
    public static void runCommandFromServerThread(String command) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() ->
                server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), command));
        }
    }

    /**
     * Sends a chat message to all players on the server thread.
     */
    private static void broadcastToPlayers(Component text) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> server.getPlayerList().broadcastSystemMessage(text, false));
        }
    }
}
