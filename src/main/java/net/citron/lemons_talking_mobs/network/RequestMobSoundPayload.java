package net.citron.lemons_talking_mobs.network;

import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RequestMobSoundPayload(UUID entityUuid, String entityTypeKey, String soundKey, boolean isGlobal) implements CustomPacketPayload {

    public static final Type<RequestMobSoundPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Lemons_talking_mobs.MODID, "request_mob_sound"));

    public static final StreamCodec<FriendlyByteBuf, RequestMobSoundPayload> STREAM_CODEC = StreamCodec.ofMember(
            RequestMobSoundPayload::write,
            RequestMobSoundPayload::new
    );

    public RequestMobSoundPayload(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.entityUuid);
        buf.writeUtf(this.entityTypeKey);
        buf.writeUtf(this.soundKey);
        buf.writeBoolean(this.isGlobal);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}