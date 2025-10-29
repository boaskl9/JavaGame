package com.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

/**
 * Temporary entity that plays a death animation and then removes itself.
 */
public class DeathAnimationEntity extends GameObject {
    private static final float FRAME_DURATION = 0.15f; // Duration per frame
    private static final int FRAME_COUNT = 5; // Death animation has 5 frames

    private Transform transform;
    private Animation<TextureRegion> deathAnimation;
    private float stateTime;
    private boolean alive;

    /**
     * Creates a death animation at the specified position.
     * @param x World X position
     * @param y World Y position
     */
    public DeathAnimationEntity(float x, float y) {
        this.stateTime = 0f;
        this.alive = true;

        // Set up transform
        transform = new Transform(x, y);
        addComponent(transform);

        // Load death animation sprite
        Texture spiritSheet = new Texture(Gdx.files.internal("FX/Magic/Spirit/SpriteSheet.png"));

        // Create animation (5 frames in a horizontal sprite sheet)
        int frameWidth = spiritSheet.getWidth() / FRAME_COUNT;
        int frameHeight = spiritSheet.getHeight();

        TextureRegion[] frames = new TextureRegion[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = new TextureRegion(spiritSheet, i * frameWidth, 0, frameWidth, frameHeight);
        }

        // Create LibGDX animation (non-looping)
        deathAnimation = new Animation<>(FRAME_DURATION, frames);
        deathAnimation.setPlayMode(Animation.PlayMode.NORMAL); // Play once, no loop
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        stateTime += delta;

        // Remove when animation is done
        if (deathAnimation.isAnimationFinished(stateTime)) {
            alive = false;
        }
    }

    /**
     * Renders the death animation.
     */
    public void render(SpriteBatch batch) {
        if (!alive) return;

        TextureRegion currentFrame = deathAnimation.getKeyFrame(stateTime);
        if (currentFrame != null) {
            // Center the animation on the position
            float width = currentFrame.getRegionWidth();
            float height = currentFrame.getRegionHeight();

            batch.draw(
                currentFrame,
                transform.getX() - width / 2f,
                transform.getY() - height / 2f,
                width,
                height
            );
        }
    }

    public boolean isAlive() {
        return alive;
    }

    public Transform getTransform() {
        return transform;
    }
}
