package net.umf.polyfactory.block.entity;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * {@link SimpleEnergyHandler} exposes no way to change its capacity/transfer limits after
 * construction, but Energy Upgrades need to raise them at runtime. Subclassing gets protected
 * access to the underlying fields so {@link #setLimits} can adjust them in place without losing
 * the energy currently stored.
 */
public class FabricatorEnergyHandler extends SimpleEnergyHandler {

    public FabricatorEnergyHandler(int capacity, int maxInsert, int maxExtract) {
        super(capacity, maxInsert, maxExtract);
    }

    public void setLimits(int capacity, int maxInsert, int maxExtract) {
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        if (this.energy > capacity) {
            this.energy = capacity;
        }
    }
}
