package net.citron.lemons_talking_mobs;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.citron.lemons_talking_mobs.network.RecordingResultPayload; // NEW: needed to push the auto-stop result to the client
import net.minecraft.server.level.ServerPlayer;                       // NEW
import net.neoforged.neoforge.server.ServerLifecycleHooks;            // NEW: lets us find the ServerPlayer from just a UUID

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class TalkingMobsVoicePlugin implements VoicechatPlugin {

    private static VoicechatApi api;
    private static VoicechatServerApi serverApi;
    private static VoicechatClientApi clientApi;


    private static final int MAX_RECORDING_SECONDS = 10;
    private static final int MAX_RECORDING_FRAMES = (MAX_RECORDING_SECONDS * 1000) / 20;


    private static final Map<UUID, RecordingSession> ACTIVE_RECORDINGS = new ConcurrentHashMap<>();

    //  stream plus a running frame count
    private static class RecordingSession {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int frameCount = 0;
    }

    @Override
    public String getPluginId() {
        return Lemons_talking_mobs.MODID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        TalkingMobsVoicePlugin.api = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(ClientVoicechatInitializationEvent.class, this::onClientInit);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
    }

    private void onClientInit(ClientVoicechatInitializationEvent event) {
        clientApi = event.getVoicechat();
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;

        UUID playerId = sender.getPlayer().getUuid();
        RecordingSession session = ACTIVE_RECORDINGS.get(playerId);
        if (session == null) return; // this player isn't currently recording with our mod

        synchronized (session) {

            if (!ACTIVE_RECORDINGS.containsKey(playerId)) return;

            try {
                byte[] opusFrame = event.getPacket().getOpusEncodedData();
                DataOutputStream dataOut = new DataOutputStream(session.stream);
                dataOut.writeShort(opusFrame.length);
                dataOut.write(opusFrame);
                session.frameCount++;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // hit the 10-second cap — finalize and push the result to the
            // client automatically, exactly like a manual stop would.
            if (session.frameCount >= MAX_RECORDING_FRAMES) {
                ACTIVE_RECORDINGS.remove(playerId);
                byte[] finishedAudio = session.stream.toByteArray();
                sendResultToPlayer(playerId, finishedAudio);
            }
        }
    }

    // looks up the actual ServerPlayer from just a UUID (we only have the
    // UUID inside onMicrophonePacket, not a full player/connection reference)
    // and sends them the finished recording, same payload used for manual stop.
    private static void sendResultToPlayer(UUID playerId, byte[] audioData) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.connection.send(new RecordingResultPayload(audioData));
        }
    }

    private static float currentMicLevel = 0.0F;

    public static float getCurrentMicLevel() {
        return currentMicLevel;
    }

    public static void startRecordingFor(UUID playerId) {
        ACTIVE_RECORDINGS.put(playerId, new RecordingSession()); // CHANGED: new RecordingSession() instead of new ByteArrayOutputStream()
    }


    public static byte[] stopRecordingFor(UUID playerId) {
        RecordingSession session = ACTIVE_RECORDINGS.remove(playerId);
        if (session == null) return null;
        synchronized (session) {
            return session.stream.toByteArray();
        }
    }

    public static void playRecordedPreview(byte[] recordedOpusData) {
        if (recordedOpusData == null || recordedOpusData.length == 0 || clientApi == null) return;

        ClientStaticAudioChannel channel = clientApi.createStaticAudioChannel(UUID.randomUUID());
        if (channel == null) return;

        new Thread(() -> {
            OpusDecoder decoder = api.createDecoder();

            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(recordedOpusData))) {
                while (in.available() > 0) {
                    short length = in.readShort();
                    byte[] opusFrame = new byte[length];
                    in.readFully(opusFrame);

                    short[] rawAudio = decoder.decode(opusFrame);
                    channel.play(rawAudio);

                    Thread.sleep(20);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (decoder != null) {
                    decoder.close();
                }
            }
        }, "MobSound-OpusPreview-Thread").start();
    }

    public static VoicechatServerApi getServerApi() {
        return serverApi;
    }

    public static VoicechatClientApi getClientApi() {
        return clientApi;
    }

    public static VoicechatApi getApi() {
        return api;
    }
}