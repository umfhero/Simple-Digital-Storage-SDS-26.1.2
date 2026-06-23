# Poly Factory — Implementation Summary

> **Purpose:** This document tracks every change made to the codebase. It is written for AI continuity — any AI assistant picking up this project should read this file first to understand what exists, what was changed, and what state the project is in.

---

## Project Setup

| Key | Value |
|---|---|
| Mod ID | `polyfactory` |
| Base Package | `net.umf.polyfactory` |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.75 |
| Java | 25 |
| Build System | Gradle + NeoForge ModDev plugin 2.0.141 |
| Source Root | `src/main/java/net/umf/polyfactory/` |
| Resources Root | `src/main/resources/` |

---

## Current File Inventory

### Java Sources (`src/main/java/net/umf/polyfactory/`)

| File | Role |
|---|---|
| `PolyFactory.java` | Main `@Mod` entrypoint. Registers the Fabricator block/item, the creative tab, and the common config. |
| `PolyFactoryClient.java` | Client entrypoint. Registers the config screen and the Fabricator's `MenuScreens` binding. |
| `Config.java` | Common config: `fabricatorEnergyCapacity`, `fabricatorMaxEnergyInsert`. |
| `block/FabricatorBlock.java` | Directional (`facing`) + `active` blockstate. Opens `FabricatorBlockEntity`'s menu on right-click; provides the server tick via `EntityBlock.getTicker`. |
| `block/entity/FabricatorItemHandler.java` | 2-slot `ItemStacksResourceHandler` (input=0, output=1); `isValid` rejects external inserts into the output slot. |
| `block/entity/FabricatorBlockEntity.java` | Owns the item handler + a `SimpleEnergyHandler` (FE buffer). `serverTick` matches a `FabricatingRecipe`, drains energy per tick, advances progress, and moves the result to the output slot when done. Persists via `ValueInput`/`ValueOutput`. |
| `block/entity/ModBlockEntities.java` | `DeferredRegister<BlockEntityType<?>>` for the Fabricator. |
| `block/entity/ModCapabilities.java` | Registers `Capabilities.Item.BLOCK` and `Capabilities.Energy.BLOCK` for the Fabricator block entity (any side) so other mods' pipes/cables can interact with it. |
| `recipe/FabricatingRecipe.java` | `SingleItemRecipe` subclass adding `processing_time` (ticks) and `energy_per_tick` (FE) fields. Datapack type `polyfactory:fabricating`. |
| `recipe/ModRecipes.java` | `DeferredRegister`s for the recipe type + serializer. |
| `gui/FabricatorMenu.java` | `AbstractContainerMenu`: 2 `ResourceHandlerSlot`s bound directly to the block entity's item handler, standard player inventory slots, and 4 `ContainerData` values (progress, maxProgress, energy, maxEnergy) synced automatically by vanilla's menu networking — no custom packets needed. |
| `gui/FabricatorScreen.java` | Programmatic GUI (filled rectangles, no texture atlas): panel, slot frames, a progress arrow, and a vertical FE bar with a tooltip. |
| `gui/ModMenuTypes.java` | `DeferredRegister<MenuType<?>>` for the Fabricator menu. |

### Resources

| File / Dir | Purpose |
|---|---|
| `assets/polyfactory/textures/block/fabricator_{front,front_on,side,top}.png` | Block textures (user-provided). `side` is reused for east/west/south/bottom since no dedicated bottom texture exists. |
| `assets/polyfactory/blockstates/fabricator.json` | `facing` x `active` variants -> `fabricator`/`fabricator_on` models. |
| `assets/polyfactory/models/block/fabricator.json`, `fabricator_on.json` | Cube models referencing the textures above. |
| `assets/polyfactory/items/fabricator.json` | Item model (reuses the block model). |
| `assets/polyfactory/lang/en_us.json` | Translations. |
| `data/polyfactory/recipe/iron_ingot_from_raw_iron.json` | Example `polyfactory:fabricating` recipe (raw iron -> iron ingot, 100 ticks, 20 FE/tick). |
| `data/polyfactory/loot_table/blocks/fabricator.json` | Drop-self loot table (NeoForge's default loot table key is `<modid>:blocks/<path>`, derived from the block's registry name — this was missing for the old Storage Hub block, which is why it never dropped anything). |

---

## Change Log

### 2026-06-23 — Fix: pipes pulled from the input slot, and output never stacked past 1

Two more bugs surfaced once the energy fix above let processing actually run:

**Pipes extracting from the input slot instead of the output slot.** The capability registered
for `Capabilities.Item.BLOCK` was the raw `FabricatorItemHandler` itself. `FabricatorItemHandler`
only restricts *insertion* via `isValid` (slot 0 valid, slot 1 not) — it places no restriction on
*extraction*, so the default no-index `extract()` happily pulled from slot 0 (tried first) before
ever considering slot 1. Fixed by adding `FabricatorIoView`, a thin `ResourceHandler<ItemResource>`
wrapper that's the only thing now registered for the capability: it forwards reads as-is but hard-codes
insert -> slot 0 only, extract -> slot 1 only. The GUI and `serverTick` still talk to the real
`FabricatorItemHandler` directly and are unaffected.

**Output capping at 1 item instead of stacking to 64.** `canInsertOutput` checked
`handler.getCapacityAsLong(SLOT_OUTPUT, resultResource)` to decide if there was room for another
result. But `StacksResourceHandler.getCapacityAsLong` is gated by the same `isValid` used for
insertion — `resource.isEmpty() || isValid(index, resource) ? getCapacity(...) : 0` — and
`isValid(SLOT_OUTPUT, anything-non-empty)` is deliberately `false` (see above). So once the output
slot had *any* item in it, the reported capacity collapsed to 0 and every subsequent batch got
blocked until the slot was emptied by hand. Fixed by checking against `resultResource.getMaxStackSize()`
directly instead of going through the handler's (insert-oriented) capacity method.

**Lesson for future additions:** `isValid` on `StacksResourceHandler` is a single flag shared by
insertion *and* the capacity query used internally — it can't express "don't accept external
inserts here" without also affecting "how much room is here." Any future per-slot logic that needs
those two questions answered differently has to route around `isValid`/`getCapacityAsLong`, the way
`canInsertOutput` and `FabricatorIoView` both now do.

Also fixed the process-arrow GUI glyph not being centered between the slots (the constants were
eyeballed off by a couple pixels both axes); it's now computed from the actual slot geometry instead
of separate magic numbers, so it can't drift out of sync again.

### 2026-06-23 — Fix: Fabricator never processed despite having power and a matching recipe

**The bug:** `FabricatorBlockEntity`'s `SimpleEnergyHandler` was constructed with `maxExtract = 0`
(`new SimpleEnergyHandler(capacity, maxInsert, 0)`), on the reasoning that external mods shouldn't
be able to drain the buffer back out. But `EnergyHandler.extract()` is the *same* method
`serverTick` calls internally to consume FE for processing — there's no separate "internal use"
vs. "external capability" path. With `maxExtract = 0`, `extract()` always returned `0` regardless
of how much energy was stored, so `energized` was always `false` and processing never started,
even though the recipe matched correctly and the item sat untouched in the input slot. The energy
bar still charged fine (insertion was unaffected), which made it look like only the
charging/animation worked and everything else was silently broken.

**The fix:** `maxExtract` is now `Config.FABRICATOR_MAX_ENERGY_INSERT.get()` (same as `maxInsert`)
instead of `0`. If per-side I/O restrictions are wanted later, they need a wrapper around the real
handler for the *external* capability registration only — `SimpleEnergyHandler` itself can't
distinguish "my own tick logic" from "an external mod's capability call."

**How this was found:** the actual decompiled NeoForge/Minecraft source for this exact version
(26.1.2.75) was already available locally (NeoForm's `recompile_*_output.jar` /
`decompile_*_output.jar` in `~/.gradle/caches/neoformruntime/intermediate_results`, and NeoForge's
own `-sources.jar`), which is far more reliable than searching the web for a version this new.
Recipe matching was verified directly by hooking `ServerStartedEvent` to call
`level.recipeAccess().getRecipeFor(...)` with a synthetic input, then end-to-end by placing a real
Fabricator via code, force-loading its chunk (`ServerLevel.setChunkForced`), and feeding it energy
and items directly through the capabilities — both confirmed (`finished processing, output now
minecraft:iron_ingot`) before the fix was considered done. Also added: a crafting recipe so the
Fabricator can be made in survival (`data/polyfactory/recipe/fabricator.json`, iron+furnace+redstone).

### 2026-06-23 — Rebuilt from Simple Digital Storage into Poly Factory

The old storage-network mod (Storage Hub, Storage Cable, the BFS network scanner, the unified-grid
GUI, and all of its custom networking packets) was deleted outright — none of it is reused. What
*was* kept from the old project: the Gradle/NeoForge project scaffolding, the package conventions,
and the general "BlockEntity + Menu + Screen" shape, since those still apply.

**What changed:**
- Renamed the mod end-to-end: `simpledigitalstorage` -> `polyfactory`, `net.umf.simpledigitalstorage` -> `net.umf.polyfactory`, `Simple Digital Storage` -> `Poly Factory`.
- Deleted `StorageCableBlock`, `StorageHubBlock`, `StorageHubBlockEntity`, `StorageHubMenu`, `StorageHubScreen`, and the entire `network/` package (`ExtractItemPacket`, `ModNetwork`, `StorageNetworkScanner`, `SyncNetworkItemsPacket`). None of it applies to a single processing machine, and the GUI no longer needs custom packets since machine state fits in 4 `ContainerData` ints.
- Added the Fabricator: block, block entity, item handler, recipe type, menu, and screen (see file inventory above).
- Registered the Fabricator's item/energy capabilities so external pipe mods can drive it — this was the main ask: "pipe in an item, have it process and turn into another, pipe it out", plus FE support.
- Fixed a latent bug inherited from the template: blocks need an explicit `data/<modid>/loot_table/blocks/<path>.json` to drop anything; the old Storage Hub never had one. The Fabricator does now.
- Rewrote `Config.java` (max cable range no longer applies) and properly registered it via `modContainer.registerConfig(...)` — the old `Config.SPEC` was defined but never actually registered, so it had no effect.

---

## Architecture Notes for Future AI

- This NeoForge version (26.1.2.75) uses the modern transfer API (`net.neoforged.neoforge.transfer.*`: `ResourceHandler<ItemResource>`, `EnergyHandler`/`SimpleEnergyHandler`, `Transaction`) instead of the legacy `IItemHandler`/`IEnergyStorage`. Capabilities are still registered the usual way via `RegisterCapabilitiesEvent.registerBlockEntity(...)`.
- Vanilla `BlockEntity` persistence uses `ValueInput`/`ValueOutput` (not `CompoundTag`) — see `BlockEntity#loadAdditional`/`saveAdditional`.
- Recipes use the modern codec-based `Recipe<T>`/`RecipeSerializer<T>`/`RecipeType<T>` with `MapCodec`/`StreamCodec`. `SingleItemRecipe` (vanilla, used by furnace/stonecutter) is the right base class for any "1 ingredient -> 1 result" recipe — extend it rather than implementing `Recipe` from scratch. Recipe result fields are `ItemStackTemplate`, not `ItemStack`.
- GUI data syncing for simple numeric state (progress, energy, etc.) should use vanilla `ContainerData` + `AbstractContainerMenu.addDataSlots` — it's synced automatically every tick by the vanilla menu networking, no custom `CustomPacketPayload` needed. Only reach for custom packets for data that doesn't fit that model (the old mod's unified item grid needed one; a single machine doesn't).

---

*Last updated: 2026-06-23*
