package net.citron.lemons_talking_mobs.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class SoundListWidget extends ObjectSelectionList<SoundListWidget.SoundEntry> {

    private final Consumer<String> onSelect;

    public SoundListWidget(Minecraft mc, int width, int height, int y, int itemHeight,
                           List<String> sounds, Consumer<String> onSelect) {
        super(mc, width, height, y, itemHeight);
        this.onSelect = onSelect;


        if (sounds.isEmpty()) {
            this.addEntry(new SoundEntry("ambient"));
        } else {
            for (String sound : sounds) {
                this.addEntry(new SoundEntry(sound));
            }
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }


    @Override
    public void setSelected(SoundEntry entry) {
        super.setSelected(entry);
        if (entry != null && this.onSelect != null) {
            this.onSelect.accept(entry.soundKey);
        }
    }

    public class SoundEntry extends ObjectSelectionList.Entry<SoundEntry> {
        public final String soundKey;

        public SoundEntry(String soundKey) {
            this.soundKey = soundKey;
        }

        @Override
        public Component getNarration() {
            return Component.literal(soundKey);
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            boolean isSelected = getSelected() == this;
            int textColor = isSelected ? 0xFFFF55 : (isHovered ? 0xFFFFA0 : 0xFFFFFF);

            // Tex compiler
            String display = Minecraft.getInstance().font.plainSubstrByWidth(soundKey.toUpperCase(), getRowWidth() - 8);

            // X = 0 middle of the slot
            // drawCenteredString set text into middle of the slot
            guiGraphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    display,
                    0,
                    4,
                    textColor
            );
        }
    }
}