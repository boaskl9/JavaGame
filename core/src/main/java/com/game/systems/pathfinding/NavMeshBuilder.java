package com.game.systems.pathfinding;

import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;
import com.game.systems.collision.SpatialQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a navigation mesh from collision data.
 * Uses Delaunay triangulation on walkable points.
 */
public class NavMeshBuilder {

    /**
     * Builds a NavMesh from the collision system.
     *
     * @param worldWidth World width in pixels
     * @param worldHeight World height in pixels
     * @param collisionSystem Collision system with obstacles
     * @param sampleDistance Distance between sample points (larger = fewer triangles)
     * @return Generated NavMesh
     */
    public static NavMesh build(float worldWidth, float worldHeight,
                                 SpatialQuery collisionSystem,
                                 float sampleDistance) {
        System.out.println("NavMeshBuilder: Building NavMesh for " + worldWidth + "x" + worldHeight + " world");
        System.out.println("NavMeshBuilder: Sample distance: " + sampleDistance);

        NavMesh navMesh = new NavMesh(worldWidth, worldHeight);

        // Step 1: Generate walkable sample points
        List<Vector2> walkablePoints = generateWalkablePoints(
            worldWidth, worldHeight, collisionSystem, sampleDistance
        );

        System.out.println("NavMeshBuilder: Found " + walkablePoints.size() + " walkable points");

        if (walkablePoints.size() < 3) {
            System.out.println("NavMeshBuilder: Not enough walkable points for triangulation");
            return navMesh;
        }

        // Step 2: Triangulate using Delaunay
        List<NavMeshTriangle> triangles = triangulate(walkablePoints);

        System.out.println("NavMeshBuilder: Created " + triangles.size() + " triangles");

        // Step 3: Filter out triangles that overlap with obstacles
        List<NavMeshTriangle> validTriangles = filterTriangles(triangles, collisionSystem);

        System.out.println("NavMeshBuilder: " + validTriangles.size() + " valid triangles after filtering");

        // Step 4: Add triangles to NavMesh
        for (NavMeshTriangle triangle : validTriangles) {
            navMesh.addTriangle(triangle);
        }

        // Step 5: Build neighbor connections
        navMesh.buildConnections();

        return navMesh;
    }

    /**
     * Generates a grid of walkable sample points.
     */
    private static List<Vector2> generateWalkablePoints(float worldWidth, float worldHeight,
                                                          SpatialQuery collisionSystem,
                                                          float sampleDistance) {
        List<Vector2> points = new ArrayList<>();

        // Add world corners
        points.add(new Vector2(0, 0));
        points.add(new Vector2(worldWidth, 0));
        points.add(new Vector2(worldWidth, worldHeight));
        points.add(new Vector2(0, worldHeight));

        // Sample points in a grid
        for (float x = sampleDistance; x < worldWidth; x += sampleDistance) {
            for (float y = sampleDistance; y < worldHeight; y += sampleDistance) {
                // Check if this point is walkable (not in a collider)
                if (!collisionSystem.testPoint(x, y)) {
                    points.add(new Vector2(x, y));
                }
            }
        }

        // Add boundary points along edges
        for (float x = sampleDistance; x < worldWidth; x += sampleDistance) {
            if (!collisionSystem.testPoint(x, 0)) {
                points.add(new Vector2(x, 0));
            }
            if (!collisionSystem.testPoint(x, worldHeight)) {
                points.add(new Vector2(x, worldHeight));
            }
        }

        for (float y = sampleDistance; y < worldHeight; y += sampleDistance) {
            if (!collisionSystem.testPoint(0, y)) {
                points.add(new Vector2(0, y));
            }
            if (!collisionSystem.testPoint(worldWidth, y)) {
                points.add(new Vector2(worldWidth, y));
            }
        }

        return points;
    }

    /**
     * Triangulates points using Delaunay triangulation.
     */
    private static List<NavMeshTriangle> triangulate(List<Vector2> points) {
        // Convert points to float array for DelaunayTriangulator
        float[] pointsArray = new float[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            pointsArray[i * 2] = points.get(i).x;
            pointsArray[i * 2 + 1] = points.get(i).y;
        }

        // Triangulate
        DelaunayTriangulator triangulator = new DelaunayTriangulator();
        ShortArray triangleIndices = triangulator.computeTriangles(pointsArray, false);

        // Convert to NavMeshTriangles
        List<NavMeshTriangle> triangles = new ArrayList<>();
        int triangleId = 0;

        for (int i = 0; i < triangleIndices.size; i += 3) {
            int idx1 = triangleIndices.get(i);
            int idx2 = triangleIndices.get(i + 1);
            int idx3 = triangleIndices.get(i + 2);

            Vector2 a = points.get(idx1);
            Vector2 b = points.get(idx2);
            Vector2 c = points.get(idx3);

            triangles.add(new NavMeshTriangle(triangleId++, a, b, c));
        }

        return triangles;
    }

    /**
     * Filters out triangles that overlap with collision obstacles.
     */
    private static List<NavMeshTriangle> filterTriangles(List<NavMeshTriangle> triangles,
                                                           SpatialQuery collisionSystem) {
        List<NavMeshTriangle> validTriangles = new ArrayList<>();

        for (NavMeshTriangle triangle : triangles) {
            // Check if triangle center is walkable
            if (collisionSystem.testPoint(triangle.center.x, triangle.center.y)) {
                continue; // Center is in a collider, skip this triangle
            }

            // Check if triangle has reasonable area (filter out slivers)
            if (triangle.getArea() < 1.0f) {
                continue; // Too small
            }

            validTriangles.add(triangle);
        }

        return validTriangles;
    }
}
