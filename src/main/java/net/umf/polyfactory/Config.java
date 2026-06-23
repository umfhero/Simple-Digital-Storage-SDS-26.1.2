package net.umf.polyfactory;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common configuration for Poly Factory.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue FABRICATOR_ENERGY_CAPACITY = BUILDER
            .comment("How much FE the Fabricator can store internally.")
            .defineInRange("fabricatorEnergyCapacity", 10000, 1000, 1000000);

    public static final ModConfigSpec.IntValue FABRICATOR_MAX_ENERGY_INSERT = BUILDER
            .comment("Maximum FE per tick the Fabricator will accept from external energy sources.")
            .defineInRange("fabricatorMaxEnergyInsert", 200, 1, 100000);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
