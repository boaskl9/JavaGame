package com.game.tools;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

/**
 * Utility class to pack UI textures into an atlas.
 * Run this class to generate the wood theme texture atlas.
 */
public class TexturePackerTool {

    public static void main(String[] args) {
        // Settings for the texture packer
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.duplicatePadding = false;
        settings.edgePadding = true;
        settings.stripWhitespaceX = false;
        settings.stripWhitespaceY = false;
        settings.atlasExtension = ".atlas";
        settings.filterMin = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        settings.filterMag = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;

        // Input directory containing the wood theme PNGs
        String inputDir = "assets/ui/Theme/Theme Wood";

        // Output directory and atlas name
        String outputDir = "assets/ui";
        String atlasName = "wood-theme";

        System.out.println("Packing textures from: " + inputDir);
        System.out.println("Output to: " + outputDir + "/" + atlasName + ".atlas");

        try {
            TexturePacker.process(settings, inputDir, outputDir, atlasName);
            System.out.println("Texture packing completed successfully!");
        } catch (Exception e) {
            System.err.println("Error packing textures: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
