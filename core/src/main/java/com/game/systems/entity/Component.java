package com.game.systems.entity;

/**
 * Base interface for entity components.
 * Components are pure data or behavior units that can be attached to entities.
 */
public interface Component {
    /**
     * Update this component.
     * @param delta Time since last update in seconds
     */
    default void update(float delta) {}

    /**
     * Called when component is added to an entity.
     */
    default void onAttach() {}

    /**
     * Called when component is removed from an entity.
     */
    default void onDetach() {}
}
