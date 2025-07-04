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

/**
 * Mod configuration options loaded via Forge's config system.
 */
@Mod.EventBusSubscriber(modid = OurMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    private Config() {}
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue WEBSOCKET_PORT = BUILDER
            .comment("Port for the WebSocket server")
            .defineInRange("websocketPort", 9001, 1, 65535);

    public static final ForgeConfigSpec.BooleanValue ENABLE_WEBSOCKET = BUILDER
            .comment("Whether to launch the WebSocket server")
            .define("enableWebsocket", true);

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
    public static int webSocketPort;
    public static boolean enableWebSocket;

    static boolean validateItemName(final Object obj)
    {
        if (!(obj instanceof String itemName)) return false;
        ResourceLocation rl = ResourceLocation.tryParse(itemName);
        return rl != null && ForgeRegistries.ITEMS.containsKey(rl);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (!event.getConfig().getModId().equals(OurMod.MODID)) {
            return;
        }
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        webSocketPort = WEBSOCKET_PORT.get();
        enableWebSocket = ENABLE_WEBSOCKET.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(ResourceLocation::tryParse)
                .map(rl -> rl == null ? null : ForgeRegistries.ITEMS.getValue(rl))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), java.util.Collections::unmodifiableSet));
    }
}
