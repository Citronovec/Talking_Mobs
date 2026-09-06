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
        // only main hand
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();

        //  is sneaking
        if (player.isShiftKeyDown() && event.getTarget() instanceof LivingEntity livingTarget) {


            if (livingTarget == player) {
                return;
            }

            // open GUI
            if (event.getLevel().isClientSide()) {
                Minecraft.getInstance().setScreen(new MobSoundScreen(livingTarget));
            }

            // cancel vanilla events
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}