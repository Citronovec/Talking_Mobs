package net.citron.lemons_talking_mobs.network;
/*
import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayCustomMobSoundPayload(double x, double y, double z, byte[] audioData) implements CustomPacketPayload {

    public static final Type<PlayCustomMobSoundPayload> TYPE = new Type<>(Identifier.parse(Lemons_talking_mobs.MODID + ":play_custom_mob_sound"));

    public static final StreamCodec<FriendlyByteBuf, PlayCustomMobSoundPayload> STREAM_CODEC = CustomPacketPayload.codec(
            PlayCustomMobSoundPayload::write,
            PlayCustomMobSoundPayload::new
    );

    public PlayCustomMobSoundPayload(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readByteArray());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeByteArray(this.audioData);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

 */
