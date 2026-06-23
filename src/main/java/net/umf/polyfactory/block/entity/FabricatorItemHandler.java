package net.umf.polyfactory.block.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * The Fabricator's inventory: up to {@link #MAX_LANES} independent input/output pairs, one per
 * Slot Upgrade level (lane 0 is always present). Slots are laid out as all inputs first, then all
 * outputs: {@code inputSlot(lane) = lane}, {@code outputSlot(lane) = MAX_LANES + lane}.
 * <p>
 * Only input slots are ever {@code isValid} (so external inserts and the GUI both reject the
 * output slots), and only slots belonging to an unlocked lane ({@code lane < activeLanes}) are
 * valid at all - lanes beyond the current Slot Upgrade level are inert until unlocked.
 */
public class FabricatorItemHandler extends ItemStacksResourceHandler {

    public static final int MAX_LANES = 4;
    public static final int SLOT_COUNT = MAX_LANES * 2;

    private int activeLanes = 1;
    private boolean splitInputs;

    public FabricatorItemHandler() {
        super(SLOT_COUNT);
    }

    public static int inputSlot(int lane) {
        return lane;
    }

    public static int outputSlot(int lane) {
        return MAX_LANES + lane;
    }

    public static boolean isInputSlot(int slot) {
        return slot < MAX_LANES;
    }

    public static int laneOf(int slot) {
        return slot % MAX_LANES;
    }

    public int getActiveLanes() {
        return this.activeLanes;
    }

    public void setActiveLanes(int activeLanes) {
        this.activeLanes = activeLanes;
    }

    public boolean isSplitInputs() {
        return this.splitInputs;
    }

    public void setSplitInputs(boolean splitInputs) {
        this.splitInputs = splitInputs;
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return isInputSlot(slot) && laneOf(slot) < this.activeLanes;
    }

    /**
     * If the split toggle is on, evenly redistributes whatever is sitting in the unlocked lanes'
     * input slots across every input slot that is either empty or already holds the same
     * resource - regardless of how it got there. This is what lets a stack dropped directly into
     * one lane via the GUI spread itself into the others, since a GUI placement targets a specific
     * slot and never goes through the index-less {@link FabricatorIoView#insert} that normally
     * handles splitting for pipe inserts. Lanes already holding a different resource are left
     * untouched.
     *
     * @return {@code true} if any slot's contents changed.
     */
    public boolean rebalanceInputs() {
        if (!this.splitInputs || this.activeLanes <= 1) {
            return false;
        }

        ItemResource resource = ItemResource.EMPTY;
        long total = 0;
        int[] lanes = new int[this.activeLanes];
        int laneCount = 0;
        for (int lane = 0; lane < this.activeLanes; lane++) {
            int slot = inputSlot(lane);
            ItemResource res = this.getResource(slot);
            if (res.isEmpty()) {
                lanes[laneCount++] = lane;
                continue;
            }
            if (resource.isEmpty()) {
                resource = res;
            }
            if (res.equals(resource)) {
                total += this.getAmountAsLong(slot);
                lanes[laneCount++] = lane;
            }
        }

        if (resource.isEmpty() || laneCount <= 1 || total == 0) {
            return false;
        }

        long base = total / laneCount;
        long remainder = total % laneCount;
        int maxStack = resource.getMaxStackSize();

        boolean changed = false;
        for (int i = 0; i < laneCount; i++) {
            int slot = inputSlot(lanes[i]);
            long target = Math.min(maxStack, base + (i < remainder ? 1 : 0));
            if (this.getAmountAsLong(slot) != target) {
                this.set(slot, target == 0 ? ItemResource.EMPTY : resource, (int) target);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        // A save from before Slot Upgrades existed serialized only 2 slots; StacksResourceHandler's
        // deserialize() replaces the stacks list outright with whatever size was saved, which would
        // otherwise silently shrink this handler and crash any lane access beyond index 1.
        if (this.size() != SLOT_COUNT) {
            NonNullList<ItemStack> loaded = this.copyToList();
            NonNullList<ItemStack> padded = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(loaded.size(), SLOT_COUNT); i++) {
                padded.set(i, loaded.get(i));
            }
            this.setStacks(padded);
        }
    }
}
