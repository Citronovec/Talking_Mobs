package net.citron.lemons_talking_mobs.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class MobSoundScreen extends Screen {

    private final LivingEntity targetEntity;
    private final List<String> soundTypes = List.of("ambient", "hurt", "death", "step");
    private String selectedSound = "ambient";
    private boolean isRecording = false;

    private Button recordButton;
    private Button previewButton;
    private Button saveButton;

    public MobSoundScreen(LivingEntity entity) {
        super(Component.translatable("gui.lemons_talking_mobs.title", entity.getName()));
        this.targetEntity = entity;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 1. Tlačítka pro výběr kategorie zvuku (vlevo)
        int listY = centerY - 50;
        for (String soundKey : soundTypes) {
            this.addRenderableWidget(Button.builder(
                    Component.literal(soundKey.toUpperCase()),
                    btn -> this.selectedSound = soundKey
            ).bounds(centerX - 170, listY, 80, 20).build());
            listY += 24;
        }

        // 2. Akční tlačítka (pod náhledem entity a vpravo)
        recordButton = this.addRenderableWidget(Button.builder(
                Component.literal("Record"),
                btn -> toggleRecording()
        ).bounds(centerX + 60, centerY - 30, 90, 20).build());

        previewButton = this.addRenderableWidget(Button.builder(
                Component.literal("Preview"),
                btn -> playPreview()
        ).bounds(centerX + 60, centerY - 5, 90, 20).build());

        saveButton = this.addRenderableWidget(Button.builder(
                Component.literal("Save & Apply"),
                btn -> saveAndSendToServer()
        ).bounds(centerX + 60, centerY + 20, 90, 20).build());

        // Tlačítko pro zavření
        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose()
        ).bounds(centerX + 60, centerY + 50, 90, 20).build());
    }

    private void toggleRecording() {
        isRecording = !isRecording;
        if (isRecording) {
            recordButton.setMessage(Component.literal("§c● STOP"));
            // TODO: Spustit AudioRecorder / Voice Chat nahrávání
        } else {
            recordButton.setMessage(Component.literal("Record"));
            // TODO: Zastavit nahrávání a uložit do dočasného bufferu
        }
    }

    private void playPreview() {
        // TODO: Přehrát do sluchátek nahraný buffer
    }

    private void saveAndSendToServer() {
        // TODO: Poslat packet se zvukem na server pro targetEntity.getId()
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Tmavé poloprůhledné pozadí
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Nadpis
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 80, 0xFFFFFF);

        // Informace o vybraném zvuku
        guiGraphics.drawString(this.font, "Typ: §e" + selectedSound, centerX + 60, centerY - 50, 0xAAAAAA);

        // 3. Vykreslení živé 3D entity uprostřed obrazovky
        if (targetEntity != null) {
            InventoryScreen.renderEntityInInventoryFollowsAngle(
                    guiGraphics,
                    centerX - 10, centerY - 40, centerX + 30, centerY + 40,
                    30, // Měřítko (velikost moba)
                    0.0625f,
                    mouseX, mouseY,
                    targetEntity
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Hra běží dál na pozadí (důležité pro multiplayer)
    }
}
