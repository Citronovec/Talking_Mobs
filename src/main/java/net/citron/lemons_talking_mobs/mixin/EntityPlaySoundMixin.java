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

        // Kontrola, aby kód běžel výhradně na serveru
        if (entity.level().isClientSide()) return;

        // Typ entity (např. "creeper", "cow", "warden")
        String entityTypeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

        // Klíč zvuku (např. "entity.creeper.death" -> "death" nebo celý path)
        String soundKey = sound.location().getPath();
        //String soundKey = sound.getLocation().getPath();

        // 1. Zkusíme přímou shodu se soundKey (např. "entity.creeper.death")
        byte[] data = ServerSoundStorage.getEffectiveSound(entity.getUUID(), entityTypeKey, soundKey);

        // 2. Pokud nic nenajde, zkusíme zjednodušený suffix (např. z "entity.creeper.death" vezmeme jen "death")
        if (data == null && soundKey.contains(".")) {
            String simplifiedKey = soundKey.substring(soundKey.lastIndexOf('.') + 1);
            data = ServerSoundStorage.getEffectiveSound(entity.getUUID(), entityTypeKey, simplifiedKey);
        }

        // 3. Pokud zvuk existuje: odehrajeme náš zvuk a ZRUŠÍME vanilla zvuk
        if (data != null && data.length > 0) {
            MobVoicePlayer.playCustomMobSound(entity, data);
            ci.cancel(); // Stopne vanilla přehrání
        }
    }
}