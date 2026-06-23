package net.umf.polyfactory.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.umf.polyfactory.Config;
import net.umf.polyfactory.PolyFactory;
import net.umf.polyfactory.block.FabricatorBlock;
import net.umf.polyfactory.gui.FabricatorMenu;
import net.umf.polyfactory.recipe.FabricatingRecipe;
import net.umf.polyfactory.recipe.ModRecipes;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Block entity for the Fabricator. Holds a 2-slot item inventory (input/output) and an internal
 * FE buffer, and drives the input -> output {@link FabricatingRecipe} processing each server tick.
 */
public class FabricatorBlockEntity extends BlockEntity implements MenuProvider {

    private final FabricatorItemHandler itemHandler = new FabricatorItemHandler();
    private final FabricatorIoView ioView = new FabricatorIoView(this.itemHandler);
    // maxExtract must stay >= the highest energy_per_tick used by any recipe: SimpleEnergyHandler.extract()
    // is also what serverTick uses internally to drain FE for processing, not just external capability access.
    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(
            Config.FABRICATOR_ENERGY_CAPACITY.get(), Config.FABRICATOR_MAX_ENERGY_INSERT.get(), Config.FABRICATOR_MAX_ENERGY_INSERT.get());
    private final RecipeManager.CachedCheck<SingleRecipeInput, FabricatingRecipe> quickCheck =
            RecipeManager.createCheck(ModRecipes.FABRICATING_TYPE.get());

    private int progress;
    private int maxProgress;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case FabricatorMenu.DATA_PROGRESS -> FabricatorBlockEntity.this.progress;
                case FabricatorMenu.DATA_MAX_PROGRESS -> FabricatorBlockEntity.this.maxProgress;
                case FabricatorMenu.DATA_ENERGY -> FabricatorBlockEntity.this.energyHandler.getAmountAsInt();
                case FabricatorMenu.DATA_MAX_ENERGY -> FabricatorBlockEntity.this.energyHandler.getCapacityAsInt();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case FabricatorMenu.DATA_PROGRESS -> FabricatorBlockEntity.this.progress = value;
                case FabricatorMenu.DATA_MAX_PROGRESS -> FabricatorBlockEntity.this.maxProgress = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return FabricatorMenu.DATA_COUNT;
        }
    };

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FABRICATOR.get(), pos, state);
    }

    public FabricatorItemHandler getItemHandler() {
        return this.itemHandler;
    }

    public FabricatorIoView getIoView() {
        return this.ioView;
    }

    public SimpleEnergyHandler getEnergyHandler() {
        return this.energyHandler;
    }

    public ContainerData getContainerData() {
        return this.dataAccess;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FabricatorBlockEntity be) {
        boolean changed = false;

        ItemResource inputResource = be.itemHandler.getResource(FabricatorItemHandler.SLOT_INPUT);
        long inputAmount = be.itemHandler.getAmountAsLong(FabricatorItemHandler.SLOT_INPUT);

        FabricatingRecipe recipe = null;
        ItemStack result = ItemStack.EMPTY;
        if (!inputResource.isEmpty() && inputAmount > 0) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(inputResource.toStack((int) inputAmount));
            Optional<RecipeHolder<FabricatingRecipe>> match = be.quickCheck.getRecipeFor(recipeInput, level);
            if (match.isPresent()) {
                recipe = match.get().value();
                result = recipe.assemble(recipeInput);
            }
        }

        if (recipe == null || result.isEmpty() || !canInsertOutput(be.itemHandler, result)) {
            if (be.progress != 0) {
                be.progress = 0;
                changed = true;
            }
            be.maxProgress = 0;
        } else {
            be.maxProgress = recipe.processingTime();
            int energyNeeded = recipe.energyPerTick();
            boolean energized;
            try (Transaction tx = Transaction.openRoot()) {
                int extracted = be.energyHandler.extract(energyNeeded, tx);
                energized = extracted >= energyNeeded;
                if (energized) {
                    tx.commit();
                }
            }

            if (energized) {
                if (be.progress == 0) {
                    PolyFactory.LOGGER.debug("Fabricator at {} started processing {} -> {}", pos, inputResource, result);
                }
                be.progress++;
                changed = true;
                if (be.progress >= recipe.processingTime()) {
                    try (Transaction tx = Transaction.openRoot()) {
                        be.itemHandler.extract(FabricatorItemHandler.SLOT_INPUT, inputResource, 1, tx);
                        tx.commit();
                    }
                    insertOutput(be.itemHandler, result);
                    be.progress = 0;
                    PolyFactory.LOGGER.debug("Fabricator at {} finished processing, output now {}", pos, be.itemHandler.getResource(FabricatorItemHandler.SLOT_OUTPUT));
                }
            } else if (level.getGameTime() % 20 == 0) {
                PolyFactory.LOGGER.debug("Fabricator at {} has a valid recipe for {} but not enough energy ({} stored, needs {}/tick)",
                        pos, inputResource, be.energyHandler.getAmountAsLong(), energyNeeded);
            }
        }

        boolean wasActive = state.getValue(FabricatorBlock.ACTIVE);
        boolean isActive = be.progress > 0;
        if (wasActive != isActive) {
            level.setBlock(pos, state.setValue(FabricatorBlock.ACTIVE, isActive), 3);
            changed = true;
        }

        if (changed) {
            be.setChanged();
        }
    }

    private static boolean canInsertOutput(FabricatorItemHandler handler, ItemStack result) {
        ItemResource resultResource = ItemResource.of(result);
        ItemResource currentOutput = handler.getResource(FabricatorItemHandler.SLOT_OUTPUT);
        if (currentOutput.isEmpty()) {
            return true;
        }
        if (!currentOutput.equals(resultResource)) {
            return false;
        }
        // Not handler.getCapacityAsLong(...): that's gated by isValid(), which is deliberately false
        // for the output slot (to reject external/GUI inserts there) and would always report 0 room.
        long currentAmount = handler.getAmountAsLong(FabricatorItemHandler.SLOT_OUTPUT);
        return currentAmount + result.getCount() <= resultResource.getMaxStackSize();
    }

    private static void insertOutput(FabricatorItemHandler handler, ItemStack result) {
        ItemResource resultResource = ItemResource.of(result);
        long currentAmount = handler.getAmountAsLong(FabricatorItemHandler.SLOT_OUTPUT);
        handler.set(FabricatorItemHandler.SLOT_OUTPUT, resultResource, (int) (currentAmount + result.getCount()));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.itemHandler.deserialize(input.childOrEmpty("items"));
        this.energyHandler.deserialize(input.childOrEmpty("energy"));
        this.progress = input.getIntOr("progress", 0);
        this.maxProgress = input.getIntOr("max_progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.itemHandler.serialize(output.child("items"));
        this.energyHandler.serialize(output.child("energy"));
        output.putInt("progress", this.progress);
        output.putInt("max_progress", this.maxProgress);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.polyfactory.fabricator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return FabricatorMenu.createServerMenu(containerId, playerInv, this.worldPosition);
    }
}
