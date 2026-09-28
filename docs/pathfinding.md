# Enemy pathfinding

Pathfinding works well enough now. This doc covers how it works, and the approaches that failed so they don't get retried.

## How it works

- **Grid:** `systems/pathfinding/GridPathfinder` is our own A* (no library) on an **8×8 px** grid, built per level by `WorldManager.buildGridPathfinder` from the map's collision.
  - A cell is blocked if its **center point** hits a collider (`SpatialQuery.testPoint`). That's deliberately lenient, so enemies can get around corners.
  - `setCellWalkable` / `setPositionWalkable` / `setAreaWalkable` change the grid at runtime.
- **Path smoothing:** line-of-sight simplification removes waypoints that aren't needed, then a funnel pass smooths the path around corners.
- **Chasing:** enemies re-path every `EnemyEntity.PATHFINDING_UPDATE_INTERVAL` (0.5 s) and follow the waypoints with `PathFollowComponent`.
- **Feet-based:** paths start and end at the enemy's **environment collider** (the feet box at the bottom of the sprite), not the sprite origin. Enemies have two colliders: feet for walls, body for combat.
- **Spacing:** `SeparationComponent` pushes enemies apart so they don't clump.
- **Debug:** `/debug navmesh` (F4) draws the blocked grid cells; F3 shows colliders (cyan feet, magenta body on enemies).

`com.github.xaguzman:pathfinding` is still declared in `core/build.gradle` but isn't imported anywhere. It's left over from an earlier attempt.

## What didn't work (October 2025)

The core problem: Tiled collision shapes are sub-tile and irregular (a plant's 8×6 px polygon, a 3 px trunk next to a 15 px wall), while grid A* assumes obstacles line up with cells.

| Attempt | Why it failed |
|---|---|
| 16×16 grid, 9 sample points per tile | Too coarse: one small plant blocked a whole tile |
| 4×4 grid, point sampling | Small colliders slipped between the sample points |
| 4×4 grid, 10×10 rectangle test per cell | Still missed edge cases, and 40,000 nodes for a 50×50-tile map |
| 20×20 "buffer" footprint to keep off walls | A player standing near a wall became unreachable |
| Forcing the destination cell walkable | Paths cut corners and enemies got stuck on them |
| Aggressive line-of-sight simplification | Removed the waypoints that were needed to get around corners |
| Stuck detection (repath after 1.5 s without moving) | Unreliable; enemies still got stuck indefinitely |

Lessons:
- Separating the feet collider from the body collider was the most useful single change.
- Destinations need different rules from intermediate waypoints.
- Buffers and simplification trade safety for smoothness, and there's no single setting that fixes both.

What finally worked was a medium grid (8 px) with lenient center-point blocking, plus smoothing and separation steering.

If this ever needs to go further, the known options are:
- a navigation mesh (handles arbitrary shapes, but turning Tiled collision into a mesh is non-trivial);
- a coarse grid for the overall route plus local steering and avoidance for the details.
