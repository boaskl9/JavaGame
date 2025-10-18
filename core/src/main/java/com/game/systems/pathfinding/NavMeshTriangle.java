package com.game.systems.pathfinding;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a triangle in the navigation mesh.
 * Each triangle is a walkable area and knows its neighbors.
 */
public class NavMeshTriangle {
    public final Vector2 a, b, c; // Vertices
    public final Vector2 center;  // Center point (for A* heuristic)
    public final int id;          // Unique identifier

    // Navigation graph
    public final List<NavMeshTriangle> neighbors;
    public final List<Vector2> edgeMidpoints; // Midpoints of shared edges with neighbors

    public NavMeshTriangle(int id, Vector2 a, Vector2 b, Vector2 c) {
        this.id = id;
        this.a = new Vector2(a);
        this.b = new Vector2(b);
        this.c = new Vector2(c);

        // Calculate center (centroid)
        this.center = new Vector2(
            (a.x + b.x + c.x) / 3f,
            (a.y + b.y + c.y) / 3f
        );

        this.neighbors = new ArrayList<>();
        this.edgeMidpoints = new ArrayList<>();
    }

    /**
     * Checks if a point is inside this triangle.
     */
    public boolean contains(Vector2 point) {
        return contains(point.x, point.y);
    }

    /**
     * Checks if a point is inside this triangle using barycentric coordinates.
     */
    public boolean contains(float px, float py) {
        float denominator = ((b.y - c.y) * (a.x - c.x) + (c.x - b.x) * (a.y - c.y));
        if (Math.abs(denominator) < 0.0001f) return false; // Degenerate triangle

        float alpha = ((b.y - c.y) * (px - c.x) + (c.x - b.x) * (py - c.y)) / denominator;
        float beta = ((c.y - a.y) * (px - c.x) + (a.x - c.x) * (py - c.y)) / denominator;
        float gamma = 1.0f - alpha - beta;

        return alpha >= 0 && beta >= 0 && gamma >= 0;
    }

    /**
     * Adds a neighbor triangle and calculates the shared edge midpoint.
     */
    public void addNeighbor(NavMeshTriangle neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);

            // Find shared edge midpoint
            Vector2 midpoint = findSharedEdgeMidpoint(neighbor);
            if (midpoint != null) {
                edgeMidpoints.add(midpoint);
            } else {
                // Fallback to center if no shared edge (shouldn't happen)
                edgeMidpoints.add(new Vector2(center));
            }
        }
    }

    /**
     * Finds the midpoint of the shared edge with another triangle.
     */
    private Vector2 findSharedEdgeMidpoint(NavMeshTriangle other) {
        Vector2[] myVertices = {a, b, c};
        Vector2[] otherVertices = {other.a, other.b, other.c};

        List<Vector2> sharedVertices = new ArrayList<>();

        for (Vector2 myVert : myVertices) {
            for (Vector2 otherVert : otherVertices) {
                if (myVert.epsilonEquals(otherVert, 0.1f)) {
                    sharedVertices.add(myVert);
                    break;
                }
            }
        }

        if (sharedVertices.size() == 2) {
            // Return midpoint of shared edge
            return new Vector2(
                (sharedVertices.get(0).x + sharedVertices.get(1).x) / 2f,
                (sharedVertices.get(0).y + sharedVertices.get(1).y) / 2f
            );
        }

        return null;
    }

    /**
     * Gets the area of this triangle.
     */
    public float getArea() {
        return Math.abs((a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y)) / 2f);
    }

    @Override
    public String toString() {
        return "Triangle[" + id + "] at " + center;
    }
}
