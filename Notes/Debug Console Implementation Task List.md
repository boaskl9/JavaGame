# Debug Console Implementation Task List

## Overview
Create an in-game debug console system with command support for spawning items/enemies, manipulating health, and toggling debug visualizations.

## Architecture Components

### 1. Core Debug System
- [ ] **DebugManager.java** - Central manager for all debug features
  - Manages debug state flags (collision, frames, yrender, navmesh, etc.)
  - Provides API for toggling debug features
  - Holds references to game systems that need debugging
  - Singleton or passed through systems

- [ ] **DebugCommand.java** - Interface/abstract class for commands
  - `execute(String[] args, DebugContext context)` method
  - `getUsage()` for help text
  - `getName()` for command identification

- [ ] **DebugCommandRegistry.java** - Registers and dispatches commands
  - Map of command name → DebugCommand instance
  - Parse input and route to correct command
  - Handle invalid commands and argument validation

- [ ] **DebugContext.java** - Context object passed to commands
  - Access to: player, world, worldItemManager, uiManager
  - Provides helper methods for common operations
  - Output callback for sending messages back to console

### 2. UI Components

- [ ] **DebugConsole.java** - Scene2D widget for console UI
  - TextField for command input
  - ScrollPane with Label for command history/output
  - Toggle visibility (default: hidden, show with ` or F4)
  - Command history (up/down arrow navigation)
  - Auto-complete suggestions (optional, advanced)
  - Semi-transparent background
  - Positioned at top or bottom of screen

- [ ] **ConsoleInputProcessor.java** - Input handling
  - Intercept ` or F4 to toggle console
  - Handle Enter to submit command
  - Arrow keys for command history
  - When console is open, block game input
  - When console is closed, restore game input

### 3. Command Implementations

#### Item/Entity Spawning
- [ ] **SpawnItemCommand.java** - `/spawn item <itemId> [quantity]`
  - Spawns item at player's feet
  - Default quantity: 1
  - Validates item exists in ItemFactory
  - Example: `/spawn wood 10`

- [ ] **SpawnEnemyCommand.java** - `/spawn enemy <enemyType>`
  - Spawns enemy at mouse position (or player position)
  - Types: slime, frog, cat (or lizard, axolot, cat)
  - Example: `/spawn enemy slime`

#### Health Manipulation
- [ ] **DamageCommand.java** - `/damage <target> <amount>`
  - Targets: player, nearest, all, [entityId]
  - Amount in HP (quarter hearts)
  - Example: `/damage player 4` (1 full heart)
  - Example: `/damage nearest 1` (1/4 heart)

- [ ] **HealCommand.java** - `/heal <target> <amount>`
  - Same targeting as damage
  - Example: `/heal player 8` (2 full hearts)

- [ ] **SetHealthCommand.java** - `/setHealth <target> <current> [max]`
  - Set health directly
  - Example: `/setHealth player 12 20`

#### Debug Visualization
- [ ] **ShowDebugCommand.java** - `/showDebug <feature>`
  - Features: yrender, frames, colliders, navmesh, paths, all, none
  - Toggles or enables specific debug rendering
  - Examples:
    - `/showDebug colliders` - toggle collision boxes
    - `/showDebug all` - enable all debug views
    - `/showDebug none` - disable all debug views

- [ ] **HideDebugCommand.java** - `/hideDebug <feature>`
  - Opposite of showDebug
  - Same feature list

#### Utility Commands
- [ ] **HelpCommand.java** - `/help [command]`
  - With no args: list all commands
  - With command name: show detailed usage
  - Example output:
    ```
    Available commands:
    /spawn item <itemId> [quantity] - Spawn an item
    /spawn enemy <enemyType> - Spawn an enemy
    /damage <target> <amount> - Damage entity
    /heal <target> <amount> - Heal entity
    /showDebug <feature> - Toggle debug view
    /help [command] - Show this help

    Type /help <command> for details
    ```

- [ ] **ClearCommand.java** - `/clear`
  - Clears console output history

- [ ] **TeleportCommand.java** - `/tp <x> <y>` (optional, nice to have)
  - Teleport player to coordinates
  - Example: `/tp 100 200`

- [ ] **GodModeCommand.java** - `/god` (optional)
  - Toggle invincibility
  - Player takes no damage

### 4. Integration Points

- [ ] **GameScreen Integration**
  - Create DebugManager instance
  - Pass to debug rendering methods
  - Initialize DebugConsole UI
  - Set up InputMultiplexer to handle console vs game input

- [ ] **DebugManager Refactoring**
  - Consolidate existing debug flags:
    - `debugMode` → `DebugManager.isEnabled("global")`
    - Move collision debug flag to DebugManager
    - Move navmesh debug flag to DebugManager
    - Add flags for: fps, yrender, entities, etc.

- [ ] **Rendering Integration**
  - `renderCollisionDebug()` checks `DebugManager.isEnabled("colliders")`
  - `renderNavMeshDebug()` checks `DebugManager.isEnabled("navmesh")`
  - `renderDebugStats()` checks `DebugManager.isEnabled("stats")`
  - Add `renderYRenderDebug()` for y-sort visualization
  - Add `renderFrameDebug()` for FPS/frame time graphs

### 5. Advanced Features (Optional)

- [ ] **Command aliases** - `/s` for `/spawn`, `/h` for `/help`
- [ ] **Tab completion** - Press Tab to auto-complete commands
- [ ] **Command history persistence** - Save/load history to file
- [ ] **Script files** - Load and execute `.debug` script files
- [ ] **Variable system** - `$player`, `$mouse`, etc.
- [ ] **Conditional commands** - `if hp < 4 then heal player 16`

## Implementation Order (Recommended)

### Phase 1: Core Infrastructure (Foundation)
1. Create DebugManager with basic flag system
2. Create DebugCommand interface
3. Create DebugCommandRegistry
4. Create DebugContext

### Phase 2: UI (Console Window)
5. Create DebugConsole widget
6. Create ConsoleInputProcessor
7. Integrate with GameScreen (InputMultiplexer)
8. Test opening/closing console

### Phase 3: Basic Commands (Proof of Concept)
9. Implement HelpCommand
10. Implement ClearCommand
11. Implement ShowDebugCommand (start with "colliders" only)
12. Test command execution pipeline

### Phase 4: Spawn Commands (Most Useful)
13. Implement SpawnItemCommand
14. Implement SpawnEnemyCommand
15. Test spawning various items and enemies

### Phase 5: Health Commands
16. Implement DamageCommand
17. Implement HealCommand
18. Implement SetHealthCommand (optional)
19. Test health manipulation with UI update

### Phase 6: Debug Visualizations
20. Refactor existing debug rendering to use DebugManager flags
21. Add more debug visualization options (yrender, frames, etc.)
22. Implement HideDebugCommand
23. Test toggling various debug views

### Phase 7: Polish
24. Add command history (up/down arrows)
25. Improve console styling (colors, fonts, transparency)
26. Add error messages for invalid commands
27. Add confirmation for dangerous commands (if any)

## File Structure
```
com/game/systems/debug/
├── DebugManager.java
├── DebugContext.java
├── commands/
│   ├── DebugCommand.java (interface)
│   ├── DebugCommandRegistry.java
│   ├── HelpCommand.java
│   ├── ClearCommand.java
│   ├── SpawnItemCommand.java
│   ├── SpawnEnemyCommand.java
│   ├── DamageCommand.java
│   ├── HealCommand.java
│   ├── SetHealthCommand.java
│   ├── ShowDebugCommand.java
│   ├── HideDebugCommand.java
│   └── TeleportCommand.java (optional)
└── ui/
    ├── DebugConsole.java
    └── ConsoleInputProcessor.java
```

## UI Design Mockup

```
┌─────────────────────────────────────────────────────┐
│ > /spawn item wood 10                               │ <- Input field
│ Spawned 10x Wood at (123, 456)                      │
│ > /damage player 4                                  │
│ Player damaged for 4 HP (1 heart)                   │
│ > /showDebug colliders                              │
│ Debug view 'colliders' enabled                      │
│ > /help spawn                                       │
│ Usage: /spawn <type> <id> [args]                    │
│   /spawn item <itemId> [quantity]                   │
│   /spawn enemy <enemyType>                          │
└─────────────────────────────────────────────────────┘
```

## Debug Features to Support

### Existing Debug Rendering (Move to DebugManager)
- [x] Collision boxes (red rectangles/polygons)
- [x] Player colliders (green feet, yellow body)
- [x] Enemy colliders (cyan feet, magenta body)
- [x] NavMesh triangles (blue wireframe)
- [x] Pathfinding paths (green lines, yellow waypoints)
- [x] FPS and memory stats

### New Debug Rendering (To Add)
- [ ] Y-sort layers visualization (color-code by depth)
- [ ] Frame time graph (FPS over time)
- [ ] Entity labels (show entity type and ID above sprites)
- [ ] Grid overlay (show tile grid)
- [ ] Inventory debug (show all items in console)
- [ ] AI state labels (show enemy AI state above them)

## Testing Checklist

### Console Functionality
- [ ] Console opens/closes with ` or F4
- [ ] Input field accepts text
- [ ] Enter submits command
- [ ] Command history works (up/down arrows)
- [ ] Output displays correctly
- [ ] Console doesn't interfere with game when closed
- [ ] Console blocks game input when open

### Command Execution
- [ ] `/help` lists all commands
- [ ] `/help <command>` shows command details
- [ ] `/spawn item wood 10` spawns 10 wood at player position
- [ ] `/spawn enemy slime` spawns slime at mouse position
- [ ] `/damage player 4` reduces health by 1 heart
- [ ] `/heal player 4` restores 1 heart
- [ ] `/showDebug colliders` toggles collision debug
- [ ] `/showDebug all` enables all debug views
- [ ] Invalid commands show error message

### Edge Cases
- [ ] Command with wrong argument count shows usage
- [ ] Spawning invalid item ID shows error
- [ ] Spawning invalid enemy type shows error
- [ ] Healing beyond max health caps correctly
- [ ] Damaging below 0 HP doesn't break
- [ ] Long console history doesn't cause lag
- [ ] Console works across level transitions

## Success Criteria

The debug console implementation is complete when:
1. ✅ Console can be toggled with a single key press
2. ✅ All planned commands work correctly
3. ✅ Help system is comprehensive and accurate
4. ✅ Debug rendering can be toggled independently
5. ✅ No crashes or errors from invalid input
6. ✅ Console UI is readable and doesn't obstruct gameplay
7. ✅ Game input is properly blocked/restored when console opens/closes

## Notes

- Use **InputMultiplexer** to switch between console input and game input
- Commands should be **case-insensitive** for better UX
- Consider **throttling** spawn commands to prevent spam
- Add **cooldown** or confirmation for dangerous commands
- Use **Scene2D** for console UI (integrates with existing UI)
- Store console output in a **circular buffer** to prevent memory issues
- Consider **logging** all commands to a file for debugging

## Future Enhancements

- Remote console (web interface for debugging)
- Console macros (bind commands to keys)
- Scripting language integration (Lua?)
- Performance profiler integration
- Memory leak detector
- Asset hot-reloading commands
- Level editor integration
