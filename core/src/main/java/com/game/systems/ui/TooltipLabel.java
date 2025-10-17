package com.game.systems.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

/**
 * A tooltip window that displays item information on hover.
 */
public class TooltipLabel extends Window {
    private Label contentLabel;
    private static final float MAX_WIDTH = 200f;
    private static final float PADDING = 8f;

    public TooltipLabel(Skin skin) {
        super("", skin);

        contentLabel = new Label("", skin);
        contentLabel.setWrap(true);

        add(contentLabel).width(MAX_WIDTH).pad(PADDING);
        pack();

        setVisible(false);
        setMovable(false);
    }

    /**
     * Shows the tooltip with the given text at the specified position.
     * Position is to the right and below the cursor, with screen boundary checks.
     * @param text The tooltip text
     * @param x Screen x position (cursor)
     * @param y Screen y position (cursor)
     */
    public void show(String text, float x, float y) {
        if (text == null || text.isEmpty()) {
            hide();
            return;
        }

        contentLabel.setText(text);
        pack();

        // Offset from cursor
        float offsetX = 10f; // Right of cursor
        float offsetY = 10f; // Space between cursor and tooltip

        // Calculate desired position
        // Position to the right of cursor, and below it
        // In Scene2D, (x,y) is bottom-left corner, so we subtract tooltip height to place it below cursor
        float tooltipX = x + offsetX;
        float tooltipY = y - getHeight() - offsetY; // Below cursor (cursor Y minus tooltip height minus offset)

        // Screen boundary checks
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float padding = 5f;

        // Keep tooltip on screen (right boundary)
        if (tooltipX + getWidth() > screenWidth - padding) {
            // If too far right, position to left of cursor instead
            tooltipX = x - getWidth() - offsetX;
            // If still off screen, clamp to edge
            if (tooltipX < padding) {
                tooltipX = padding;
            }
        }

        // Keep tooltip on screen (left boundary)
        if (tooltipX < padding) {
            tooltipX = padding;
        }

        // Keep tooltip on screen (bottom boundary)
        if (tooltipY < padding) {
            // If too far down, position above cursor instead
            tooltipY = y + offsetY;
            // If still off screen, clamp to edge
            if (tooltipY + getHeight() > screenHeight - padding) {
                tooltipY = screenHeight - getHeight() - padding;
            }
        }

        // Keep tooltip on screen (top boundary)
        if (tooltipY + getHeight() > screenHeight - padding) {
            tooltipY = screenHeight - getHeight() - padding;
        }

        setPosition(tooltipX, tooltipY);

        setVisible(true);
        toFront();
    }

    /**
     * Hides the tooltip.
     */
    public void hide() {
        setVisible(false);
    }
}
