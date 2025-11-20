package com.game.systems.item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.game.integration.WorldItemManager;
import com.game.systems.combat.WeaponStats;
import com.game.systems.combat.WeaponType;
import com.game.systems.inventory.EquipmentSlot;
import com.game.systems.loot.QuantityMultiplierModifier;

/**
 * Registers test items for debugging and development.
 */
public class TestItems {

    /**
     * Registers all test items.
     */
    public static void registerTestItems() {
        // Register wood item
        ItemDefinition wood = new ItemDefinition(
            "wood",
            "Wood",
            "A piece of wood from a branch.",
            ItemType.MATERIAL,
            64, // Max stack size
            "assets/Items/Resource/Branch.png",
            false // Not consumable
        );
        ItemRegistry.register(wood);

        // Register bag item - 12 slots
        ItemDefinition bag = new ItemDefinition(
            "bag",
            "Traveler's Bag",
            "A simple bag for carrying items. Can hold 12 items.",
            ItemType.BAG,
            1, // Bags don't stack
            "assets/Items/Object/Bag.png",
            false,
            12 // Bag size: 12 slots
        );
        ItemRegistry.register(bag);

        // Register bag2 item - 6 slots (smaller bag)
        ItemDefinition bag2 = new ItemDefinition(
            "bag2",
            "Small Pouch",
            "A small pouch for carrying items. Can hold 6 items.",
            ItemType.BAG,
            1, // Bags don't stack
            "assets/Items/Object/Pouch.png",
            false,
            6 // Bag size: 6 slots
        );
        ItemRegistry.register(bag2);

        // Register bag3 item - 32 slots (huge bag)
        ItemDefinition bag3 = new ItemDefinition(
            "bag3",
            "Huge Pouch",
            "A huge pouch for carrying items. Can hold 32 items!",
            ItemType.BAG,
            1, // Bags don't stack
            "assets/Items/Object/BagGreen.png",
            false,
            32 // Bag size: 6 slots
        );
        ItemRegistry.register(bag3);

        ItemDefinition stone = new ItemDefinition(
            "stone",
            "Stone",
            "A small stone. Useful for crafting.",
            ItemType.MATERIAL,
            99,
            "assets/Items/Resource/Rock.png",
            false
        );
        ItemRegistry.register(stone);

        ItemDefinition coin = new ItemDefinition(
            "coin",
            "Coin",
            "A small coin. Probably not worth much",
            ItemType.RESOURCE,
            999,
            "assets/Items/Treasure/GoldCoin.png",
            false
        );
        ItemRegistry.register(coin);

        // Test consumable
        ItemDefinition potion = new ItemDefinition(
            "health_potion",
            "Health Potion",
            "Restores health when consumed.",
            ItemType.CONSUMABLE,
            10,
            "assets/Items/Potion/LifePot.png",
            true
        );
        ItemRegistry.register(potion);

        // ===== EQUIPMENT ITEMS =====

        // Helmet
        ItemDefinition ironHelmet = new ItemDefinition(
            "iron_helmet",
            "Iron Helmet",
            "A sturdy iron helmet that protects your head.",
            ItemType.ARMOR,
            1, // Not stackable
            "assets/Items/Equipment/iron_helmet.png",
            false,
            null, // Not a bag
            EquipmentSlot.HEAD
        );
        ItemRegistry.register(ironHelmet);


        // Amulet
        ItemDefinition goldAmulet = new ItemDefinition(
            "gold_amulet",
            "Gold Amulet",
            "A beautiful gold amulet.",
            ItemType.ARMOR,
            1,
            "assets/Items/Equipment/gold_amulet.png",
            false,
            null,
            EquipmentSlot.AMULET
        );
        ItemRegistry.register(goldAmulet);

        // Rings
        ItemDefinition rubyRing = new ItemDefinition(
            "ruby_ring",
            "Ruby Ring",
            "A ring with a glowing ruby gem.",
            ItemType.ARMOR,
            1,
            "assets/Items/Equipment/ruby_ring.png",
            false,
            null,
            EquipmentSlot.RING_1 // Can go in either ring slot
        );
        rubyRing.setLootModifier(new QuantityMultiplierModifier(1.5f));
        ItemRegistry.register(rubyRing);

        ItemDefinition emeraldRing = new ItemDefinition(
            "emerald_ring",
            "Emerald Ring",
            "A ring with a brilliant emerald.",
            ItemType.ARMOR,
            1,
            "assets/Items/Equipment/emerald_ring.png",
            false,
            null,
            EquipmentSlot.RING_2
        );
        ItemRegistry.register(emeraldRing);

        // ===== WEAPONS =====

        // Wooden Sword - Balanced arc swing weapon
        WeaponStats woodenSwordStats = new WeaponStats(
            WeaponType.SWORD,
            5,      // damage
            1.2f,   // attackSpeed (1.2 attacks per second)
            50f,    // range (pixels)
            500,   // knockback force
            0,  // windup duration
            0.4f,   // swing duration
            0.15f,  // recovery duration
            220f,   // swing arc (degrees)
            0.5f    // movement multiplier (50% speed while attacking)
        );
        ItemDefinition woodenSword = new ItemDefinition(
            "wooden_sword",
            "Wooden Sword",
            "A simple wooden training sword. Balanced attack speed and damage.",
            ItemType.WEAPON,
            1,
            "assets/Items/Weapons/Sword3/longsword.png",
            false,
            null,
            EquipmentSlot.WEAPON,
            woodenSwordStats,
            "assets/Items/Weapons/Sword3/longsword.png"
        );
        ItemRegistry.register(woodenSword);

        // Spear - Long range thrust weapon
        WeaponStats spearStats = new WeaponStats(
            WeaponType.SPEAR,
            4,      // damage (lower than sword)
            1.5f,   // attackSpeed (faster than sword)
            48f,    // range (much longer reach)
            60f,    // knockback (less than sword)
            0.0f,   // windup duration (quick)
            0.2f,   // swing duration (fast thrust)
            0.1f,   // recovery duration (quick recovery)
            30f,    // swing arc (narrow - thrust attack)
            0.7f    // movement multiplier (less penalty, can move better)
        );
        ItemDefinition spear = new ItemDefinition(
            "spear",
            "Wooden Spear",
            "A long spear with excellent reach. Fast thrust attacks.",
            ItemType.WEAPON,
            1,
            "assets/Items/Weapons/Lance/Sprite.png",
            false,
            null,
            EquipmentSlot.WEAPON,
            spearStats,
            "assets/Items/Weapons/Lance/Sprite.png"
        );
        ItemRegistry.register(spear);

        // War Hammer - Heavy slam weapon
        WeaponStats hammerStats = new WeaponStats(
            WeaponType.HAMMER,
            12,     // damage (very high)
            0.6f,   // attackSpeed (slow - 1 attack per 1.67 seconds)
            28f,    // range (short)
            200f,   // knockback (huge!)
            0.0f,   // windup duration (slow windup)
            0.4f,   // swing duration (heavy swing)
            0.3f,   // recovery duration (slow recovery)
            150f,   // swing arc (wide area)
            0.2f    // movement multiplier (very slow while attacking)
        );
        ItemDefinition hammer = new ItemDefinition(
            "war_hammer",
            "War Hammer",
            "A massive war hammer. Slow but devastating. Area-of-effect damage.",
            ItemType.WEAPON,
            1,
            "assets/Items/Weapons/Hammer/Sprite.png",
            false,
            null,
            EquipmentSlot.WEAPON,
            hammerStats,
            "assets/Items/Weapons/Hammer/SpriteInHand.png"
        );
        ItemRegistry.register(hammer);

        // Dagger - Fast, low damage weapon
        WeaponStats daggerStats = new WeaponStats(
            WeaponType.DAGGER,
            3,      // damage (low)
            2.5f,   // attackSpeed (very fast)
            20f,    // range (very short)
            40f,    // knockback (minimal)
            0f,  // windup duration (nearly instant)
            0.15f,  // swing duration
            0.05f,  // recovery duration (quick recovery)
            90f,    // swing arc
            0.8f    // movement multiplier (minimal penalty)
        );
        ItemDefinition dagger = new ItemDefinition(
            "dagger",
            "Iron Dagger",
            "A quick iron dagger. Fast attacks with low damage.",
            ItemType.WEAPON,
            1,
            "assets/Items/Weapons/Sword2/Sprite.png",
            false,
            null,
            EquipmentSlot.WEAPON,
            daggerStats,
            "assets/Items/Weapons/Sword2/SpriteInHand.png"
        );
        ItemRegistry.register(dagger);

        // ===== ENEMY WEAPONS =====

        // Claw Attack - Fast, short-range swipes for enemies
        WeaponStats clawStats = new WeaponStats(
            WeaponType.CLAW,
            1,      // damage (fast, low damage)
            1.5f,   // attackSpeed (1.5 attacks/sec - fast)
            55f,    // range (short - claws are close range)
            360f,    // knockback (light)
            0.3f,   // windup (quick)
            0.25f,   // swing duration (fast swipe)
            0f,   // recovery (quick)
            0f,    // swing arc (short arc - half of sword's 120°)
            0.6f    // movement multiplier (can move while swiping)
        );
        ItemDefinition claw = new ItemDefinition(
            "claw_attack",
            "Claw",
            "Sharp claws for swift strikes.",
            ItemType.WEAPON,
            1,
            null,  // NO ICON - enemy-only weapon, no inventory icon needed
            false,
            null,
            EquipmentSlot.WEAPON,
            clawStats,
            "assets/FX/SlashFx/Claw/SpriteSheet.png"  // 4-frame animation sprite
        );
        ItemRegistry.register(claw);

        System.out.println("Registered " + ItemRegistry.size() + " test items");
    }

    /**
     * Automatically loads textures for all registered items.
     * Reads the iconPath from each ItemDefinition and loads the texture.
     * @param worldItemManager The world item manager
     */
    public static void loadTextures(WorldItemManager worldItemManager) {
        int loadedCount = 0;
        int skippedCount = 0;

        // Load textures for all registered items
        for (ItemDefinition item : ItemRegistry.getAll()) {
            String iconPath = item.getIconPath();

            // Skip items without textures
            if (iconPath == null || iconPath.isEmpty()) {
                skippedCount++;
                continue;
            }

            try {
                // Load and register texture
                Texture texture = new Texture(Gdx.files.internal(iconPath));
                TextureRegion region = new TextureRegion(texture);
                worldItemManager.registerTexture(iconPath, region);
                loadedCount++;
            } catch (Exception e) {
                System.err.println("Failed to load texture for '" + item.getId() + "' at path: " + iconPath);
                System.err.println("  Error: " + e.getMessage());
            }
        }

        System.out.println("Loaded " + loadedCount + " item textures (" + skippedCount + " items without textures)");
    }
}
