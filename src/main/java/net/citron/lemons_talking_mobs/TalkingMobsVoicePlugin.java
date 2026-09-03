package net.citron.lemons_talking_mobs;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

@ForgeVoicechatPlugin
public class TalkingMobsVoicePlugin implements VoicechatPlugin {

    private static VoicechatApi api;

    @Override
    public String getPluginId() {
        return Lemons_talking_mobs.MODID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        TalkingMobsVoicePlugin.api = api;
        System.out.println("Voice Chat API uspesne inicializovano pro " + getPluginId());
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // Zde se registrují posluchače událostí
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        // Serverové API pro práci s mikrofonem a kanály entit
    }

    public static VoicechatApi getApi() {
        return api;
    }
}