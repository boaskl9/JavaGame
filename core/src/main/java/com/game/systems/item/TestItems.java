package com.game.systems.item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.game.integration.WorldItemManager;

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

        // Register bag item (simple bag for now)
        ItemDefinition bag = new ItemDefinition(
            "bag",
            "Traveler's Bag",
            "A simple bag for carrying items. Can hold 12 items.",
            ItemType.MISC,
            1, // Bags don't stack
            "assets/Items/Object/Bag.png",
            false
        );
        ItemRegistry.register(bag);

        // Register more test items as needed
        ItemDefinition stone = new ItemDefinition(
            "stone",
            "Stone",
            "A small stone. Useful for crafting.",
            ItemType.MATERIAL,
            99,
            null, // No texture for now
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
            null,
            true
        );
        ItemRegistry.register(potion);

        System.out.println("Registered " + ItemRegistry.size() + " test items");
    }

    /**
     * Loads and registers item textures.
     * @param worldItemManager The world item manager
     */
    public static void loadTextures(WorldItemManager worldItemManager) {
        try {
            // Load wood texture
            Texture branchTexture = new Texture(Gdx.files.internal("assets/Items/Resource/Branch.png"));
            TextureRegion branchRegion = new TextureRegion(branchTexture);
            worldItemManager.registerTexture("assets/Items/Resource/Branch.png", branchRegion);

            // Load bag texture
            Texture bagTexture = new Texture(Gdx.files.internal("assets/Items/Object/Bag.png"));
            TextureRegion bagRegion = new TextureRegion(bagTexture);
            worldItemManager.registerTexture("assets/Items/Object/Bag.png", bagRegion);

            System.out.println("Loaded item textures");
        } catch (Exception e) {
            System.err.println("Error loading item textures: " + e.getMessage());
        }
    }
}
