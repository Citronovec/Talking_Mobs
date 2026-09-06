package net.citron.lemons_talking_mobs.client.gui;

import net.citron.lemons_talking_mobs.Lemons_talking_mobs;
import net.citron.lemons_talking_mobs.TalkingMobsVoicePlugin;
import net.citron.lemons_talking_mobs.client.DynamicSoundRegistry;
import net.citron.lemons_talking_mobs.client.MicLevelTracker;
import net.citron.lemons_talking_mobs.network.RequestMobSoundPayload;
import net.citron.lemons_talking_mobs.network.SaveMobSoundPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.citron.lemons_talking_mobs.network.StartRecordingPayload;
import net.citron.lemons_talking_mobs.network.StopRecordingPayload;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MobSoundScreen extends Screen {

    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(
            Lemons_talking_mobs.MODID, "textures/gui/mob_speeker_gui.png"
    );

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 210;
    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 512;

    private static final int VISIBLE_SLOTS = 8;
    private static final int SLOT_HEIGHT = 23;

    private final LivingEntity targetEntity;
    private Map<String, SoundEvent> availableSounds;
    private List<String> soundKeys = new ArrayList<>();
    private String selectedSound = "";

    private int leftPos;
    private int topPos;
    private int scrollOffset = 0;

    private boolean isGlobal = true;
    private boolean isRecording = false;
    private byte[] recordedAudioBuffer = null;

    private static final int MAX_RECORDING_SECONDS_DISPLAY = 10;

    private long recordingStartTimeMillis = 0L;
    private static final int MAX_RECORDING_MS = 10_000;

    private final List<Button> soundSlotButtons = new ArrayList<>();
    private Button saveButton;

    public MobSoundScreen(LivingEntity entity) {
        super(Component.translatable("gui.lemons_talking_mobs.title", entity.getName()));
        this.targetEntity = entity;
    }

    @Override
    protected void init() {
        super.init();

        MicLevelTracker.start();

        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        this.availableSounds = DynamicSoundRegistry.getSoundsForEntity(this.targetEntity);
        this.soundKeys = new ArrayList<>(this.availableSounds.keySet());

        if (!soundKeys.isEmpty() && this.selectedSound.isEmpty()) {
            this.selectedSound = soundKeys.get(0);
        }

        // 1 Sounds left panel (8 slots)
        soundSlotButtons.clear();
        int listStartX = this.leftPos + 8;
        int listStartY = this.topPos + 10;

        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            final int slotIndex = i;
            int y = listStartY + (i * SLOT_HEIGHT);

            Button btn = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
                int dataIndex = scrollOffset + slotIndex;
                if (dataIndex < soundKeys.size()) {
                    selectSound(soundKeys.get(dataIndex));
                }
            }).bounds(listStartX, y, 98, 20).build());

            soundSlotButtons.add(btn);
        }
        updateSoundButtons();

        // 2 Icons buttons
        // Local/ Global button
        this.addRenderableWidget(Button.builder(Component.empty(), b -> {
                    this.isGlobal = !this.isGlobal;
                    this.recordedAudioBuffer = null;
                    requestCurrentSoundFromServer();
                    b.setTooltip(Tooltip.create(Component.literal(this.isGlobal ? "Global mode - all mobs of same species" : "Global mod off - only this mob")));
                }).bounds(this.leftPos + 196, this.topPos + 8, 20, 19)
                .tooltip(Tooltip.create(Component.literal(this.isGlobal ? "Global mode - all mobs of same species" : "Global mod of - only this mob")))
                .build()).setAlpha(0.0F);

        int actionY = this.topPos + 143;

        // Record / Stop
        this.addRenderableWidget(Button.builder(Component.empty(), b -> {
                    toggleRecording();
                    b.setTooltip(Tooltip.create(Component.literal(this.isRecording ? "Stop recording" : "Start recording")));
                }).bounds(this.leftPos + 120, actionY, 16, 16)
                .tooltip(Tooltip.create(Component.literal("Start recording")))
                .build()).setAlpha(0.0F);

        // Play Preview
        this.addRenderableWidget(Button.builder(Component.empty(), b -> playPreview())
                .bounds(this.leftPos + 142, actionY, 16, 16)
                .tooltip(Tooltip.create(Component.literal("Play preview")))
                .build()).setAlpha(0.0F);

        // Reset / Delete
        this.addRenderableWidget(Button.builder(Component.empty(), b -> this.recordedAudioBuffer = null)
                .bounds(this.leftPos + 199, actionY, 16, 16)
                .tooltip(Tooltip.create(Component.literal("Delete record")))
                .build()).setAlpha(0.0F);

        // 3 Save Button
        this.saveButton = this.addRenderableWidget(Button.builder(
                Component.literal("Save"),
                b -> saveAndSendToServer()
        ).bounds(this.leftPos + 120, this.topPos + 172, 95, 22).build());

        requestCurrentSoundFromServer();
    }

    private void selectSound(String newSoundKey) {
        if (this.selectedSound.equalsIgnoreCase(newSoundKey)) return;
        this.selectedSound = newSoundKey;
        this.recordedAudioBuffer = null;
        updateSoundButtons();
        requestCurrentSoundFromServer();
    }

    private void updateSoundButtons() {
        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            Button btn = soundSlotButtons.get(i);
            int dataIndex = scrollOffset + i;

            if (dataIndex < soundKeys.size()) {
                String key = soundKeys.get(dataIndex);
                btn.visible = true;

                String shortKey = key.length() > 10 ? key.substring(0, 9) + "…" : key;
                if (key.equals(this.selectedSound)) {
                    btn.setMessage(Component.literal("§e> " + shortKey));
                } else {
                    btn.setMessage(Component.literal(shortKey));
                }
            } else {
                btn.visible = false;
            }
        }
    }

    private void requestCurrentSoundFromServer() {
        if (!this.selectedSound.isEmpty() && this.minecraft != null && this.minecraft.getConnection() != null) {
            String entityTypeKey = BuiltInRegistries.ENTITY_TYPE.getKey(this.targetEntity.getType()).getPath();
            this.minecraft.getConnection().send(new RequestMobSoundPayload(
                    this.targetEntity.getUUID(),
                    entityTypeKey,
                    this.selectedSound,
                    this.isGlobal
            ));
        }
    }

    public void onSoundSynced(String soundKey, byte[] audioData) {
        if (this.selectedSound.equalsIgnoreCase(soundKey)) {
            this.recordedAudioBuffer = (audioData != null && audioData.length > 0) ? audioData : null;
        }
    }


    private void toggleRecording() {
        this.isRecording = !this.isRecording;
        if (this.minecraft == null || this.minecraft.getConnection() == null) return;

        if (this.isRecording) {
            this.recordingStartTimeMillis = System.currentTimeMillis(); // NEW: mark the start for the timer
            this.minecraft.getConnection().send(new StartRecordingPayload());
        } else {
            this.minecraft.getConnection().send(new StopRecordingPayload());
        }
    }

    public void onRecordingResult(byte[] audioData) {
        this.recordedAudioBuffer = (audioData != null && audioData.length > 0) ? audioData : null;
        this.isRecording = false;
    }




    private void playPreview() {
        if (this.recordedAudioBuffer != null && this.recordedAudioBuffer.length > 0) {
            TalkingMobsVoicePlugin.playRecordedPreview(this.recordedAudioBuffer);
            return;
        }

        if (this.availableSounds != null && this.availableSounds.containsKey(this.selectedSound)) {
            SoundEvent event = this.availableSounds.get(this.selectedSound);
            if (event != null && this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F, 1.0F));
            }
        }
    }

    private void saveAndSendToServer() {
        if (this.recordedAudioBuffer != null && this.recordedAudioBuffer.length > 0) {
            if (this.minecraft != null && this.minecraft.getConnection() != null) {
                String entityTypeKey = BuiltInRegistries.ENTITY_TYPE.getKey(this.targetEntity.getType()).getPath();
                this.minecraft.getConnection().send(new SaveMobSoundPayload(
                        this.targetEntity.getUUID(),
                        entityTypeKey,
                        this.selectedSound,
                        this.recordedAudioBuffer,
                        this.isGlobal
                ));
            }
        }
        this.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScrollOffset = Math.max(0, soundKeys.size() - VISIBLE_SLOTS);
        if (maxScrollOffset > 0) {
            if (scrollY > 0 && scrollOffset > 0) {
                scrollOffset--;
                updateSoundButtons();
                return true;
            } else if (scrollY < 0 && scrollOffset < maxScrollOffset) {
                scrollOffset++;
                updateSoundButtons();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1 GUI Backround
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, GUI_WIDTH, GUI_HEIGHT, TEX_WIDTH, TEX_HEIGHT);


        // rising recording timer, drawn near the record button.
        if (this.isRecording) {
            long elapsedMs = System.currentTimeMillis() - this.recordingStartTimeMillis;

            // Client-side watchdog: stop right at 10s instead of waiting for the
            // server's own cutoff notification to arrive. The server enforces the
            // real limit on its own regardless — this just makes the UI feel instant.
            if (elapsedMs >= MAX_RECORDING_MS) {
                this.isRecording = false;
                if (this.minecraft != null && this.minecraft.getConnection() != null) {
                    this.minecraft.getConnection().send(new StopRecordingPayload());
                }
                elapsedMs = MAX_RECORDING_MS;
            }

            float progress = Mth.clamp(elapsedMs / (float) MAX_RECORDING_MS, 0.0F, 1.0F);
            int barMaxWidth = 96;
            int barWidth = Math.round(barMaxWidth * progress);

            // Reuses the same VU-meter style strip further down in your texture,
            // just as a rising bar instead of a mic-level bar. Turns red past 8s
            // as a heads-up that the cutoff is close.
            int color = progress >= 0.8F ? 0xFFFF5555 : 0xFFFFFFFF;
            if (barWidth > 0) {
                guiGraphics.fill(this.leftPos + 123, this.topPos + 133, this.leftPos + 123 + barWidth, this.topPos + 136, color);
            }

            String label = String.format("%.1fs / %ds", elapsedMs / 1000.0F, MAX_RECORDING_SECONDS_DISPLAY);
            guiGraphics.drawString(this.font, label, this.leftPos + 123, this.topPos + 122, 0xFFFFFF, true);
        }

        // 2 scrollbar
        int totalItems = soundKeys.size();
        int maxScroll = Math.max(0, totalItems - VISIBLE_SLOTS);
        int thumbY = this.topPos + 8;
        if (maxScroll > 0) {
            thumbY += (int) (((float) this.scrollOffset / maxScroll) * (190 - 15));
        }
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, this.leftPos + 106, thumbY, 232.0F, 74.0F, 12, 16, TEX_WIDTH, TEX_HEIGHT);

        // 3 Global/ Local switcher
        int globeX = this.leftPos + 198;
        int globeY = this.topPos + 8;
        boolean hoverGlobe = mouseX >= globeX && mouseX <= globeX + 20 && mouseY >= globeY && mouseY <= globeY + 20;

        float iconU = hoverGlobe ? 253.0F : 232.0F;
        float iconV = this.isGlobal ? 91.0F : 112.0F;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, globeX, globeY, iconU, iconV, 20, 20, TEX_WIDTH, TEX_HEIGHT);

        // 4 Action buttons (Record / Stop / Play / Reset)
        int actionY = this.topPos + 143;

        // Record / Stop
        int recX = this.leftPos + 120;
        boolean hoverRec = mouseX >= recX && mouseX <= recX + 16 && mouseY >= actionY && mouseY <= actionY + 16;
        float recU = hoverRec ? 249.0F : 232.0F;
        float recV = this.isRecording ? 21.0F : 3.0F;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, recX, actionY, recU, recV, 16, 16, TEX_WIDTH, TEX_HEIGHT);

        // Play Preview
        int playX = this.leftPos + 142;
        boolean hoverPlay = mouseX >= playX && mouseX <= playX + 16 && mouseY >= actionY && mouseY <= actionY + 16;
        float playU = hoverPlay ? 249.0F : 232.0F;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, playX, actionY, playU, 39.0F, 16, 16, TEX_WIDTH, TEX_HEIGHT);

        // Reset / Delete
        int resetX = this.leftPos + 199;
        boolean hoverReset = mouseX >= resetX && mouseX <= resetX + 16 && mouseY >= actionY && mouseY <= actionY + 17;
        float resetU = hoverReset ? 249.0F : 232.0F;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, resetX, actionY, resetU, 56.0F, 16, 16, TEX_WIDTH, TEX_HEIGHT);

        // 5 Color VU meter

        float micLevel = MicLevelTracker.getCurrentLevel();
        int maxVuWidth = 96;
        int activeWidth = Math.round(maxVuWidth * Mth.clamp(micLevel, 0.0F, 1.0F));

        if (activeWidth > 0) {
            // 40% opacity when not recording, 100% opacity when recording
            float alpha = this.isRecording ? 1.0F : 0.4F;



            int alphaBits = (this.isRecording ? 255 : 100) << 24;
            int color = alphaBits | 0x00FFFFFF;


            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE,
                    this.leftPos + 123,
                    this.topPos + 117,
                    3.0F, 212.0F,
                    activeWidth, 15,
                    TEX_WIDTH,
                    TEX_HEIGHT, color);


        }



        // 6 3D entity
        if (this.targetEntity != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    this.leftPos + 125, this.topPos + 16,
                    this.leftPos + 220, this.topPos + 115,
                    32,
                    0.0625f,
                    mouseX, mouseY,
                    this.targetEntity
            );
        }

        // 7 Vanilla widget on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        MicLevelTracker.stop();
        super.onClose();
    }
}