package net.citron.lemons_talking_mobs;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatConnection; // NEW: needed to identify which player sent a mic packet
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap; // NEW: thread-safe per-player recording map

@ForgeVoicechatPlugin
public class TalkingMobsVoicePlugin implements VoicechatPlugin {

    private static VoicechatApi api;
    private static VoicechatServerApi serverApi;
    private static VoicechatClientApi clientApi;

    // CHANGED: removed the single global `isRecording` / `dataOut` / `recordedStream` static fields.
    // Root cause of the bug: MicrophonePacketEvent only ever fires on the SERVER
    // (it's a ServerEvent in the Simple Voice Chat API). Your old code set the
    // recording flag from CLIENT-side GUI code, so on a real client/server split
    // (LAN opened to others, or a dedicated server) the client and server are two
    // separate JVMs with two separate copies of these statics — the server-side
    // event handler never saw the client's flag change, so nothing was ever recorded.
    // It only appeared to work in plain singleplayer because client + integrated
    // server share the same JVM there.
    //
    // Fix: recording state now lives ONLY server-side, keyed by player UUID, so it's
    // per-player and correct regardless of client/server topology. The client no
    // longer touches these fields at all — see StartRecordingPayload / StopRecordingPayload.
    private static final Map<UUID, ByteArrayOutputStream> ACTIVE_RECORDINGS = new ConcurrentHashMap<>();

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

    // CHANGED: this now runs purely server-side (as it always effectively did) and:
    //   1. Identifies WHICH player sent the packet via event.getSenderConnection().
    //   2. Only buffers audio for players who are actually in ACTIVE_RECORDINGS.
    // Your old version had no sender check at all, so on a real server with more
    // than one person online, whoever happened to be talking would get recorded
    // instead of specifically the player who pressed your mod's Record button.
    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return; // packet wasn't sent by a real player connection

        UUID playerId = sender.getPlayer().getUuid();
        ByteArrayOutputStream stream = ACTIVE_RECORDINGS.get(playerId);
        if (stream == null) return; // this player isn't currently recording with our mod

        synchronized (stream) {
            try {
                byte[] opusFrame = event.getPacket().getOpusEncodedData();
                DataOutputStream dataOut = new DataOutputStream(stream);
                dataOut.writeShort(opusFrame.length);
                dataOut.write(opusFrame);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static float currentMicLevel = 0.0F;

    public static float getCurrentMicLevel() {
        return currentMicLevel;
    }

    // NEW: replaces the old client-callable `startRecording()`.
    // This must only ever be called from a SERVER-side context — i.e. from the
    // StartRecordingPayload handler in Lemons_talking_mobs, using the UUID of the
    // player who sent the packet. Never call this from client code.
    public static void startRecordingFor(UUID playerId) {
        ACTIVE_RECORDINGS.put(playerId, new ByteArrayOutputStream());
    }

    // NEW: replaces the old client-callable `stopRecording()`.
    // Also server-side only. Returns the finished Opus byte stream for that player,
    // which the payload handler then sends back down to their client.
    public static byte[] stopRecordingFor(UUID playerId) {
        ByteArrayOutputStream stream = ACTIVE_RECORDINGS.remove(playerId);
        if (stream == null) return new byte[0];
        synchronized (stream) {
            return stream.toByteArray();
        }
    }

    // Unchanged — this already only ran client-side and was fine.
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