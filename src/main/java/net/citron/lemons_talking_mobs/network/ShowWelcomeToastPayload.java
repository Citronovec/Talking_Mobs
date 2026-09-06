package net.citron.lemons_talking_mobs.network;

import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShowWelcomeToastPayload() implements CustomPacketPayload {

    public static final Type<ShowWelcomeToastPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Lemons_talking_mobs.MODID, "show_welcome_toast"));

    public static final StreamCodec<FriendlyByteBuf, ShowWelcomeToastPayload> STREAM_CODEC =
            StreamCodec.unit(new ShowWelcomeToastPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
