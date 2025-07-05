# a-long-walk-to-water

Hey Codex, we’re building a custom Minecraft Forge mod called OurMod for version 1.20.1. It’s created by Big Ev, and it's got a clean setup using Gradle with proper configuration files like mods.toml and Forge config specs. The mod currently adds a new food item, a custom block, and a creative mode tab to organize them. It logs some debug info like block names and user info to make sure everything's registering right.

The mod is now **version 2.0.0** and depends on the `Java-WebSocket` library so it can listen on a local WebSocket port and react to chat commands. The port and whether the server is launched are configurable via `websocketPort` and `enableWebsocket` in `common.toml`.
Setting the port to `0` will let the OS pick a free port automatically.

Forge already provides the SLF4J logging framework, so the build excludes that
dependency from the shaded WebSocket library to avoid module conflicts.

### Requirements
- Java 17
- Minecraft Forge **1.20.1**
- The included Gradle wrapper (no separate Gradle installation needed)

Big Ev is also experimenting with commands that interact with the world—like triggering TNT explosions based on external inputs (think YouTube or Twitch chat). We’ve wired up deferred registries, custom config files, and event listeners to prep for adding those interactive mechanics. The mod has clean client/server setup logic using Forge’s event bus and annotation system.

We’re debugging a crash related to missing or mismatched metadata in the mods.toml, but that’s being fixed by making sure modId, version, and displayName match exactly across files. Once stable, this will be a powerful modding base for live Minecraft interactivity and creative world effects. It’s tight, well-structured, and ready to evolve into something wild.

### Building and running
Use the included Gradle wrapper from the `OurMod` directory:

```bash
cd OurMod
./gradlew runclient

The bundled `startup.bat` script runs the same command and saves the output to
`run/client.log` for easier debugging on Windows.
```

This will download dependencies and start a development client with the mod loaded.

When you join a world in this dev environment you should see the chat message
`Welcome to Big Ev's world`. Use `/websocket start` to spin up the WebSocket
server and watch the console for **WEBSOCKET SERVER STARTED** to verify it's
running.

### Debugging the WebSocket
When the server starts, look for a log entry like:

```
[main/INFO] [OurMod]: Attempting WebSocket connection to ws://localhost:9001
```

If you see `WebSocket server started` shortly after, the socket is ready.

You can control the socket in-game with `/websocket start` and `/websocket stop`.
Players will see a chat message whenever the server starts or stops.

Below is the code from the mod for reference:
### Config.java
```java
# a-long-walk-to-water

Hey Codex, we’re building a custom Minecraft Forge mod called OurMod for version 1.20.1. It’s created by Big Ev, and it's got a clean setup using Gradle with proper configuration files like mods.toml and Forge config specs. The mod currently adds a new food item, a custom block, and a creative mode tab to organize them. It logs some debug info like block names and user info to make sure everything's registering right.

The mod is now **version 2.0.0** and depends on the `Java-WebSocket` library so it can listen on a local WebSocket port and react to chat commands. The port and whether the server is launched are configurable via `websocketPort` and `enableWebsocket` in `common.toml`.
Setting the port to `0` will let the OS pick a free port automatically.

Forge already provides the SLF4J logging framework, so the build excludes that
dependency from the shaded WebSocket library to avoid module conflicts.


### Requirements
- Java 17
- Minecraft Forge **1.20.1**
- The included Gradle wrapper (no separate Gradle installation needed)



Big Ev is also experimenting with commands that interact with the world—like triggering TNT explosions based on external inputs (think YouTube or Twitch chat). We’ve wired up deferred registries, custom config files, and event listeners to prep for adding those interactive mechanics. The mod has clean client/server setup logic using Forge’s event bus and annotation system.

We’re debugging a crash related to missing or mismatched metadata in the mods.toml, but that’s being fixed by making sure modId, version, and displayName match exactly across files. Once stable, this will be a powerful modding base for live Minecraft interactivity and creative world effects. It’s tight, well-structured, and ready to evolve into something wild.

### Building and running
Use the included Gradle wrapper from the `OurMod` directory:

```bash
cd OurMod
./gradlew runclient

The bundled `startup.bat` script runs the same command and saves the output to
`run/client.log` for easier debugging on Windows.
```

This will download dependencies and start a development client with the mod loaded.

When you join a world in this dev environment you should see the chat message
`Welcome to Big Ev's world`. Use `/websocket start` to spin up the WebSocket
server and watch the console for **WEBSOCKET SERVER STARTED** to verify it's
running.

### Debugging the WebSocket
When the server starts, look for a log entry like:

```
[main/INFO] [OurMod]: Attempting WebSocket connection to ws://localhost:9001
```

If you see `WebSocket server started` shortly after, the socket is ready.

You can control the socket in-game with `/websocket start` and `/websocket stop`.
Players will see a chat message whenever the server starts or stops.

Below is the code from the mod for reference:
### Config.java
```java
package com.example.ourmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = OurMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj)
    {
        if (!(obj instanceof String itemName)) return false;
        ResourceLocation rl = ResourceLocation.tryParse(itemName);
        return rl != null && ForgeRegistries.ITEMS.containsKey(rl);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> {
                    ResourceLocation rl = ResourceLocation.tryParse(itemName);
                    return rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
```

### LockOnHandler.java
```java
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
                    tnt.setFuse(40); // 2 seconds

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
```

### OurMod.java
```java
package com.example.ourmod;

import com.example.ourmod.events.LockOnHandler;
import com.example.ourmod.WebSocketTNTListener; // ✅ NEW: WebSocket import
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
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
import net.minecraftforge.event.server.ServerStartingEvent;
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

    public OurMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(LockOnHandler.class);
        MinecraftForge.EVENT_BUS.register(TNTCommand.class); // ✅ TNT command

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("OurMod v2.0.0 loaded successfully.");

        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        }

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item.toString()));

        // WebSocket server is started via the /websocket start command
    }
        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item.toString()));

        // WebSocket server is started via the /websocket start command
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    // ✅ WebSocket helper
    public static void runCommandFromServerThread(String command) {
        Minecraft.getInstance().tell(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.connection.sendCommand(command);
            }
        });
    }
}
```

### TNTCommand.java
```java
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

        double radius = 3.0;
        int count = 4;

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = pos.x + radius * Math.cos(angle);
            double z = pos.z + radius * Math.sin(angle);
            double y = pos.y;

            PrimedTnt tnt = new PrimedTnt(EntityType.TNT, player.level());
            tnt.setPos(x, y, z);
            tnt.setFuse(40); // 2 seconds (20 ticks per second)
            player.level().addFreshEntity(tnt);
        }

        return 1;
    }
}
```

### WebSocketTNTListener.java
```java
package com.example.ourmod;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;

public class WebSocketTNTListener extends WebSocketServer {

    public WebSocketTNTListener(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("WebSocket connection opened: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("WebSocket connection closed: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("WebSocket message: " + message);
        if (message.equalsIgnoreCase("!tnt")) {
            // Run Minecraft command from the server thread
            OurMod.runCommandFromServerThread("tnt");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started");
    }
}

```


### Troubleshooting
If you encounter compile errors like "identifier expected" or "class expected", make sure every `class` and method in `OurMod.java` or `WebSocketCommand.java` has matching braces. Gradle build failures usually mean something was pasted incorrectly.
=======
```

