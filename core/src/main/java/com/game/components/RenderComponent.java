package com.game.components;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.game.systems.entity.Component;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

/**
 * Component that handles rendering.
 * Separates rendering logic from entity logic.
 */
public class RenderComponent implements Component {
    private int width;
    private int height;
    private float offsetX;
    private float offsetY;

    public RenderComponent(int width, int height) {
        this.width = width;
        this.height = height;
        this.offsetX = 0;
        this.offsetY = 0;
    }

    /**
     * Render using an animation component.
     */
    public void render(SpriteBatch batch, GameObject gameObject) {
        Transform transform = gameObject.getComponent(Transform.class);
        AnimationComponent animComp = gameObject.getComponent(AnimationComponent.class);

        if (transform == null) return;

        if (animComp != null) {
            TextureRegion frame = animComp.getCurrentFrame();
            if (frame != null) {
                batch.draw(frame,
                    transform.getX() + offsetX,
                    transform.getY() + offsetY,
                    width, height);
            }
        }
    }

    public void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
