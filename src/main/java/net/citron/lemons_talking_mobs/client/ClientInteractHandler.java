package net.citron.lemons_talking_mobs.client;

import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.citron.lemons_talking_mobs.client.gui.MobSoundScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Lemons_talking_mobs.MODID, value = Dist.CLIENT)
public class ClientInteractHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // 1. Zpracováváme pouze hlavní ruku, aby se event nespouštěl dvakrát (pro obě ruce)
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();

        // 2. Kontrola, zda hráč drží Shift (plížení) a klikl na živou bytost
        if (player.isShiftKeyDown() && event.getTarget() instanceof LivingEntity livingTarget) {

            // 3. Kontrola, aby hráč neklikal sám na sebe (např. ve 3rd person pohledu)
            if (livingTarget == player) {
                return;
            }

            // 4. Otevření obrazovky na klientovi
            if (event.getLevel().isClientSide()) {
                Minecraft.getInstance().setScreen(new MobSoundScreen(livingTarget));
            }

            // 5. Zrušení vanilla chování (zabrání nasednutí, krmení, otevření cizího GUI)
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}