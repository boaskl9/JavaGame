package com.game.components;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.game.systems.entity.Component;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

/**
 * Component that handles rendering a weapon sprite separately from the character.
 * Allows weapons to rotate, animate, and be layered properly.
 */
public class WeaponRenderComponent implements Component {

    private Texture weaponTexture;
    private float weaponWidth;
    private float weaponHeight;

    // Weapon attachment point (offset from entity center)
    private float attachOffsetX;  // X offset from entity center to weapon handle
    private float attachOffsetY;  // Y offset from entity center to weapon handle

    // Weapon pivot point (where weapon rotates around, relative to weapon sprite)
    private float pivotX;  // X position in weapon sprite (typically handle/grip)
    private float pivotY;  // Y position in weapon sprite (typically handle/grip)

    // Rendering
    private boolean renderInFront = true;  // Whether weapon renders in front of character

    public WeaponRenderComponent() {
        this.weaponTexture = null;
        this.weaponWidth = 16f;
        this.weaponHeight = 16f;
        this.attachOffsetX = 0f;
        this.attachOffsetY = 0f;
        this.pivotX = 2f;  // Default: pivot near left edge (handle)
        this.pivotY = 8f;  // Default: pivot at vertical center
    }

    /**
     * Sets the weapon sprite to render.
     *
     * @param texture The weapon texture
     * @param width Width to render
     * @param height Height to render
     */
    public void setWeaponSprite(Texture texture, float width, float height) {
        this.weaponTexture = texture;
        this.weaponWidth = width;
        this.weaponHeight = height;
    }

    /**
     * Sets the attachment point offset from entity center.
     *
     * @param offsetX X offset in pixels
     * @param offsetY Y offset in pixels
     */
    public void setAttachmentPoint(float offsetX, float offsetY) {
        this.attachOffsetX = offsetX;
        this.attachOffsetY = offsetY;
    }

    /**
     * Sets the pivot point within the weapon sprite (where it rotates around).
     *
     * @param pivotX X position in weapon sprite (typically the handle)
     * @param pivotY Y position in weapon sprite
     */
    public void setPivotPoint(float pivotX, float pivotY) {
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    /**
     * Renders the weapon sprite.
     *
     * @param batch SpriteBatch to render with
     * @param gameObject The entity this component belongs to
     */
    public void render(SpriteBatch batch, GameObject gameObject) {
        if (weaponTexture == null) {
            return; // No weapon to render
        }

        Transform transform = gameObject.getComponent(Transform.class);
        AttackComponent attackComponent = gameObject.getComponent(AttackComponent.class);

        if (transform == null) {
            return;
        }

        // Calculate weapon rotation
        float weaponAngle = 0f;
        if (attackComponent != null && attackComponent.isAttacking()) {
            weaponAngle = attackComponent.getAbsoluteWeaponAngle();
        }

        // Calculate weapon position (entity center + attachment offset)
        float entityCenterX = transform.getX() + 8f; // Assuming 16x16 entity
        float entityCenterY = transform.getY() + 8f;

        float weaponX = entityCenterX + attachOffsetX;
        float weaponY = entityCenterY + attachOffsetY;

        // Render weapon with rotation
        // LibGDX draws from bottom-left, rotates around origin (in sprite space)
        batch.draw(
            weaponTexture,
            weaponX - pivotX,  // Position adjusted for pivot
            weaponY - pivotY,
            pivotX,            // Origin X (rotation point in sprite space)
            pivotY,            // Origin Y
            weaponWidth,       // Width
            weaponHeight,      // Height
            1f,                // Scale X
            1f,                // Scale Y
            weaponAngle,       // Rotation in degrees
            0,                 // Source X
            0,                 // Source Y
            (int) weaponWidth, // Source width
            (int) weaponHeight,// Source height
            false,             // Flip X
            false              // Flip Y
        );
    }

    /**
     * Determines if weapon should render in front of character based on facing direction.
     *
     * @param facingAngle Character facing angle (0=right, 90=up, 180=left, 270=down)
     */
    public void updateRenderOrder(int facingAngle) {
        // Weapon renders behind character when facing up, in front otherwise
        renderInFront = (facingAngle != 0); // Up is 0 degrees
    }

    // Getters

    public boolean shouldRenderInFront() {
        return renderInFront;
    }

    public Texture getWeaponTexture() {
        return weaponTexture;
    }

    public void setRenderInFront(boolean renderInFront) {
        this.renderInFront = renderInFront;
    }
}
