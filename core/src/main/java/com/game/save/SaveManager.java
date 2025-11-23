package com.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.furniture.FurnitureManager;
import com.game.integration.WorldItemManager;
import com.game.systems.inventory.PlayerInventory;

import java.util.*;

/**
 * Singleton manager for saving and loading game state.
 * Uses LibGDX's Json class to serialize SaveData to/from JSON files.
 */
public class SaveManager {
    private static SaveManager instance;
    private static final String SAVE_DIR = "saves/";
    private static final String SAVE_EXTENSION = ".json";
    private static final String META_EXTENSION = ".meta.json";

    // References to game systems (set before saving/loading)
    private PlayerEntity player;
    private PlayerInventory playerInventory;
    private FurnitureManager furnitureManager;
    private WorldItemManager worldItemManager;
    private String currentLevelId;
    private String currentLevelType; // "tiled_map" or "dungeon"
    private int playtimeSeconds = 0;

    private SaveManager() {
    }

    public static SaveManager getInstance() {
        if (instance == null) {
            instance = new SaveManager();
        }
        return instance;
    }

    /**
     * Initialize with game system references.
     * Call this from GameScreen after all systems are created.
     */
    public void initialize(PlayerEntity player, PlayerInventory inventory,
                          FurnitureManager furnitureManager, WorldItemManager worldItemManager) {
        this.player = player;
        this.playerInventory = inventory;
        this.furnitureManager = furnitureManager;
        this.worldItemManager = worldItemManager;
    }

    /**
     * Update current level information.
     * Call this whenever the player changes levels.
     */
    public void setCurrentLevel(String levelId, String levelType) {
        this.currentLevelId = levelId;
        this.currentLevelType = levelType;
    }

    /**
     * Update playtime tracker.
     * Call this from GameScreen.update(delta).
     */
    public void updatePlaytime(float deltaSeconds) {
        playtimeSeconds += (int) deltaSeconds;
    }

    /**
     * Reset playtime for new game.
     */
    public void resetPlaytime() {
        playtimeSeconds = 0;
    }

    /**
     * Save current game state to a named save file.
     */
    public void save(String saveName) {
        if (player == null || playerInventory == null) {
            System.err.println("SaveManager: Cannot save - systems not initialized");
            return;
        }

        try {
            // Capture current game state
            SaveData saveData = captureSaveData(saveName);

            // Serialize to JSON
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json); // Pretty print
            String jsonString = json.toJson(saveData);

            // Write to file
            FileHandle saveFile = Gdx.files.local(SAVE_DIR + saveName + SAVE_EXTENSION);
            saveFile.writeString(jsonString, false);

            // Write metadata separately for fast listing
            SaveMetadata metadata = saveData.extractMetadata();
            String metaJson = json.toJson(metadata);
            FileHandle metaFile = Gdx.files.local(SAVE_DIR + saveName + META_EXTENSION);
            metaFile.writeString(metaJson, false);

            System.out.println("SaveManager: Saved game to " + saveFile.path());
        } catch (Exception e) {
            System.err.println("SaveManager: Failed to save game");
            e.printStackTrace();
        }
    }

    /**
     * Quick save to "autosave" slot.
     */
    public void quickSave() {
        save("autosave");
    }

    /**
     * Load game state from a named save file.
     */
    public SaveData load(String saveName) {
        try {
            FileHandle saveFile = Gdx.files.local(SAVE_DIR + saveName + SAVE_EXTENSION);
            if (!saveFile.exists()) {
                System.err.println("SaveManager: Save file not found: " + saveName);
                return null;
            }

            String jsonString = saveFile.readString();
            Json json = new Json();
            SaveData saveData = json.fromJson(SaveData.class, jsonString);

            System.out.println("SaveManager: Loaded save '" + saveName + "'");
            return saveData;
        } catch (Exception e) {
            System.err.println("SaveManager: Failed to load save '" + saveName + "'");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load the most recently saved game.
     * Used for "Continue" button.
     */
    public SaveData loadMostRecent() {
        List<SaveMetadata> saves = listSaves();
        if (saves.isEmpty()) {
            System.out.println("SaveManager: No saves found");
            return null;
        }

        // Saves are already sorted by timestamp (most recent first)
        String mostRecentName = saves.get(0).saveName;
        return load(mostRecentName);
    }

    /**
     * List all available save files with metadata.
     * Returns list sorted by timestamp (most recent first).
     */
    public List<SaveMetadata> listSaves() {
        List<SaveMetadata> saves = new ArrayList<>();
        FileHandle saveDir = Gdx.files.local(SAVE_DIR);

        if (!saveDir.exists()) {
            return saves; // Empty list
        }

        Json json = new Json();
        for (FileHandle file : saveDir.list(META_EXTENSION)) {
            try {
                String metaJson = file.readString();
                SaveMetadata metadata = json.fromJson(SaveMetadata.class, metaJson);
                saves.add(metadata);
            } catch (Exception e) {
                System.err.println("SaveManager: Failed to read metadata from " + file.name());
            }
        }

        // Sort by timestamp (most recent first)
        saves.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return saves;
    }

    /**
     * Check if a save file exists.
     */
    public boolean saveExists(String saveName) {
        FileHandle saveFile = Gdx.files.local(SAVE_DIR + saveName + SAVE_EXTENSION);
        return saveFile.exists();
    }

    /**
     * Delete a save file.
     */
    public void deleteSave(String saveName) {
        FileHandle saveFile = Gdx.files.local(SAVE_DIR + saveName + SAVE_EXTENSION);
        FileHandle metaFile = Gdx.files.local(SAVE_DIR + saveName + META_EXTENSION);

        if (saveFile.exists()) {
            saveFile.delete();
        }
        if (metaFile.exists()) {
            metaFile.delete();
        }

        System.out.println("SaveManager: Deleted save '" + saveName + "'");
    }

    /**
     * Capture current game state into a SaveData object.
     */
    private SaveData captureSaveData(String saveName) {
        // TODO: Implement actual data capture from game systems
        // For now, create placeholder structure

        // Capture player data
        PlayerData playerData = capturePlayerData();

        // Capture world data
        WorldData worldData = captureWorldData();

        return new SaveData(saveName, playtimeSeconds, playerData, worldData);
    }

    /**
     * Capture player state.
     */
    private PlayerData capturePlayerData() {
        float x = player.getTransform().getX();
        float y = player.getTransform().getY();
        int currentHealth = player.getHealthComponent().getCurrentHealth();
        int maxHealth = player.getHealthComponent().getMaxHealth();

        // Capture inventory data
        InventoryData inventoryData = playerInventory.exportSaveData();

        return new PlayerData(x, y, currentHealth, maxHealth, inventoryData);
    }

    /**
     * Capture world state.
     */
    private WorldData captureWorldData() {
        // Capture furniture from FurnitureManager
        Map<String, List<FurnitureData>> furnitureByLevel = furnitureManager.exportSaveData();

        // Capture dropped items from WorldItemManager
        Map<String, List<DroppedItemData>> droppedItemsByLevel = worldItemManager.exportSaveData();

        return new WorldData(currentLevelId, currentLevelType, furnitureByLevel, droppedItemsByLevel);
    }

    public int getPlaytimeSeconds() {
        return playtimeSeconds;
    }

    public void setPlaytimeSeconds(int playtimeSeconds) {
        this.playtimeSeconds = playtimeSeconds;
    }

    /**
     * Apply loaded save data to the game.
     * Restores player state, inventory, furniture, and dropped items.
     * Call this after loading SaveData to restore game state.
     */
    public void applySaveData(SaveData saveData) {
        if (saveData == null) {
            System.err.println("SaveManager: Cannot apply null save data");
            return;
        }

        // Restore playtime
        this.playtimeSeconds = saveData.playtimeSeconds;

        // Restore player state
        if (player != null && saveData.player != null) {
            player.loadFromSaveData(saveData.player);
        }

        // Restore inventory
        if (playerInventory != null && saveData.player != null && saveData.player.inventory != null) {
            playerInventory.importSaveData(saveData.player.inventory);
        }

        // Restore furniture
        if (furnitureManager != null && saveData.world != null && saveData.world.furnitureByLevel != null) {
            furnitureManager.importSaveData(saveData.world.furnitureByLevel);
        }

        // Restore dropped items
        if (worldItemManager != null && saveData.world != null && saveData.world.droppedItemsByLevel != null) {
            worldItemManager.importSaveData(saveData.world.droppedItemsByLevel);
        }

        // Update current level info
        if (saveData.world != null) {
            this.currentLevelId = saveData.world.currentLevelId;
            this.currentLevelType = saveData.world.levelType;
        }

        System.out.println("SaveManager: Applied save data '" + saveData.saveName + "'");
    }
}
