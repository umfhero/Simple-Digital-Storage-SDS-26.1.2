package net.umf.polyfactory.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.umf.polyfactory.PolyFactory;
import net.umf.polyfactory.block.entity.FabricatorBlockEntity;
import net.umf.polyfactory.block.entity.FabricatorItemHandler;

/**
 * Server/client container for the Fabricator GUI: input/output slots backed directly by the
 * block entity's {@link FabricatorItemHandler}, the player's inventory, and synced progress/energy data.
 */
public class FabricatorMenu extends AbstractContainerMenu {

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_ENERGY = 2;
    public static final int DATA_MAX_ENERGY = 3;
    public static final int DATA_COUNT = 4;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    private FabricatorMenu(int containerId, Inventory playerInv, BlockPos pos, FabricatorItemHandler itemHandler, ContainerData data) {
        super(ModMenuTypes.FABRICATOR_MENU.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;

        this.addSlot(new ResourceHandlerSlot(itemHandler, itemHandler::set, FabricatorItemHandler.SLOT_INPUT, 56, 38));
        this.addSlot(new ResourceHandlerSlot(itemHandler, itemHandler::set, FabricatorItemHandler.SLOT_OUTPUT, 104, 38));
        this.addStandardInventorySlots(playerInv, 8, 84);
        this.addDataSlots(data);
    }

    public static FabricatorMenu createServerMenu(int containerId, Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof FabricatorBlockEntity fabricator) {
            return new FabricatorMenu(containerId, playerInv, pos, fabricator.getItemHandler(), fabricator.getContainerData());
        }
        return new FabricatorMenu(containerId, playerInv, pos, new FabricatorItemHandler(), new SimpleContainerData(DATA_COUNT));
    }

    public static FabricatorMenu clientFactory(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new FabricatorMenu(containerId, playerInv, pos, new FabricatorItemHandler(), new SimpleContainerData(DATA_COUNT));
    }

    public float getProgress() {
        int max = this.data.get(DATA_MAX_PROGRESS);
        int current = this.data.get(DATA_PROGRESS);
        return max != 0 ? Mth.clamp((float) current / max, 0.0F, 1.0F) : 0.0F;
    }

    public float getEnergyRatio() {
        int max = this.data.get(DATA_MAX_ENERGY);
        int current = this.data.get(DATA_ENERGY);
        return max != 0 ? Mth.clamp((float) current / max, 0.0F, 1.0F) : 0.0F;
    }

    public int getEnergyStored() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergyStored() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, PolyFactory.FABRICATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        if (index == FabricatorItemHandler.SLOT_OUTPUT) {
            if (!this.moveItemStackTo(stackInSlot, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index == FabricatorItemHandler.SLOT_INPUT) {
            if (!this.moveItemStackTo(stackInSlot, 2, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stackInSlot, FabricatorItemHandler.SLOT_INPUT, FabricatorItemHandler.SLOT_INPUT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return copy;
    }
}
