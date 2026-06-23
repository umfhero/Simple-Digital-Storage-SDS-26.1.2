package net.umf.polyfactory.block.entity;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The item capability exposed to external pipes/cables: inserts only ever land in an input slot
 * and extraction only ever pulls from an output slot, regardless of which side connects, and only
 * for lanes the Fabricator's current Slot Upgrade level has actually unlocked.
 * <p>
 * This is intentionally separate from {@link FabricatorItemHandler} itself, whose {@code isValid}
 * only governs insertion (it has to stay permissive for reads, since {@code canInsertOutput} and
 * the GUI both need the real per-slot state) — a single {@code isValid} flag can't also express
 * "but extraction is fine here", so the direction split lives in this wrapper instead.
 * <p>
 * When the "split" toggle is on, an index-less insert (the form pipes generally use, since it lets
 * the handler choose where things go) is divided evenly across every unlocked lane instead of the
 * default {@link ResourceHandler#insert(Object, int, TransactionContext) behavior} of filling lane
 * 0 completely before ever touching lane 1 - that default is exactly why extra lanes from a Slot
 * Upgrade would otherwise sit empty.
 */
public final class FabricatorIoView implements ResourceHandler<ItemResource> {
    private final FabricatorItemHandler handler;

    public FabricatorIoView(FabricatorItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public int size() {
        return this.handler.size();
    }

    @Override
    public ItemResource getResource(int index) {
        return this.handler.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return this.handler.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return this.handler.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return this.handler.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        // handler.insert() already checks isValid(), which covers both "is this an input slot"
        // and "is this slot's lane currently unlocked" - nothing extra to gate here.
        return this.handler.insert(index, resource, amount, transaction);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        int activeLanes = this.handler.getActiveLanes();
        if (!this.handler.isSplitInputs() || activeLanes <= 1) {
            return ResourceHandler.super.insert(resource, amount, transaction);
        }

        int remaining = amount;
        int totalInserted = 0;

        // First pass: give every lane an even share.
        int share = Math.max(1, amount / activeLanes);
        for (int lane = 0; lane < activeLanes && remaining > 0; lane++) {
            int inserted = this.insert(FabricatorItemHandler.inputSlot(lane), resource, Math.min(share, remaining), transaction);
            totalInserted += inserted;
            remaining -= inserted;
        }
        // Second pass: dump whatever's left (rounding, or some lanes were full/blocked) into
        // whichever lanes still have room.
        for (int lane = 0; lane < activeLanes && remaining > 0; lane++) {
            int inserted = this.insert(FabricatorItemHandler.inputSlot(lane), resource, remaining, transaction);
            totalInserted += inserted;
            remaining -= inserted;
        }
        return totalInserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        boolean activeOutputSlot = !FabricatorItemHandler.isInputSlot(index)
                && FabricatorItemHandler.laneOf(index) < this.handler.getActiveLanes();
        return activeOutputSlot ? this.handler.extract(index, resource, amount, transaction) : 0;
    }
}
