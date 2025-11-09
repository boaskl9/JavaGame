package com.game.systems.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.game.systems.level.LevelData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template for a dungeon room extracted from a theme file.
 * Contains all data needed to place and render the room in a generated dungeon.
 */
public class RoomTemplate {
    private final String id;
    private final String name;
    private final RoomBounds bounds;

    // Tile data (stored as Cell arrays per layer)
    private final List<LayerData> tileLayers;

    // Connections
    private final List<DoorConnection> doors;

    // Collision shapes (relative to room origin)
    private final List<Rectangle> collisionShapes;

    // Entity/object data (relative to room origin)
    private final List<LevelData.LevelObject> objects;

    // Metadata
    private final int cost;
    private final int weight;
    private final String category;  // "small", "medium", "large"
    private final Set<String> tags;
    private final boolean isCloser;  // Tagged as dead-end closer for final cleanup

    /**
     * Data for a tile layer.
     */
    public static class LayerData {
        public final String name;
        public final TiledMapTileLayer.Cell[][] cells;  // [x][y]

        public LayerData(String name, TiledMapTileLayer.Cell[][] cells) {
            this.name = name;
            this.cells = cells;
        }
    }

    private RoomTemplate(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.bounds = builder.bounds;
        this.tileLayers = new ArrayList<>(builder.tileLayers);
        this.doors = new ArrayList<>(builder.doors);
        this.collisionShapes = new ArrayList<>(builder.collisionShapes);
        this.objects = new ArrayList<>(builder.objects);
        this.cost = builder.cost;
        this.weight = builder.weight;
        this.category = builder.category;
        this.tags = new HashSet<>(builder.tags);
        this.isCloser = builder.isCloser;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RoomBounds getBounds() {
        return bounds;
    }

    public List<LayerData> getTileLayers() {
        return new ArrayList<>(tileLayers);
    }

    public List<DoorConnection> getDoors() {
        return new ArrayList<>(doors);
    }

    public List<Rectangle> getCollisionShapes() {
        return new ArrayList<>(collisionShapes);
    }

    public List<LevelData.LevelObject> getObjects() {
        return new ArrayList<>(objects);
    }

    public int getCost() {
        return cost;
    }

    public int getWeight() {
        return weight;
    }

    public String getCategory() {
        return category;
    }

    public Set<String> getTags() {
        return new HashSet<>(tags);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public boolean isCloser() {
        return isCloser;
    }

    /**
     * Get doors facing a specific direction.
     */
    public List<DoorConnection> getDoorsWithDirection(Direction direction) {
        List<DoorConnection> result = new ArrayList<>();
        for (DoorConnection door : doors) {
            if (door.getDirection() == direction) {
                result.add(door);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("RoomTemplate[id=%s, name=%s, bounds=%s, cost=%d, weight=%d, doors=%d, category=%s, tags=%s]",
            id, name, bounds, cost, weight, doors.size(), category, tags);
    }

    /**
     * Builder pattern for constructing RoomTemplate.
     */
    public static class Builder {
        private String id;
        private String name;
        private RoomBounds bounds;
        private List<LayerData> tileLayers = new ArrayList<>();
        private List<DoorConnection> doors = new ArrayList<>();
        private List<Rectangle> collisionShapes = new ArrayList<>();
        private List<LevelData.LevelObject> objects = new ArrayList<>();
        private int cost = 10;
        private int weight = 10;
        private String category = "medium";
        private Set<String> tags = new HashSet<>();
        private boolean isCloser = false;

        public Builder(String id, RoomBounds bounds) {
            this.id = id;
            this.name = id;
            this.bounds = bounds;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addTileLayer(String layerName, TiledMapTileLayer.Cell[][] cells) {
            this.tileLayers.add(new LayerData(layerName, cells));
            return this;
        }

        public Builder addDoor(DoorConnection door) {
            this.doors.add(door);
            return this;
        }

        public Builder addCollisionShape(Rectangle shape) {
            this.collisionShapes.add(shape);
            return this;
        }

        public Builder addObject(LevelData.LevelObject object) {
            this.objects.add(object);
            return this;
        }

        public Builder cost(int cost) {
            this.cost = cost;
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags = new HashSet<>(tags);
            return this;
        }

        public Builder isCloser(boolean isCloser) {
            this.isCloser = isCloser;
            return this;
        }

        public RoomTemplate build() {
            return new RoomTemplate(this);
        }
    }
}
