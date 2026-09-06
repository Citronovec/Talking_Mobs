package net.citron.lemons_talking_mobs.server;

import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.citron.lemons_talking_mobs.network.ShowWelcomeToastPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Lemons_talking_mobs.MODID)
public class WelcomeToastListener {


    private static final String SEEN_TOAST_KEY = "seen_welcome_toast";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag modData = persistentData.getCompoundOrEmpty(Lemons_talking_mobs.MODID);

        if (!modData.getBooleanOr(SEEN_TOAST_KEY, false)) {
            modData.putBoolean(SEEN_TOAST_KEY, true);
            persistentData.put(Lemons_talking_mobs.MODID, modData);

            player.connection.send(new ShowWelcomeToastPayload());
        }
    }
}