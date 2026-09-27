# Multiplayer Architecture

Co-op LAN multiplayer: one player hosts ("Open to LAN" in the pause menu) and owns the world and the save file; others join from Main Menu → Multiplayer.

## Authority model

| Thing | Who decides | How others see it |
|---|---|---|
| A player's movement, animation, health | **The machine that player sits at** | `PlayerState` at 30Hz → interpolated copy |
| Enemies, AI, damage, breakables, loot, world items | **Host** | `EntitySpawn` / `EntityStateBatch` (20Hz) / `EntityDespawn` / `Effect` |
| What an attack hits | **Host** | A guest sends `AttackRequest`; the host resolves it with the guest's copy |
| Hits on a guest | **Host** detects, **guest** applies | `PlayerHit` (damage + knockback) → the guest's own player |
| Picking up items | **Host** | `PickupRequest` → despawn for all + `ItemGrant` to the picker |
| Guest inventory | **Guest**, saved by the host | `InventorySync` every 5s and on leave → `SaveData.guestPlayers[name]` |

Players never get position corrections, so there is no rubber-banding. Other players and enemies render ~100ms in the past (`Entity.INTERPOLATION_DELAY_MS`) using the existing snapshot buffer. Sender timestamps are mapped to local time by `ClockSync`, so packets that arrive in bursts still animate smoothly.

## Code map

- `networking/Packets.java`: every message (and Kryo registration). Add new packets here.
- `networking/GameServer`, `GameClient`: thin KryoNet transports that push everything into a `PacketQueue`.
- `networking/HostSession`, `ClientSession` (both `NetSession`): all multiplayer logic, run on the game thread once per frame (`GameScreen.render` → `session.update`).
- `networking/ReplicatedEntities`: entity ⇄ spawn packet. **To replicate a new enemy/breakable type, add it here in both directions.**
- `world/GameWorld`: the simulation on one machine, without rendering or UI. It covers loaded levels, the local player, other players' copies, pickups, gateways, and starting/joining a session. It implements `NetGameContext` (what sessions need from the game). `GameScreen` owns one and draws it through the `GameWorld.Presenter` interface; tests run it headless.
- `world/LevelInstance`, `LevelInstanceFactory`: the only way to build a level. `AUTHORITATIVE` (host/single-player) populates breakables and furniture. `REPLICA` (client) builds only the map, collision and gateways; the host sends the rest.

## Levels

- The host keeps a `LevelInstance` per visited level (Tiled maps are cached, so revisiting preserves state). It simulates every level that has any player in it (`GameScreen.updateLevels`).
- Players can be in different levels. A guest loads the new level locally right away and sends `LevelChange(level, epoch)`. The host moves the guest's copy and replies with a `LevelSnapshot`.
- **Epochs:** every host→guest world packet carries the guest's level epoch; the client drops mismatches. This stops late packets from the old level leaking into the new one.
- Replication is automatic: the host listens to `WorldManager` add/remove and `WorldItemManager` spawn/remove, so debug spawns, map objects and loot all replicate without extra code.

## Known limitations / next steps

- Everything goes over TCP. Moving `PlayerState` and `EntityStateBatch` to UDP (`sendUDP`; the ports are already bound) would help on lossy Wi-Fi.
- Furniture is host-only (not replicated); guests can't place it.
- Generated dungeons can't be shared (a guest can't rebuild them from a level ID). Guests joining while the host is in a dungeon start in `Maps/prototype.tmx`.
- Player death isn't handled (pre-existing).
- A partial item pickup on the host doesn't update the quantity on clients.

## Roadmap (branch `feature/multiplayer`)

Each phase ends with passing tests (`./gradlew test`) plus a two-window check.

### Phase 0: test foundation ✅
- JUnit 5 + Mockito + libGDX headless in `core`; run with `./gradlew test` (~7s, 50 tests).
- No asset refactor needed: the headless backend plus a mocked GL loads real textures and maps.
- Game logic was moved out of `GameScreen` into `GameWorld`, so tests drive the real code.
- `MultiplayerTest` runs a real host + client over localhost (`MultiplayerRig`, paced to real time).
- Also covered: packets, clock sync, player data codec, world items, level instances, single-player `GameWorld`, saves, inventory, seeded dungeon generation.
- Bug found by the tests and fixed: enemies only started attacks when frame times varied (`ai.getStateTimer() < delta`); with a steady frame rate they often never attacked.

### Phase 1: guest characters continue where they left off ✅
- **Identity:** `networking/identity/`.
  - `PlayerIdentity(provider, id, displayName)`; its key `provider:id` is where the host saves the character.
  - `IdentityProvider` supplies the identity. `LocalIdentityProvider` generates a UUID once per install and keeps it in libGDX Preferences. Run with `-Dgame.profile=NAME` to get a separate ID (e.g. a second copy of the game on the same PC).
  - A Steam provider can be added later without changing the save format.
- `Hello` carries provider, ID and display name. Clients that send no identity are keyed `name:<displayName>`.
- `PlayerData` has `levelId` and `displayName`. On join, the host restores the saved level and position:
  - blocked spot → that level's spawn point;
  - unknown level or dungeon → join the host.
- On leave, and when the host stops hosting, the host records the guest's latest level and position from its copy of their player. Inventory comes from the guest's last `InventorySync` (every 5s and on leave).
- If the same identity is already connected, the extra connection gets its own save key (`key#2`).
- Legacy characters saved under a plain display name are moved to the new key the first time someone joins with that name.
- Guest data only reaches disk when the host saves (the menu autosave, or F6).
- Tests: `GuestCharacterTest`, `LocalIdentityProviderTest`.

### Phase 2: shared furniture ✅
- Furniture replicates like breakables (`ReplicatedEntities`, type `furniture:<itemId>`), so guests see it and collide with it.
- `systems/furniture/FurnitureFactory` is the single place that turns item IDs into furniture. It also converts chest contents for saves and packets.
- `GameWorld` furniture API: `requestPlaceFurniture`, `pickUpFurniture`, `openChest`, `closeChest`, plus authoritative `placeFurniture` / `removeFurniture`.
- **Placing:** a guest's request is checked by the host (floor must be free). The item leaves the inventory only on `PlaceFurnitureResult(placed=true)`; otherwise the guest sees "Can't place here" and stays in placement mode.
- **Chests, one player at a time:** the host keeps `chestLocks` (chest → player, the host included).
  - Opening asks for the lock and receives the contents; otherwise the player sees "In use".
  - The holder's changes are sent to the host whenever the chest's contents change.
  - The lock is released on close, level change, disconnect, or the chest being removed.
- **Picking up:** only empty chests, and not while someone else is using them. Guests get the item back via `ItemGrant`.
  - ⚠️ There is still no key or menu option to pick furniture up; only `GameWorld.pickUpFurniture` exists (this was already true before).
- Fixed: loading a save imported furniture twice, so the world's objects weren't the ones being saved. `SaveManager.applySaveData` no longer imports furniture; it is imported once, before levels are built.
- Tests: `FurnitureTest` (single-player), `SharedFurnitureTest` (host + guest). The placement preview and chest window UI are checked only by compiling, not by tests.

## Tests

`./gradlew test` (or `core:test`). Support code lives in `core/src/test/java/com/game/testsupport`:
- `GdxTest`: boots libGDX headless.
- `GameTestBase`: fresh singletons and registries; `TestWorld` is one machine's `GameWorld` plus a `TestPresenter`.
- `ScriptedInput`: drive the local player (move / attack).
- `MultiplayerRig`: host + clients on a private port; `runUntil(condition, description)` steps every machine until the condition holds.

Note that the host and clients in one test JVM share singletons (`SaveManager`, `FurnitureManager`, `LootSystem`). Only the host should initialise `LootSystem`.

## Testing on one machine

Start the game twice. Instance A: New Game → Esc → Open to LAN. Instance B: Multiplayer → `localhost`.
Use `-Dgame.port=NNNN` on both to run a session on a port other than 25565.
