package net.citron.lemons_talking_mobs.server;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.citron.lemons_talking_mobs.TalkingMobsVoicePlugin;
import net.minecraft.core.registries.BuiltInRegistries; // NEW: needed to get the entity's species id
import net.minecraft.world.entity.Entity;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Map;                                    // NEW
import java.util.Set;                                     // NEW
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;             // NEW

public class MobVoicePlayer {

    // NEW: caps how many entities of the SAME species can have a custom sound
    // playing at once, keyed by entity type id (e.g. "minecraft:cow"). This is
    // what prevents the OpenAL-source-exhaustion crash when a lot of the same
    // mob (a farm/grinder) all make noise together, without affecting different
    // species making sounds at the same time.
    private static final Map<String, Set<UUID>> ACTIVE_BY_SPECIES = new ConcurrentHashMap<>();

    // NEW: how many concurrent sounds are allowed per species. Start here and
    // tune by ear/log-watching — lower if you still see OpenAL errors with
    // several species going off together, raise if a single farm feels muted.
    private static final int MAX_CONCURRENT_PER_SPECIES = 4;

    public static void playCustomMobSound(Entity entity, byte[] opusData) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        VoicechatServerApi api = TalkingMobsVoicePlugin.getServerApi();
        if (api == null) return;

        // NEW: figure out this entity's species group and try to claim a slot
        // in it before doing any of the (more expensive) channel setup below.
        String speciesKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        Set<UUID> speciesGroup = ACTIVE_BY_SPECIES.computeIfAbsent(speciesKey, k -> ConcurrentHashMap.newKeySet());
        UUID entityId = entity.getUUID();

        synchronized (speciesGroup) {
            if (speciesGroup.size() >= MAX_CONCURRENT_PER_SPECIES) {
                return; // this species is already at its cap — skip this sound entirely
            }
            speciesGroup.add(entityId);
        }

        try {
            ServerLevel level = api.fromServerLevel(serverLevel);
            Position pos = api.createPosition(entity.getX(), entity.getEyeY(), entity.getZ());
            LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), level, pos);
            if (channel == null) {
                speciesGroup.remove(entityId); // NEW: release the slot if we're bailing out early
                return;
            }
            channel.setDistance(24.0f);

            new Thread(() -> {
                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(opusData))) {
                    while (in.available() > 0) {
                        short length = in.readShort();
                        if (length <= 0 || length > 4096) break;

                        byte[] frame = new byte[length];
                        in.readFully(frame);

                        channel.send(frame);
                        Thread.sleep(20);
                    }
                } catch (Exception ignored) {
                } finally {
                    speciesGroup.remove(entityId); // NEW: free this species' slot once playback ends
                }
            }, "TalkingMobs-VoiceSender").start();

        } catch (Exception e) {
            e.printStackTrace();
            speciesGroup.remove(entityId); // NEW: also release the slot if setup throws before the thread even starts
        }
    }
}