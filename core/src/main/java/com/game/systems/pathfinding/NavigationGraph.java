package com.game.systems.pathfinding;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.game.systems.collision.SpatialQuery;
import org.xguzm.pathfinding.grid.GridCell;
import org.xguzm.pathfinding.grid.NavigationGrid;
import org.xguzm.pathfinding.grid.finders.AStarGridFinder;
import org.xguzm.pathfinding.grid.finders.GridFinderOptions;

import java.util.List;

/**
 * Navigation graph for A* pathfinding using xguzm pathfinding library.
 * Built from collision data to allow enemies to navigate around obstacles.
 */
public class NavigationGraph {
    private NavigationGrid<GridCell> grid;
    private AStarGridFinder<GridCell> finder;
    private int width;
    private int height;
    private int tileSize;
    private SpatialQuery collisionSystem;

    public NavigationGraph(int worldWidth, int worldHeight, int tileSize, SpatialQuery collisionSystem) {
        this.width = worldWidth;
        this.height = worldHeight;
        this.tileSize = tileSize;
        this.collisionSystem = collisionSystem;

        System.out.println("new naviagtion graph");

        buildGraph();
    }

    /**
     * Builds the navigation grid from the collision data.
     */
    private void buildGraph() {
        // Create grid cells
        GridCell[][] cells = new GridCell[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float worldX = x * tileSize + tileSize / 2f;
                float worldY = y * tileSize + tileSize / 2f;

                // Check if this tile is walkable
                boolean walkable = isWalkable(worldX, worldY);
                cells[x][y] = new GridCell(x, y, walkable);
            }
        }

        // Create navigation grid
        grid = new NavigationGrid<>(cells, true); // true = allow diagonal movement

        // Create A* finder with options
        GridFinderOptions options = new GridFinderOptions();
        options.allowDiagonal = true;
        finder = new AStarGridFinder<>(GridCell.class, options);

        System.out.println("NavigationGraph: Created " + width + "x" + height + " grid");
    }

    /**
     * Checks if a position is walkable.
     * Tests an area that represents enemy footprint plus buffer zone.
     */
    private boolean isWalkable(float worldX, float worldY) {
        // Enemy colliders are typically 12x12 (75% of 16x16 sprite)
        // Add 4 pixel buffer on each side to keep enemies away from walls
        float footprintSize = 12f + 8f; // 20 pixels total (12 + 4*2 buffer)
        float halfFootprint = footprintSize / 2f;

        // Create a test rectangle centered on the cell's world position
        Rectangle testRect = new Rectangle(
            worldX - halfFootprint,
            worldY - halfFootprint,
            footprintSize,
            footprintSize
        );

        // Use testRectangle for proper collision detection (not just point sampling)
        return !collisionSystem.testRectangle(testRect);
    }

    /**
     * Converts world position to grid coordinates.
     */
    private int[] worldToGrid(float worldX, float worldY) {
        int gridX = (int) (worldX / tileSize);
        int gridY = (int) (worldY / tileSize);

        // Clamp to grid bounds
        gridX = Math.max(0, Math.min(gridX, width - 1));
        gridY = Math.max(0, Math.min(gridY, height - 1));

        return new int[]{gridX, gridY};
    }

    /**
     * Converts grid coordinates to world position (center of tile).
     */
    public Vector2 gridToWorld(int gridX, int gridY) {
        return new Vector2(
            gridX * tileSize + tileSize / 2f,
            gridY * tileSize + tileSize / 2f
        );
    }

    /**
     * Finds a path from start to end position using A*.
     * Returns a list of waypoints in world coordinates, or null if no path exists.
     */
    public Array<Vector2> findPath(Vector2 start, Vector2 end) {
        int[] startGrid = worldToGrid(start.x, start.y);
        int[] endGrid = worldToGrid(end.x, end.y);

        GridCell startCell = grid.getCell(startGrid[0], startGrid[1]);
        GridCell endCell = grid.getCell(endGrid[0], endGrid[1]);

        if (startCell == null || endCell == null) {
            return null;
        }

        // Allow pathfinding to destination even if it's in buffer zone
        // (player might be standing near a wall)
        boolean endWasUnwalkable = false;
        if (!endCell.isWalkable()) {
            // Temporarily mark as walkable for pathfinding
            endCell.setWalkable(true);
            endWasUnwalkable = true;
        }

        // Find path
        List<GridCell> path = finder.findPath(startCell, endCell, grid);

        // Restore original walkability
        if (endWasUnwalkable) {
            endCell.setWalkable(false);
        }

        if (path == null || path.isEmpty()) {
            return null;
        }

        // Convert to world coordinates
        Array<Vector2> waypoints = new Array<>();
        for (GridCell cell : path) {
            waypoints.add(gridToWorld(cell.x, cell.y));
        }

        // Simplify the path to reduce waypoint density
        return simplifyPath(waypoints);
    }

    /**
     * Simplifies a path by removing unnecessary intermediate waypoints.
     * Uses line-of-sight testing to skip waypoints when direct path is clear.
     */
    private Array<Vector2> simplifyPath(Array<Vector2> path) {
        if (path == null || path.size <= 2) {
            return path; // Already minimal
        }

        Array<Vector2> simplified = new Array<>();
        simplified.add(path.get(0)); // Always keep start

        int currentIndex = 0;
        while (currentIndex < path.size - 1) {
            // Try to skip as many waypoints as possible
            int farthestVisible = currentIndex + 1;

            // Check how far ahead we can see
            for (int i = currentIndex + 2; i < path.size; i++) {
                if (hasLineOfSight(path.get(currentIndex), path.get(i))) {
                    farthestVisible = i;
                } else {
                    break; // Can't see further, stop checking
                }
            }

            // Add the farthest visible waypoint
            simplified.add(path.get(farthestVisible));
            currentIndex = farthestVisible;
        }

        return simplified;
    }

    /**
     * Checks if there's a clear line of sight between two points.
     * Samples points along the line to detect obstacles.
     */
    private boolean hasLineOfSight(Vector2 from, Vector2 to) {
        float distance = from.dst(to);
        // Sample more frequently for better corner detection (every cell instead of every 2 cells)
        int samples = Math.max(3, (int)(distance / tileSize));

        for (int i = 0; i <= samples; i++) {
            float t = i / (float)samples;
            float x = from.x + (to.x - from.x) * t;
            float y = from.y + (to.y - from.y) * t;

            if (!isWalkable(x, y)) {
                return false; // Obstacle in the way
            }
        }

        return true; // Clear line of sight
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTileSize() {
        return tileSize;
    }

    public NavigationGrid<GridCell> getGrid() {
        return grid;
    }
}
