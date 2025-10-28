package com.game.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

/**
 * Temporary entity that displays floating damage numbers.
 * Floats upward and fades out over time.
 */
public class DamageNumberEntity extends GameObject {
    private static final float LIFETIME = 1.0f; // Total duration in seconds
    private static final float FLOAT_SPEED = 30f; // Pixels per second upward
    private static final float FADE_START = 0.3f; // Start fading after this many seconds

    private final String damageText;
    private final BitmapFont font;
    private final Color color;
    private float lifeTimer;
    private Transform transform;

    /**
     * Creates a damage number entity.
     * @param x World X position
     * @param y World Y position
     * @param damage Damage amount to display
     * @param font Font to render with
     */
    public DamageNumberEntity(float x, float y, int damage, BitmapFont font) {
        this.damageText = String.valueOf(damage);
        this.font = font;
        this.lifeTimer = 0f;

        // Set color based on damage amount (more damage = darker red)
        if (damage >= 10) {
            this.color = new Color(1f, 0f, 0f, 1f); // Bright red for high damage
        } else if (damage >= 5) {
            this.color = new Color(1f, 0.3f, 0f, 1f); // Orange for medium damage
        } else {
            this.color = new Color(1f, 1f, 0f, 1f); // Yellow for low damage
        }

        transform = new Transform(x, y);
        addComponent(transform);
    }

    /**
     * Creates a healing number entity (green color).
     */
    public DamageNumberEntity(float x, float y, int healAmount, BitmapFont font, boolean isHealing) {
        this.damageText = "+" + healAmount;
        this.font = font;
        this.lifeTimer = 0f;
        this.color = new Color(0f, 1f, 0f, 1f); // Green for healing

        transform = new Transform(x, y);
        addComponent(transform);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        lifeTimer += delta;

        // Float upward
        transform.setPosition(transform.getX(), transform.getY() + FLOAT_SPEED * delta);

        // Check if lifetime expired
        if (lifeTimer >= LIFETIME) {
            // Mark for removal (will be handled by WorldManager)
            setAlive(false);
        }
    }

    /**
     * Renders the damage number.
     * @param batch The sprite batch to render with
     */
    public void render(SpriteBatch batch) {
        if (!isAlive()) return;

        // Calculate alpha based on lifetime
        float alpha = 1f;
        if (lifeTimer > FADE_START) {
            // Fade out after FADE_START seconds
            float fadeProgress = (lifeTimer - FADE_START) / (LIFETIME - FADE_START);
            alpha = 1f - fadeProgress;
        }

        // Set font color with alpha
        Color renderColor = new Color(color);
        renderColor.a = alpha;
        font.setColor(renderColor);

        // Scale text slightly based on lifetime (start larger, shrink)
        float scale = 0.5f - (lifeTimer / LIFETIME) * 0.4f; // 1.2 -> 0.8
        font.getData().setScale(scale);

        // Render text centered on position using GlyphLayout for accurate measurement
        GlyphLayout layout = new GlyphLayout(font, damageText);
        float textX = transform.getX() - layout.width / 2f;
        float textY = transform.getY() + layout.height / 2f;

        font.draw(batch, damageText, textX, textY);

        // Reset font scale and color to avoid affecting other text
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    /**
     * Simple alive tracking for removal.
     */
    private boolean alive = true;

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public Transform getTransform() {
        return transform;
    }
}
