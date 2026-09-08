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
import java.util.Comparator; // NEW: needed to sort candidates by distance
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

        // CHANGED: shrunk from a 1.5-block half-extent down to 0.3. This was the
        // actual bug — a box that wide happily matched ANY nearby living entity,
        // not just the one that actually made this sound. In a packed fish tank,
        // an unrelated fish's own sound firing within 1.5 blocks of a nearby sheep
        // was enough for this code to mistake the SHEEP for the sound's source and
        // play the sheep's custom recording instead, even though nothing happened
        // to the sheep. A sound genuinely tied to a specific entity (like shearing)
        // fires essentially AT that entity's own exact position, so a much tighter
        // box still reliably catches the real source while excluding anything just
        // standing or swimming nearby.
        AABB searchBox = new AABB(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> !(e instanceof Player)
        );

        // NEW: if more than one candidate still falls inside even this tight box
        // (rare — e.g. two fish schooling right on top of each other), prefer
        // whichever is physically closest to the sound's exact origin, rather than
        // just taking whatever order the game happened to return the list in.
        nearbyEntities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z)));

        for (LivingEntity entity : nearbyEntities) {
            if (checkAndPlayCustomSound(entity, soundPath)) {
                ci.cancel();
                return;
            }
        }
    }

    // Unchanged — this one already receives the actual target entity directly
    // from the game, so there was never any ambiguity here to begin with.
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