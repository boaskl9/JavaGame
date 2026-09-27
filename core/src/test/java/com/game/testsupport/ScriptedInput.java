package com.game.testsupport;

import com.badlogic.gdx.math.Vector2;
import com.game.systems.input.InputSource;

/**
 * An InputSource tests can drive: hold a direction, or press attack once toward an angle.
 */
public class ScriptedInput implements InputSource {
    private final Vector2 movement = new Vector2();
    private final Vector2 aim = new Vector2(1, 0);
    private boolean attackQueued = false;
    private boolean attackThisFrame = false;
    private boolean enabled = true;

    public void move(float x, float y) {
        movement.set(x, y);
    }

    /** Press attack for exactly one frame, aimed at the given angle in degrees. */
    public void attack(float angleDegrees) {
        aim.set(1, 0).setAngleDeg(angleDegrees);
        attackQueued = true;
    }

    @Override
    public void update(float delta) {
        attackThisFrame = attackQueued;
        attackQueued = false;
    }

    @Override
    public Vector2 getMovementInput() {
        return enabled ? movement : Vector2.Zero;
    }

    @Override
    public boolean isAttackPressed() {
        return enabled && attackThisFrame;
    }

    @Override
    public boolean isAttackJustPressed() {
        return enabled && attackThisFrame;
    }

    @Override
    public Vector2 getAimDirection() {
        return aim;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
