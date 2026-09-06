package net.citron.lemons_talking_mobs.network;


import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StartRecordingPayload() implements CustomPacketPayload {

    public static final Type<StartRecordingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Lemons_talking_mobs.MODID, "start_recording"));

    // No fields to send — StreamCodec.unit() always encodes/decodes to this one instance.
    public static final StreamCodec<FriendlyByteBuf, StartRecordingPayload> STREAM_CODEC =
            StreamCodec.unit(new StartRecordingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}