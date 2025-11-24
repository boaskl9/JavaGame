package com.game.networking;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.input.InputSource;

/**
 * InputSource implementation for network-controlled players.
 * Receives input from network packets instead of local keyboard/mouse.
 * Used by the server to control remote players.
 */
public class NetworkInputSource implements InputSource {
    private final Vector2 movementInput = new Vector2();
    private final Vector2 aimDirection = new Vector2(1, 0); // Default right
    private boolean attackPressed = false;
    private boolean attackJustPressed = false;
    private boolean running = false;
    private boolean enabled = true;

    // Track previous attack state to detect "just pressed"
    private boolean wasAttackPressed = false;

    public NetworkInputSource() {
    }

    /**
     * Update this input source with data from an InputPacket.
     */
    public void updateFromPacket(InputPacket packet) {
        movementInput.set(packet.getMovementX(), packet.getMovementY());
        aimDirection.set(packet.getAimDirectionX(), packet.getAimDirectionY());

        // Track previous attack state
        wasAttackPressed = attackPressed;
        attackPressed = packet.isAttackPressed();

        // Attack just pressed if it's pressed now but wasn't last update
        attackJustPressed = attackPressed && !wasAttackPressed;

        running = packet.isRunning();
    }

    @Override
    public Vector2 getMovementInput() {
        return movementInput;
    }

    @Override
    public boolean isAttackPressed() {
        return attackPressed;
    }

    @Override
    public boolean isAttackJustPressed() {
        boolean result = attackJustPressed;
        // Reset after being read once (to prevent multiple attacks)
        attackJustPressed = false;
        return result;
    }

    @Override
    public Vector2 getAimDirection() {
        return aimDirection;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void update(float delta) {
        // Network input doesn't need per-frame updates
        // It's updated when packets arrive via updateFromPacket()
    }
}
