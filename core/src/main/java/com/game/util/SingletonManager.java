package com.game.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry and lifecycle manager for all game singletons.
 * Provides a single point of control for resetting all singletons
 * when transitioning between game sessions.
 *
 * Usage:
 * 1. Singletons register themselves on first initialization
 * 2. Call resetAllGameSingletons() when starting a new game session
 */
public class SingletonManager {
    private static final List<GameSingleton> registeredSingletons = new ArrayList<>();

    /**
     * Register a singleton for lifecycle management.
     * Should be called by singletons during initialization.
     *
     * @param singleton The singleton to register
     */
    public static void register(GameSingleton singleton) {
        if (!registeredSingletons.contains(singleton)) {
            registeredSingletons.add(singleton);
            System.out.println("SingletonManager: Registered " + singleton.getClass().getSimpleName());
        }
    }

    /**
     * Unregister a singleton from lifecycle management.
     *
     * @param singleton The singleton to unregister
     */
    public static void unregister(GameSingleton singleton) {
        registeredSingletons.remove(singleton);
        System.out.println("SingletonManager: Unregistered " + singleton.getClass().getSimpleName());
    }

    /**
     * Reset all registered game singletons.
     * This should be called when creating a new GameScreen to ensure
     * all singletons are properly cleaned up and ready for re-initialization.
     *
     * Singletons are reset in reverse registration order to handle dependencies.
     */
    public static void resetAllGameSingletons() {
        System.out.println("SingletonManager: Resetting " + registeredSingletons.size() + " singletons...");

        // Reset in reverse order to handle dependencies
        // (last registered = likely most dependent on others)
        for (int i = registeredSingletons.size() - 1; i >= 0; i--) {
            GameSingleton singleton = registeredSingletons.get(i);
            try {
                System.out.println("  - Resetting " + singleton.getClass().getSimpleName());
                singleton.reset();
            } catch (Exception e) {
                System.err.println("  ! Error resetting " + singleton.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Clear the registry after reset
        registeredSingletons.clear();
        System.out.println("SingletonManager: All singletons reset");
    }

    /**
     * Get the number of registered singletons.
     * Useful for debugging.
     *
     * @return Number of registered singletons
     */
    public static int getRegisteredCount() {
        return registeredSingletons.size();
    }

    /**
     * Check if any singletons are registered.
     *
     * @return true if at least one singleton is registered
     */
    public static boolean hasRegisteredSingletons() {
        return !registeredSingletons.isEmpty();
    }
}
