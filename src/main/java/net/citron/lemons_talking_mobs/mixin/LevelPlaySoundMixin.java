package net.citron.lemons_talking_mobs.mixin;

import net.citron.lemons_talking_mobs.server.MobVoicePlayer;
import net.citron.lemons_talking_mobs.server.ServerSoundStorage;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

@Mixin(ServerLevel.class)
public abstract class LevelPlaySoundMixin {

    @Inject(
            method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPlaySeededSoundAtPos(
            @Nullable Entity exceptEntity,
            double x,
            double y,
            double z,
            Holder<SoundEvent> soundHolder,
            SoundSource source,
            float volume,
            float pitch,
            long seed,
            CallbackInfo ci
    ) {
        ServerLevel level = (ServerLevel) (Object) this;
        SoundEvent sound = soundHolder.value();
        String soundPath = sound.location().getPath();

      
        AABB searchBox = new AABB(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> !(e instanceof Player)
        );

        
        nearbyEntities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z)));

        for (LivingEntity entity : nearbyEntities) {
            if (checkAndPlayCustomSound(entity, soundPath)) {
                ci.cancel();
                return;
            }
        }
    }

    
    @Inject(
            method = "playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPlaySeededSoundOnEntity(
            @Nullable Entity exceptEntity,
            Entity targetEntity,
            Holder<SoundEvent> soundHolder,
            SoundSource source,
            float volume,
            float pitch,
            long seed,
            CallbackInfo ci
    ) {
        if (targetEntity instanceof LivingEntity living && !(targetEntity instanceof Player)) {
            SoundEvent sound = soundHolder.value();
            if (checkAndPlayCustomSound(living, sound.location().getPath())) {
                ci.cancel();
            }
        }
    }

    private static boolean checkAndPlayCustomSound(LivingEntity entity, String soundPath) {
        String entityTypeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

        byte[] data = ServerSoundStorage.getEffectiveSound(entity.getUUID(), entityTypeKey, soundPath);

        if (data == null && soundPath.contains(".")) {
            String simplifiedKey = soundPath.substring(soundPath.lastIndexOf('.') + 1);
            data = ServerSoundStorage.getEffectiveSound(entity.getUUID(), entityTypeKey, simplifiedKey);
        }

        if (data != null && data.length > 0) {
            MobVoicePlayer.playCustomMobSound(entity, data);
            return true;
        }

        return false;
    }
}
