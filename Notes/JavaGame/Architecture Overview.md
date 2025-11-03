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
│   ├── combat/       # Attack system, weapon stats, strategies
│   ├── input/        # InputManager with rebindable keys
│   ├── inventory/    # Inventory containers & bags
│   ├── item/         # Item definitions & factory
│   ├── level/        # Tiled map parser
│   ├── loot/         # Loot drops, modifiers, loot tables
│   └── ui/           # UIManagerNew + components
├── components/        # ECS components (Transform, Render, Collider, Health, Attack, LootTable, etc.)
├── entity/           # Concrete entities (Player, Enemy, NPC, ItemPickup, Gateway)
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

### 9. Combat System
- **Architecture**: Strategy-based attack system with multi-phase timing
- **WeaponStats**: Defines weapon damage, speed, range, knockback, and attack pattern timing (windup/swing/recovery)
- **WeaponType**: 8 weapon types (SWORD, SPEAR, HAMMER, BOW, DAGGER, AXE, STAFF, WHIP) with unique behaviors
- **AttackComponent**: Manages attack state machine with phases (IDLE → WINDUP → ACTIVE → RECOVERY → COOLDOWN)
- **AttackSystem**: Unified system for all entity attacks; handles hit detection, damage, and knockback
- **Attack Strategies**:
  - ArcSwingStrategy: Sweeping arcs for swords, axes, whips
  - SpearThrustStrategy: Forward thrusts for spears, staffs
  - HammerSlamStrategy: Overhead slams
  - StabStrategy: Quick stabs for daggers
- **Hit Detection**: Arc-based and directional hitboxes; prevents multi-hitting via hit tracking
- **CombatUtils**: Helper methods for angles, knockback, and hitbox calculations
- **Health System**: Entity base class with health/damage/death; HealthComponent for UI integration
- **Enemy AI**: State machine (IDLE/WANDER/CHASE/ATTACK/FLEE) with pathfinding, detection range, and attack range
- **Knockback**: Applied to VelocityComponent with direction-based physics
- **Death Handling**: onDeath() callback for enemies; supports death animations and loot drops

### 10. Loot Drop System
- **Architecture**: Component-based with dynamic runtime modifiers
- **LootTableComponent**: Attached to enemies; defines base drops with chance/quantity ranges
- **LootDrop**: Data class for individual drop definitions (itemId, chance, min/max quantity)
- **LootContext**: Carries context information (enemy, player, modifiers) for intelligent drop decisions
- **LootModifier Interface**: Applied by equipment/skills to modify drops dynamically
- **Built-in Modifiers**:
  - AddDropModifier: Adds new drops (e.g., coffee ring adds coffee)
  - HideHarvesterModifier: Context-aware drops based on enemy tags (cow → cow_hide)
  - ChanceMultiplierModifier: Increases drop chances (luck stat)
  - QuantityMultiplierModifier: Increases quantities (greedy perk)
- **Tag System**: Enemies have tags (e.g., "animal:cow") for context-aware modifier behavior
- **LootSystem Manager**: Orchestrates drop generation; collects player modifiers, rolls drops, spawns items
- **Integration**: Automatic on death via EnemyEntity.onDeath() → WorldItemManager for spawning
- **Extensible**: Custom modifiers implement LootModifier interface; priority-based ordering

### 11. Equipment System
- **Architecture**: Slot-based equipment with UI integration
- **EquipmentSlot**: Enum defining 6 slots (Head, Body, Amulet, Ring1, Ring2, Weapon)
- **PlayerEquipment**: Container for equipped items; manages equip/unequip/swap logic
- **EquipmentWindow**: Scene2D UI for equipment management; drag-and-drop support
- **Integration**: Connected to PlayerEntity and InventorySystem
- **Item Types**: ItemType enum (WEAPON, ARMOR, CONSUMABLE, MATERIAL, MISC)
- **Validation**: Slots accept specific item types (WEAPON slot only accepts weapons, etc.)
- **Extensible**: Ready for stat bonuses, set bonuses, and loot modifiers

### 12. Pathfinding System
- **Architecture**: Grid-based A* with path smoothing and line-of-sight optimization
- **GridPathfinder**: Efficient pathfinding using walkability grid (16×16 cell size)
- **Path Smoothing**: Line-of-sight simplification removes unnecessary waypoints
- **Integration**: Used by EnemyEntity chase state; updates paths every 0.5s
- **Collision**: Uses SpatialQuery for walkability checks and line-of-sight tests
- **Performance**: Coarse grid (16×16) with local navigation for smooth movement
- **Feet-Based**: Pathfinding uses enemy "feet" position (environment collider center)
- **Separation Steering**: Enemies maintain personal space (avoid bunching)

### 13. Audio System
- **Architecture**: Singleton manager with lazy loading and resource pooling
- **SoundSystem**: Manages sound effects and background music
- **SoundRegistry**: Enum mapping sound IDs to file paths
- **MusicTrack**: Enum for 37 background music tracks
- **Features**: Lazy loading, 16-sound limit, master/music/sfx volume controls
- **Volume Persistence**: Settings saved via GameSettings preferences
- **Integration**: Sounds for combat, item pickup, breakables, UI interactions

### 14. Breakable Objects System
- **Architecture**: Entity-based destructible world objects
- **BreakableEntity**: Base class for pots, crates, barrels with health and animations
- **BreakableObjectRegistry**: JSON-based configuration for object types
- **Features**: Break animations, loot drops, particle effects, dual colliders
- **Colliders**: Environment collider (blocks movement) + combat collider (takes damage)
- **Loot Integration**: Uses LootSystem for drop generation
- **Runtime Spawning**: Debug commands support spawning breakables

### 15. Debug System
- **Architecture**: In-game console with command system
- **DebugConsole**: Scene2D widget with command input and output history
- **DebugManager**: Manages debug visualization flags (colliders, navmesh, fps, etc.)
- **Commands**: spawn, damage, heal, setmaxhealth, debug, timescale, items, clear, help
- **Time Scale**: Debug feature to speed up/slow down game (0.25x - 4.0x)
- **Input**: F4 toggles console; F3 toggles debug overlay
- **Features**: Command history (up/down), validation, error messages

---

## Architecture Patterns

1. **Component-Entity System**: Flexible composition over inheritance
2. **Factory Pattern**: ItemFactory decouples creation from logic
3. **Manager/Coordinator**: WorldManager, WorldItemManager, UIManagerNew orchestrate systems
4. **Registry Pattern**: ItemRegistry for data-driven item definitions
5. **Separation of Concerns**: Standalone systems (SpatialQuery, TiledMapParser) are reusable
6. **Observer Pattern**: ItemDropCallback (UI → world communication)

---

## Planned Systems (Not Yet Implemented)

- **Equipment Stats & Bonuses**: Equipment slots exist; needs stat bonuses, set effects, loot modifier integration
- **NPCs**: Base class exists, needs dialogue/quests/schedules
- **Enemy Spawning**: Spawn logic, spawn points, waves
- **Shops**: Buy/sell UI + economy
- **Dungeons**: Procedural generation, room templates
- **Minigames**: Fishing, card game, others
- **Perks/Skills**: Special items with unique mechanics that synergize
- **Day/Night Cycle**: 30-min timer, visual transitions
- **Projectiles**: Ranged weapons (bows), enemy projectiles
- **World Progression Gates**: Abilities to unlock areas (cut trees, place staircases, jump ledges)

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
