package com.game.systems.dungeon;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.dungeon.generation.PlacedRoom;

import java.util.List;

/**
 * Debug renderer for procedurally generated dungeons.
 * Consolidates the working debug visualization from DungeonTestScreen.
 * Renders collision shapes, room bounds, and door states.
 */
public class DungeonDebugRenderer {
    private boolean showCollision = true;
    private boolean showRoomBounds = false;
    private boolean showDoors = false;

    /**
     * Render dungeon debug overlays.
     * @param shapeRenderer ShapeRenderer to use (must be in LINE mode)
     * @param collisionSystem Collision system containing shapes
     * @param placedRooms List of placed rooms (for room bounds and doors)
     * @param offsetX Pixel offset X (from AssembledDungeon)
     * @param offsetY Pixel offset Y (from AssembledDungeon)
     */
    public void render(ShapeRenderer shapeRenderer, SpatialQuery collisionSystem,
                      List<PlacedRoom> placedRooms, float offsetX, float offsetY) {

        // Render collision shapes (RED)
        if (showCollision && collisionSystem != null) {
            shapeRenderer.setColor(Color.RED);
            for (Rectangle rect : collisionSystem.getRectangles()) {
                shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
            }
            for (Polygon poly : collisionSystem.getPolygons()) {
                shapeRenderer.polygon(poly.getTransformedVertices());
            }
        }

        // Render room bounds (GREEN)
        if (showRoomBounds && placedRooms != null) {
            shapeRenderer.setColor(Color.GREEN);
            for (PlacedRoom room : placedRooms) {
                com.game.systems.dungeon.RoomBounds bounds = room.getTemplate().getBounds();
                float x = room.getWorldX() - offsetX;
                float y = room.getWorldY() - offsetY;
                float w = bounds.getWidth() * 16;
                float h = bounds.getHeight() * 16;
                shapeRenderer.rect(x, y, w, h);
            }
        }

        // Render doors (YELLOW for unconnected, CYAN for connected)
        if (showDoors && placedRooms != null) {
            for (PlacedRoom room : placedRooms) {
                for (PlacedRoom.WorldDoor door : room.getWorldDoors()) {
                    if (door.isConnected()) {
                        shapeRenderer.setColor(Color.CYAN);
                    } else {
                        shapeRenderer.setColor(Color.YELLOW);
                    }
                    float doorX = door.getWorldX() - offsetX;
                    float doorY = door.getWorldY() - offsetY;
                    float doorW = door.getDoor().getWidth();
                    float doorH = door.getDoor().getHeight();
                    shapeRenderer.rect(doorX, doorY, doorW, doorH);
                }
            }
        }
    }

    /**
     * Simplified render method using DungeonLevelSource.
     * @param shapeRenderer ShapeRenderer to use (must be in LINE mode)
     * @param collisionSystem Collision system
     * @param dungeonSource Dungeon level source containing debug data
     */
    public void render(ShapeRenderer shapeRenderer, SpatialQuery collisionSystem,
                      DungeonLevelSource dungeonSource) {
        if (dungeonSource == null) {
            return;
        }

        float[] offset = dungeonSource.getDebugOffset();
        render(shapeRenderer, collisionSystem, dungeonSource.getPlacedRooms(),
            offset[0], offset[1]);
    }

    // Configuration methods

    public void setShowCollision(boolean show) {
        this.showCollision = show;
    }

    public void setShowRoomBounds(boolean show) {
        this.showRoomBounds = show;
    }

    public void setShowDoors(boolean show) {
        this.showDoors = show;
    }

    public void setShowAll(boolean show) {
        this.showCollision = show;
        this.showRoomBounds = show;
        this.showDoors = show;
    }

    public boolean isShowCollision() {
        return showCollision;
    }

    public boolean isShowRoomBounds() {
        return showRoomBounds;
    }

    public boolean isShowDoors() {
        return showDoors;
    }
}
