package net.citron.lemons_talking_mobs.network;

// NEW FILE: sent server -> client after StopRecordingPayload is handled.
// Carries the finished Opus byte array back down so MobSoundScreen can populate
// recordedAudioBuffer for preview/save, exactly like the old (broken) client-only
// stopRecording() used to return locally.

import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RecordingResultPayload(byte[] audioData) implements CustomPacketPayload {

    public static final Type<RecordingResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Lemons_talking_mobs.MODID, "recording_result"));

    public static final StreamCodec<FriendlyByteBuf, RecordingResultPayload> STREAM_CODEC =
            StreamCodec.ofMember(RecordingResultPayload::write, RecordingResultPayload::new);

    public RecordingResultPayload(FriendlyByteBuf buf) {
        this(buf.readByteArray());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByteArray(this.audioData);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
