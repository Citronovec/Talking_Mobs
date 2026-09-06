package net.citron.lemons_talking_mobs.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class DynamicSoundRegistry {

    // Map: "ambient" -> SoundEvent
    public static Map<String, SoundEvent> getSoundsForEntity(LivingEntity entity) {
        Map<String, SoundEvent> entitySounds = new LinkedHashMap<>();

        // Mob identificator get
        Identifier entityId = EntityType.getKey(entity.getType());
        String entityName = entityId.getPath(); // call back only entity name

        // finding prefix sounds in vanilla game
        String expectedPrefix = "entity." + entityName + ".";

        // go trought all sounds in minecraft (sounds.json)
        for (SoundEvent soundEvent : BuiltInRegistries.SOUND_EVENT) {
            String soundPath = soundEvent.location().getPath();

            if (soundPath.startsWith(expectedPrefix)) {
                // prefix cut of ("entity.villager.ambient" -> "ambient")
                String cleanName = soundPath.substring(expectedPrefix.length());

                // dots and underlines replace by space
                cleanName = cleanName.replace('.', ' ');

                entitySounds.put(cleanName, soundEvent);
            }
        }

        // Fallback: if mob doesn't have normal prefix
        if (entitySounds.isEmpty()) {
            // try to find normal "entity.<mob>.ambient"
            BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.parse(expectedPrefix + "ambient"))
                    .ifPresent(sound -> entitySounds.put("ambient", sound));

            // fallback if not found
            if (entitySounds.isEmpty()) {
                entitySounds.put("generic hurt", SoundEvents.GENERIC_HURT);
            }
        }

        return entitySounds;
    }
}