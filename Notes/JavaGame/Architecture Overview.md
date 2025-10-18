# JavaGame - Architecture Overview

## Project Vision
**Genre**: Top-down RPG with 30-minute day cycles (explore → dungeon → town → repeat)
**Style**: Stardew Valley-like view with focus on exploration, interconnected systems, and gradual power acquisition
**Stage**: Early development - movement, scenes, and inventory functional; combat, NPCs, shops, dungeons, minigames (fishing, card games) planned

**Core Loop**: Explore overworld → Complete quests/talk to NPCs → Delve semi-random dungeons → Earn perks/special items → Systems synergize
**Design Philosophy**: Many small perks and unique items that interact with interconnected systems (no farming)

---

## Tech Stack
- **Engine**: LibGDX (Java)
- **Map Editor**: Tiled (.tmx format)
- **UI**: Scene2D with JSON skin system
- **Build**: Gradle multi-module (core + lwjgl3 desktop)
- **Art**: Asset pack (mostly complete)

---

## Project Structure

```
core/src/main/java/com.game/
├── main/              # Entry points (Main.java, GameScreen.java)
├── systems/           # Reusable core systems
│   ├── entity/       # Component-Entity System
│   ├── animation/    # Sprite animation engine
│   ├── collision/    # Spatial queries (SpatialQuery)
│   ├── input/        # InputManager with rebindable keys
│   ├── inventory/    # Inventory containers & bags
│   ├── item/         # Item definitions & factory
│   ├── level/        # Tiled map parser
│   └── ui/           # UIManagerNew + components
├── components/        # ECS components (Transform, Render, Collider, etc.)
├── entity/           # Concrete entities (Player, NPC, ItemPickup, Gateway)
├── integration/      # WorldManager, WorldItemManager (glue layer)
├── rendering/        # YSortRenderer (depth sorting)
└── world/            # World object definitions
```

---

## Core Systems (Implemented)

### 1. Component-Entity System (ECS)
- **GameObject**: Base container with type-safe component HashMap
- **Component**: Interface with `update()`, `onAttach()`, `onDetach()`
- **Entity**: GameObject + health/stats for living things
- **Key Components**: Transform, RenderComponent, AnimationComponent, VelocityComponent, ColliderComponent, ItemMagnetComponent

### 2. Player & Movement
- 4-directional sprite animation (Up/Down/Left/Right)
- WASD controls, walk 80px/s, run 160px/s (Shift)
- Separate X/Y collision checks for wall sliding
- Camera follows player (clamped to world bounds)
- Collision: Separate hitboxes for environment (feet) vs combat (full body, planned)

### 3. Rendering (YSortRenderer)
- Proper depth sorting for top-down view (like Stardew Valley)
- Three-pass system:
  1. **Background layers**: Terrain
  2. **Y-sorted layers**: Trees/objects/entities sorted by Y position
  3. **Top layers**: Roofs/overlays
- Configured via Tiled layer properties (`foregroundRender`, `topLayer`)
- Debug mode shows render order

### 4. Inventory System
**Architecture**: PlayerInventory → default container + 4 bag slots
- **ItemDefinition**: Immutable templates (id, name, max stack, icon)
- **ItemStack**: Definition reference + quantity
- **ItemFactory**: Registry-based item creation
- **BagInstance**: Equippable bags with internal containers
- Drag-and-drop UI with context menus, tooltips (0.5s delay)
- Drop items back to world

### 5. World Item System
- **WorldItemManager**: Spawns ItemPickupEntity in world, limits max items
- **Item magnetism**: Player pulls nearby items (configurable radius)
- Pickup grace period prevents immediate re-pickup

### 6. Input System
- **InputManager**: Centralized, rebindable keys
- Default: WASD (move), B (inventory), E (interact), F3 (debug)
- Tracks "pressed" and "just pressed" states per frame

### 7. Level/Map System
- **TiledMapParser**: Extracts spawn points, dimensions, object properties
- **Gateways**: Level transitions via collision (targetLevel, targetSpawn properties)
- **WorldManager**: Manages all GameObjects, provides `isPositionWalkable()` queries
- Tile size: 16×16px

### 8. UI System (Scene2D)
- **UIManagerNew**: Manages Stage + Skin (wood-theme.json)
- **Components**: ContainerWindow (inventory), ItemSlotUI, drag-and-drop, ContextMenu, TooltipLabel, BottomHUD (quick bar)
- Resolution-independent via ScreenViewport

---

## Architecture Patterns

1. **Component-Entity System**: Flexible composition over inheritance
2. **Factory Pattern**: ItemFactory decouples creation from logic
3. **Manager/Coordinator**: WorldManager, WorldItemManager, UIManagerNew orchestrate systems
4. **Registry Pattern**: ItemRegistry for data-driven item definitions
5. **Separation of Concerns**: Standalone systems (SpatialQuery, TiledMapParser) are reusable
6. **Observer Pattern**: ItemDropCallback (UI → world communication)

---

## Planned Systems (Not Implemented)

- **Combat**: Hitboxes exist, needs damage/stats/AI
- **NPCs**: Base class exists, needs dialogue/quests/schedules
- **Enemies**: AI, spawn logic, drops
- **Shops**: Buy/sell UI + economy
- **Dungeons**: Procedural generation, room templates
- **Minigames**: Fishing, card game, others
- **Perks/Skills**: Special items with unique mechanics that synergize
- **Settings**: Options menu, keybinds UI, audio controls
- **Day/Night Cycle**: 30-min timer, visual transitions

---

## Key Technical Details

- **Coordinate System**: Top-left origin, Y down (libGDX standard)
- **Grid-Based**: 16×16 tile alignment for world
- **4-Directional**: All character animations use cardinal directions
- **Collision**: AABB with separate X/Y axis checking (SpatialQuery supports polygons too)
- **Asset Manifest**: Auto-generated `assets.txt` via Gradle

---

## Quick Reference for Future Work

### Adding a New Item
1. Create ItemDefinition in ItemRegistry (id, name, stack size, icon path)
2. Register in ItemFactory
3. Icon must exist in `assets/Items/`

### Adding a New Entity
1. Extend `Entity` or `GameObject`
2. Add components in constructor (Transform, RenderComponent, ColliderComponent, etc.)
3. Override `update()` for custom logic
4. Register in WorldManager

### Adding a New Level
1. Create .tmx in Tiled with layers: Background, Features (Y-sorted), Top
2. Add collision objects in "Collision" layer
3. Add spawn points in "Entities" object layer (type="spawn", name="player_spawn" or custom)
4. Add gateways with `targetLevel` and `targetSpawn` properties
5. Load via TiledMapParser

### Adding UI Components
1. Define styles in `assets/ui/wood-theme.json`
2. Create component extending Scene2D actors
3. Register in UIManagerNew
4. Add to Stage

### Key Files to Check
- **GameScreen.java**: Main game loop, rendering pipeline
- **PlayerEntity.java**: Player movement, input handling
- **WorldManager.java**: GameObject orchestration, collision queries
- **ItemRegistry.java**: All item definitions
- **UIManagerNew.java**: UI component setup

---

## Notes
- Systems designed to be standalone and reusable
- Integration layer (WorldManager, WorldItemManager) connects decoupled systems
- ECS allows flexible entity composition for special item powers
- Y-sort renderer handles depth automatically via Tiled properties
