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
- `networking/NetGameContext`: what sessions need from the game (implemented by `GameScreen`).
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

## Testing on one machine

Start the game twice. Instance A: New Game → Esc → Open to LAN. Instance B: Multiplayer → `localhost`.
Use `-Dgame.port=NNNN` on both to run a session on a port other than 25565.
