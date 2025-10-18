# Wood Theme UI Setup Guide

## Overview
This guide explains how to set up and use the custom wood-themed UI skin for your game.

## Files Created

1. **`core/build.gradle`** - Added `packWoodTheme` Gradle task
2. **`core/src/main/java/com/game/tools/TexturePackerTool.java`** - Standalone texture packing utility
3. **`core/src/main/java/com/game/systems/ui/SkinManager.java`** - Skin management system
4. **`assets/ui/wood-theme.json`** - Wood theme skin definition
5. **`assets/ui/wood-theme.atlas`** - Atlas file (will be generated)
6. **`assets/ui/wood-theme.png`** - Atlas texture (will be generated)

## Step 1: Pack the Textures

You need to convert your individual PNG files in `assets/ui/Theme/Theme Wood/` into a texture atlas.

### Option A: Using Gradle (Recommended)

Run this command from your project root:

```bash
./gradlew :core:packWoodTheme
```

Or on Windows:

```cmd
gradlew.bat :core:packWoodTheme
```

This will create:
- `assets/ui/wood-theme.atlas` - Atlas definition file
- `assets/ui/wood-theme.png` - Packed texture atlas image

### Option B: Using IntelliJ IDEA

1. Open the Gradle panel (View → Tool Windows → Gradle)
2. Navigate to: JavaGame → core → Tasks → other → packWoodTheme
3. Double-click to run

### Option C: Manual Java Execution

If Gradle doesn't work, you can run the TexturePackerTool directly:

1. Compile the project
2. Run `com.game.tools.TexturePackerTool` as a Java application

## Step 2: Using the Wood Theme in Your Game

### Quick Start

Replace your current Skin loading code with the SkinManager:

```java
// Old way:
// Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

// New way:
import com.game.systems.ui.SkinManager;
import com.game.systems.ui.SkinManager.SkinTheme;

// Initialize with default theme
SkinManager.initialize();

// Or load wood theme directly
SkinManager.loadTheme(SkinTheme.WOOD);

// Get the skin to use in UI code
Skin skin = SkinManager.getSkin();
```

### Switching Themes at Runtime

```java
// Switch to wood theme
SkinManager.loadTheme(SkinTheme.WOOD);

// Switch back to default
SkinManager.loadTheme(SkinTheme.DEFAULT);
```

### Using in UI Code

```java
// Create UI elements with the wood theme
TextButton button = new TextButton("Click Me", SkinManager.getSkin());
Window window = new Window("My Window", SkinManager.getSkin());
CheckBox checkbox = new CheckBox("Option", SkinManager.getSkin());
```

### Example: Updating UIManagerNew

If you're using UIManagerNew, update it like this:

```java
public class UIManagerNew {
    private Skin skin;

    public UIManagerNew() {
        // Load wood theme
        SkinManager.loadTheme(SkinTheme.WOOD);
        this.skin = SkinManager.getSkin();

        // Now create your UI...
    }

    public void dispose() {
        SkinManager.dispose();
    }
}
```

## Step 3: Clean Up

Don't forget to dispose of the skin when your game exits:

```java
@Override
public void dispose() {
    SkinManager.dispose();
    // ... other cleanup
}
```

## Available UI Components

The wood theme includes styles for:

- **Buttons**: `default`, `toggle`
- **Text Buttons**: `default`, `toggle`
- **CheckBox**: `default`, `radio`
- **Windows**: `default`, `dialog`
- **Labels**: `default`, `white`
- **Lists**: `default`
- **SelectBox**: `default`
- **TextField**: `default`
- **Sliders**: `default-horizontal`, `default-vertical`, `hover-horizontal`
- **ProgressBar**: `default-horizontal`, `default-vertical`
- **ScrollPane**: `default`
- **ImageButton**: `default`
- **ImageTextButton**: `default`
- **Tooltips**: `default`

## Customization

### Adding New Styles

Edit `assets/ui/wood-theme.json` to add or modify styles. The file uses LibGDX's Skin JSON format.

### Modifying Colors

The color palette is defined at the top of `wood-theme.json`:

```json
com.badlogic.gdx.graphics.Color: {
  wood-text: { r: 0.2, g: 0.15, b: 0.1, a: 1 },
  wood-dark: { r: 0.4, g: 0.3, b: 0.2, a: 1 },
  wood-light: { r: 0.9, g: 0.8, b: 0.7, a: 1 },
  // ... customize these values
}
```

### Using Custom Atlas Settings

Edit the `packWoodTheme` task in `core/build.gradle` to adjust packing parameters:

```groovy
settings.maxWidth = 2048  // Max atlas width
settings.maxHeight = 2048 // Max atlas height
settings.paddingX = 2     // Horizontal padding
settings.paddingY = 2     // Vertical padding
```

## Troubleshooting

### Atlas files not found
- Make sure you ran the `packWoodTheme` task
- Check that `wood-theme.atlas` and `wood-theme.png` exist in `assets/ui/`

### Images look wrong
- Verify all PNG files are in `assets/ui/Theme/Theme Wood/`
- Re-run the packing task after modifying images

### Skin loading fails
- Check the console for error messages
- Ensure all referenced drawables in the JSON exist in the atlas
- The SkinManager will automatically fall back to the default skin if loading fails

## Asset Attribution

Wood theme assets are located in: `assets/ui/Theme/Theme Wood/`

The theme includes these texture types:
- Buttons (normal, hover, pressed, disabled, checked states)
- Checkboxes and radio buttons
- Sliders (horizontal and vertical)
- Panels and backgrounds (nine-patch)
- Tabs (selected, unselected, hover, disabled)
- Arrows (left/right with hover states)
- Inventory cells

---

**Note**: Make sure to run the texture packer (`packWoodTheme` task) whenever you add, remove, or modify PNG files in the Theme Wood directory!
