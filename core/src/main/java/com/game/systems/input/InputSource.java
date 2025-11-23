package com.game.systems.input;

import com.badlogic.gdx.math.Vector2;

/**
 * Abstraction for player input sources.
 * This allows for different input methods (local keyboard, network, AI, replays, etc.).
 *
 * Key design principle: Each PlayerEntity has its own InputSource.
 * - LocalKeyboardInput: For local players (different key sets per player)
 * - NetworkInputSource: For remote players (receives input over network) - Phase 2
 * - AIInputSource: For bots/testing - Future
 */
public interface InputSource {
    /**
     * Get the current movement input direction.
     * @return Normalized direction vector (or zero vector if no input)
     */
    Vector2 getMovementInput();

    /**
     * Check if attack is pressed this frame.
     * @return true if attack button is pressed
     */
    boolean isAttackPressed();

    /**
     * Check if attack was just pressed this frame (rising edge).
     * @return true if attack button was just pressed
     */
    boolean isAttackJustPressed();

    /**
     * Get the aim direction for attacks.
     * For mouse input: direction from player to mouse cursor
     * For controller: right stick direction
     * For network: last received aim direction
     * @return Direction vector for aiming
     */
    Vector2 getAimDirection();

    /**
     * Check if running modifier is held.
     * @return true if running
     */
    boolean isRunning();

    /**
     * Check if input is enabled.
     * Used to disable input during UI interactions, cutscenes, etc.
     * @return true if input should be processed
     */
    boolean isEnabled();

    /**
     * Enable or disable this input source.
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * Update input state (for edge detection, etc.).
     * Call once per frame before reading input.
     * @param delta Time since last frame
     */
    void update(float delta);
}
