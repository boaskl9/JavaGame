# Saves

`save/SaveManager` (singleton) writes libGDX `Json` to `Gdx.files.local("saves/")`, i.e. `saves/` under the working directory (the repo root in development):

- `saves/<name>.json`: the full `SaveData`.
- `saves/<name>.meta.json`: a small `SaveMetadata` (name, time, playtime, level, health) so the Load screen can list saves without reading whole files.

## When the game saves and loads

| Trigger | What happens |
|---|---|
| Main menu → New Game | Asks for a save name (`ui/NewGameDialog`); nothing is written yet |
| Pause menu → Return to Main Menu | Stops multiplayer, then saves under the current save name |
| F6 / F7 (debug) | Save / load the `debug_save` slot |
| Main menu → Continue | `loadMostRecent()` (newest timestamp) |
| Main menu → Load Save | `LoadGameScreen`: list, load, delete |

Nothing saves on a timer or when the window closes. `SaveManager.quickSave()` (an "autosave" slot) exists but isn't called anywhere.

## What's in a save

```
SaveData      version, saveName, timestamp, playtimeSeconds
├─ player     PlayerData: x, y, health, max health, InventoryData (default slots, bags, equipment)
├─ world      WorldData: currentLevelId, levelType,
│             furnitureByLevel (incl. chest contents), droppedItemsByLevel,
│             day, dayElapsed, worldSeed (see world/DayCycle; 0 in older saves)
└─ guestPlayers  Map<characterId, PlayerData>: multiplayer guest characters
                 (also displayName, levelId, lastPlayedBy; see docs/multiplayer.md)
```

Not saved, by design: dungeons (rebuilt from the world seed and the day, so after a reload today's dungeon has all its monsters back; saving inside one loads you at the start level), enemies, breakables (reset when a level is built), and today's dungeon lockouts.

## Loading order

`GameScreen` imports furniture **before** building levels, then enters the level, then `SaveManager.applySaveData` restores the player, inventory, dropped items and guest characters. `applySaveData` must not import furniture again (that was a bug: the world then held different objects from the ones being saved).

## Adding something to the save

1. Add public fields to a DTO in `save/` (it needs a no-argument constructor for `Json`). Missing fields load as null or 0, so older saves keep working.
2. Export it in `SaveManager.captureSaveData` and apply it in `applySaveData`.
3. Add a round-trip case to `core/src/test/java/com/game/save/SaveDataTest.java`.

`SaveData.version` is "1.0.0". There is no migration code yet; so far every change has been additive.
