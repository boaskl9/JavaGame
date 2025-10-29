# Loot System - Usage Guide

## Overview
The loot system provides dynamic, context-aware item drops for enemies. It supports:
- Base loot tables per enemy
- Runtime modifiers from player equipment/skills
- Context-aware drops (e.g., hide harvester weapon)
- Mixed modifier stacking (additive chance, multiplicative quantity)

---

## Quick Start

### 1. Initialize LootSystem (in GameScreen)

```java
// Initialize LootSystem singleton once during startup
// (already done in GameScreen constructor)
LootSystem.initialize(worldItemManager);
```

Note: LootSystem is a singleton and doesn't need to be passed to enemies!

### 2. Define Enemy Loot Table

```java
// In your enemy subclass constructor
LootTableComponent lootTable = new LootTableComponent()
    .addDrop("coin", 1.0f, 3, 8)           // Always drops 3-8 coins
    .addDrop("health_potion", 0.25f, 1, 1) // 25% chance for 1 potion
    .addDrop("rusty_dagger", 0.05f, 1, 1); // 5% rare drop
addComponent(lootTable);
```

### 3. Add Tags for Context-Aware Drops (Optional)

```java
// In enemy constructor
addTag("animal:cow");  // For hide harvester to drop cow_hide
```

---

## Examples

### Example 1: Basic Enemy Loot

```java
public class GoblinEntity extends EnemyEntity {
    public GoblinEntity(WorldManager world, float x, float y) {
        super(world, 20, x, y); // 20 HP

        // Define loot
        LootTableComponent loot = new LootTableComponent()
            .addDrop("coin", 1.0f, 2, 5)      // Always 2-5 coins
            .addDrop("health_potion", 0.2f, 1) // 20% health potion
            .addDrop("goblin_ear", 0.5f, 1);   // 50% goblin ear

        addComponent(loot);
    }
}
```

### Example 2: Animal with Tag for Hide Harvesting

```java
public class CowEntity extends EnemyEntity {
    public CowEntity(WorldManager world, float x, float y) {
        super(world, 30, x, y);

        // Add tag for hide harvester
        addTag("animal:cow");

        // Base loot (without hide harvester)
        LootTableComponent loot = new LootTableComponent()
            .addDrop("meat", 0.8f, 1, 3);  // 80% chance for 1-3 meat

        addComponent(loot);

        // Note: If player has hide harvester weapon equipped,
        // it will add cow_hide to this table dynamically
    }
}
```

### Example 3: Using Modifiers (TODO: Implement in equipment system)

```java
// When player equips coffee ring
AddDropModifier coffeeRing = new AddDropModifier("coffee", 1.0f, 1);
// This would be collected by LootSystem.collectPlayerModifiers()

// When player equips hide harvester weapon
HideHarvesterModifier hideHarvester = new HideHarvesterModifier(1.0f, 1);
// Automatically adds cow_hide for cows, rabbit_hide for rabbits, etc.

// When player has "Lucky" perk
ChanceMultiplierModifier luckPerk = new ChanceMultiplierModifier(1.5f);
// All drop chances multiplied by 1.5x

// When player has "Greedy" perk
QuantityMultiplierModifier greedPerk = new QuantityMultiplierModifier(2.0f, "coin");
// Double coin drops only
```

---

## Creating Custom Modifiers

### Example: Boss-Killer Modifier
```java
public class BossKillerModifier implements LootModifier {
    @Override
    public void modify(List<LootDrop> drops, LootContext context) {
        // Triple drops from bosses
        if (context.getEnemy().hasTag("boss")) {
            for (int i = 0; i < drops.size(); i++) {
                LootDrop drop = drops.get(i);
                int newMin = drop.getMinQuantity() * 3;
                int newMax = drop.getMaxQuantity() * 3;
                drops.set(i, drop.withQuantity(newMin, newMax));
            }
        }
    }

    @Override
    public int getPriority() {
        return 0; // Default priority
    }
}
```

---

## Tag System Reference

Common tag patterns:
- `animal:cow`, `animal:rabbit`, `animal:wolf` - For hide harvesting
- `undead:skeleton`, `undead:zombie` - For holy weapons
- `humanoid:goblin`, `humanoid:orc` - For specific loot
- `boss` - For special modifiers
- `magical` - For mana/essence drops

---

## Integration Checklist

- [x] Initialize `LootSystem` singleton in GameScreen (already done!)
- [ ] Add `LootTableComponent` to enemy types
- [ ] Add tags to enemies for context-aware drops
- [ ] Implement `collectPlayerModifiers()` in LootSystem (future: equipment system)
- [ ] Create items that enemies can drop (in ItemRegistry)
- [x] Test with sample enemy (LizardEnemy has loot table)

---

## Testing

```java
// Quick test without player/modifiers
LootTableComponent table = new LootTableComponent()
    .addDrop("coin", 1.0f, 5, 10);

List<ItemStack> drops = table.rollDrops();
System.out.println("Dropped: " + drops);

// Test with modifiers
LootContext context = new LootContext(enemy, player);
context.addModifier(new AddDropModifier("coffee", 1.0f, 1));
List<ItemStack> dropsWithModifiers = table.rollDrops(context);
```

---

## Architecture Diagram

```
Enemy dies
    ↓
EnemyEntity.onDeath()
    ↓
LootSystem.generateAndSpawnLoot(enemy, player)
    ↓
1. Get enemy's LootTableComponent
2. Collect player's active modifiers
3. Create LootContext
4. LootTableComponent.rollDrops(context)
    ↓
    a. Copy base drops
    b. Apply modifiers in priority order
    c. Roll each drop (chance check)
    d. Determine quantities
    e. Create ItemStacks
    ↓
5. Spawn items via WorldItemManager (scatter pattern)
```

---

## Notes

- **No loot table**: Enemies without `LootTableComponent` drop nothing
- **Item limit**: WorldItemManager enforces max world items
- **Grace period**: Dropped items can't be picked up for 0.5s
- **Scatter radius**: Items spawn within 12 pixels of enemy center
- **Modifier stacking**: Applied in priority order (100 → 0 → -50)
- **Extensible**: Create custom modifiers by implementing `LootModifier`
