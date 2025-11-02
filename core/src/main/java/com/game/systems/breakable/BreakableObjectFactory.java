package com.game.systems.breakable;

import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.breakables.Pot;

/**
 * Factory for creating breakable objects based on type string.
 * Maps object type identifiers to concrete classes.
 *
 * To add new breakable types:
 * 1. Create a new class extending BreakableEntity (e.g., Crate.java)
 * 2. Add entry to BreakableObjectConfig.json
 * 3. Add case to create() method below
 */
public class BreakableObjectFactory {

    /**
     * Creates a breakable object of the specified type.
     *
     * @param objectType Type identifier (e.g., "pot", "crate", "barrel")
     * @param x World X position
     * @param y World Y position
     * @return The created breakable object, or null if type not recognized
     */
    public static BreakableEntity create(String objectType, float x, float y) {
        if (objectType == null) {
            System.err.println("BreakableObjectFactory: null objectType");
            return null;
        }

        // Map type string to concrete class
        switch (objectType.toLowerCase()) {
            case "pot":
            case "clay_pot":
                return new Pot(x, y);

            // Future types:
            // case "crate":
            //     return new Crate(x, y);
            // case "barrel":
            //     return new Barrel(x, y);
            // case "chest":
            //     return new Chest(x, y);

            default:
                System.err.println("BreakableObjectFactory: Unknown object type: " + objectType);
                return null;
        }
    }

    /**
     * Checks if a breakable object type is supported.
     */
    public static boolean isSupported(String objectType) {
        if (objectType == null) return false;

        switch (objectType.toLowerCase()) {
            case "pot":
            case "clay_pot":
                return true;
            default:
                return false;
        }
    }
}
