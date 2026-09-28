# Multiplayer

Co-op LAN multiplayer over KryoNet. One player hosts (Esc → **Open to LAN**) and owns the world and the save file; others join from Main Menu → **Multiplayer**, then pick or create a character.

## Authority model

| Thing | Who decides | How others see it |
|---|---|---|
| A player's movement, animation, health | **The machine that player sits at** | `PlayerState` at 30 Hz → interpolated copy |
| Enemies, AI, damage, breakables, loot, world items | **Host** | `EntitySpawn` / `EntityStateBatch` (20 Hz) / `EntityDespawn` / `Effect` |
| What an attack hits | **Host** | The guest sends `AttackRequest`; the host resolves it with its copy of the guest |
| Hits on a guest | **Host** detects, **guest** applies | `PlayerHit` (damage + knockback) → the guest's own player |
| Picking up items | **Host** | `PickupRequest` → despawn for everyone + `ItemGrant` to the picker |
| Furniture placement and pickup | **Host** | `PlaceFurnitureRequest` → `PlaceFurnitureResult`; `PickUpFurnitureRequest` → `ItemGrant` |
| Chest contents | **Whoever holds the chest's lock** | `ChestOpenRequest` → `ChestOpenResult`; `ChestContents` on change; `ChestClose` |
| Guest inventory | **Guest**, saved by the host | `InventorySync` every 5 s and on leave → `SaveData.guestPlayers[characterId]` |

Players never get position corrections, so there is no rubber-banding. Other players and enemies render ~100 ms in the past (`Entity.INTERPOLATION_DELAY_MS`) from a snapshot buffer. `ClockSync` maps sender timestamps to local time, so packets that arrive in bursts still animate smoothly.

## Code map

- `networking/Packets.java`: every message, plus Kryo registration. **New packets go here** (register them; host and client use the same order).
- `networking/GameServer`, `GameClient`: thin KryoNet transports that push everything onto a `PacketQueue`. Nothing is handled on the network thread.
- `networking/HostSession`, `ClientSession` (both `NetSession`): all multiplayer logic, run on the game thread once per frame (`GameWorld.updateNetwork` → `session.update`). `NetSession` is null in single-player.
- `networking/NetGameContext`: what sessions need from the game. `world/GameWorld` implements it.
- `networking/ReplicatedEntities`: entity ⇄ spawn packet. **To replicate a new enemy or breakable type, add it here in both directions.** Furniture uses the type `furniture:<itemId>`.
- `networking/PlayerDataCodec`: player state and saved player data ⇄ packets / JSON.
- `networking/identity/`: `PlayerIdentity(provider, id, displayName)` and `IdentityProvider`. `LocalIdentityProvider` generates a UUID once per install (libGDX Preferences, one per `-Dgame.profile`). A Steam provider can be added later.
- `world/LevelInstanceFactory`: the only way to build a level. `AUTHORITATIVE` (host / single-player) adds breakables and furniture. `REPLICA` (client) builds only the map, collision and gateways; the host sends the rest.

## Joining: characters belong to the world (Stardew-style)

Guest characters are stored in the host's save, not per machine. Any guest can play any character nobody is currently playing, or create a new one.

1. Client → `Hello` (identity only).
2. Host → `CharacterList`: every guest character in `SaveManager.getGuestCharacters()`, each with `inUse` and `lastPlayedByYou`. Your characters come first, then the rest by name.
3. Client → `ChooseCharacter` (`characterId` for an existing one, or `newName` for a new one).
4. Host → `Welcome` (player ID, level, position, saved player JSON) + `LevelSnapshot`. If the choice is refused (the character is in use, the name is taken case-insensitively, or the name is empty), the host sends the `CharacterList` again with a `message` instead.

- Guests who are still choosing get a fresh list whenever someone joins or leaves.
- New characters get the ID `character:<uuid>` and are written to the save as soon as they join, so their name is reserved right away.
- Saves from before this system keyed characters by `local:<id>`, `name:<name>` or a plain name. Those still load as ordinary characters; a missing `displayName` falls back to the key after its last `:`.
- The identity is only used for `PlayerData.lastPlayedBy`.
- **Where a character starts:**
  - at its saved level and position, if that level can be shared;
  - at that level's spawn point, if the saved spot is blocked;
  - next to the host otherwise (new characters, unknown levels, dungeons);
  - at `Maps/prototype.tmx` if the host is in a dungeon.
- **Recording:** the host saves each character's level, position, name and `lastPlayedBy` on join, on leave, and when hosting stops. Inventory comes from `InventorySync`. Everything reaches disk only when the host saves (see `docs/saves.md`).
- **Client UI:** `ui/CharacterSelectDialog`, shown by `GameScreen` through `GameWorld.Presenter.chooseCharacter` until the local player exists. It calls `GameWorld.playCharacter(id)` / `createCharacter(name)`.

## Levels

- The host keeps a `LevelInstance` per visited level (Tiled maps are cached, so revisiting keeps the state). It simulates every level that has a player in it (`GameWorld.updateLevels`, `NetSession.isLevelOccupied`).
- Players can be in different levels. A guest loads the new level locally right away and sends `LevelChange(level, epoch)`. The host moves the guest's copy and replies with a `LevelSnapshot`.
- **Epochs:** every host→guest world packet carries the guest's level epoch, and the client drops mismatches. This stops late packets from the old level leaking into the new one.
- Replication is automatic: the host listens to `WorldManager` add/remove and `WorldItemManager` spawn/remove. Debug spawns, map objects, loot and furniture replicate without extra code.

## Furniture and chests

- Guests' placement requests are checked by the host (the floor must be free). The item leaves the guest's inventory only on `PlaceFurnitureResult(placed=true)`; otherwise they see "Can't place here" and stay in placement mode.
- **Chests are used by one player at a time.** The host keeps `chestLocks` (chest → player, the host included). The lock is released on close, level change, disconnect, or when the chest is removed. Only empty, unlocked chests can be picked up.
- `GameWorld` API: `requestPlaceFurniture`, `pickUpFurniture`, `openChest`, `closeChest`, plus the authoritative `placeFurniture` / `removeFurniture`.

## Known limitations

- Everything goes over TCP. Moving `PlayerState` and `EntityStateBatch` to UDP (`sendUDP`; the UDP port is already bound) would help on lossy Wi-Fi.
- Generated dungeons can't be shared (a guest can't rebuild them from a level ID).
- No dedicated start spot for new characters (they appear next to the host).
- No way to delete a guest character. No key or menu to pick furniture up (only `GameWorld.pickUpFurniture`).
- Player death isn't handled.
- A partial item pickup on the host doesn't update the quantity on clients.

## Tests

`./gradlew core:test`. Support code is in `core/src/test/java/com/game/testsupport`:

- `GdxTest` boots libGDX headless. `GameTestBase` gives fresh singletons and registries. `TestWorld` is one machine's `GameWorld` plus a `TestPresenter` (records what the simulation asked to show, including character lists).
- `ScriptedInput` drives the local player (move / attack).
- `MultiplayerRig`: a real host and clients on a private localhost port, stepped together and paced to real time.
  - `join(name)` connects and plays the character with that name, or creates it.
  - `connect(identity)` stops at the character list.
  - `runUntil(condition, description)` steps until the condition holds.
  - `savedCharacter(name)` finds a guest character in the save.
- Host and clients in one test JVM share singletons (`SaveManager`, `FurnitureManager`, `LootSystem`); only the host should initialise `LootSystem`.
- Main suites: `MultiplayerTest`, `GuestCharacterTest`, `SharedFurnitureTest`, `PacketTransportTest`, `ClockSyncTest`, `PlayerDataCodecTest`.
