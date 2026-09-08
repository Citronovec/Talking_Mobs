package net.citron.lemons_talking_mobs.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ServerSoundStorage {

    // Mx sound cache on RAM
    private static final int MAX_CACHE_SIZE = 250;

    //  LRU cache ( when more than ram can handle, send old recorded sound from RAM)
    private static final Map<String, byte[]> MEMORY_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, byte[]>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private static Path storageDir;


    //Initialization file in saves/world/talking_mobs
    //called when server is started in ServerStartingEvent
    public static void init(MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        storageDir = worldDir.resolve("talking_mobs");

        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            MEMORY_CACHE.clear();
            System.out.println("[TalkingMobs] Saving file initialize in: " + storageDir);
        } catch (IOException e) {
            System.err.println("[TalkingMobs] Failed to initialize saving file: " + e.getMessage());
        }
    }


    //saving sound base on global/local mode
    public static void saveSound(UUID entityUuid, String entityTypeKey, String soundKey, byte[] opusData, boolean isGlobal) {
        String key = resolveStorageKey(entityUuid, entityTypeKey, soundKey, isGlobal);
        MEMORY_CACHE.put(key, opusData);

        if (storageDir != null) {
            new Thread(() -> {
                Path file = storageDir.resolve(key + ".opus");
                try {
                    // CREATE + TRUNCATE_EXISTING zajistí vytvoření nebo čisté přepsání stávajícího souboru
                    Files.write(file, opusData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                    System.out.println("[TalkingMobs] Sound successfully saved: " + file.getFileName());
                } catch (IOException e) {
                    System.err.println("[TalkingMobs] Error in saving sound: " + file.getFileName());
                    e.printStackTrace();
                }
            }, "TalkingMobs-SaveThread").start();
        }
    }

    //deleting record option

    public static void deleteSound(UUID entityUuid, String entityTypeKey, String soundKey, boolean isGlobal) {
        String key = resolveStorageKey(entityUuid, entityTypeKey, soundKey, isGlobal);
        MEMORY_CACHE.remove(key);

        if (storageDir != null) {
            Path file = storageDir.resolve(key + ".opus");
            try {
                boolean deleted = Files.deleteIfExists(file);
                if (deleted) {
                    System.out.println("[TalkingMobs] Sound successfully deleted: " + file.getFileName());
                } else {
                    System.out.println("[TalkingMobs] No file to delete (already absent): " + file.getFileName());
                }
            } catch (IOException e) {
                System.err.println("[TalkingMobs] Error deleting sound: " + file.getFileName());
                e.printStackTrace();
            }
        }
    }


    //get sound for specific selected mode (global/local) (used when GUI is open)
    public static byte[] getExactSound(UUID entityUuid, String entityTypeKey, String soundKey, boolean isGlobal) {
        String key = resolveStorageKey(entityUuid, entityTypeKey, soundKey, isGlobal);
        return fetchSoundData(key);
    }

    /**
     * get sound for playing it in game (example: entity is hurt):
     * 1. Priority: Local recording of a specific mob (UUID)
     * 2. Priority: Global recording of a mob specie (EntityType)
     * 3. If no recording found call back null (play vanilla sound)
     */

    public static byte[] getEffectiveSound(UUID entityUuid, String entityTypeKey, String soundKey) {
        // Local recording of a specific mob (UUID)
        byte[] localSound = getExactSound(entityUuid, entityTypeKey, soundKey, false);
        if (localSound != null && localSound.length > 0) {
            return localSound;
        }

        // Global recording of a mob specie (EntityType)
        return getExactSound(entityUuid, entityTypeKey, soundKey, true);
    }


    //Lazy-loading: Finding in ram, when is missing, find in disc and save on ram
    private static byte[] fetchSoundData(String storageKey) {
        // finding in RAM
        byte[] cached = MEMORY_CACHE.get(storageKey);
        if (cached != null) {
            return cached;
        }

        // finding on disc
        if (storageDir != null) {
            Path file = storageDir.resolve(storageKey + ".opus");
            if (Files.exists(file)) {
                try {
                    byte[] data = Files.readAllBytes(file);
                    MEMORY_CACHE.put(storageKey, data);
                    return data;
                } catch (IOException e) {
                    System.err.println("[TalkingMobs] File cannot be read: " + file.getFileName());
                }
            }
        }

        return null;
    }


    //build normal file name
    public static String resolveStorageKey(UUID entityUuid, String entityTypeKey, String soundKey, boolean isGlobal) {
        String cleanSound = soundKey.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        if (isGlobal) {
            String cleanType = entityTypeKey.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
            return cleanType + "_" + cleanSound;
        } else {
            return entityUuid.toString() + "_" + cleanSound;
        }
    }
}