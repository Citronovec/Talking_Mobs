package net.citron.lemons_talking_mobs.mixin;

import net.citron.lemons_talking_mobs.server.MobVoicePlayer;
import net.citron.lemons_talking_mobs.server.ServerSoundStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPlaySoundMixin {

    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        // only run on server
        if (entity.level().isClientSide()) return;

        // entity type
        String entityTypeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

        // Sound key (např. "entity.creeper.death" -> "death" or full path)
        String soundKey = sound.location().getPath();
        //String soundKey = sound.getLocation().getPath();

        // try compare data with soundKey
        byte[] data = ServerSoundStorage.getEffectiveSound(entity.getUUID(), entityTypeKey, soundKey);

        // if not playing try simple suffix ("entity.creeper.death" -> "death")
        if (data == null && soundKey.contains(".")) {
            String simplifiedKey = soundKey.substring(soundKey.lastIndexOf('.') + 1);
            data = ServerSoundStorage.getEffectiveSound(entity.getUUID(), entityTypeKey, simplifiedKey);
        }

        // if sound exist play recordet sound and cancel vanilla sound
        if (data != null && data.length > 0) {
            MobVoicePlayer.playCustomMobSound(entity, data);
            ci.cancel(); // Stopne vanilla přehrání
        }
    }
}