package com.game.systems.animation;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Helper class for building animations from sprite sheets.
 * Provides utilities for loading and splitting sprite sheets.
 */
public class AnimationBuilder {

    /**
     * Load a sprite sheet with 4 directional animations (down, up, left, right).
     * Layout: 4 columns (directions) x N rows (frames)
     *
     * @param animator The animator to add animations to
     * @param stateName The state name for these animations
     * @param spriteSheet The texture containing the sprite sheet
     * @param framesPerDirection Number of frames per direction
     * @param frameDuration Duration of each frame
     */
    public static void loadFourDirectional(SpriteAnimator animator, String stateName,
                                          Texture spriteSheet, int framesPerDirection,
                                          float frameDuration) {
        int frameWidth = spriteSheet.getWidth() / 4;
        int frameHeight = spriteSheet.getHeight() / framesPerDirection;

        TextureRegion[][] frames = TextureRegion.split(spriteSheet, frameWidth, frameHeight);

        // Extract frames for each direction
        TextureRegion[] downFrames = new TextureRegion[framesPerDirection];
        TextureRegion[] upFrames = new TextureRegion[framesPerDirection];
        TextureRegion[] leftFrames = new TextureRegion[framesPerDirection];

        for (int i = 0; i < framesPerDirection; i++) {
            downFrames[i] = frames[i][0];  // Column 0 - Down
            upFrames[i] = frames[i][1];    // Column 1 - Up
            leftFrames[i] = frames[i][2];  // Column 2 - Left (right uses flip)
        }

        // Add animations with standard angles
        animator.addAnimation(stateName, 180, downFrames, frameDuration);    // Down
        animator.addAnimation(stateName, 0, upFrames, frameDuration);        // Up
        animator.addAnimation(stateName, 90, leftFrames, frameDuration);     // Left
        animator.addAnimation(stateName, 270, leftFrames, frameDuration);    // Right (same frames, will flip)
    }

    /**
     * Load a sprite sheet with 8 directional animations.
     * Layout: 8 columns (directions) x N rows (frames)
     */
    public static void loadEightDirectional(SpriteAnimator animator, String stateName,
                                           Texture spriteSheet, int framesPerDirection,
                                           float frameDuration) {
        int frameWidth = spriteSheet.getWidth() / 8;
        int frameHeight = spriteSheet.getHeight() / framesPerDirection;

        TextureRegion[][] frames = TextureRegion.split(spriteSheet, frameWidth, frameHeight);

        int[] angles = {180, 135, 90, 45, 0, 315, 270, 225}; // 8 directions

        for (int dir = 0; dir < 8; dir++) {
            TextureRegion[] dirFrames = new TextureRegion[framesPerDirection];
            for (int i = 0; i < framesPerDirection; i++) {
                dirFrames[i] = frames[i][dir];
            }
            animator.addAnimation(stateName, angles[dir], dirFrames, frameDuration);
        }
    }

    /**
     * Load a single static frame for all directions (useful for idle states).
     */
    public static void loadStatic(SpriteAnimator animator, String stateName,
                                  Texture spriteSheet) {
        loadFourDirectional(animator, stateName, spriteSheet, 1, 1.0f);
    }
}
