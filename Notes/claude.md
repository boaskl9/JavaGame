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
7. **Dungeons** - Procedural generation not started
8. **Day/Night Cycle** - Not implemented (30-min timer planned)
9. **Minigames** - Fishing, card games planned
10. **Progression Gates** - Abilities to unlock areas (cut trees, jump ledges, etc.)

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
