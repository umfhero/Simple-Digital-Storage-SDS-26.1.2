package net.umf.polyfactory.block.entity;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The item capability exposed to external pipes/cables: inserts only ever land in the input slot
 * and extraction only ever pulls from the output slot, regardless of which side connects.
 * <p>
 * This is intentionally separate from {@link FabricatorItemHandler} itself, whose {@code isValid}
 * only governs insertion (it has to stay permissive for reads, since {@code canInsertOutput} and
 * the GUI both need the real per-slot state) — a single {@code isValid} flag can't also express
 * "but extraction is fine here", so the direction split lives in this wrapper instead.
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
        return index == FabricatorItemHandler.SLOT_INPUT ? this.handler.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return index == FabricatorItemHandler.SLOT_OUTPUT ? this.handler.extract(index, resource, amount, transaction) : 0;
    }
}
