package com.game.systems.input;

/**
 * Enumeration of all input actions in the game.
 * Each action can be bound to a key.
 */
public enum InputAction {
    // Movement
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,

    // Inventory
    OPEN_INVENTORY,

    // UI
    OPEN_SETTINGS,

    // Interaction
    INTERACT,

    // Debug
    DEBUG_SPAWN_ITEM,
    DEBUG_SPAWN_BAG,
    DEBUG_SPAWN_BAG2,
    DEBUG_SPAWN_BAG3,
    DEBUG_SPAWN_SLIME,
    DEBUG_SPAWN_FROG,
    DEBUG_SPAWN_CAT,
    DEBUG_TOGGLE,
    DEBUG_CONSOLE
}
