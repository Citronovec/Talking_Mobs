package net.citron.lemons_talking_mobs.client;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class WelcomeToastClientHandler {

    public static void showToast() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        SystemToast.add(
                mc.getToastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("Talking Mobs"),
                Component.literal("Shift + Right-Click any mob to record a custom sound for it!")
        );
    }
}
