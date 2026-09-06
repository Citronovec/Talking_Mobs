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

    // Maximální počet zvukových souborů držených v RAM současně
    private static final int MAX_CACHE_SIZE = 250;

    // Vláknově bezpečná LRU cache (při překročení kapacity automaticky uvolní nejstarší nepoužitý zvuk z RAM)
    private static final Map<String, byte[]> MEMORY_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, byte[]>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private static Path storageDir;

    /**
     * Inicializace složky ve světě (saves/<world>/talking_mobs/).
     * Volá se při spuštění serveru v ServerStartingEvent.
     */
    public static void init(MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        storageDir = worldDir.resolve("talking_mobs");

        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            MEMORY_CACHE.clear();
            System.out.println("[TalkingMobs] Uloziste inicializovano v: " + storageDir);
        } catch (IOException e) {
            System.err.println("[TalkingMobs] Selhala inicializace slozky pro zvuky: " + e.getMessage());
        }
    }

    /**
     * Uloží zvuk podle zvoleného režimu:
     * - isGlobal = true  -> creeper_death.opus (pro všechny creepery)
     * - isGlobal = false -> <UUID>_death.opus (pouze pro tohoto konkrétního jedince)
     */
    public static void saveSound(UUID entityUuid, String entityTypeKey, String soundKey, byte[] opusData, boolean isGlobal) {
        String key = resolveStorageKey(entityUuid, entityTypeKey, soundKey, isGlobal);
        MEMORY_CACHE.put(key, opusData);

        if (storageDir != null) {
            new Thread(() -> {
                Path file = storageDir.resolve(key + ".opus");
                try {
                    // CREATE + TRUNCATE_EXISTING zajistí vytvoření nebo čisté přepsání stávajícího souboru
                    Files.write(file, opusData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                    System.out.println("[TalkingMobs] Zvuk uspesne zapsan na disk: " + file.getFileName());
                } catch (IOException e) {
                    System.err.println("[TalkingMobs] Chyba pri zapisu na disk: " + file.getFileName());
                    e.printStackTrace();
                }
            }, "TalkingMobs-SaveThread").start();
        }
    }

    /**
     * Získá přesný zvuk pro zadaný režim (používá se při otevření GUI, kdy hráč chce vidět stav daného režimu).
     */
    public static byte[] getExactSound(UUID entityUuid, String entityTypeKey, String soundKey, boolean isGlobal) {
        String key = resolveStorageKey(entityUuid, entityTypeKey, soundKey, isGlobal);
        return fetchSoundData(key);
    }

    /**
     * Získá efektivní zvuk pro přehrání ve hře (např. při zranění/smrti entity):
     * 1. Priorita: Lokální nahrávka konkrétního moba (UUID)
     * 2. Priorita: Globální nahrávka druhu moba (EntityType)
     * 3. Pokud neexistuje ani jedna, vrátí null (a zahraje se vanilla zvuk).
     */
    public static byte[] getEffectiveSound(UUID entityUuid, String entityTypeKey, String soundKey) {
        // 1. Zkusíme specifický zvuk pro daného moba
        byte[] localSound = getExactSound(entityUuid, entityTypeKey, soundKey, false);
        if (localSound != null && localSound.length > 0) {
            return localSound;
        }

        // 2. Fallback na globální zvuk pro celou rasu
        return getExactSound(entityUuid, entityTypeKey, soundKey, true);
    }

    /**
     * Lazy-loading: Nejprve prohledá RAM, pokud chybí, bleskově sáhne na disk a uloží do RAM.
     */
    private static byte[] fetchSoundData(String storageKey) {
        // 1. Kontrola v paměti
        byte[] cached = MEMORY_CACHE.get(storageKey);
        if (cached != null) {
            return cached;
        }

        // 2. Kontrola na disku
        if (storageDir != null) {
            Path file = storageDir.resolve(storageKey + ".opus");
            if (Files.exists(file)) {
                try {
                    byte[] data = Files.readAllBytes(file);
                    MEMORY_CACHE.put(storageKey, data);
                    return data;
                } catch (IOException e) {
                    System.err.println("[TalkingMobs] Nelze precist soubor: " + file.getFileName());
                }
            }
        }

        return null;
    }

    /**
     * Sestaví normalizovaný název souboru bez nepovolených znaků.
     */
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