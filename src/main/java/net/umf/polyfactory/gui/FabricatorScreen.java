package net.umf.polyfactory.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.umf.polyfactory.PolyFactory;
import net.umf.polyfactory.network.ToggleSplitPacket;

/**
 * Client-side GUI for the Fabricator. Rendered programmatically (no texture atlas): a panel, one
 * colour-coded input/output row per unlocked lane each with a plain arrow that fills in as it
 * processes, a green FE energy bar spanning the lane area on the right, a small stack of upgrade
 * icons (with an "x{level}" label each) on the left of the lane area, and (once a Slot Upgrade is
 * installed) a button to toggle evenly splitting incoming items across every unlocked lane instead
 * of filling lane 0 first.
 */
public class FabricatorScreen extends AbstractContainerScreen<FabricatorMenu> {

    private static final int INPUT_BORDER_COLOR = 0xFFB33A3A;
    private static final int OUTPUT_BORDER_COLOR = 0xFF3A5FB3;

    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 14;
    private static final int ARROW_X = (FabricatorMenu.LANE_INPUT_X + FabricatorMenu.SLOT_SIZE)
            + ((FabricatorMenu.LANE_OUTPUT_X - (FabricatorMenu.LANE_INPUT_X + FabricatorMenu.SLOT_SIZE)) - ARROW_WIDTH) / 2;

    private static final int ENERGY_WIDTH = 6;

    private static final int BLOCKED_COLOR = 0xFFE53935;

    private static final int SPLIT_BUTTON_Y = 6;
    private static final int SPLIT_BUTTON_HEIGHT = 12;

    private static final int UPGRADE_ICON_X = 8;
    private static final float UPGRADE_ICON_SCALE = 0.7F;
    private static final int UPGRADE_ICON_SIZE = Mth.floor(16 * UPGRADE_ICON_SCALE);
    private static final int UPGRADE_ROW_HEIGHT = UPGRADE_ICON_SIZE + 2;

    public FabricatorScreen(FabricatorMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, FabricatorMenu.imageHeight(menu.getActiveLanes()));
        // The default (imageHeight - 94) assumes vanilla's fixed single-row machine GUI size; ours
        // grows with the lane count, so the label position has to be derived the same way the
        // actual inventory slots are, or it ends up sitting on top of the lane rows above it.
        this.inventoryLabelY = FabricatorMenu.inventoryY(menu.getActiveLanes()) - 10;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        int activeLanes = this.menu.getActiveLanes();
        int machineSlotCount = activeLanes * 2;
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            int frameX = this.leftPos + slot.x - 1;
            int frameY = this.topPos + slot.y - 1;
            if (i < machineSlotCount) {
                boolean isInput = i % 2 == 0;
                renderColoredSlotFrame(graphics, frameX, frameY, isInput ? INPUT_BORDER_COLOR : OUTPUT_BORDER_COLOR);
            } else {
                renderSlotFrame(graphics, frameX, frameY);
            }
        }

        for (int lane = 0; lane < activeLanes; lane++) {
            renderProgressArrow(graphics, lane);
        }
        renderEnergyBar(graphics, mouseX, mouseY, activeLanes);
        renderUpgradeIcons(graphics, activeLanes);
        renderSplitToggle(graphics);

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    private int splitButtonWidth() {
        return this.font.width("Split") + 6;
    }

    private int splitButtonX() {
        return this.leftPos + this.imageWidth - 8 - this.splitButtonWidth();
    }

    private void renderSplitToggle(GuiGraphicsExtractor graphics) {
        if (this.menu.getActiveLanes() <= 1) {
            return;
        }
        int x = this.splitButtonX();
        int y = this.topPos + SPLIT_BUTTON_Y;
        boolean on = this.menu.isSplitInputs();
        graphics.fill(x, y, x + this.splitButtonWidth(), y + SPLIT_BUTTON_HEIGHT, on ? 0xFF3A7D3A : 0xFF5A5A5A);
        graphics.text(this.font, "Split", x + 3, y + 2, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.menu.getActiveLanes() > 1) {
            int x = this.splitButtonX();
            int y = this.topPos + SPLIT_BUTTON_Y;
            if (event.x() >= x && event.x() < x + this.splitButtonWidth() && event.y() >= y && event.y() < y + SPLIT_BUTTON_HEIGHT) {
                ClientPacketDistributor.sendToServer(new ToggleSplitPacket(this.menu.getPos()));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Draws one small icon + "x{level}" label per installed upgrade, stacked vertically on the
     * left edge and centered on the lane area - the left-side counterpart to the energy bar on
     * the right. Upgrades not yet installed (level 0) are skipped entirely, so the stack only
     * takes up as much room as there are upgrades to show.
     */
    private void renderUpgradeIcons(GuiGraphicsExtractor graphics, int activeLanes) {
        Item[] items = {PolyFactory.UPGRADE_SPEED_ITEM.get(), PolyFactory.UPGRADE_ENERGY_ITEM.get(), PolyFactory.UPGRADE_SLOTS_ITEM.get()};
        int[] levels = {this.menu.getSpeedLevel(), this.menu.getEnergyLevel(), this.menu.getSlotLevel()};

        int rows = 0;
        for (int level : levels) {
            if (level > 0) {
                rows++;
            }
        }
        if (rows == 0) {
            return;
        }

        int laneAreaCenterY = this.topPos + FabricatorMenu.LANE_Y_START + (activeLanes * FabricatorMenu.LANE_HEIGHT) / 2;
        int x = this.leftPos + UPGRADE_ICON_X;
        int y = laneAreaCenterY - (rows * UPGRADE_ROW_HEIGHT) / 2;

        for (int i = 0; i < items.length; i++) {
            if (levels[i] <= 0) {
                continue;
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(UPGRADE_ICON_SCALE, UPGRADE_ICON_SCALE);
            graphics.item(new ItemStack(items[i]), 0, 0);
            graphics.pose().popMatrix();

            graphics.text(this.font, "x" + levels[i], x + UPGRADE_ICON_SIZE + 3, y + 1, 0xFFFFFFFF, false);
            y += UPGRADE_ROW_HEIGHT;
        }
    }

    private void renderProgressArrow(GuiGraphicsExtractor graphics, int lane) {
        int slotY = this.topPos + FabricatorMenu.LANE_Y_START + lane * FabricatorMenu.LANE_HEIGHT;
        int x = this.leftPos + ARROW_X;
        int y = slotY + (FabricatorMenu.SLOT_SIZE - ARROW_HEIGHT) / 2;

        boolean blocked = this.menu.isBlocked(lane);
        drawArrow(graphics, x, y, ARROW_WIDTH, ARROW_HEIGHT, 1.0F, blocked ? BLOCKED_COLOR : 0xFF8B8B8B);

        if (!blocked) {
            float progress = this.menu.getProgress(lane);
            if (progress > 0.0F) {
                drawArrow(graphics, x, y, ARROW_WIDTH, ARROW_HEIGHT, progress, 0xFF4CAF50);
            }
        }
    }

    /**
     * Draws a plain rightward arrow (rectangular shaft + triangular head) inside a
     * {@code width}x{@code height} box, filled from the left up to {@code fillFraction}.
     * <p>
     * The head is a clean 45-degree taper: each row's width changes by exactly 1 pixel from the
     * next, so the diagonal edge has no jagged double-steps. Its widest rows are
     * {@code height / 2 - 1} and {@code height / 2} (both {@code (height + 1) / 2} pixels wide),
     * and the shaft is centered on that same pair of rows by construction, so the two pieces
     * always line up regardless of {@code height}.
     */
    private void drawArrow(GuiGraphicsExtractor g, int x, int y, int width, int height, float fillFraction, int color) {
        int headWidth = (height + 1) / 2;
        int shaftWidth = width - headWidth;
        int shaftHeight = Math.max(2, (height / 6) * 2);
        int shaftTop = y + (height - shaftHeight) / 2;
        int shaftBottom = shaftTop + shaftHeight;
        int fillCutoffX = x + Mth.ceil(width * fillFraction);

        int shaftEnd = Math.min(x + shaftWidth, fillCutoffX);
        if (shaftEnd > x) {
            g.fill(x, shaftTop, shaftEnd, shaftBottom, color);
        }

        for (int row = 0; row < height; row++) {
            int rowWidth = Math.min(row, height - 1 - row) + 1;
            int rowStart = x + shaftWidth;
            int rowEnd = Math.min(rowStart + rowWidth, fillCutoffX);
            if (rowEnd > rowStart) {
                g.fill(rowStart, y + row, rowEnd, y + row + 1, color);
            }
        }
    }

    private void renderEnergyBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int activeLanes) {
        int height = activeLanes * FabricatorMenu.LANE_HEIGHT - 6;
        int x = this.leftPos + this.imageWidth - 8 - ENERGY_WIDTH;
        int y = this.topPos + FabricatorMenu.LANE_Y_START;
        graphics.fill(x, y, x + ENERGY_WIDTH, y + height, 0xFF373737);
        int filledHeight = Mth.ceil(this.menu.getEnergyRatio() * (height - 2));
        if (filledHeight > 0) {
            graphics.fill(x + 1, y + height - 1 - filledHeight, x + ENERGY_WIDTH - 1, y + height - 1, 0xFF4CAF50);
        }

        if (mouseX >= x && mouseX < x + ENERGY_WIDTH && mouseY >= y && mouseY < y + height) {
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

    private void renderColoredSlotFrame(GuiGraphicsExtractor g, int x, int y, int borderColor) {
        g.fill(x, y, x + 18, y + 1, borderColor);
        g.fill(x, y + 17, x + 18, y + 18, borderColor);
        g.fill(x, y, x + 1, y + 18, borderColor);
        g.fill(x + 17, y, x + 18, y + 18, borderColor);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }
}
