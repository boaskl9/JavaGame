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
        this.pivotX = 2;  // Default: pivot near left edge (handle)
        this.pivotY = 8f;  // Default: pivot at vertical center
    }

    /**
     * Sets the weapon sprite to render.
     * Automatically uses the texture's actual dimensions.
     *
     * @param texture The weapon texture
     */
    public void setWeaponSprite(Texture texture) {
        this.weaponTexture = texture;
        if (texture != null) {
            this.weaponWidth = texture.getWidth();
            this.weaponHeight = texture.getHeight();
            // Default pivot point at bottom center (handle of weapon)
            this.pivotX = 0;
            this.pivotY = 0; // Near bottom edge (handle)
        } else {
            this.weaponWidth = 0;
            this.weaponHeight = 0;
        }
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
     * Only shows weapon during attack phases (WINDUP, ACTIVE, RECOVERY).
     * Uses the same positioning calculations as the hitbox for perfect alignment.
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

        if (transform == null || attackComponent == null) {
            return;
        }

        // Only render weapon during attack animation (not during IDLE or COOLDOWN)
        if (!attackComponent.isAttacking()) {
            return;
        }

        if (attackComponent.getCurrentWeapon() == null) {
            return;
        }

        // Get attack strategy
        com.game.systems.combat.AttackStrategy strategy =
            com.game.systems.combat.AttackSystem.getStrategy(attackComponent.getCurrentWeapon().getType());

        // Use the EXACT same calculations as the hitbox
        float baseAngle = attackComponent.getAttackDirection();
        float angleOffset = strategy.getWeaponAngleOffset(attackComponent, attackComponent.getCurrentWeapon());
        float weaponAngle = baseAngle + angleOffset;

        // Calculate entity center (same as hitbox)
        float entityCenterX = transform.getX() + 8f; // Assuming 16x16 entity
        float entityCenterY = transform.getY() + 8f;

        // Calculate direction vector (same as hitbox)
        float angleRad = (float) Math.toRadians(weaponAngle);
        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);

        // Weapon handle is at the entity center (where hitbox starts)
        float weaponHandleX = entityCenterX;
        float weaponHandleY = entityCenterY;

        // Render weapon sprite extending from handle in the direction of the hitbox
        batch.draw(
            weaponTexture,
            weaponHandleX - pivotX,  // Position adjusted for pivot
            weaponHandleY - pivotY,
            pivotX,                  // Origin X (rotation point in sprite space = handle)
            pivotY,                  // Origin Y
            weaponWidth,             // Width
            weaponHeight,            // Height
            1f,                      // Scale X
            1f,                      // Scale Y
            weaponAngle - 90,        // Rotation (-90 because sprite default points up)
            0,                       // Source X
            0,                       // Source Y
            (int) weaponWidth,       // Source width
            (int) weaponHeight,      // Source height
            false,                   // Flip X
            false                    // Flip Y
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
