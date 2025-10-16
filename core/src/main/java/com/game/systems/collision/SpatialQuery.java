package com.game.systems.collision;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Shape2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone spatial collision query system.
 * Can be used in any project for 2D collision detection.
 * No dependencies on game-specific classes.
 */
public class SpatialQuery {
    private List<Rectangle> rectangles = new ArrayList<>();
    private List<Polygon> polygons = new ArrayList<>();

    /**
     * Add a rectangular collision shape.
     */
    public void addRectangle(float x, float y, float width, float height) {
        rectangles.add(new Rectangle(x, y, width, height));
    }

    /**
     * Add a rectangular collision shape.
     */
    public void addRectangle(Rectangle rect) {
        rectangles.add(new Rectangle(rect));
    }

    /**
     * Add a polygonal collision shape.
     */
    public void addPolygon(Polygon poly) {
        polygons.add(poly);
    }

    /**
     * Check if a point collides with any registered shapes.
     */
    public boolean testPoint(float x, float y) {
        for (Rectangle rect : rectangles) {
            if (rect.contains(x, y)) {
                return true;
            }
        }

        for (Polygon poly : polygons) {
            if (poly.contains(x, y)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a rectangular area collides with any shapes.
     * Tests the four corners and center point.
     */
    public boolean testArea(float x, float y, float width, float height) {
        // Check corners
        if (testPoint(x, y)) return true;
        if (testPoint(x + width, y)) return true;
        if (testPoint(x, y + height)) return true;
        if (testPoint(x + width, y + height)) return true;

        // Check center
        if (testPoint(x + width/2, y + height/2)) return true;

        return false;
    }

    /**
     * Check if a rectangle overlaps with any registered shapes.
     */
    public boolean testRectangle(Rectangle testRect) {
        // Check against all rectangles
        for (Rectangle rect : rectangles) {
            if (testRect.overlaps(rect)) {
                return true;
            }
        }

        // Check against polygons using corner/center test
        return testArea(testRect.x, testRect.y, testRect.width, testRect.height);
    }

    /**
     * Find all rectangles that intersect with the given rectangle.
     */
    public List<Rectangle> queryRectangles(Rectangle testRect) {
        List<Rectangle> results = new ArrayList<>();
        for (Rectangle rect : rectangles) {
            if (testRect.overlaps(rect)) {
                results.add(rect);
            }
        }
        return results;
    }

    /**
     * Clear all collision shapes.
     */
    public void clear() {
        rectangles.clear();
        polygons.clear();
    }

    /**
     * Get all registered rectangles (useful for debug rendering).
     */
    public List<Rectangle> getRectangles() {
        return new ArrayList<>(rectangles);
    }

    /**
     * Get all registered polygons (useful for debug rendering).
     */
    public List<Polygon> getPolygons() {
        return new ArrayList<>(polygons);
    }

    /**
     * Get the total number of collision shapes.
     */
    public int getShapeCount() {
        return rectangles.size() + polygons.size();
    }
}
