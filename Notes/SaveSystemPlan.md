# Save System & Main Menu Implementation Plan

## Requirements Summary
- ✅ Unlimited save slots with custom names
- ✅ Continue button (loads most recent save)
- ✅ Save dropped items per level
- ✅ Dungeons are temporary (don't save, regenerate on re-entry)
- ✅ JSON format using LibGDX's built-in Json class
- ✅ Cross-platform save location via `Gdx.files.local()`

## Phase 1: Save Data Structure (Foundation)

### Files to Create:
1. `com.game.save.SaveData` - Root save data container
2. `com.game.save.PlayerData` - Player state
3. `com.game.save.InventoryData` - Inventory contents
4. `com.game.save.WorldData` - World state
5. `com.game.save.FurnitureData` - Placed furniture
6. `com.game.save.ItemStackData` - Serializable ItemStack
7. `com.game.save.SaveMetadata` - Save file metadata

### Data Structure:
```
SaveData {
  - String version
  - String saveName
  - long timestamp
  - int playtimeSeconds
  - PlayerData player
  - WorldData world
  - SaveMetadata metadata
}

PlayerData {
  - float x, y
  - int currentHealth, maxHealth
  - InventoryData inventory
}

InventoryData {
  - List<ItemStackData> defaultSlots (20 slots)
  - List<BagData> bags (3 slots, can be null)
  - Map<String, ItemStackData> equipment (6 slots by EquipmentSlot name)
}

BagData {
  - int slotIndex
  - String bagItemId
  - List<ItemStackData> contents
}

ItemStackData {
  - String itemId
  - int quantity
}

WorldData {
  - String currentLevelId
  - String levelType ("tiled_map" only, skip dungeons)
  - Map<String, List<FurnitureData>> furnitureByLevel
  - Map<String, List<DroppedItemData>> droppedItemsByLevel
}

FurnitureData {
  - String itemId
  - float x, y
  - List<ItemStackData> inventoryContents (for chests, null for non-container furniture)
}

DroppedItemData {
  - String itemId
  - int quantity
  - float x, y
}

SaveMetadata {
  - String saveName
  - long timestamp
  - int playtimeSeconds
  - String currentLevelId
  - int playerHealth
}
```

## Phase 2: Save Manager (Core Logic)

### File: SaveManager.java
```java
public class SaveManager {
    private static SaveManager instance;

    // Save operations
    public void save(String saveName) throws IOException
    public void quickSave() // Save to "autosave"

    // Load operations
    public SaveData load(String saveName) throws IOException
    public SaveData loadMostRecent() // For "Continue" button

    // Save file management
    public List<SaveMetadata> listSaves()
    public void deleteSave(String saveName)
    public boolean saveExists(String saveName)

    // Internal
    private SaveData captureSaveData()
    private void applySaveData(SaveData data, GameScreen gameScreen)
}
```

### Key Implementation Details:
- Use LibGDX `Json` for serialization
- Save location: `Gdx.files.local("saves/<saveName>.json")`
- Metadata location: `Gdx.files.local("saves/<saveName>.meta.json")`
- Sort saves by timestamp for "most recent"

### Files to Modify:
1. `FurnitureManager.java` - Add export/import methods
2. `WorldItemManager.java` - Add dropped item persistence
3. `PlayerInventory.java` - Add export/import methods
4. `PlayerEntity.java` - Add loadFromSaveData()

## Phase 3: GameScreen Integration

### Changes to GameScreen.java:
1. Add constructor: `GameScreen(SaveData saveData)`
2. Add method: `loadFromSaveData(SaveData data)`
3. Track playtime (accumulate delta)
4. Modify `loadLevel()` to load furniture/dropped items from SaveData

### Dungeon Handling:
- Don't save dungeon state
- When leaving dungeon → discard furniture/items
- When re-entering → fresh generation

## Phase 4: Main Menu Screen

### MainMenuScreen.java
```
┌─────────────────────────────┐
│      GAME TITLE             │
│                             │
│  [ Continue ]               │  ← Only if saves exist
│  [ New Game ]               │
│  [ Load Game ]              │
│  [ Settings ]               │
│  [ Exit ]                   │
└─────────────────────────────┘
```

### LoadGameScreen.java
```
┌─────────────────────────────────────────┐
│  Load Game                        [Back]│
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ Save: "My Playthrough"          │   │
│  │ Level: prototype.tmx            │   │
│  │ Playtime: 1h 30m                │   │
│  │ Last Played: 2024-11-22 10:30   │   │
│  │           [Load] [Delete]       │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### NewGameDialog.java
- Text input for save name
- Default names: "Save 1", "Save 2", etc.
- Validation: no empty names, warn on overwrite

### Changes to Main.java:
```java
@Override
public void create() {
    setScreen(new MainMenuScreen(this)); // Instead of GameScreen
}
```

## Phase 5: In-Game Save/Load

### Changes to SettingsMenu.java:
- Add "Save Game" button
- Add "Load Game" button (return to main menu)
- Add "Return to Main Menu" button

### Optional Hotkeys:
- F5: Quick save
- F9: Quick load

## Phase 6: Testing & Polish

### Test Cases:
1. New game → save → load → verify state
2. Multiple saves → verify each loads correctly
3. Place furniture → save → load → verify persistence
4. Drop items → save → load → verify persistence
5. Enter dungeon → place furniture → leave → re-enter → verify fresh dungeon
6. Edge cases: corrupt saves, missing items

### Error Handling:
- IOException during save/load → error dialog
- Save version validation
- Missing item IDs → skip gracefully

## Implementation Order

1. **START HERE** - Phase 1: Create SaveData DTOs (pure data classes)
2. Phase 2: SaveManager with JSON serialization
3. Phase 3: Add export/import to game systems
4. Phase 3: Modify GameScreen for SaveData
5. Test save/load via console
6. Phase 4: MainMenuScreen (basic New Game)
7. Phase 4: Continue button
8. Phase 4: LoadGameScreen
9. Phase 5: In-game save/load
10. Phase 6: Testing & polish

## Files Summary

### New Files (11):
1. `com.game.save.SaveData`
2. `com.game.save.PlayerData`
3. `com.game.save.InventoryData`
4. `com.game.save.WorldData`
5. `com.game.save.FurnitureData`
6. `com.game.save.ItemStackData`
7. `com.game.save.SaveMetadata`
8. `com.game.save.SaveManager`
9. `com.game.main.MainMenuScreen`
10. `com.game.main.LoadGameScreen`
11. `com.game.ui.NewGameDialog`

### Files to Modify (7):
1. `Main.java` - Start with MainMenuScreen
2. `GameScreen.java` - SaveData constructor
3. `PlayerEntity.java` - loadFromSaveData()
4. `PlayerInventory.java` - import/export
5. `FurnitureManager.java` - import/export
6. `WorldItemManager.java` - dropped item persistence
7. `SettingsMenu.java` - save/load buttons
