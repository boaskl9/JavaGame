package com.game.systems.item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.game.integration.WorldItemManager;
import com.game.systems.inventory.EquipmentSlot;

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

        // Register more test items as needed
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

        // Weapon
        ItemDefinition woodenSword = new ItemDefinition(
            "wooden_sword",
            "Wooden Sword",
            "A simple wooden training sword.",
            ItemType.WEAPON,
            1,
            "assets/Items/Weapons/Sword/Sprite.png",
            false,
            null,
            EquipmentSlot.WEAPON
        );
        ItemRegistry.register(woodenSword);

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
