package com.game.systems.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.game.systems.entity.Transform;

/**
 * Local keyboard/mouse input for a player.
 * Supports different key sets to allow multiple local players.
 *
 * Example configurations:
 * - Player 1: WASD + Mouse (default)
 * - Player 2: Arrow keys + Different attack button
 */
public class LocalKeyboardInput implements InputSource {
    private final int upKey;
    private final int downKey;
    private final int leftKey;
    private final int rightKey;
    private final int runKey;
    private final int attackButton; // Mouse button code

    private Camera camera; // Needed for mouse world position
    private Transform playerTransform; // Needed for aim direction calculation

    private boolean enabled = true;

    // For edge detection
    private boolean wasAttackPressed = false;

    /**
     * Create input with custom key bindings.
     * @param upKey Up movement key
     * @param downKey Down movement key
     * @param leftKey Left movement key
     * @param rightKey Right movement key
     * @param runKey Run modifier key
     * @param attackButton Mouse button for attack (from Input.Buttons)
     */
    public LocalKeyboardInput(int upKey, int downKey, int leftKey, int rightKey,
                              int runKey, int attackButton) {
        this.upKey = upKey;
        this.downKey = downKey;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
        this.runKey = runKey;
        this.attackButton = attackButton;
    }

    /**
     * Create input with default WASD + Mouse bindings (Player 1).
     */
    public static LocalKeyboardInput createPlayer1() {
        return new LocalKeyboardInput(
            Input.Keys.W,
            Input.Keys.S,
            Input.Keys.A,
            Input.Keys.D,
            Input.Keys.SHIFT_LEFT,
            Input.Buttons.LEFT
        );
    }

    /**
     * Create input with Arrow keys + Mouse bindings (Player 2).
     */
    public static LocalKeyboardInput createPlayer2() {
        return new LocalKeyboardInput(
            Input.Keys.UP,
            Input.Keys.DOWN,
            Input.Keys.LEFT,
            Input.Keys.RIGHT,
            Input.Keys.SHIFT_RIGHT,
            Input.Buttons.RIGHT // Right click for player 2 attack
        );
    }

    /**
     * Set the camera for mouse position calculations.
     * @param camera The game camera
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    /**
     * Set the player transform for aim direction calculations.
     * @param playerTransform The player's transform component
     */
    public void setPlayerTransform(Transform playerTransform) {
        this.playerTransform = playerTransform;
    }

    @Override
    public Vector2 getMovementInput() {
        if (!enabled) return Vector2.Zero;

        Vector2 input = new Vector2();

        if (Gdx.input.isKeyPressed(upKey)) {
            input.y += 1;
        }
        if (Gdx.input.isKeyPressed(downKey)) {
            input.y -= 1;
        }
        if (Gdx.input.isKeyPressed(leftKey)) {
            input.x -= 1;
        }
        if (Gdx.input.isKeyPressed(rightKey)) {
            input.x += 1;
        }

        // Normalize diagonal movement
        if (input.len() > 0) {
            input.nor();
        }

        return input;
    }

    @Override
    public boolean isAttackPressed() {
        if (!enabled) return false;
        return Gdx.input.isButtonPressed(attackButton);
    }

    @Override
    public boolean isAttackJustPressed() {
        if (!enabled) return false;
        boolean isPressed = Gdx.input.isButtonPressed(attackButton);
        boolean justPressed = isPressed && !wasAttackPressed;
        return justPressed;
    }

    @Override
    public Vector2 getAimDirection() {
        if (!enabled || camera == null || playerTransform == null) {
            return new Vector2(1, 0); // Default: aim right
        }

        // Get mouse position in world coordinates
        Vector3 screenCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        Vector3 worldCoords = camera.unproject(screenCoords);

        // Calculate direction from player to mouse
        float playerCenterX = playerTransform.getX() + 8; // Assuming 16x16 player size
        float playerCenterY = playerTransform.getY() + 8;

        Vector2 direction = new Vector2(
            worldCoords.x - playerCenterX,
            worldCoords.y - playerCenterY
        );

        if (direction.len() > 0) {
            direction.nor();
        } else {
            direction.set(1, 0); // Default: aim right
        }

        return direction;
    }

    @Override
    public boolean isRunning() {
        if (!enabled) return false;
        return Gdx.input.isKeyPressed(runKey);
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
        // Update edge detection state
        wasAttackPressed = Gdx.input.isButtonPressed(attackButton);
    }
}
