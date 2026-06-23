package net.umf.polyfactory.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Client-side GUI for the Fabricator. Rendered programmatically (no texture atlas): a panel,
 * slot frames, a labelled input/output row with an animated process glyph between them, and an
 * FE energy bar.
 */
public class FabricatorScreen extends AbstractContainerScreen<FabricatorMenu> {

    private static final int SLOT_SIZE = 16;
    private static final int INPUT_X = 56;
    private static final int OUTPUT_X = 104;
    private static final int SLOT_Y = 38;
    private static final int LABEL_Y = 27;

    private static final int ARROW_WIDTH = 22;
    private static final int ARROW_HEIGHT = 14;
    // Centered in the gap between the slots horizontally, and against the slot icons vertically.
    private static final int ARROW_X = (INPUT_X + SLOT_SIZE) + ((OUTPUT_X - (INPUT_X + SLOT_SIZE)) - ARROW_WIDTH) / 2;
    private static final int ARROW_Y = SLOT_Y + (SLOT_SIZE - ARROW_HEIGHT) / 2;

    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 24;
    private static final int ENERGY_WIDTH = 6;
    private static final int ENERGY_HEIGHT = 46;

    public FabricatorScreen(FabricatorMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, 166);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        for (Slot slot : this.menu.slots) {
            renderSlotFrame(graphics, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
        }

        renderSlotLabels(graphics);
        renderProgressArrow(graphics);
        renderEnergyBar(graphics, mouseX, mouseY);

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSlotLabels(GuiGraphicsExtractor graphics) {
        drawCenteredLabel(graphics, "Input", INPUT_X);
        drawCenteredLabel(graphics, "Output", OUTPUT_X);
    }

    private void drawCenteredLabel(GuiGraphicsExtractor graphics, String text, int slotX) {
        int textX = this.leftPos + slotX + 8 - this.font.width(text) / 2;
        int textY = this.topPos + LABEL_Y;
        graphics.text(this.font, text, textX, textY, 0x404040, false);
    }

    private void renderProgressArrow(GuiGraphicsExtractor graphics) {
        int x = this.leftPos + ARROW_X;
        int y = this.topPos + ARROW_Y;

        graphics.fill(x, y, x + ARROW_WIDTH, y + ARROW_HEIGHT, 0xFF2B2B2B);

        float progress = this.menu.getProgress();
        int filled = Mth.ceil(progress * (ARROW_WIDTH - 2));
        if (filled > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + filled, y + ARROW_HEIGHT - 1, 0xFF4CAF50);
        }

        boolean active = progress > 0.0F;
        int glyphColor;
        if (active) {
            boolean pulse = (System.currentTimeMillis() / 300L) % 2L == 0L;
            glyphColor = pulse ? 0xFFFFFFFF : 0xFFB0B0B0;
        } else {
            glyphColor = 0xFF777777;
        }
        int glyphSize = 8;
        drawRightArrowGlyph(graphics, x + ARROW_WIDTH / 2 - glyphSize / 2, y + ARROW_HEIGHT / 2 - glyphSize / 2, glyphSize, glyphColor);
    }

    /** Draws a filled right-pointing triangle (a "process" glyph) inside a {@code size}x{@code size} box. */
    private void drawRightArrowGlyph(GuiGraphicsExtractor g, int x, int y, int size, int color) {
        int half = size / 2;
        for (int row = 0; row <= size; row++) {
            int distFromCenter = Math.abs(row - half);
            int rowWidth = size - distFromCenter * 2;
            if (rowWidth > 0) {
                g.fill(x, y + row, x + rowWidth, y + row + 1, color);
            }
        }
    }

    private void renderEnergyBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = this.leftPos + ENERGY_X;
        int y = this.topPos + ENERGY_Y;
        graphics.fill(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, 0xFF373737);
        int filledHeight = Mth.ceil(this.menu.getEnergyRatio() * (ENERGY_HEIGHT - 2));
        if (filledHeight > 0) {
            graphics.fill(x + 1, y + ENERGY_HEIGHT - 1 - filledHeight, x + ENERGY_WIDTH - 1, y + ENERGY_HEIGHT - 1, 0xFFE06A1A);
        }

        if (mouseX >= x && mouseX < x + ENERGY_WIDTH && mouseY >= y && mouseY < y + ENERGY_HEIGHT) {
            graphics.setTooltipForNextFrame(
                    Component.literal(this.menu.getEnergyStored() + " / " + this.menu.getMaxEnergyStored() + " FE"), mouseX, mouseY);
        }
    }

    private void renderPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        g.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        g.fill(x, y + 1, x + 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + 1, 0xFFC6C6C6);
        g.fill(x, y + h - 1, x + 1, y + h, 0xFFC6C6C6);
    }

    private void renderSlotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y + 1, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 17, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }
}
