# Tiled maps

Maps live in `assets/Maps/` (`prototype.tmx` is the default start level, plus `StartArea`, `WestArea`, `glade`, `house1`, ...). A level ID is the path under `assets/`, e.g. `Maps/prototype.tmx`. A map is turned into a playable level by `world/LevelInstanceFactory` (see `docs/multiplayer.md` for the host/client split).

## Layers and depth (`rendering/YSortRenderer`)

Every tile layer is one of three kinds, chosen by a **bool custom property** on the layer:

| Property | Kind | Drawn | Use for |
|---|---|---|---|
| *(none)* | Background | First, unsorted | Terrain, ground details, paths, water |
| `foregroundRender = true` | Y-sorted | Together with entities, by Y | Trees, buildings, fences, anything the player can walk in front of or behind |
| `topLayer = true` | Top | Last | Roofs, ceilings, overlays |

Y-sorted items are sorted by their bottom edge in **descending Y**. The world is Y-up, so something higher on screen (larger Y) is drawn first and appears behind. A player standing below a tree's base is drawn over the tree; a player above it is hidden behind it.

On load the renderer prints how many layers it found of each kind (`YSortRenderer configured: ...`). If there are 0 Y-sorted layers, the property is missing. `ySortRenderer.setLayerConfiguration(background[], ySorted[], top[])` overrides the automatic detection by layer index.

## Collision

Collision shapes come from the **tile collision editor** in the tilesets (objects on tiles), loaded by `systems/collision/TiledMapCollisionLoader` into `SpatialQuery`. Rectangles and polygons both work. Toggle `/debug colliders` (F4 console) or F3 to see them.

## Objects: the `Entities` object layer

`systems/level/TiledMapParser` reads the object layer named exactly `Entities`:

| Object | Meaning |
|---|---|
| name `player_spawn` | Default spawn point |
| name `spawn_<something>` | Named spawn point, the target of gateways |
| type `gateway` (rectangle) | Level transition; properties `targetLevel` (level ID, e.g. `Maps/house1.tmx`) and `targetSpawn` (spawn name in that level) |
| type `pot`, `clay_pot` | Breakable object (types in `LevelInstanceFactory.BREAKABLE_TYPES`, configured in `assets/data/BreakableObjectConfig.json`) |

Other custom properties on objects are kept in `LevelData.LevelObject` (`getPropertyString`) for new object types. Enemies aren't placed from maps yet.

## Dungeon rooms

Rooms for procedural dungeons are Tiled maps too. Doors are **rectangles at the room's edge**; their direction comes from which edge they touch, and doors only connect to doors of exactly the same size. See `systems/dungeon/parsing/RoomDataExtractor` and the theme maps that `DungeonThemeRegistry` registers (`Maps/dungeons/forest_rooms.tmx`, `cave_rooms.tmx`): one map per theme, containing many rooms.
