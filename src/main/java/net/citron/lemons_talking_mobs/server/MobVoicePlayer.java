package net.citron.lemons_talking_mobs.server;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.citron.lemons_talking_mobs.TalkingMobsVoicePlugin;
import net.minecraft.world.entity.Entity;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class MobVoicePlayer {

    public static void playCustomMobSound(Entity entity, byte[] opusData) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        VoicechatServerApi api = TalkingMobsVoicePlugin.getServerApi();
        if (api == null) return;

        try {
            ServerLevel level = api.fromServerLevel(serverLevel);
            Position pos = api.createPosition(entity.getX(), entity.getEyeY(), entity.getZ());

            LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), level, pos);
            if (channel == null) return;

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
                } catch (Exception ignored) {}
            }, "TalkingMobs-VoiceSender").start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
