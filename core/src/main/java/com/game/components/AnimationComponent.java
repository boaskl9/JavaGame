package com.game.components;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.game.systems.animation.SpriteAnimator;
import com.game.systems.entity.Component;

/**
 * Component that wraps the SpriteAnimator system.
 * This is glue code that integrates the animation system with the entity system.
 */
public class AnimationComponent implements Component {
    private SpriteAnimator animator;
    private String currentState;
    private int currentDirection;

    public AnimationComponent() {
        this.animator = new SpriteAnimator();
        this.currentState = "idle";
        this.currentDirection = 0;
    }

    public void setState(String state, Vector2 direction, boolean flip) {
        this.currentState = state;
        animator.setState(state, direction, flip);
    }

    public void setState(String state, int directionAngle, boolean flip) {
        this.currentState = state;
        this.currentDirection = directionAngle;
        animator.setState(state, directionAngle, flip);
    }

    @Override
    public void update(float delta) {
        animator.update(delta);
    }

    public TextureRegion getCurrentFrame() {
        return animator.getCurrentFrame();
    }

    public SpriteAnimator getAnimator() {
        return animator;
    }

    public String getCurrentState() {
        return currentState;
    }

    public int getCurrentDirection() {
        return currentDirection;
    }
}
