package com.game.systems.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages input bindings and provides a centralized way to check input.
 * Supports rebindable keys.
 */
public class InputManager {
    private final Map<InputAction, Integer> keyBindings;
    private final Map<InputAction, Boolean> actionPressed;
    private final Map<InputAction, Boolean> actionJustPressed;

    public InputManager() {
        this.keyBindings = new HashMap<>();
        this.actionPressed = new HashMap<>();
        this.actionJustPressed = new HashMap<>();

        // Set default key bindings
        setDefaultBindings();
    }

    /**
     * Sets the default key bindings.
     */
    private void setDefaultBindings() {
        // Movement
        bind(InputAction.MOVE_UP, Input.Keys.W);
        bind(InputAction.MOVE_DOWN, Input.Keys.S);
        bind(InputAction.MOVE_LEFT, Input.Keys.A);
        bind(InputAction.MOVE_RIGHT, Input.Keys.D);

        // Inventory
        bind(InputAction.OPEN_INVENTORY, Input.Keys.B);

        // Interaction
        bind(InputAction.INTERACT, Input.Keys.E);

        // Debug
        bind(InputAction.DEBUG_SPAWN_ITEM, Input.Keys.NUM_1);
        bind(InputAction.DEBUG_SPAWN_BAG, Input.Keys.NUM_2);
        bind(InputAction.DEBUG_TOGGLE, Input.Keys.F3);
    }

    /**
     * Binds an action to a key.
     * @param action The input action
     * @param keyCode The key code (from Input.Keys)
     */
    public void bind(InputAction action, int keyCode) {
        keyBindings.put(action, keyCode);
    }

    /**
     * Updates input state. Should be called once per frame.
     */
    public void update() {
        for (InputAction action : InputAction.values()) {
            Integer keyCode = keyBindings.get(action);
            if (keyCode != null) {
                boolean wasPressed = actionPressed.getOrDefault(action, false);
                boolean isPressed = Gdx.input.isKeyPressed(keyCode);

                actionPressed.put(action, isPressed);
                actionJustPressed.put(action, isPressed && !wasPressed);
            }
        }
    }

    /**
     * Checks if an action is currently pressed.
     * @param action The input action
     * @return true if pressed
     */
    public boolean isPressed(InputAction action) {
        return actionPressed.getOrDefault(action, false);
    }

    /**
     * Checks if an action was just pressed this frame.
     * @param action The input action
     * @return true if just pressed
     */
    public boolean isJustPressed(InputAction action) {
        return actionJustPressed.getOrDefault(action, false);
    }

    /**
     * Gets the key code bound to an action.
     * @param action The input action
     * @return The key code, or -1 if not bound
     */
    public int getBinding(InputAction action) {
        return keyBindings.getOrDefault(action, -1);
    }

    /**
     * Gets the name of the key bound to an action.
     * @param action The input action
     * @return The key name
     */
    public String getBindingName(InputAction action) {
        int keyCode = getBinding(action);
        if (keyCode == -1) {
            return "Unbound";
        }
        return Input.Keys.toString(keyCode);
    }

    /**
     * Clears all bindings.
     */
    public void clearBindings() {
        keyBindings.clear();
        actionPressed.clear();
        actionJustPressed.clear();
    }

    /**
     * Resets to default bindings.
     */
    public void resetToDefaults() {
        clearBindings();
        setDefaultBindings();
    }
}
