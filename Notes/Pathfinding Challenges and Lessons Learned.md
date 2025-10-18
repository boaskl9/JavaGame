# Pathfinding Challenges and Lessons Learned

## Date: 2025-10-19

## Context
Attempted to implement A* pathfinding for enemies in a top-down 2D game using the `com.github.xaguzm:pathfinding:0.2.6` library. The game uses:
- **Tiled map editor** for level design
- **16x16 pixel tiles** as the base grid
- **Sub-tile colliders** for features (plants, furniture, decorations)
- **Variable-sized collision shapes** that don't align to the tile grid

## What We Tried

### Attempt 1: Tile-Based Grid (16x16)
- **Approach**: Created navigation grid matching the tile size (16x16 pixels)
- **Testing Method**: Used `testPoint()` to check if tiles were walkable (9 sample points per tile)
- **Problem**: Too coarse-grained. A small plant sprite would block an entire 16x16 tile, even though there was plenty of walkable space around it.

### Attempt 2: Fine Grid (4x4) with Point Sampling
- **Approach**: Subdivided tiles into 4x4 pixel cells for finer pathfinding
- **Grid Calculation**: `(worldWidth * 16) / 4 = worldWidth * 4` cells per dimension
- **Testing Method**: Still used point sampling with `testPoint()`
- **Problem**: Small colliders (like plants) would slip through the gaps between sample points. Enemies could pathfind through colliders because the point tests missed them.

### Attempt 3: Fine Grid (4x4) with Rectangle Testing
- **Approach**: Same 4x4 cell grid, but used `testRectangle()` instead of point sampling
- **Testing Method**: Test a 10x10 pixel rectangle at each cell center
- **Problem**: Still too sparse. Rectangle testing helped but didn't catch all edge cases.

### Attempt 4: Adding Buffer Zone
- **Approach**: Increased test footprint from 10x10 to 20x20 pixels (12px enemy + 8px buffer)
- **Goal**: Keep enemies away from walls to prevent getting stuck
- **Problem**: Made the destination cell (where player stands) unwalkable if player was near a wall. Enemies would lose sight of the player.

### Attempt 5: Destination Exception
- **Approach**: Temporarily mark destination cell as walkable even if in buffer zone
- **Method**: `endCell.setWalkable(true)` before pathfinding, restore after
- **Problem**: Created paths that cut corners too aggressively, enemies got stuck on corners.

### Attempt 6: Path Simplification
- **Approach**: Added line-of-sight testing to skip intermediate waypoints
- **Goal**: Reduce waypoint density for smoother movement
- **Problem**: Too aggressive - removed waypoints needed to navigate corners safely. Enemies would try to cut straight through obstacles.

### Attempt 7: Stuck Detection System
- **Approach**: Track enemy position, detect when not moving for 1.5+ seconds, force path recalculation
- **Implementation**: Added stuck timer to `PathFollowComponent`
- **Problem**: System didn't work reliably. Enemies would still get stuck indefinitely.

## Core Problems Identified

### 1. Grid Resolution vs. Collision Precision Mismatch
- **Issue**: Fixed grid (even 4x4) doesn't match the organic, variable-sized collision shapes created in Tiled
- **Example**: A 8x6 pixel plant sprite creates an irregular collision shape that doesn't align with any grid size
- **Result**: Either grid is too coarse (misses paths) or too fine (performance issues, overcomplicated paths)

### 2. Buffer Zone Paradox
- **Without buffer**: Enemies hug walls, get caught on corners, slide along edges awkwardly
- **With buffer**: Enemies maintain distance BUT can't reach targets near walls, destinations become "invisible"
- **No middle ground**: Buffer needs to apply to intermediate waypoints but not destinations, creating complex edge cases

### 3. Path Simplification vs. Safety Trade-off
- **More waypoints**: Safe navigation but jerky, unnatural movement with constant direction changes
- **Fewer waypoints**: Smooth movement but cuts corners, gets stuck on obstacles
- **Line-of-sight checks**: Expensive and unreliable with complex collision shapes

### 4. Testing Method Limitations
- **Point sampling**: Fast but misses small colliders between sample points
- **Rectangle testing**: Better but still has gaps, doesn't match actual enemy collision shape
- **Polygon testing**: Would be most accurate but prohibitively expensive for every grid cell

### 5. Dynamic Collision Shapes
- **Problem**: Tiled maps have varied collision shapes (rectangles, polygons, different sizes)
- **Grid limitation**: Grid-based pathfinding assumes uniform, tile-aligned obstacles
- **Mismatch**: A 3-pixel wide tree trunk vs. a 15-pixel wide wall both affect pathfinding differently

## Why Grid-Based A* Struggles Here

1. **Assumption Violation**: Grid-based A* assumes obstacles align to grid cells. Tiled sub-tile colliders violate this.

2. **Resolution Dilemma**:
   - Coarse grid (16x16): Fast but inaccurate
   - Fine grid (4x4 or smaller): More accurate but exponentially more cells to process
   - At 4x4, a 50x50 tile world becomes 200x200 cells = 40,000 nodes!

3. **Enemy Footprint Problem**: Enemies have a 12x12 collision box (with 8px buffer = 20x20). This spans 5x5 grid cells at 4x4 resolution. Each path node should validate a 5x5 area, not just 1 cell.

4. **Continuous vs. Discrete**: Enemies move continuously in pixel space, but pathfinding reasons about discrete grid cells. The translation is lossy.

## Key Learnings

### What Worked
- ✅ Separating environment collider (feet) from combat collider (body) on enemies
- ✅ Debug rendering showing both collider types (cyan feet, magenta body)
- ✅ Using `testRectangle()` for more accurate collision detection than point sampling
- ✅ The xguzm pathfinding library itself is solid and easy to use

### What Didn't Work
- ❌ Grid-based pathfinding on sub-tile, non-grid-aligned collision shapes
- ❌ Static buffer zones around all obstacles
- ❌ Aggressive path simplification with line-of-sight
- ❌ Simple stuck detection based on movement threshold
- ❌ Treating destinations the same as intermediate waypoints

## Recommended Approaches for Future Implementation

### Option 1: Navigation Mesh (NavMesh)
**Best for**: Organic, non-grid environments like ours

**Approach**:
- Build a polygon mesh of walkable areas (not a grid)
- Obstacles create "holes" in the mesh
- A* on mesh triangles/polygons instead of grid cells
- Paths are direct lines between polygon centers

**Pros**:
- Handles arbitrary collision shapes naturally
- Fewer nodes than fine grid (better performance)
- Smooth, natural paths
- No grid alignment issues

**Cons**:
- Complex to implement from scratch
- Mesh generation from Tiled collision data is non-trivial
- Needs to be regenerated if obstacles change

**Libraries**:
- `gdx-ai` has NavMesh support (but more complex)
- Custom implementation possible using ear-clipping triangulation

### Option 2: Steering Behaviors with Local Avoidance
**Best for**: Simple AI that feels natural

**Approach**:
- No global pathfinding, just local obstacle avoidance
- Enemy steers toward target, avoids obstacles in front
- Use raycasting or proximity checks to detect obstacles
- Apply forces to steer around them (like "boids" algorithm)

**Pros**:
- Very simple to implement
- Naturally smooth movement
- No grid needed
- Handles dynamic obstacles well

**Cons**:
- Can get stuck in concave corners or U-shaped obstacles
- May not find optimal paths
- Works best in relatively open areas

**Implementation**:
- Cast rays in movement direction
- If obstacle detected, apply avoidance force perpendicular to obstacle
- Combine seeking force (toward target) + avoidance force

### Option 3: Hybrid Grid + Local Refinement
**Best for**: Balancing performance and accuracy

**Approach**:
- Use coarse grid (16x16) for global pathfinding (find general route)
- Use local steering/avoidance for final movement (navigate details)
- Grid path gives "waypoint regions" not exact positions
- Steering handles the precise navigation between regions

**Pros**:
- Fast global pathfinding (few nodes)
- Accurate local navigation
- Combines strengths of both approaches

**Cons**:
- More complex than either approach alone
- Tuning the balance between global/local can be tricky

**Implementation**:
- A* on 16x16 grid for rough path
- Within each waypoint region, use raycasting + steering to navigate
- Switch to next waypoint region when current one is clear

### Option 4: Sparse Visibility Graph
**Best for**: Pre-computed optimal paths

**Approach**:
- Pre-compute visibility between obstacle corners
- Build graph of visible connections
- A* on visibility graph when path is needed
- Paths go around obstacle edges

**Pros**:
- Optimal shortest paths
- Fast pathfinding (small graph)
- Works well with polygon obstacles

**Cons**:
- Requires convex obstacles or complex preprocessing
- Hard to update if obstacles change
- May look unnatural (hugs walls)

## Technical Notes

### Collision System Architecture
```java
// Current setup (good foundation)
SpatialQuery collisionSystem = new SpatialQuery();
- Stores Rectangle and Polygon collision shapes
- testPoint(x, y) - point collision
- testRectangle(rect) - rectangle overlap
- testArea(x, y, w, h) - samples 5 points (corners + center)
```

### Enemy Collider Setup
```java
// Environment collider (feet) - for walking
8x4 pixels at bottom center (50% width, 25% height)
Offset: (SIZE * 0.25f, 0)

// Combat collider (body) - for combat
12x12 pixels centered (75% of 16x16 sprite)
Offset: (SIZE * 0.125f, SIZE * 0.125f)
```

### Grid Calculation
```java
// World dimensions in tiles
int worldWidth = 50;  // tiles
int worldHeight = 50; // tiles

// Cell size for pathfinding
int cellSize = 4;  // pixels

// Grid dimensions in cells
int gridWidth = (worldWidth * 16) / cellSize;   // = 200 cells
int gridHeight = (worldHeight * 16) / cellSize; // = 200 cells
// Total nodes: 40,000 for a 50x50 tile world!
```

## Specific Implementation Recommendations

### For This Game (Top-Down RPG, Stardew Valley Style)

**Recommended**: **Option 2 (Steering Behaviors)** or **Option 3 (Hybrid)**

**Reasoning**:
1. Levels are relatively open with scattered obstacles (plants, furniture)
2. Don't need perfect pathfinding around complex mazes
3. Performance matters (many enemies on screen)
4. Natural movement more important than optimal paths
5. Sub-tile colliders make grid-based approaches very difficult

### Steering Behaviors Implementation Sketch

```java
// In EnemyEntity.handleChaseState()

// 1. Seek toward target
Vector2 desiredVelocity = target.position - this.position;
desiredVelocity.nor().scl(maxSpeed);

// 2. Cast rays to detect obstacles
Vector2 avoidanceForce = new Vector2(0, 0);
for (int angle = -45; angle <= 45; angle += 15) {
    Vector2 rayDir = desiredVelocity.rotated(angle);
    if (raycastHitsObstacle(position, rayDir, lookAheadDistance)) {
        // Add force away from obstacle
        avoidanceForce.add(rayDir.rotate(90).scl(-1));
    }
}

// 3. Combine forces
Vector2 finalVelocity = desiredVelocity.add(avoidanceForce.scl(avoidanceWeight));
finalVelocity.nor().scl(moveSpeed);

this.velocity = finalVelocity;
```

### Hybrid Implementation Sketch

```java
// Global pathfinding (coarse 16x16 grid)
List<Vector2> waypoints = findCoarsePath(start, target);
Vector2 currentWaypoint = waypoints.get(0);

// Local navigation (steering toward waypoint)
Vector2 toWaypoint = currentWaypoint - position;
Vector2 avoidance = calculateLocalAvoidance();
Vector2 finalDirection = (toWaypoint + avoidance).nor();

// Advance to next waypoint when close
if (position.dst(currentWaypoint) < waypointRadius) {
    waypoints.remove(0);
}
```

## Questions to Answer Before Next Implementation

1. **How complex are the levels?**
   - Mostly open with scattered obstacles → Steering works
   - Tight corridors and rooms → Need proper pathfinding (NavMesh/Grid)

2. **How many enemies on screen?**
   - Few (5-10) → Can afford complex pathfinding
   - Many (20+) → Need lightweight approach (Steering)

3. **How smart should enemies be?**
   - Dumb but natural-feeling → Steering
   - Intelligent, finding optimal paths → NavMesh or Grid

4. **Do obstacles change dynamically?**
   - Static → Pre-computed graph/mesh works
   - Dynamic → Need runtime pathfinding or local avoidance

5. **Is the game more action or strategy?**
   - Action (Zelda-like) → Steering, feels responsive
   - Strategy (tactics) → Grid pathfinding, optimal movement

## Resources

### Libraries
- `gdx-ai` - https://github.com/libgdx/gdx-ai (NavMesh, Steering, FSM)
- `xaguzm/pathfinding` - https://github.com/xaguzman/pathfinding (Grid-based A*, what we tried)

### Algorithms
- **Steering Behaviors**: "Steering Behaviors for Autonomous Characters" by Craig Reynolds
- **Navigation Meshes**: "Simple Stupid Funnel Algorithm" for NavMesh pathfinding
- **Local Avoidance**: "Reciprocal Velocity Obstacles" (RVO) for multi-agent avoidance

### Articles
- Red Blob Games: https://www.redblobgames.com/pathfinding/ (Excellent visual explanations)
- Steering Behaviors: http://www.red3d.com/cwr/steer/

## Conclusion

Grid-based A* pathfinding is a poor fit for Tiled maps with sub-tile, non-grid-aligned collision shapes. The fundamental mismatch between discrete grids and continuous collision geometry creates unsolvable edge cases.

**For this game, recommend implementing steering behaviors (Option 2) as a first attempt**, then evaluate if more sophisticated pathfinding is needed. Steering is:
- Simpler to implement (100-200 lines)
- More forgiving with collision shapes
- More performant
- Likely sufficient for the gameplay style

If enemies need to navigate complex indoor areas with many walls and corridors, **revisit NavMesh (Option 1)** at that time.

The pathfinding library (`xaguzm:pathfinding:0.2.6`) remains in the project for potential future use with a proper grid-aligned level structure, or if we implement a custom NavMesh that uses their A* on the mesh graph.
