package com.game.systems.debug;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple manager for debug visualization flags.
 * Stores and manages boolean flags for various debug rendering features.
 */
public class DebugManager {
    private final Map<String, Boolean> flags = new HashMap<>();

    public DebugManager() {
        // Initialize default flags
        flags.put("colliders", false);
        flags.put("navmesh", false);
        flags.put("fps", false);
        flags.put("yrender", false);
        flags.put("frames", false);
        flags.put("all", false);
    }

    /**
     * Enable a specific debug feature.
     * @param feature The feature name (colliders, navmesh, fps, etc.)
     */
    public void enable(String feature) {
        if ("all".equals(feature)) {
            enableAll();
        } else if ("none".equals(feature)) {
            disableAll();
        } else {
            flags.put(feature, true);
        }
    }

    /**
     * Disable a specific debug feature.
     * @param feature The feature name
     */
    public void disable(String feature) {
        if ("all".equals(feature)) {
            disableAll();
        } else {
            flags.put(feature, false);
        }
    }

    /**
     * Toggle a debug feature on/off.
     * @param feature The feature name
     */
    public void toggle(String feature) {
        if ("all".equals(feature)) {
            boolean anyEnabled = flags.values().stream().anyMatch(b -> b);
            if (anyEnabled) {
                disableAll();
            } else {
                enableAll();
            }
        } else {
            flags.put(feature, !isEnabled(feature));
        }
    }

    /**
     * Check if a debug feature is enabled.
     * @param feature The feature name
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled(String feature) {
        return flags.getOrDefault(feature, false) || flags.getOrDefault("all", false);
    }

    /**
     * Enable all debug features.
     */
    private void enableAll() {
        for (String key : flags.keySet()) {
            flags.put(key, true);
        }
    }

    /**
     * Disable all debug features.
     */
    private void disableAll() {
        for (String key : flags.keySet()) {
            flags.put(key, false);
        }
    }

    /**
     * Get all available feature names.
     * @return Array of feature names
     */
    public String[] getFeatures() {
        return new String[]{"colliders", "navmesh", "fps", "yrender", "frames", "all", "none"};
    }
}
