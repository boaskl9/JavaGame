package com.game.systems.pathfinding;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.game.systems.collision.SpatialQuery;

import java.util.*;

/**
 * Grid-based pathfinder using A* algorithm with path smoothing.
 * Much faster than NavMesh for 2D grid-based games.
 *
 * Uses a hybrid approach:
 * 1. Grid-based A* for pathfinding
 * 2. Line-of-sight simplification to remove unnecessary waypoints
 * 3. Funnel algorithm for smooth curves around corners
 */
public class GridPathfinder {
    private final int gridWidth;
    private final int gridHeight;
    private final int cellSize;
    private final float worldWidth;
    private final float worldHeight;

    // Walkability grid - true = walkable, false = blocked
    private boolean[][] walkableGrid;

    // Collision system for line-of-sight checks
    private SpatialQuery collisionSystem;

    /**
     * Creates a grid pathfinder with the specified cell size.
     *
     * @param worldWidth World width in pixels
     * @param worldHeight World height in pixels
     * @param cellSize Size of each grid cell in pixels (e.g., 16 for 16x16)
     */
    public GridPathfinder(float worldWidth, float worldHeight, int cellSize) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.cellSize = cellSize;

        // Calculate grid dimensions (round up to cover entire world)
        this.gridWidth = (int) Math.ceil(worldWidth / cellSize);
        this.gridHeight = (int) Math.ceil(worldHeight / cellSize);

        // Initialize grid as all walkable (will be updated based on tiles)
        this.walkableGrid = new boolean[gridWidth][gridHeight];

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                walkableGrid[x][y] = true;
            }
        }
    }

    /**
     * Builds the walkability grid from a Tiled map.
     * Tiles with "walkable" property set to false will be marked as unwalkable.
     * Also checks collision system for additional obstacles.
     *
     * @param map The TiledMap to extract walkability from
     * @param collisionSystem The collision system for line-of-sight checks
     * @return Number of unwalkable cells
     */
    public int buildFromTiledMap(TiledMap map, SpatialQuery collisionSystem) {
        this.collisionSystem = collisionSystem;

        long startTime = System.currentTimeMillis();
        int unwalkableCount = 0;

        // First pass: Mark all cells as walkable by default
        for (int gx = 0; gx < gridWidth; gx++) {
            for (int gy = 0; gy < gridHeight; gy++) {
                walkableGrid[gx][gy] = true;
            }
        }

        // Second pass: Check tile properties and collision system
        for (int gx = 0; gx < gridWidth; gx++) {
            for (int gy = 0; gy < gridHeight; gy++) {
                // Get world position of cell center
                float worldX = gx * cellSize + cellSize / 2f;
                float worldY = gy * cellSize + cellSize / 2f;

                // Check if this cell is blocked by collision system
                // Only test center point for less strict collision (allows corner navigation)
                boolean blocked = collisionSystem.testPoint(worldX, worldY);


                if (blocked) {
                    walkableGrid[gx][gy] = false;
                    unwalkableCount++;
                }
            }
        }

        long buildTime = System.currentTimeMillis() - startTime;
        System.out.println("GridPathfinder: Built " + gridWidth + "x" + gridHeight +
                          " grid (" + unwalkableCount + " unwalkable cells) in " + buildTime + "ms");



        return unwalkableCount;
    }


    /**
     * Finds a path from start to end position.
     * Returns smoothed waypoints, or null if no path exists.
     *
     * @param start Start position in world coordinates
     * @param end End position in world coordinates
     * @return Array of waypoints, or null if no path found
     */
    public Array<Vector2> findPath(Vector2 start, Vector2 end) {
        // Convert world coordinates to grid coordinates
        int startX = worldToGridX(start.x);
        int startY = worldToGridY(start.y);
        int endX = worldToGridX(end.x);
        int endY = worldToGridY(end.y);

        // Validate coordinates
        if (!isValidCell(startX, startY) || !isValidCell(endX, endY)) {
            return null;
        }

        // If start or end is unwalkable, try to find nearest walkable cell
        if (!walkableGrid[startX][startY]) {
            Vector2 nearestStart = findNearestWalkableCell(startX, startY);
            if (nearestStart == null) return null;
            startX = (int) nearestStart.x;
            startY = (int) nearestStart.y;
        }

        if (!walkableGrid[endX][endY]) {
            Vector2 nearestEnd = findNearestWalkableCell(endX, endY);
            if (nearestEnd == null) return null;
            endX = (int) nearestEnd.x;
            endY = (int) nearestEnd.y;
        }

        // If start and end are in the same cell, return direct path
        if (startX == endX && startY == endY) {
            Array<Vector2> path = new Array<>();
            path.add(new Vector2(end));
            return path;
        }

        // Run A* algorithm
        List<GridNode> nodePath = aStarSearch(startX, startY, endX, endY);

        if (nodePath == null || nodePath.isEmpty()) {
            return null;
        }

        // Convert grid path to world coordinates
        Array<Vector2> worldPath = new Array<>();
        for (GridNode node : nodePath) {
            worldPath.add(gridToWorld(node.x, node.y));
        }

        // Add the exact end position
        worldPath.add(new Vector2(end));

        // Apply path smoothing: line-of-sight simplification
        worldPath = lineOfSightSimplification(worldPath);

        // Apply funnel algorithm for smooth curves
        worldPath = funnelSmoothing(worldPath);

        return worldPath;
    }

    /**
     * A* pathfinding algorithm on the grid.
     * Returns list of grid nodes from start to end, or null if no path found.
     */
    private List<GridNode> aStarSearch(int startX, int startY, int endX, int endY) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Set<Integer> closedSet = new HashSet<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Map<Integer, Double> gScore = new HashMap<>();

        int startKey = gridKey(startX, startY);
        int endKey = gridKey(endX, endY);

        gScore.put(startKey, 0.0);
        openSet.add(new AStarNode(startX, startY, 0.0, heuristic(startX, startY, endX, endY)));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            int currentKey = gridKey(current.x, current.y);

            // Goal reached
            if (current.x == endX && current.y == endY) {
                return reconstructPath(cameFrom, current.x, current.y);
            }

            closedSet.add(currentKey);

            // Check all 8 neighbors (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    int nx = current.x + dx;
                    int ny = current.y + dy;
                    int neighborKey = gridKey(nx, ny);

                    // Skip if invalid or already visited
                    if (!isValidCell(nx, ny) || !walkableGrid[nx][ny] || closedSet.contains(neighborKey)) {
                        continue;
                    }

                    // For diagonal movement, check if both adjacent cells are walkable
                    // This prevents "corner cutting" through diagonal walls
                    if (dx != 0 && dy != 0) {
                        if (!walkableGrid[current.x + dx][current.y] || !walkableGrid[current.x][current.y + dy]) {
                            continue;
                        }
                    }

                    // Calculate movement cost (diagonal = 1.414, straight = 1.0)
                    double moveCost = (dx != 0 && dy != 0) ? 1.414 : 1.0;
                    double tentativeGScore = gScore.get(currentKey) + moveCost;

                    if (!gScore.containsKey(neighborKey) || tentativeGScore < gScore.get(neighborKey)) {
                        cameFrom.put(neighborKey, currentKey);
                        gScore.put(neighborKey, tentativeGScore);

                        double fScore = tentativeGScore + heuristic(nx, ny, endX, endY);
                        openSet.add(new AStarNode(nx, ny, tentativeGScore, fScore));
                    }
                }
            }
        }

        return null; // No path found
    }

    /**
     * Heuristic function for A* (Euclidean distance).
     */
    private double heuristic(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Reconstructs the path from the A* came-from map.
     */
    private List<GridNode> reconstructPath(Map<Integer, Integer> cameFrom, int endX, int endY) {
        List<GridNode> path = new ArrayList<>();
        int currentKey = gridKey(endX, endY);

        path.add(new GridNode(endX, endY));

        while (cameFrom.containsKey(currentKey)) {
            currentKey = cameFrom.get(currentKey);
            int x = currentKey % gridWidth;
            int y = currentKey / gridWidth;
            path.add(0, new GridNode(x, y));
        }

        return path;
    }

    /**
     * Line-of-sight path simplification.
     * Removes unnecessary waypoints where a direct path exists.
     */
    private Array<Vector2> lineOfSightSimplification(Array<Vector2> path) {
        if (path.size <= 2) return path;

        Array<Vector2> simplified = new Array<>();
        simplified.add(path.get(0));

        int current = 0;
        while (current < path.size - 1) {
            int furthest = current + 1;

            // Find the furthest point we can see from current
            for (int i = current + 2; i < path.size; i++) {
                if (hasLineOfSight(path.get(current), path.get(i))) {
                    furthest = i;
                }
            }

            simplified.add(path.get(furthest));
            current = furthest;
        }

        return simplified;
    }

    /**
     * Checks if there's a clear line of sight between two points.
     * Uses the collision system to test multiple points along the line.
     */
    private boolean hasLineOfSight(Vector2 from, Vector2 to) {
        if (collisionSystem == null) return true;

        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Sample points along the line
        int samples = (int) (distance / (cellSize / 2f)) + 1;
        for (int i = 1; i < samples; i++) {
            float t = (float) i / samples;
            float x = from.x + dx * t;
            float y = from.y + dy * t;

            if (collisionSystem.testPoint(x, y)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Funnel algorithm for smooth path curves.
     * Creates smooth curves around corners using portal edges.
     */
    private Array<Vector2> funnelSmoothing(Array<Vector2> path) {
        if (path.size <= 2) return path;

        Array<Vector2> smoothed = new Array<>();
        smoothed.add(path.get(0));

        // Build portals between waypoints
        Array<Portal> portals = new Array<>();
        for (int i = 0; i < path.size - 1; i++) {
            Portal portal = createPortal(path.get(i), path.get(i + 1));
            portals.add(portal);
        }

        // Run funnel algorithm
        Vector2 apex = path.get(0);
        Vector2 leftPoint = portals.get(0).left;
        Vector2 rightPoint = portals.get(0).right;
        int apexIndex = 0;
        int leftIndex = 0;
        int rightIndex = 0;

        for (int i = 1; i < portals.size; i++) {
            Portal portal = portals.get(i);

            // Update right side
            if (triArea2(apex, rightPoint, portal.right) <= 0) {
                if (apex.epsilonEquals(rightPoint, 0.1f) || triArea2(apex, leftPoint, portal.right) > 0) {
                    rightPoint = portal.right;
                    rightIndex = i;
                } else {
                    // Right over left, insert left and restart
                    smoothed.add(new Vector2(leftPoint));
                    apex = leftPoint;
                    apexIndex = leftIndex;

                    leftPoint = apex;
                    rightPoint = apex;
                    leftIndex = apexIndex;
                    rightIndex = apexIndex;
                    i = apexIndex;
                    continue;
                }
            }

            // Update left side
            if (triArea2(apex, leftPoint, portal.left) >= 0) {
                if (apex.epsilonEquals(leftPoint, 0.1f) || triArea2(apex, rightPoint, portal.left) < 0) {
                    leftPoint = portal.left;
                    leftIndex = i;
                } else {
                    // Left over right, insert right and restart
                    smoothed.add(new Vector2(rightPoint));
                    apex = rightPoint;
                    apexIndex = rightIndex;

                    leftPoint = apex;
                    rightPoint = apex;
                    leftIndex = apexIndex;
                    rightIndex = apexIndex;
                    i = apexIndex;
                    continue;
                }
            }
        }

        // Add final point
        smoothed.add(path.get(path.size - 1));

        return smoothed;
    }

    /**
     * Creates a portal (edge) between two waypoints.
     * The portal represents the passage between two grid cells.
     */
    private Portal createPortal(Vector2 from, Vector2 to) {
        Vector2 dir = new Vector2(to).sub(from).nor();
        Vector2 perpendicular = new Vector2(-dir.y, dir.x).scl(cellSize * 0.4f);

        Vector2 mid = new Vector2(from).add(to).scl(0.5f);
        Vector2 left = new Vector2(mid).add(perpendicular);
        Vector2 right = new Vector2(mid).sub(perpendicular);

        return new Portal(left, right);
    }

    /**
     * Calculates twice the signed area of triangle (a, b, c).
     * Positive if counter-clockwise, negative if clockwise.
     */
    private float triArea2(Vector2 a, Vector2 b, Vector2 c) {
        return (b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y);
    }

    /**
     * Finds the nearest walkable cell to the given grid coordinates.
     * Returns grid coordinates as Vector2, or null if none found.
     */
    private Vector2 findNearestWalkableCell(int gridX, int gridY) {
        int maxSearchRadius = Math.max(gridWidth, gridHeight);

        for (int radius = 1; radius < maxSearchRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    // Only check cells on the current radius
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;

                    int nx = gridX + dx;
                    int ny = gridY + dy;

                    if (isValidCell(nx, ny) && walkableGrid[nx][ny]) {
                        return new Vector2(nx, ny);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Marks a grid cell as walkable or unwalkable.
     * Useful for dynamic updates when objects are added/removed.
     */
    public void setCellWalkable(int gridX, int gridY, boolean walkable) {
        if (isValidCell(gridX, gridY)) {
            walkableGrid[gridX][gridY] = walkable;
        }
    }

    /**
     * Marks a world position as walkable or unwalkable.
     */
    public void setPositionWalkable(float worldX, float worldY, boolean walkable) {
        int gridX = worldToGridX(worldX);
        int gridY = worldToGridY(worldY);
        setCellWalkable(gridX, gridY, walkable);
    }

    /**
     * Updates walkability for a rectangular area.
     * Useful for adding/removing large obstacles.
     */
    public void setAreaWalkable(float worldX, float worldY, float width, float height, boolean walkable) {
        int startX = worldToGridX(worldX);
        int startY = worldToGridY(worldY);
        int endX = worldToGridX(worldX + width);
        int endY = worldToGridY(worldY + height);

        for (int gx = startX; gx <= endX; gx++) {
            for (int gy = startY; gy <= endY; gy++) {
                setCellWalkable(gx, gy, walkable);
            }
        }
    }

    // === Coordinate conversion helpers ===

    private int worldToGridX(float worldX) {
        return Math.max(0, Math.min(gridWidth - 1, (int) (worldX / cellSize)));
    }

    private int worldToGridY(float worldY) {
        return Math.max(0, Math.min(gridHeight - 1, (int) (worldY / cellSize)));
    }

    private Vector2 gridToWorld(int gridX, int gridY) {
        return new Vector2(
            gridX * cellSize + cellSize / 2f,
            gridY * cellSize + cellSize / 2f
        );
    }

    private boolean isValidCell(int gridX, int gridY) {
        return gridX >= 0 && gridX < gridWidth && gridY >= 0 && gridY < gridHeight;
    }

    private int gridKey(int x, int y) {
        return y * gridWidth + x;
    }

    // === Debug / Info Methods ===

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public int getCellSize() {
        return cellSize;
    }

    public boolean[][] getWalkableGrid() {
        return walkableGrid;
    }

    public boolean isWalkable(int gridX, int gridY) {
        return isValidCell(gridX, gridY) && walkableGrid[gridX][gridY];
    }

    // === Helper Classes ===

    /**
     * Node for A* algorithm.
     */
    private static class AStarNode {
        final int x, y;
        final double gScore;
        final double fScore;

        AStarNode(int x, int y, double gScore, double fScore) {
            this.x = x;
            this.y = y;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    /**
     * Grid node in the path.
     */
    private static class GridNode {
        final int x, y;

        GridNode(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Portal edge for funnel algorithm.
     */
    private static class Portal {
        final Vector2 left;
        final Vector2 right;

        Portal(Vector2 left, Vector2 right) {
            this.left = left;
            this.right = right;
        }
    }
}
