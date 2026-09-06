package net.citron.lemons_talking_mobs.network;


import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StopRecordingPayload() implements CustomPacketPayload {

    public static final Type<StopRecordingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Lemons_talking_mobs.MODID, "stop_recording"));

    public static final StreamCodec<FriendlyByteBuf, StopRecordingPayload> STREAM_CODEC =
            StreamCodec.unit(new StopRecordingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}