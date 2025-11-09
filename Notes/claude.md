# Claude Agent Quick Start Guide

**Purpose**: Get Claude agents up to speed quickly with minimal token usage.

## 🚀 First Steps (Do This First!)

1. **Read this file completely** - It's short and saves thousands of tokens
2. **Browse file titles in `Notes/JavaGame/`** - Understand what documentation exists
3. **Read `Architecture Overview.md`** - 5-minute comprehensive overview
4. **Use Glob tool** - Find files matching patterns before reading entire files
5. **Use Grep tool** - Search for specific code patterns before reading files

## 📁 Critical Documentation Files (In Order)

**Start here:**
1. `Notes/JavaGame/Tasks.md` - Current TODO list and priorities

**Domain-specific:**
2. `Notes/JavaGame/LootSystemUsageExample.md` - Loot system implementation guide
3. `Notes/Pathfinding Challenges and Lessons Learned.md` - Pathfinding history and solutions
4. `Notes/JavaGame/Ideas.md` - Game design vision (progression gates, metroidvania elements)
5. `Notes/JavaGame/Ideas about dungeons.md` - Dungeon design goals
6. `Notes/JavaGame/Fun ideas.md` - NPC interaction systems

## 🎮 Game Overview (30 seconds)

**Genre**: Top-down RPG (Stardew Valley style view)
**Core Loop**: Explore overworld → Dungeon runs → Town interactions → Repeat (30-min cycles)
**Stage**: Early development - core systems functional, content-light

**Tech**: LibGDX (Java), Tiled maps, Scene2D UI, ECS architecture

## ✅ Implemented Systems (What Works Now)

### Core Gameplay
- ✅ **Player Movement** - WASD, collision, camera following, wall sliding
- ✅ **Combat System** - Strategy-based attacks, 8 weapon types, knockback, multi-phase timing
- ✅ **Enemy AI** - State machine (IDLE/WANDER/CHASE/ATTACK), pathfinding, separation steering
- ✅ **Health System** - Entity health, death handling, UI integration
- ✅ **Pathfinding** - Grid-based A* with smoothing, line-of-sight optimization (WORKS WELL)

### Progression & Items
- ✅ **Inventory System** - Drag-and-drop UI, bags, stacking, tooltips, context menus
- ✅ **Equipment System** - 6 slots (weapon, head, body, amulet, 2 rings), drag-and-drop UI
- ✅ **Loot System** - Dynamic drops, modifiers, context-aware (tag-based), extensible
- ✅ **Item System** - Definitions, factory pattern, types (weapon, armor, consumable, etc.)
- ✅ **Breakable Objects** - Pots/crates with animations, loot, particles, dual colliders

### World & Rendering
- ✅ **Level System** - Tiled map loading, gateways, spawn points, collision layers
- ✅ **Y-Sort Rendering** - Proper depth sorting (trees, objects, entities by Y position)
- ✅ **World Items** - Item pickups in world, magnetism, grace period
- ✅ **Collision** - SpatialQuery (rectangles, polygons), dual colliders (environment + combat)

### Audio & UI
- ✅ **Audio System** - Sound effects, music tracks, volume controls, lazy loading, pooling
- ✅ **UI System** - Scene2D, inventory window, equipment window, tooltips, settings menu
- ✅ **Debug Console** - F4 console with commands (spawn, damage, heal, debug, timescale)
- ✅ **Debug Overlay** - F3 toggles FPS/stats, collision boxes, paths, time scale control

## 🔨 What Needs Work (Priorities)

### High Priority
1. **Equipment Stats** - Equipment exists but no stat bonuses, set effects, or loot modifiers yet
2. **Enemy Spawning** - No spawn system; enemies are manually placed or debug-spawned
3. **NPC System** - Completely missing

### Medium Priority
4. **Shop System** - No economy, buying/selling
5. **More Maps** - Limited to test areas; needs town, dungeons, overworld
6. **Projectiles** - No ranged weapons or enemy projectiles yet

### Low Priority (Design Phase)
7. **Day/Night Cycle** - Not implemented (30-min timer planned)
8. **Minigames** - Fishing, card games planned
9. **Progression Gates** - Abilities to unlock areas (cut trees, jump ledges, etc.)

## 🔧 Active Refactors / In Progress

### Dungeon Generation System Refactor (IN PROGRESS)

**Context**: Basic dungeon generation system exists but has critical issues:
- Door system uses points (no size/dimensions) → rooms can't match properly
- Only 2-3 rooms place successfully out of 30+ allocated
- Testing requires debug console commands (clunky workflow)
- Many hardcoded values, difficult to iterate/tune

**Location**: `systems/dungeon/` - generation, assembly, parsing subsystems

#### Phase 1: Fix Door System (Rectangle-based with Size Matching) ✅ COMPLETED
**Goal**: Enable proper door-to-door connections with exact size matching

- [x] **DoorConnection.java**: Add `width` and `height` fields
- [x] **DoorConnection.java**: Auto-calculate direction from position relative to room bounds
  - Top edge → NORTH, Bottom → SOUTH, Left → WEST, Right → EAST
- [x] **DoorConnection.java**: Update `canConnectWith()` to require exact size match
- [x] **RoomDataExtractor.java**: Change from point objects to rectangle objects
- [x] **RoomDataExtractor.java**: Read rectangle dimensions (width/height) from Tiled
- [x] **RoomDataExtractor.java**: Implement auto-direction calculation
- [x] **RoomDataExtractor.java**: Validate doors are at room edges (not interior)
- [x] **DoorMatcher.java**: Update `canDoorsConnect()` to check size compatibility
- [x] **DoorMatcher.java**: Update alignment calculation for door sizes
- [x] **DoorMatcher.java**: Add directional offset to prevent room overlaps
- [x] **RoomPlacer.java**: Remove random offset for clean grid-aligned coordinates

**Tiled Workflow Change**: Draw door rectangles at room edges (size defines connection area)

**Key Fixes Applied**:
- Doors now have width/height and only connect with exact size matches
- Direction auto-calculated from door position relative to room edges
- Room placement now accounts for door dimensions to prevent overlaps
- Removed fractional coordinates - rooms now align to clean grid positions
- Rooms successfully connect both horizontally AND vertically

**Result**: Complex dungeon layouts now generate successfully with proper door connections!

#### Phase 2: Create Parameter System ✅ COMPLETED
**Goal**: Make generation tunable without code changes

- [x] **DungeonGenerationParams.java** (NEW): Create parameter container class
  - Fields: `theme`, `budget`, `seed`, `maxRooms`, `doorMatchPriority`
  - Uses builder pattern for construction
- [ ] **DungeonGenerator.java**: Accept `DungeonGenerationParams` instead of individual args (OPTIONAL - not needed for test screen)
- [ ] **BudgetAllocator.java**: Accept params object (OPTIONAL)
- [ ] **RoomPlacer.java**: Accept params object (OPTIONAL)

**Note**: Params class created but not yet integrated into generator. Test screen uses params internally, then calls existing generator methods.

#### Phase 3: Create Testing Screen ✅ COMPLETED
**Goal**: Fast iteration workflow with visual feedback

- [x] **DungeonTestScreen.java** (NEW): Implement LibGDX `Screen` interface
  - Full screen layout with controls on left, preview on right
  - Controls: Theme text input, budget/maxRooms/doorPriority sliders, seed display, buttons
  - Camera controls: WASD to move, +/- to zoom
  - Green room boundary overlays for debugging
  - Stats display: room count, connected doors, map size
- [x] **DebugConsole.java**: Add `/dungeon test` command to launch testing screen
- [x] Screen switching implemented using `Game.setScreen()`

**Test Screen Features**:
- Live dungeon generation with adjustable parameters
- Visual preview with TiledMap rendering
- Interactive camera controls (WASD move, +/- zoom)
- Real-time statistics showing room placement success
- Seed control with randomize button
- One-click regeneration

#### Phase 4: Integration & Polish
**Goal**: Connect everything into smooth workflow

- [ ] Implement "Load into Game" button: assemble → switch to GameScreen → load dungeon
- [ ] Add param validation before generation
- [ ] Show error messages in UI if generation fails
- [ ] Display quality warnings (e.g., "Only 5/30 rooms placed")
- [ ] Store last params for quick iteration

**Files Created**:
- ✅ `systems/dungeon/DungeonGenerationParams.java`
- ✅ `systems/dungeon/DungeonTestScreen.java`

**Files Modified**:
- ✅ `systems/dungeon/DoorConnection.java`
- ✅ `systems/dungeon/parsing/RoomDataExtractor.java`
- ✅ `systems/dungeon/generation/DoorMatcher.java`
- ✅ `systems/dungeon/generation/RoomPlacer.java`
- ✅ `systems/debug/DebugConsole.java`

**New Workflow (READY TO USE)**:
1. F4 → `/dungeon test` → Testing screen opens
2. Adjust theme/budget/seed → Click "Generate Dungeon" → See instant preview
3. Iterate quickly (no game restart needed!)
4. Use WASD to explore, +/- to zoom
5. Stats panel shows room count and connection success

## 🗂️ Project Structure (Key Directories)

```
D:\JavaGame\
├── core/src/main/java/com/game/
│   ├── main/                    # GameScreen.java, Main.java (entry points)
│   ├── systems/                 # All game systems (SEE BELOW)
│   ├── components/              # ECS components (Transform, Health, Attack, etc.)
│   ├── integration/             # WorldManager, WorldItemManager (glue layer)
│   └── rendering/               # YSortRenderer
├── assets/                      # Art, audio, maps, UI skins
│   ├── Audio/                   # Sounds/ and Musics/
│   ├── levels/                  # Tiled .tmx files
│   ├── Items/                   # Item icons
│   └── ui/                      # wood-theme.json
└── Notes/                       # Documentation (YOU ARE HERE)
    └── JavaGame/                # Game-specific docs
```

### Key System Directories (systems/)
```
systems/
├── entity/                      # GameObject, Entity, Component (ECS core)
│   └── entities/                # PlayerEntity, EnemyEntity, BreakableEntity, etc.
├── combat/                      # AttackSystem, WeaponStats, strategies
├── loot/                        # LootSystem, LootTableComponent, modifiers
├── inventory/                   # Containers, bags, PlayerEquipment, EquipmentSlot
├── item/                        # ItemDefinition, ItemFactory, ItemRegistry
├── pathfinding/                 # GridPathfinder (A* with smoothing)
├── audio/                       # SoundSystem, SoundRegistry, MusicTrack
├── ui/                          # UIManagerNew, windows, HUD
├── input/                       # InputManager, InputAction
├── collision/                   # SpatialQuery (AABB, polygon collision)
├── animation/                   # AnimationBuilder
├── level/                       # TiledMapParser
├── breakable/                   # BreakableObjectRegistry
├── debug/                       # DebugConsole, DebugManager
└── settings/                    # GameSettings (preferences)
```


### Finding Code Efficiently
- Do not read Tiled files, they are too expensive token wise, and do not give AI agents a good idea of what they contain.

### Common File Locations
- **Main game loop**: `main/GameScreen.java`
- **Player**: `systems/entity/entities/PlayerEntity.java`
- **Enemies**: `systems/entity/entities/enemies/` (LizardEnemy, Axolot, CatEnemy)
- **Items**: `systems/item/ItemRegistry.java`, `systems/item/TestItems.java`
- **Combat**: `systems/combat/AttackSystem.java`, `systems/combat/WeaponStats.java`

## 🎯 Design Philosophy (Important!)

1. **Interconnected Systems** - Many small perks/items that synergize across systems
2. **Component-Based** - ECS architecture; entities are composed of components
3. **Data-Driven** - Items, breakables, loot defined in registries/JSON
4. **Manager Pattern** - WorldManager, WorldItemManager, UIManagerNew coordinate systems
5. **Singleton Pattern** - LootSystem, SoundSystem, GameSettings use lazy singleton
6. **No Farming** - Not a farming game; focus is exploration, combat, dungeons

## 🐛 Known Issues & Gotchas

### Current Limitations
- ⚠️ No equipment stat bonuses yet (slots exist but don't affect stats)
- ⚠️ No spawn system (manual placement or debug commands only)
- ⚠️ Limited content (few maps, enemies, items)

### Technical Notes
- **Coordinate System**: Top-left origin, Y down (libGDX standard)
- **Tile Size**: 16×16 pixels
- **Animation**: 4-directional (up/down/left/right)
- **Collision**: AABB with dual colliders (environment + combat)
- **Health**: 4 HP = 1 heart (quarter-heart system)

## 💡 Tips for Efficient Development

### Before Writing Code
1. **Search first** - Use Grep to find similar implementations
2. **Read selectively** - Use offset/limit on Read tool for large files
3. **Check registries** - ItemRegistry, SoundRegistry, BreakableObjectRegistry
4. **Follow patterns** - LootSystem, SoundSystem show singleton pattern

### When Adding Features
1. **Check existing systems** - Loot, combat, inventory are good reference implementations
2. **Use ECS** - Add components rather than hardcoding in entity classes
3. **Follow naming** - Systems end in "System", Components in "Component"
4. **Update registries** - ItemRegistry for items, SoundRegistry for sounds

### Testing Features
1. **Debug console (F4)** - Spawn items/enemies, modify health, toggle visualizations
2. **Time scale (F3, +/-)** - Speed up testing (0.25x - 4.0x)
3. **Debug overlay (F3)** - See FPS, colliders, paths

## 📚 Additional Resources

### If Working On...
- **Combat**: Read `systems/combat/AttackSystem.java` header comments
- **Loot**: Read `Notes/JavaGame/LootSystemUsageExample.md`
- **Pathfinding**: Read `Notes/Pathfinding Challenges and Lessons Learned.md`
- **UI**: Check `assets/ui/wood-theme.json` for Scene2D styles
- **Maps**: Use Tiled editor, reference `levels/*.tmx`

### Code Examples
- **Adding an item**: See `systems/item/TestItems.java`
- **Adding an enemy**: See `systems/entity/entities/enemies/LizardEnemy.java`
- **Adding a command**: See `systems/debug/DebugConsole.java`
- **Adding a sound**: Add to `SoundRegistry.java`, call `SoundSystem.getInstance().playSound()`

## ⚡ Token Optimization Strategies

1. **Read file lists before contents** - Use Glob to see what exists
2. **Search before reading** - Grep finds exact locations
3. **Read headers only** - Use offset/limit (e.g., lines 1-50 for class docs)
4. **Trust the architecture** - Systems follow consistent patterns
5. **Use Architecture Overview** - Comprehensive reference saves reading multiple files


---

## 🏁 Ready to Start?

1. ✅ Read this file (you're here!)
2→ Ask user what they want to work on
3→ Use Glob/Grep to find relevant files
4→ Read only what you need
5→ Start coding with existing patterns as reference

**Remember**: The codebase is well-structured. Follow existing patterns, trust the architecture, and search before reading. You've got this! 🚀
