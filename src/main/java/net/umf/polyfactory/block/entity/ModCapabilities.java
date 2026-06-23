package net.umf.polyfactory.block.entity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Exposes the Fabricator's item slots and FE buffer to other mods' pipes/cables via NeoForge
 * capabilities, regardless of which side they connect from. Item access goes through
 * {@link FabricatorIoView} so pipes can only insert into the input slot and extract from the
 * output slot, never the reverse.
 */
public final class ModCapabilities {

    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.FABRICATOR.get(),
                (be, direction) -> be.getIoView());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.FABRICATOR.get(),
                (be, direction) -> be.getEnergyHandler());
    }
}
