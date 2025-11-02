package com.game.systems.breakable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for breakable object configurations loaded from JSON.
 * Similar pattern to ItemRegistry - loads configs on startup and provides static access.
 */
public class BreakableObjectRegistry {
    private static final Map<String, BreakableConfig> configs = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Loads all breakable object configurations from JSON file.
     * Call this once during game initialization.
     */
    public static void loadConfigs() {
        if (initialized) {
            System.out.println("BreakableObjectRegistry already initialized");
            return;
        }

        try {
            FileHandle configFile = Gdx.files.internal("assets/data/BreakableObjectConfig.json");
            if (!configFile.exists()) {
                System.err.println("BreakableObjectConfig.json not found!");
                return;
            }

            String jsonText = configFile.readString();
            Json json = new Json();

            // Parse root JSON object
            JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(jsonText);

            // Iterate through each object type (pot, crate, etc.)
            for (JsonValue entry = root.child; entry != null; entry = entry.next) {
                String objectType = entry.name;
                BreakableConfig config = parseConfig(entry);
                config.objectType = objectType;

                configs.put(objectType, config);
                System.out.println("Loaded breakable object config: " + objectType);
            }

            initialized = true;
            System.out.println("BreakableObjectRegistry initialized with " + configs.size() + " object types");

        } catch (Exception e) {
            System.err.println("Failed to load BreakableObjectConfig.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parses a single breakable object configuration from JSON.
     */
    private static BreakableConfig parseConfig(JsonValue json) {
        BreakableConfig config = new BreakableConfig();

        config.name = json.getString("name", "Unknown Object");
        config.health = json.getInt("health", 1);
        config.spritePath = json.getString("spritePath");
        config.breakAnimationPath = json.getString("breakAnimationPath");
        config.breakAnimationFrames = json.getInt("breakAnimationFrames", 1);
        config.breakAnimationSpeed = json.getFloat("breakAnimationSpeed", 0.1f);
        config.particleColor = json.getString("particleColor", "white");

        // Parse loot table
        JsonValue lootArray = json.get("lootTable");
        if (lootArray != null && lootArray.isArray()) {
            for (JsonValue lootEntry = lootArray.child; lootEntry != null; lootEntry = lootEntry.next) {
                LootEntry entry = new LootEntry();
                entry.itemId = lootEntry.getString("itemId");
                entry.chance = lootEntry.getFloat("chance", 1.0f);
                entry.minQty = lootEntry.getInt("minQty", 1);
                entry.maxQty = lootEntry.getInt("maxQty", 1);

                config.lootTable.add(entry);
            }
        }

        return config;
    }

    /**
     * Gets a breakable object configuration by type.
     * @param objectType The object type (e.g., "pot", "crate")
     * @return The configuration, or null if not found
     */
    public static BreakableConfig get(String objectType) {
        if (!initialized) {
            System.err.println("BreakableObjectRegistry not initialized! Call loadConfigs() first.");
            return null;
        }
        return configs.get(objectType);
    }

    /**
     * Checks if a breakable object type exists.
     */
    public static boolean has(String objectType) {
        return configs.containsKey(objectType);
    }

    /**
     * Configuration data for a breakable object type.
     */
    public static class BreakableConfig {
        public String objectType;           // Type identifier (pot, crate, etc.)
        public String name;                 // Display name
        public int health;                  // Hit points
        public String spritePath;           // Path to idle sprite
        public String breakAnimationPath;   // Path to break animation sprite sheet
        public int breakAnimationFrames;    // Number of frames in break animation
        public float breakAnimationSpeed;   // Seconds per frame
        public String particleColor;        // Color hint for particles
        public java.util.List<LootEntry> lootTable = new java.util.ArrayList<>();

        /**
         * Gets the total duration of the break animation.
         */
        public float getBreakAnimationDuration() {
            return breakAnimationFrames * breakAnimationSpeed;
        }
    }

    /**
     * Represents a single loot drop entry.
     */
    public static class LootEntry {
        public String itemId;
        public float chance;
        public int minQty;
        public int maxQty;
    }
}
