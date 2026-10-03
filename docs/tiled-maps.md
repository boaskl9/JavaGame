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

## Background past the map's edges (`systems/level/BackgroundFill`)

When a map is smaller than the screen, or the camera reaches its edge, the area outside is filled with ground instead of black. It's all set up in Tiled:

1. **Mark the fill tiles in the tileset.** Give each ground tile a custom **string** property `background` with a name, e.g. `grass`. Mark the plain tile and its variants (a small rock, a flower) with the same name.
2. **Set how often each one appears** with Tiled's built-in tile **Probability** (in the tile's properties; default 1). E.g. plain grass 30, rock 1, flower 1.
3. **Pick the fill per map:** a custom **string** map property `background` = `grass` (Map → Map Properties).
4. Optional: the map's built-in **Background Color** (Map Properties) is the screen clear color, for anything not covered.

- The fill is drawn under the whole visible area, inside the map too, so gaps in the ground layer show it.
- Which variant goes on a tile depends only on its coordinates: no flicker, and everyone in multiplayer sees the same ground.
- A map without the `background` property keeps the old look (black, or its Background Color). If no tile carries the name, a warning is printed on load.
- Generated dungeons take `background` and the Background Color from their theme map (`DungeonAssembler`).
- For variation **inside** a map, paint with Tiled's Random Mode (dice button) or a terrain brush: they use the same tile Probability.

## Collision

Collision shapes come from the **tile collision editor** in the tilesets (objects on tiles), loaded by `systems/collision/TiledMapCollisionLoader` into `SpatialQuery`. Rectangles and polygons both work. Toggle `/debug colliders` (F4 console) or F3 to see them.

## Objects: the `Entities` object layer

`systems/level/TiledMapParser` reads the object layer named exactly `Entities`:

| Object | Meaning |
|---|---|
| name `player_spawn` | Default spawn point |
| name `spawn_<something>` | Named spawn point, the target of gateways |
| type `gateway` (rectangle) | Level transition; properties `targetLevel` (level ID, e.g. `Maps/house1.tmx`) and `targetSpawn` (spawn name in that level). See below for dungeon entrances |
| type `bed` (rectangle) | Pressing E while standing on it goes to sleep (ends the day once every player is asleep) |
| type `enemy` | An enemy, spawned by the host; property `enemyType` = `enemy:lizard`, `enemy:axolot` or `enemy:cat` (`EnemyFactory`) |
| type `pot`, `clay_pot` | Breakable object (types in `LevelInstanceFactory.BREAKABLE_TYPES`, configured in `assets/data/BreakableObjectConfig.json`) |

Other custom properties on objects are kept in `LevelData.LevelObject` (`getPropertyString`) for new object types.

In Tiled these are objects in the `Entities` layer whose **name** is the type (e.g. an object named `gateway`).

Arriving on a gateway doesn't trigger it: you have to step off and back on. That lets a spawn point sit on a gateway.

## Dungeon entrances

A gateway with `targetLevel = dungeon:<theme>` (e.g. `dungeon:cave`) enters **today's** dungeon of that theme. Its `targetSpawn` is the spawn point *in the current map* where the dungeon's exit brings you back (house1 uses `spawn_point2`, just inside the door).

- The dungeon's level ID is `dungeon:<theme>:<day>`. It's generated from the world seed and the day (`DayCycle.seedFor`), so everyone gets the same rooms and monsters all day, and a new layout the next day. The dungeon stays loaded for the rest of the day, so killed monsters stay dead when you come back (but not across a save and reload).
- The entrance is the standable tile nearest the center of the first room, and the exit is an (invisible) gateway on it with `targetLevel = dungeon:exit` (`DungeonPopulator`). Other rooms get 0-2 enemies of the theme's types (`DungeonLevelSource.enemyTypesFor`).
- Dying in a dungeon locks that player out of that theme until the next day.

## Dungeon rooms

Rooms for procedural dungeons are Tiled maps too. Doors are **rectangles at the room's edge**; their direction comes from which edge they touch, and doors only connect to doors of exactly the same size. See `systems/dungeon/parsing/RoomDataExtractor` and the theme maps that `DungeonThemeRegistry` registers (`Maps/dungeons/forest_rooms.tmx`, `cave_rooms.tmx`): one map per theme, containing many rooms.
