package com.game.components;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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

    // Frame-based animation support (null for single-sprite weapons)
    private TextureRegion[] animationFrames;
    private boolean useFrameAnimation;

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
        this.animationFrames = null;
        this.useFrameAnimation = false;
        this.attachOffsetX = 0f;
        this.attachOffsetY = 0f;
        this.pivotX = 2;  // Default: pivot near left edge (handle)
        this.pivotY = 8f;  // Default: pivot at vertical center
    }

    /**
     * Sets the weapon sprite to render.
     * Automatically uses the texture's actual dimensions.
     * For single-sprite (non-animated) weapons.
     *
     * @param texture The weapon texture
     */
    public void setWeaponSprite(Texture texture) {
        this.weaponTexture = texture;
        this.animationFrames = null;
        this.useFrameAnimation = false;

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
     * Sets weapon animation from a sprite sheet with multiple frames.
     * Used for weapons with frame-based attack animations (e.g., claw attacks).
     * Frames should be arranged horizontally in the sprite sheet.
     *
     * @param spriteSheet The texture containing all animation frames
     * @param frameCount Number of frames in the sprite sheet
     */
    public void setWeaponAnimationFrames(Texture spriteSheet, int frameCount) {
        if (spriteSheet == null || frameCount <= 0) {
            this.weaponTexture = null;
            this.animationFrames = null;
            this.useFrameAnimation = false;
            return;
        }

        this.weaponTexture = spriteSheet;
        this.useFrameAnimation = true;

        int frameWidth = spriteSheet.getWidth() / frameCount;
        int frameHeight = spriteSheet.getHeight();

        this.weaponWidth = frameWidth;
        this.weaponHeight = frameHeight;

        // Create texture regions for each frame
        this.animationFrames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            this.animationFrames[i] = new TextureRegion(
                spriteSheet,
                i * frameWidth, 0,
                frameWidth, frameHeight
            );
        }

        // Default pivot point at handle
        this.pivotX = 0;
        this.pivotY = 0;
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
     * Supports both single-sprite and frame-based animation.
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

        // Calculate perpendicular vector for centering sprite on attack line
        float perpX = -dirY;
        float perpY = dirX;

        // Weapon starts offset from entity center in the attack direction
        float forwardOffset = 6f; // Distance from player center to weapon start
        float spriteHalfWidth = weaponWidth / 2f;

        float weaponHandleX = entityCenterX
            + dirX * forwardOffset                    // Move forward in attack direction
            + perpX * (pivotX + spriteHalfWidth);     // Center perpendicular to attack
        float weaponHandleY = entityCenterY
            + dirY * forwardOffset
            + perpY * (pivotX + spriteHalfWidth);

        // Select appropriate frame or texture for rendering
        if (useFrameAnimation && animationFrames != null) {
            // Frame-based animation - select frame based on attack phase
            int frameIndex = selectAnimationFrame(attackComponent);
            TextureRegion currentFrame = animationFrames[frameIndex];

            // TextureRegion.draw has different signature than Texture.draw
            // Note: No flip parameters - flip the region beforehand if needed
            batch.draw(
                currentFrame,
                weaponHandleX - pivotX,  // Position adjusted for pivot
                weaponHandleY - pivotY,
                pivotX,                  // Origin X (rotation point in sprite space = handle)
                pivotY,                  // Origin Y
                weaponWidth,             // Width
                weaponHeight,            // Height
                1f,                      // Scale X
                1f,                      // Scale Y
                weaponAngle - 90         // Rotation (-90 because sprite default points up)
            );
        } else {
            // Single-sprite rendering (existing behavior)
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
    }

    /**
     * Selects the appropriate animation frame based on attack phase and progress.
     * Frame distribution:
     * - Frame 0: WINDUP phase
     * - Frames 1-3: ACTIVE phase (interpolated based on progress)
     * - Frame 3: RECOVERY phase (hold last frame)
     *
     * @param attackComponent The attack component
     * @return Frame index (0 to frameCount-1)
     */
    private int selectAnimationFrame(AttackComponent attackComponent) {
        if (animationFrames == null || animationFrames.length == 0) {
            return 0;
        }

        AttackComponent.AttackPhase phase = attackComponent.getPhase();
        float progress = attackComponent.getPhaseProgress();
        int frameCount = animationFrames.length;

        switch (phase) {
            case WINDUP:
                // Frame 0 during windup
                return 0;

            case ACTIVE:
                // Interpolate through frames 1 to (frameCount-1) during active phase
                if (frameCount <= 1) return 0;

                int activeFrames = frameCount - 1; // Frames 1, 2, 3...
                int frameIndex = 1 + (int) (progress * activeFrames);
                return Math.min(frameIndex, frameCount - 1);

            case RECOVERY:
                // Hold last frame during recovery
                return frameCount - 1;

            case IDLE:
            case COOLDOWN:
            default:
                return 0;
        }
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
