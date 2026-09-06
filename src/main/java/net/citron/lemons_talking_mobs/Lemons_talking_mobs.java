package net.citron.lemons_talking_mobs;

import com.mojang.logging.LogUtils;
import net.citron.lemons_talking_mobs.client.gui.MobSoundScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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


    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        LOGGER.info("[TalkingMobs] Register payloads...");

        final PayloadRegistrar registrar = event.registrar(MODID).versioned("1");

        // Saving sound (Client -> Server)
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

        // Request for existing record when GUI is open (Client -> Server)
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

        // Send record to open GUI (Server -> Client)
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

        //  Start Recording (Client -> Server)

        registrar.playToServer(
                StartRecordingPayload.TYPE,
                StartRecordingPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        TalkingMobsVoicePlugin.startRecordingFor(context.player().getUUID());
                    });
                }
        );

        // Stop recording (Client -> Server), server sends result back
        registrar.playToServer(
                StopRecordingPayload.TYPE,
                StopRecordingPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        byte[] result = TalkingMobsVoicePlugin.stopRecordingFor(context.player().getUUID());

                        if (result != null && context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            serverPlayer.connection.send(new RecordingResultPayload(result));
                        }
                    });
                }
        );

        //  Send result of a recording back to client (Server -> Client)
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

        //  Welcoming toast (Server -> Client)
        registrar.playToClient(
                ShowWelcomeToastPayload.TYPE,
                ShowWelcomeToastPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.citron.lemons_talking_mobs.client.WelcomeToastClientHandler.showToast();
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
