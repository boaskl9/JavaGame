package com.game.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.game.systems.entity.Component;

/**
 * Component that follows a path of waypoints.
 * Used by enemies to navigate around obstacles using A* pathfinding.
 */
public class PathFollowComponent implements Component {
    private Array<Vector2> currentPath;
    private int currentWaypointIndex;
    private float arrivalThreshold = 4f; // How close to get to a waypoint before moving to next

    // Stuck detection
    private Vector2 lastPosition = new Vector2();
    private float stuckTimer = 0f;
    private static final float STUCK_THRESHOLD = 1.5f; // If stuck for 1.5 seconds, path is invalid
    private static final float MIN_MOVEMENT = 2f; // Must move at least 2 pixels per second

    public PathFollowComponent() {
        this.currentPath = null;
        this.currentWaypointIndex = 0;
    }

    @Override
    public void update(float delta) {
        // Path following logic handled by the entity
    }

    @Override
    public void onAttach() {
        // No-op
    }

    @Override
    public void onDetach() {
        // No-op
    }

    /**
     * Sets a new path to follow.
     */
    public void setPath(Array<Vector2> path) {
        this.currentPath = path;
        this.currentWaypointIndex = 0;
    }

    /**
     * Clears the current path.
     */
    public void clearPath() {
        this.currentPath = null;
        this.currentWaypointIndex = 0;
    }

    /**
     * Gets the current target waypoint.
     */
    public Vector2 getCurrentTarget() {
        if (currentPath == null || currentWaypointIndex >= currentPath.size) {
            return null;
        }
        return currentPath.get(currentWaypointIndex);
    }

    /**
     * Advances to the next waypoint in the path if close enough to current target.
     * Returns true if we reached the final destination.
     */
    public boolean updatePath(Vector2 currentPosition) {
        if (currentPath == null || currentWaypointIndex >= currentPath.size) {
            return true; // No path or finished
        }

        Vector2 target = currentPath.get(currentWaypointIndex);
        float dx = target.x - currentPosition.x;
        float dy = target.y - currentPosition.y;
        float distanceSquared = dx * dx + dy * dy;

        // Check if we're close enough to the current waypoint
        if (distanceSquared < arrivalThreshold * arrivalThreshold) {
            currentWaypointIndex++;
            stuckTimer = 0f; // Reset stuck timer when reaching waypoint
            lastPosition.set(currentPosition);

            // Check if we've reached the end
            if (currentWaypointIndex >= currentPath.size) {
                currentPath = null;
                return true; // Reached destination
            }
        }

        return false; // Still following path
    }

    /**
     * Updates stuck detection. Call this every frame.
     * Returns true if the entity is stuck and path should be recalculated.
     */
    public boolean updateStuckDetection(Vector2 currentPosition, float delta) {
        if (currentPath == null || !hasPath()) {
            stuckTimer = 0f;
            return false;
        }

        // Check how far we've moved since last check
        float distanceMoved = currentPosition.dst(lastPosition);

        if (distanceMoved < MIN_MOVEMENT * delta) {
            // Not moving much - might be stuck
            stuckTimer += delta;

            if (stuckTimer >= STUCK_THRESHOLD) {
                // Stuck for too long - clear path to force recalculation
                clearPath();
                stuckTimer = 0f;
                return true;
            }
        } else {
            // Moving properly - reset stuck timer
            stuckTimer = 0f;
        }

        lastPosition.set(currentPosition);
        return false;
    }

    /**
     * Checks if we have an active path.
     */
    public boolean hasPath() {
        return currentPath != null && currentWaypointIndex < currentPath.size;
    }

    /**
     * Gets the current path.
     */
    public Array<Vector2> getPath() {
        return currentPath;
    }

    public float getArrivalThreshold() {
        return arrivalThreshold;
    }

    public void setArrivalThreshold(float threshold) {
        this.arrivalThreshold = threshold;
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }
}
