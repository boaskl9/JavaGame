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

    // Interaction
    INTERACT,

    // Debug
    DEBUG_SPAWN_ITEM,
    DEBUG_SPAWN_BAG,
    DEBUG_SPAWN_BAG2,
    DEBUG_SPAWN_BAG3,
    DEBUG_TOGGLE
}
