package net.citron.lemons_talking_mobs;

import com.mojang.logging.LogUtils;
import net.citron.lemons_talking_mobs.client.gui.MobSoundScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.citron.lemons_talking_mobs.network.*;
import net.citron.lemons_talking_mobs.server.ServerSoundStorage;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Lemons_talking_mobs.MODID)
@EventBusSubscriber(modid = Lemons_talking_mobs.MODID)
public class Lemons_talking_mobs {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "lemons_talking_mobs";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "lemons_talking_mobs" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "lemons_talking_mobs" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "lemons_talking_mobs" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "lemons_talking_mobs:example_block", combining the namespace and path
    /*
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "lemons_talking_mobs:example_block", combining the namespace and path

    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);
     */
/*
    // Creates a new food item with the id "lemons_talking_mobs:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "lemons_talking_mobs:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.lemons_talking_mobs")).withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> EXAMPLE_ITEM.get().getDefaultInstance()).displayItems((parameters, output) -> {
        output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
    }).build());

 */

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Lemons_talking_mobs(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Lemons_talking_mobs) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        //NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        //modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }
/*
    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(EXAMPLE_BLOCK_ITEM);
    }

 */

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        LOGGER.info("[TalkingMobs] Register payloads...");

        // Vytvoříme registrátor pro náš MODID s verzí "1"
        final PayloadRegistrar registrar = event.registrar(MODID).versioned("1");

        // 1. Ukládání zvuku (Klient -> Server)
        registrar.playToServer(
                SaveMobSoundPayload.TYPE,
                SaveMobSoundPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        ServerSoundStorage.saveSound(
                                payload.entityUuid(),
                                payload.entityTypeKey(),
                                payload.soundKey(),
                                payload.audioData(),
                                payload.isGlobal()
                        );
                    });
                }
        );

        // 2. Požadavek na existující nahrávku při otevření GUI (Klient -> Server)
        registrar.playToServer(
                RequestMobSoundPayload.TYPE,
                RequestMobSoundPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            byte[] data = ServerSoundStorage.getExactSound(
                                    payload.entityUuid(),
                                    payload.entityTypeKey(),
                                    payload.soundKey(),
                                    payload.isGlobal()
                            );
                            if (data == null) data = new byte[0];
                            serverPlayer.connection.send(new SyncMobSoundPayload(payload.entityUuid(), payload.soundKey(), data));
                        }
                    });
                }
        );

        // 3. Odeslání nahrávky do otevřeného GUI (Server -> Klient)
        registrar.playToClient(
                SyncMobSoundPayload.TYPE,
                SyncMobSoundPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.screen instanceof net.citron.lemons_talking_mobs.client.gui.MobSoundScreen screen) {
                            screen.onSoundSynced(payload.soundKey(), payload.audioData());
                        }
                    });
                }
        );

        // NEW: 4. Start nahrávání (Klient -> Server)
        // Client asks the server to start buffering ITS mic packets. We use
        // context.player().getUUID() so this is safely per-player even with
        // several people connected at once.
        registrar.playToServer(
                StartRecordingPayload.TYPE,
                StartRecordingPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        TalkingMobsVoicePlugin.startRecordingFor(context.player().getUUID());
                    });
                }
        );

        // NEW: 5. Stop nahrávání (Klient -> Server), server sends result back
        registrar.playToServer(
                StopRecordingPayload.TYPE,
                StopRecordingPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        byte[] result = TalkingMobsVoicePlugin.stopRecordingFor(context.player().getUUID());
                        if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            serverPlayer.connection.send(new RecordingResultPayload(result));
                        }
                    });
                }
        );

        // NEW: 6. Doručení výsledku nahrávky zpět klientovi (Server -> Klient)
        registrar.playToClient(
                RecordingResultPayload.TYPE,
                RecordingResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.screen instanceof MobSoundScreen screen) {
                            screen.onRecordingResult(payload.audioData());
                        }
                    });
                }
        );

    }

    @EventBusSubscriber(modid = MODID)
    public static class GameEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            ServerSoundStorage.init(event.getServer());
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
   /*
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    */

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
