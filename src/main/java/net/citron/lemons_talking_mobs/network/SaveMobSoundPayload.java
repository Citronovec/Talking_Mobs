package net.citron.lemons_talking_mobs.network;

import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record SaveMobSoundPayload(UUID entityUuid, String entityTypeKey, String soundKey, byte[] audioData, boolean isGlobal) implements CustomPacketPayload {

    public static final Type<SaveMobSoundPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Lemons_talking_mobs.MODID, "save_mob_sound"));

    public static final StreamCodec<FriendlyByteBuf, SaveMobSoundPayload> STREAM_CODEC = StreamCodec.ofMember(
            SaveMobSoundPayload::write,
            SaveMobSoundPayload::new
    );

    public SaveMobSoundPayload(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readByteArray(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.entityUuid);
        buf.writeUtf(this.entityTypeKey);
        buf.writeUtf(this.soundKey);
        buf.writeByteArray(this.audioData);
        buf.writeBoolean(this.isGlobal);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}