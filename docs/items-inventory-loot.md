# Items, inventory, equipment and loot

## Items (`systems/item`)

- `ItemDefinition`: an immutable template, shared by all stacks: id, name, description, `ItemType`, max stack size, icon path, consumable flag, plus optional bag size, `EquipmentSlot`, `WeaponStats`, weapon sprite and `LootModifier` (`setLootModifier`).
- `ItemStack`: a definition plus a quantity. Create stacks with `ItemFactory.create(id, quantity)`, which returns null for unknown IDs.
- **All items are registered in `TestItems.java`** (`ItemRegistry.register(...)`). Pick the constructor overload that has the fields you need. Icons load automatically from `iconPath` (`assets/Items/...`).
- `ItemType`: CONSUMABLE, WEAPON, ARMOR, TOOL, MATERIAL, QUEST, RESOURCE, BAG, FURNITURE.
- Consumable behaviour: `ConsumableEffect` implementations in `ConsumableEffects/` (healing potion, poison).
- Furniture items become placed objects through `systems/furniture/FurnitureFactory`; `wooden_chest` becomes a `ChestEntity` with its own container.

Weapons: `WeaponStats` (damage, speed, range, knockback, windup/active/recovery timing) + `WeaponType` (8 types) choose an attack strategy in `systems/combat`. The equipped weapon drives `PlayerEntity`'s attacks.

## Inventory (`systems/inventory`)

- `PlayerInventory` = a default container (`InventoryConfig.DEFAULT_INVENTORY_SIZE`, 8 slots) + bag slots (`MAX_BAG_SLOTS`, 6) + `PlayerEquipment`.
- Bags are items with a `bagSize`; equipping one adds a `BagInstance` container. A bag can go inside another bag only when it's empty, and must be empty to unequip, swap or drop.
- `InventoryContainer` is the generic slot container (also used by chests); `ItemFilter` can restrict what a container accepts (not enforced on bags yet).
- `PlayerEquipment`: slots HEAD, BODY, AMULET, RING_1, RING_2 (ARMOR items) and WEAPON (WEAPON items). **Equipment has no stat effects yet**, only loot modifiers (below).
- Saving: `PlayerInventory.exportSaveData()` / `importSaveData()` ↔ `save/InventoryData`.

Tunables live in `InventoryConfig` (world item cap 100, magnet radius, pickup scale, bounce, etc.).

## Items in the world

- `integration/WorldItemManager` owns dropped items **per level ID**, enforces the global cap, and notifies listeners on spawn/remove. That's how the multiplayer host replicates items without extra code.
- `ItemPickupEntity`: bounces, has a pickup grace period (so you don't instantly re-grab what you dropped), and is pulled toward players with an `ItemMagnetComponent`.
- Pickup is automatic on contact (`GameWorld` checks it). On a multiplayer client, pickups and drops go through the host (`NetSession.requestPickup` / `dropItem`).

## UI (`systems/ui`)

`UIManagerNew` builds the in-game Stage: `BottomHUD` (bag slots, always visible), `ContainerWindow` (inventory, bags, chests), `EquipmentWindow`, `ItemSlotUI` + `ItemDragAndDropSystem` (snap to the nearest slot on fast drags; drop outside a window to drop the item in the world), `ContextMenu`, `TooltipLabel`, `SettingsMenu`, `ItemBrowserWindow` (F5, debug). Open windows don't pause the game. After changing inventory contents from code, call `uiManager.notifyInventoryChanged()` / `refreshAllWindows()` (through `GameWorld.Presenter.onInventoryChanged` from the simulation).

## Loot (`systems/loot`)

```java
// In an enemy's constructor (see LizardEnemy, Axolot)
addComponent(new LootTableComponent()
    .addDrop("coin", 1.0f, 3, 8)            // 100%: 3-8 coins
    .addDrop("health_potion", 0.25f, 1));   // 25%: one potion
addTag("monster:lizard");                    // Tags let modifiers react to the kind of enemy
```

- When an enemy dies, `LootSystem.generateAndSpawnLoot(enemy, player)`:
  - builds a `LootContext` with the killer's modifiers;
  - rolls the table (`LootTableComponent.rollDrops(context)`);
  - spawns the items through `WorldItemManager`.
- **Modifiers** (`LootModifier`, applied in priority order):
  - `AddDropModifier`: adds a drop.
  - `HideHarvesterModifier`: adds a drop based on the enemy's `animal:*` tag.
  - `ChanceMultiplierModifier`: scales drop chances.
  - `QuantityMultiplierModifier`: scales quantities.
- A player's modifiers are the `getLootModifier()` of each equipped item (`LootSystem.collectPlayerModifiers`). Skills and buffs aren't collected yet.
- `LootSystem` is a singleton initialised with the `WorldItemManager` in `GameScreen`. In multiplayer only the host resolves deaths, so only the host rolls loot. In tests, initialise it for the host only.
- Breakables get their loot from `assets/data/BreakableObjectConfig.json`.
