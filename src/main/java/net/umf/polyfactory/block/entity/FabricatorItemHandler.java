package net.umf.polyfactory.block.entity;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * The Fabricator's 2-slot inventory: {@link #SLOT_INPUT} accepts items from pipes/players,
 * {@link #SLOT_OUTPUT} only ever receives items internally once a recipe finishes, so external
 * inserts (pipes and the GUI alike) are rejected there.
 */
public class FabricatorItemHandler extends ItemStacksResourceHandler {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    public FabricatorItemHandler() {
        super(SLOT_COUNT);
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return slot == SLOT_INPUT;
    }
}
