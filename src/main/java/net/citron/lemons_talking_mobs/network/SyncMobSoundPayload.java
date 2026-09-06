package net.citron.lemons_talking_mobs.network;

import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record SyncMobSoundPayload(UUID entityUuid, String soundKey, byte[] audioData) implements CustomPacketPayload {
    public static final Type<SyncMobSoundPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Lemons_talking_mobs.MODID, "sync_mob_sound"));

    public static final StreamCodec<FriendlyByteBuf, SyncMobSoundPayload> STREAM_CODEC = StreamCodec.ofMember(
            SyncMobSoundPayload::write,
            SyncMobSoundPayload::new
    );

    public SyncMobSoundPayload(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(), buf.readByteArray());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.entityUuid);
        buf.writeUtf(this.soundKey);
        buf.writeByteArray(this.audioData);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}