package com.game.util;

/**
 * Interface for game singletons that need lifecycle management.
 * Provides a unified way to reset singletons when creating new game sessions.
 *
 * All game singletons should implement this interface to ensure proper
 * cleanup and re-initialization when transitioning between game sessions
 * (e.g., returning to main menu and starting a new game).
 *
 * Note: Each singleton should also provide a static isInitialized() method
 * for checking initialization status without creating an instance.
 */
public interface GameSingleton {
    /**
     * Reset this singleton's state and dispose any resources.
     * This should:
     * 1. Dispose/release any held resources (textures, sounds, etc.)
     * 2. Clear any cached data or references
     * 3. Null out the singleton instance
     *
     * After calling reset(), the singleton should be ready to be re-initialized.
     */
    void reset();
}
