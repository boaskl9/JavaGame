# JavaGame: guide for agents

Top-down action RPG (Stardew Valley-style view, but no farming): explore, fight through dungeons, collect gear and perks whose effects stack together. libGDX + Tiled + Scene2D, Java 17, co-op LAN multiplayer. Early development: the core systems work, but there's little content yet.

`Notes/JavaGame/` is the owner's personal Obsidian vault (design ideas, task board). **Never edit it.** Read it for design intent if useful. Agent-facing documentation lives in `docs/`.

## Build, run, test

- **Tests:** `./gradlew core:test` (JUnit 5, headless libGDX, ~40s, 79 tests). Run them after any gameplay or networking change.
- **Run:** main class `com.game.main.lwjgl3.Lwjgl3Launcher`. Asset paths in code are `assets/...` and saves go to `saves/`, so the working directory must be the **repo root**. (`lwjgl3:run` sets it to `assets/`, which doesn't match those paths.)
- **Two instances on one PC:** instance A: New Game → Esc → Open to LAN. Instance B: Multiplayer → `localhost`. JVM flags: `-Dgame.profile=NAME` gives a separate local identity, and `-Dgame.port=NNNN` changes the port (default 25565, UDP 25566).
- Windows machine. The Bash tool is Git Bash; `gradlew` works from it.

## Architecture in one screen

```
Main → MainMenuScreen → GameScreen (render + UI only)
                            └── world/GameWorld (the simulation, headless-testable)
                                   ├── LevelInstance per loaded level (built only via LevelInstanceFactory)
                                   │      └── integration/WorldManager (GameObjects, collision queries)
                                   ├── integration/WorldItemManager (dropped items, per level)
                                   ├── systems/entity/PlayerManager (local + remote players)
                                   └── networking/NetSession (HostSession | ClientSession | null)
```

- `GameWorld` owns levels, the local player, other players' copies, pickups and gateways. It talks to the screen through `GameWorld.Presenter`; tests use `TestPresenter`. **Put new game logic in `GameWorld` (or a system it calls), not in `GameScreen`**, so it can be tested.
- Entities: `GameObject` (holds components) → `Entity` (adds health) → `PlayerEntity`, `EnemyEntity`, `BreakableEntity`, `ItemPickupEntity`, `GatewayEntity`, `NPC` (stub). Components live in `components/` plus `systems/entity/Transform`.
- Singletons: `SaveManager`, `SoundSystem`, `LootSystem`, `FurnitureManager`, `GameSettings`, `DungeonThemeRegistry`. They're registered with `util/SingletonManager` for resets between sessions. In tests the host and clients share them.

| Package | What's there |
|---|---|
| `main/` | Screens: `GameScreen`, `MainMenuScreen`, `LoadGameScreen`, `Main` |
| `world/` | `GameWorld`, `LevelInstance(Factory)` |
| `networking/` | Multiplayer. Read `docs/multiplayer.md` first |
| `integration/` | `WorldManager`, `WorldItemManager` (glue between systems) |
| `rendering/` | `YSortRenderer` (depth sorting with Tiled layers) |
| `save/` | `SaveManager` + plain JSON DTOs |
| `systems/combat` | `AttackSystem`, `WeaponStats`/`WeaponType`, attack strategies (arc, thrust, slam, stab) |
| `systems/entity` | Entity base classes, `PlayerManager`, enemies in `entities/enemies/` |
| `systems/item`, `inventory`, `loot` | Items, bags, equipment, loot tables (see `docs/items-inventory-loot.md`) |
| `systems/furniture` | Placeable furniture + chests (`FurnitureFactory` turns item IDs into furniture) |
| `systems/dungeon` | Procedural dungeons (work in progress, see below) |
| `systems/level`, `collision`, `pathfinding` | Tiled parsing, `SpatialQuery`, `GridPathfinder` (A*) |
| `systems/ui` | In-game Scene2D UI: `UIManagerNew`, HUD, inventory/equipment windows |
| `ui/` | Menu dialogs: new game, connect, character select, pause, errors |
| `systems/debug` | `DebugConsole` (F4), `DebugManager` (debug overlay flags) |

## Rules that bite

- **Coordinates are libGDX Y-up** (origin bottom-left; the Tiled loader flips map Y). Tiles are 16×16 px. Game viewport 640×360; UI stages use `ScreenViewport`.
- **Multiplayer:** each machine owns its own player's movement; the host owns everything else. New enemy or breakable types must be added to `networking/ReplicatedEntities` in both directions, or guests won't see them.
- **Levels** are built only through `LevelInstanceFactory` (`AUTHORITATIVE` for host/single-player, `REPLICA` for clients).
- Health uses quarter hearts: 4 HP = 1 heart.
- UI skin: `assets/ui/wood-theme.json`. It has disabled styles for buttons. Use `ChangeListener` (not `ClickListener`) when a disabled button must not fire.
- Don't read `.tmx` files (large and not useful to read); use `TiledMapParser` or the level code instead.
- Items are registered in code in `systems/item/TestItems.java` (IDs like `wood`, `stone`, `coin`, `bag`, `wooden_chest`, `wooden_sword`, `health_potion`). Breakables are configured in `assets/data/BreakableObjectConfig.json`.

## Controls and debug tools

Game: WASD move, left click attack, E interact, B inventory, C equipment + inventory, Esc pause menu (Settings, Open to LAN, Return to Main Menu, which autosaves).

Debug: F3 overlay, which enables number keys 1–9 to spawn items, bags, enemies and pots at the mouse (C also spawns a chest while F3 is on, the same key as the equipment window). F4 console. F5 item browser. F6/F7 save/load `debug_save`. `+`/`-`/`0` change the time scale.

Console commands (F4): `help`, `clear`, `spawn`, `damage`, `heal`, `setmaxhealth`, `debug <colliders|navmesh|fps|all|none>`, `items`, `timescale`, and `dungeon <info|generate|load|load_generated|test>`. See `DebugConsole.java` for arguments.

## State of things

Working: movement, combat (8 weapon types), enemy AI with pathfinding, health, inventory with bags, equipment slots, loot tables, breakables, furniture and chests, audio, saves with a main menu, co-op multiplayer (shared world, furniture, guest characters picked Stardew-style).

Not done or partial:
- Equipment gives no stat bonuses. It can supply loot modifiers (`ItemDefinition.getLootModifier`).
- There's no enemy spawn system: enemies only come from debug spawns (number keys, `/spawn`). Maps place breakables and gateways, not enemies. NPCs are a stub. No shops, projectiles, day/night cycle or death handling.
- Dungeons: generation works (`/dungeon test` opens `DungeonTestScreen`), but loading generated dungeons into play is rough, and they can't be shared in multiplayer.
- Furniture can't be picked up from the UI (only `GameWorld.pickUpFurniture` exists).
- A new multiplayer character starts where the host stands (a dedicated starting spot is planned).

## Docs

- `docs/multiplayer.md`: networking architecture, authority model, join flow, tests. **Read before touching networking, entities or level loading.**
- `docs/items-inventory-loot.md`: items, bags, equipment, world items, loot tables and modifiers.
- `docs/saves.md`: save format, where saves happen, what is and isn't saved.
- `docs/tiled-maps.md`: layer properties for Y-sorting, spawns, gateways.
- `docs/pathfinding.md`: how enemy pathfinding ended up working, and what failed before.

Keep these docs current when you change a system. Write down what's true now, not a progress log.
