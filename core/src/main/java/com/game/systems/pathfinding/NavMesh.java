package com.game.systems.pathfinding;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.*;

/**
 * Navigation mesh for pathfinding.
 * Consists of triangles representing walkable areas.
 * Uses QuadTree for spatial indexing to speed up triangle lookups.
 */
public class NavMesh {
    private final List<NavMeshTriangle> triangles;
    private final float worldWidth;
    private final float worldHeight;
    private QuadTree spatialIndex;

    public NavMesh(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.triangles = new ArrayList<>();
        this.spatialIndex = new QuadTree(0, new Rectangle(0, 0, worldWidth, worldHeight));
    }

    /**
     * Adds a triangle to the mesh and spatial index.
     */
    public void addTriangle(NavMeshTriangle triangle) {
        triangles.add(triangle);
        spatialIndex.insert(triangle);
    }

    /**
     * Builds neighbor connections between triangles using vertex hashing.
     * Call this after all triangles have been added.
     * Optimized from O(n²) to O(n) using vertex-based lookup.
     */
    public void buildConnections() {
        long startTime = System.currentTimeMillis();
        System.out.println("NavMesh: Building connections between " + triangles.size() + " triangles...");

        int connectionsChecked = 0;
        int connectionsFound = 0;

        // Build a map of vertices to triangles that use them
        // Two triangles share an edge if they share exactly 2 vertices
        Map<String, List<NavMeshTriangle>> vertexToTriangles = new HashMap<>();

        for (NavMeshTriangle tri : triangles) {
            Vector2[] vertices = {tri.a, tri.b, tri.c};
            for (Vector2 v : vertices) {
                String key = vertexKey(v);
                vertexToTriangles.computeIfAbsent(key, k -> new ArrayList<>()).add(tri);
            }
        }

        // For each triangle, check only triangles that share at least one vertex
        for (NavMeshTriangle tri1 : triangles) {
            // Use a set to avoid checking the same triangle multiple times
            Set<NavMeshTriangle> candidates = new HashSet<>();

            // Add all triangles that share any vertex
            Vector2[] vertices = {tri1.a, tri1.b, tri1.c};
            for (Vector2 v : vertices) {
                String key = vertexKey(v);
                List<NavMeshTriangle> sharing = vertexToTriangles.get(key);
                if (sharing != null) {
                    candidates.addAll(sharing);
                }
            }

            for (NavMeshTriangle tri2 : candidates) {
                // Skip self and already-connected neighbors
                if (tri1 == tri2 || tri1.neighbors.contains(tri2)) {
                    continue;
                }

                connectionsChecked++;

                if (sharesEdge(tri1, tri2)) {
                    tri1.addNeighbor(tri2);
                    tri2.addNeighbor(tri1);
                    connectionsFound++;
                }
            }
        }

        long buildTime = System.currentTimeMillis() - startTime;
        System.out.println("NavMesh: Connections built in " + buildTime + "ms " +
                          "(" + connectionsChecked + " checks, " + connectionsFound + " connections found)");
    }

    /**
     * Creates a hash key for a vertex (rounded to 0.1 precision).
     */
    private String vertexKey(Vector2 v) {
        int x = Math.round(v.x * 10);
        int y = Math.round(v.y * 10);
        return x + "," + y;
    }

    /**
     * Checks if two triangles share an edge.
     */
    private boolean sharesEdge(NavMeshTriangle t1, NavMeshTriangle t2) {
        Vector2[] v1 = {t1.a, t1.b, t1.c};
        Vector2[] v2 = {t2.a, t2.b, t2.c};

        int sharedVertices = 0;
        for (Vector2 vertex1 : v1) {
            for (Vector2 vertex2 : v2) {
                if (vertex1.epsilonEquals(vertex2, 0.1f)) {
                    sharedVertices++;
                    break;
                }
            }
        }

        return sharedVertices == 2; // Share exactly 2 vertices = shared edge
    }

    /**
     * Finds the triangle containing a point.
     */
    public NavMeshTriangle findTriangle(Vector2 point) {
        return findTriangle(point.x, point.y);
    }

    /**
     * Finds the triangle containing a point using spatial indexing.
     * Optimized from O(n) to O(log n) using QuadTree.
     */
    public NavMeshTriangle findTriangle(float x, float y) {
        // Query spatial index for candidate triangles
        List<NavMeshTriangle> candidates = spatialIndex.retrieve(new ArrayList<>(), x, y);

        // Check only nearby triangles
        for (NavMeshTriangle triangle : candidates) {
            if (triangle.contains(x, y)) {
                return triangle;
            }
        }
        return null;
    }

    /**
     * Finds a path from start to end position.
     * Returns waypoints, or null if no path exists.
     */
    public Array<Vector2> findPath(Vector2 start, Vector2 end) {
        NavMeshTriangle startTri = findTriangle(start);
        NavMeshTriangle endTri = findTriangle(end);

        // If start is outside mesh, find closest point on mesh
        Vector2 actualStart = start;
        if (startTri == null) {
            actualStart = findClosestPointOnMesh(start);
            startTri = findTriangle(actualStart);

            if (startTri == null) {
                System.out.println("NavMesh: Could not find valid start position near " + start);
                return null;
            }
        }

        // If end is outside mesh, find closest point on mesh
        Vector2 actualEnd = end;
        if (endTri == null) {
            actualEnd = findClosestPointOnMesh(end);
            endTri = findTriangle(actualEnd);

            if (endTri == null) {
                System.out.println("NavMesh: Could not find valid end position near " + end);
                return null;
            }
        }

        // If start and end are in the same triangle, direct path
        if (startTri == endTri) {
            Array<Vector2> path = new Array<>();
            path.add(new Vector2(actualEnd));
            return path;
        }

        // Run A* on triangle graph
        List<NavMeshTriangle> trianglePath = aStarTriangles(startTri, endTri);

        if (trianglePath == null || trianglePath.isEmpty()) {
            return null;
        }

        // Convert triangle path to waypoints
        return trianglePathToWaypoints(actualStart, actualEnd, trianglePath);
    }

    /**
     * Finds the closest point on the NavMesh to a given position.
     * Returns the closest triangle center if point is outside mesh.
     */
    public Vector2 findClosestPointOnMesh(Vector2 point) {
        if (triangles.isEmpty()) {
            return new Vector2(point);
        }

        NavMeshTriangle closestTriangle = null;
        float closestDistance = Float.MAX_VALUE;

        // Find the triangle with the closest center
        for (NavMeshTriangle triangle : triangles) {
            float distance = point.dst(triangle.center);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTriangle = triangle;
            }
        }

        // Return the center of the closest triangle
        return closestTriangle != null ? new Vector2(closestTriangle.center) : new Vector2(point);
    }

    /**
     * A* pathfinding on the triangle graph.
     */
    private List<NavMeshTriangle> aStarTriangles(NavMeshTriangle start, NavMeshTriangle goal) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Set<NavMeshTriangle> closedSet = new HashSet<>();
        Map<NavMeshTriangle, NavMeshTriangle> cameFrom = new HashMap<>();
        Map<NavMeshTriangle, Double> gScore = new HashMap<>();

        gScore.put(start, 0.0);
        openSet.add(new AStarNode(start, 0.0, heuristic(start, goal)));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();

            if (current.triangle == goal) {
                return reconstructPath(cameFrom, current.triangle);
            }

            closedSet.add(current.triangle);

            for (NavMeshTriangle neighbor : current.triangle.neighbors) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double tentativeGScore = gScore.get(current.triangle) +
                    current.triangle.center.dst(neighbor.center);

                if (!gScore.containsKey(neighbor) || tentativeGScore < gScore.get(neighbor)) {
                    cameFrom.put(neighbor, current.triangle);
                    gScore.put(neighbor, tentativeGScore);

                    double fScore = tentativeGScore + heuristic(neighbor, goal);
                    openSet.add(new AStarNode(neighbor, tentativeGScore, fScore));
                }
            }
        }

        return null; // No path found
    }

    /**
     * Heuristic for A* (Euclidean distance between triangle centers).
     */
    private double heuristic(NavMeshTriangle a, NavMeshTriangle b) {
        return a.center.dst(b.center);
    }

    /**
     * Reconstructs the path from A* result.
     */
    private List<NavMeshTriangle> reconstructPath(Map<NavMeshTriangle, NavMeshTriangle> cameFrom,
                                                    NavMeshTriangle current) {
        List<NavMeshTriangle> path = new ArrayList<>();
        path.add(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current); // Add to front
        }

        return path;
    }

    /**
     * Converts a triangle path to waypoints.
     * Uses the shared edge midpoints between triangles.
     */
    private Array<Vector2> trianglePathToWaypoints(Vector2 start, Vector2 end,
                                                     List<NavMeshTriangle> trianglePath) {
        Array<Vector2> waypoints = new Array<>();

        for (int i = 0; i < trianglePath.size() - 1; i++) {
            NavMeshTriangle current = trianglePath.get(i);
            NavMeshTriangle next = trianglePath.get(i + 1);

            // Find the edge midpoint to the next triangle
            int neighborIndex = current.neighbors.indexOf(next);
            if (neighborIndex >= 0 && neighborIndex < current.edgeMidpoints.size()) {
                waypoints.add(new Vector2(current.edgeMidpoints.get(neighborIndex)));
            }
        }

        // Add final destination
        waypoints.add(new Vector2(end));

        return waypoints;
    }

    /**
     * Gets all triangles (for debug rendering).
     */
    public List<NavMeshTriangle> getTriangles() {
        return triangles;
    }

    /**
     * Helper class for A* algorithm.
     */
    private static class AStarNode {
        final NavMeshTriangle triangle;
        final double gScore;
        final double fScore;

        AStarNode(NavMeshTriangle triangle, double gScore, double fScore) {
            this.triangle = triangle;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }
}
