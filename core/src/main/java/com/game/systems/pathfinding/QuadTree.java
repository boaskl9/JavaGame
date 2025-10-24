package com.game.systems.pathfinding;

import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Quadtree for spatial indexing of NavMesh triangles.
 * Enables fast O(log n) lookup of triangles by position.
 */
public class QuadTree {
    private static final int MAX_OBJECTS = 10;
    private static final int MAX_LEVELS = 5;

    private int level;
    private List<NavMeshTriangle> objects;
    private Rectangle bounds;
    private QuadTree[] nodes;

    /**
     * Creates a new QuadTree.
     * @param level Current level (0 = root)
     * @param bounds Bounding rectangle for this node
     */
    public QuadTree(int level, Rectangle bounds) {
        this.level = level;
        this.objects = new ArrayList<>();
        this.bounds = bounds;
        this.nodes = new QuadTree[4];
    }

    /**
     * Clears the quadtree.
     */
    public void clear() {
        objects.clear();

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].clear();
                nodes[i] = null;
            }
        }
    }

    /**
     * Splits the node into 4 sub-nodes.
     */
    private void split() {
        float subWidth = bounds.width / 2f;
        float subHeight = bounds.height / 2f;
        float x = bounds.x;
        float y = bounds.y;

        nodes[0] = new QuadTree(level + 1, new Rectangle(x + subWidth, y, subWidth, subHeight));
        nodes[1] = new QuadTree(level + 1, new Rectangle(x, y, subWidth, subHeight));
        nodes[2] = new QuadTree(level + 1, new Rectangle(x, y + subHeight, subWidth, subHeight));
        nodes[3] = new QuadTree(level + 1, new Rectangle(x + subWidth, y + subHeight, subWidth, subHeight));
    }

    /**
     * Determine which node the triangle belongs to.
     * -1 means triangle cannot completely fit within a child node and must be stored at parent level.
     */
    private int getIndex(NavMeshTriangle triangle) {
        int index = -1;
        double verticalMidpoint = bounds.x + (bounds.width / 2);
        double horizontalMidpoint = bounds.y + (bounds.height / 2);

        // Get triangle bounds
        float minX = Math.min(Math.min(triangle.a.x, triangle.b.x), triangle.c.x);
        float maxX = Math.max(Math.max(triangle.a.x, triangle.b.x), triangle.c.x);
        float minY = Math.min(Math.min(triangle.a.y, triangle.b.y), triangle.c.y);
        float maxY = Math.max(Math.max(triangle.a.y, triangle.b.y), triangle.c.y);

        // Object can completely fit within the top quadrants
        boolean topQuadrant = (minY > horizontalMidpoint);
        // Object can completely fit within the bottom quadrants
        boolean bottomQuadrant = (maxY < horizontalMidpoint);

        // Object can completely fit within the left quadrants
        if (maxX < verticalMidpoint) {
            if (topQuadrant) {
                index = 2;
            } else if (bottomQuadrant) {
                index = 1;
            }
        }
        // Object can completely fit within the right quadrants
        else if (minX > verticalMidpoint) {
            if (topQuadrant) {
                index = 3;
            } else if (bottomQuadrant) {
                index = 0;
            }
        }

        return index;
    }

    /**
     * Insert a triangle into the quadtree.
     */
    public void insert(NavMeshTriangle triangle) {
        if (nodes[0] != null) {
            int index = getIndex(triangle);

            if (index != -1) {
                nodes[index].insert(triangle);
                return;
            }
        }

        objects.add(triangle);

        if (objects.size() > MAX_OBJECTS && level < MAX_LEVELS) {
            if (nodes[0] == null) {
                split();
            }

            int i = 0;
            while (i < objects.size()) {
                int index = getIndex(objects.get(i));
                if (index != -1) {
                    nodes[index].insert(objects.remove(i));
                } else {
                    i++;
                }
            }
        }
    }

    /**
     * Return all triangles that could contain the given point.
     */
    public List<NavMeshTriangle> retrieve(List<NavMeshTriangle> returnObjects, float x, float y) {
        // Determine which quadrant the point belongs to
        int index = -1;
        if (nodes[0] != null) {
            double verticalMidpoint = bounds.x + (bounds.width / 2);
            double horizontalMidpoint = bounds.y + (bounds.height / 2);

            boolean topQuadrant = (y > horizontalMidpoint);
            boolean bottomQuadrant = (y < horizontalMidpoint);

            if (x < verticalMidpoint) {
                if (topQuadrant) {
                    index = 2;
                } else if (bottomQuadrant) {
                    index = 1;
                }
            } else if (x > verticalMidpoint) {
                if (topQuadrant) {
                    index = 3;
                } else if (bottomQuadrant) {
                    index = 0;
                }
            }
        }

        if (index != -1 && nodes[0] != null) {
            nodes[index].retrieve(returnObjects, x, y);
        }

        returnObjects.addAll(objects);

        return returnObjects;
    }

    /**
     * Return all triangles that could intersect the given rectangle.
     */
    public List<NavMeshTriangle> retrieve(List<NavMeshTriangle> returnObjects, Rectangle rect) {
        if (nodes[0] != null) {
            double verticalMidpoint = bounds.x + (bounds.width / 2);
            double horizontalMidpoint = bounds.y + (bounds.height / 2);

            boolean topQuadrant = rect.y > horizontalMidpoint;
            boolean bottomQuadrant = rect.y + rect.height < horizontalMidpoint;
            boolean leftQuadrant = rect.x + rect.width < verticalMidpoint;
            boolean rightQuadrant = rect.x > verticalMidpoint;

            if (leftQuadrant) {
                if (topQuadrant) {
                    nodes[2].retrieve(returnObjects, rect);
                } else if (bottomQuadrant) {
                    nodes[1].retrieve(returnObjects, rect);
                } else {
                    // Spans both top and bottom
                    nodes[1].retrieve(returnObjects, rect);
                    nodes[2].retrieve(returnObjects, rect);
                }
            } else if (rightQuadrant) {
                if (topQuadrant) {
                    nodes[3].retrieve(returnObjects, rect);
                } else if (bottomQuadrant) {
                    nodes[0].retrieve(returnObjects, rect);
                } else {
                    // Spans both top and bottom
                    nodes[0].retrieve(returnObjects, rect);
                    nodes[3].retrieve(returnObjects, rect);
                }
            } else {
                // Spans both left and right - query all quadrants
                if (topQuadrant) {
                    nodes[2].retrieve(returnObjects, rect);
                    nodes[3].retrieve(returnObjects, rect);
                } else if (bottomQuadrant) {
                    nodes[0].retrieve(returnObjects, rect);
                    nodes[1].retrieve(returnObjects, rect);
                } else {
                    // Spans all quadrants
                    for (QuadTree node : nodes) {
                        node.retrieve(returnObjects, rect);
                    }
                }
            }
        }

        returnObjects.addAll(objects);

        return returnObjects;
    }
}
