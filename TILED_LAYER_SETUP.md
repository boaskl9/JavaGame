# Tiled Map Layer Setup Guide

For proper Y-sorting (depth ordering) to work in your game, you need to configure your Tiled map layers with custom properties.

## Layer Types

The `YSortRenderer` automatically detects layers based on **custom properties** you set in Tiled.

### How to Set Custom Properties in Tiled:

1. Select a layer in the Layers panel
2. In the Properties panel, click the **+** button to add a custom property
3. Add the properties below as needed

### 1. **Background Layers** (Rendered First - No Sorting) ⬇️
These layers are always behind entities (terrain, ground, paths).

**Custom Properties:** None needed (this is the default)

**Use for:**
- Terrain
- Ground tiles
- Floor tiles
- Paths
- Water base

### 2. **Y-Sorted Layers** (Rendered With Entities - Sorted by Y Position) 🔀
These layers contain objects that should be sorted with entities (trees, houses, furniture).

**Custom Properties:**
- Property name: `foregroundRender`
- Property type: `bool`
- Value: `true` ✓

**Use for:**
- Trees
- Buildings/Houses
- Furniture
- Fences
- Decorative objects
- Anything that should have depth with the player

### 3. **Top Layers** (Rendered Last - Always on Top) ⬆️
These layers are always rendered on top of everything (roofs, overlays, fog).

**Custom Properties:**
- Property name: `topLayer`
- Property type: `bool`
- Value: `true` ✓

**Use for:**
- Roofs
- Ceilings
- Top overlays
- Weather effects
- Fog/lighting overlays

## Example Layer Configuration in Tiled

| Layer Name | Custom Property | Type | Value | Render Order |
|------------|----------------|------|-------|--------------|
| Terrain | *(none)* | - | - | Background |
| Ground Details | *(none)* | - | - | Background |
| Paths | *(none)* | - | - | Background |
| Features | `foregroundRender` | bool | ✓ true | Y-sorted |
| Trees | `foregroundRender` | bool | ✓ true | Y-sorted |
| Buildings | `foregroundRender` | bool | ✓ true | Y-sorted |
| Roofs | `topLayer` | bool | ✓ true | Top |
| Entities *(object layer)* | - | - | - | *(spawns only)* |

### Visual Example:

```
Tiled Layers (top to bottom):
┌─────────────────────────────────┐
│ Roofs           [topLayer=true] │ ← Always on top
├─────────────────────────────────┤
│ Buildings  [foregroundRender=✓] │ ← Y-sorted
│ Trees      [foregroundRender=✓] │ ← Y-sorted
│ Features   [foregroundRender=✓] │ ← Y-sorted
├─────────────────────────────────┤
│ Paths          [no properties]  │ ← Background
│ Ground Details [no properties]  │ ← Background
│ Terrain        [no properties]  │ ← Background
└─────────────────────────────────┘
```

## How Y-Sorting Works

- **Lower Y position = Rendered first = Appears behind**
- **Higher Y position = Rendered last = Appears in front**

When the player walks **below** a tree (lower Y):
- Player Y = 100
- Tree bottom Y = 120
- **Result**: Player renders first, tree renders on top → Player appears behind tree ✓

When the player walks **above** a tree (higher Y):
- Player Y = 120
- Tree bottom Y = 100
- **Result**: Tree renders first, player renders on top → Player appears in front of tree ✓

## Step-by-Step: Adding Properties in Tiled

1. **Open your map in Tiled**

2. **Select the layer** you want to configure (e.g., "Trees")

3. **In the Properties panel** (usually on the left):
   - Click the **+** button
   - Property name: `foregroundRender`
   - Property type: Select `bool` from dropdown
   - Check the box to set value to `true`

4. **Repeat for other layers** that need Y-sorting (Buildings, Furniture, etc.)

5. **For roof layers**:
   - Add property name: `topLayer`
   - Type: `bool`
   - Value: `true`

6. **Save your map** and run the game!

## Manual Layer Configuration (Advanced)

If you need manual control, you can configure layers in code:

```java
// In GameScreen.java, after creating ySortRenderer:
ySortRenderer.setLayerConfiguration(
    new int[]{0, 1, 2},        // Background layer indices
    new int[]{3, 4, 5},        // Y-sorted layer indices
    new int[]{6}               // Top layer indices
);
```

## Tips

1. **Default is safe**: Layers without custom properties render as background (safe default)
2. **Test with debug mode**: Press F3 in-game to see collision boxes and verify rendering
3. **Tile pivot points**: Y-sort uses the **bottom** of each tile for sorting depth
4. **Performance**: Too many Y-sorted layers may impact performance. Combine similar layers when possible
5. **Console output**: When the map loads, check the console for layer configuration:
   ```
   YSortRenderer configured:
     - Background layers: 3
     - Y-sorted layers: 2
     - Top layers: 1
   ```

## Troubleshooting

**Problem**: Trees always render behind/in front of player

**Solution**: Check that the Trees layer has `foregroundRender=true` property set

---

**Problem**: Player appears on top of roofs

**Solution**: Make sure roof layer has `topLayer=true` property set

---

**Problem**: No Y-sorting happening at all

**Solution**: Check the console output when loading the map. If Y-sorted layers = 0, you need to add the `foregroundRender` property to your layers.
