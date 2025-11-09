package com.game.systems.dungeon;

/**
 * Parameters for dungeon generation.
 * Use builder pattern for flexible configuration.
 */
public class DungeonGenerationParams {
    private final String theme;
    private final int budget;
    private final long seed;
    private final int maxRooms;

    private DungeonGenerationParams(Builder builder) {
        this.theme = builder.theme;
        this.budget = builder.budget;
        this.seed = builder.seed;
        this.maxRooms = builder.maxRooms;
    }

    public String getTheme() {
        return theme;
    }

    public int getBudget() {
        return budget;
    }

    public long getSeed() {
        return seed;
    }

    public int getMaxRooms() {
        return maxRooms;
    }

    @Override
    public String toString() {
        return String.format("DungeonParams[theme=%s, budget=%d, seed=%d, maxRooms=%d]",
            theme, budget, seed, maxRooms);
    }

    /**
     * Builder for DungeonGenerationParams.
     */
    public static class Builder {
        private String theme = "cave";
        private int budget = 15;
        private long seed = System.currentTimeMillis();
        private int maxRooms = 50;

        public Builder theme(String theme) {
            this.theme = theme;
            return this;
        }

        public Builder budget(int budget) {
            this.budget = budget;
            return this;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder maxRooms(int maxRooms) {
            this.maxRooms = maxRooms;
            return this;
        }

        public DungeonGenerationParams build() {
            return new DungeonGenerationParams(this);
        }
    }

    /**
     * Create default parameters for a theme.
     */
    public static DungeonGenerationParams defaultParams(String theme) {
        return new Builder()
            .theme(theme)
            .build();
    }

    /**
     * Create default parameters with custom seed.
     */
    public static DungeonGenerationParams withSeed(String theme, long seed) {
        return new Builder()
            .theme(theme)
            .seed(seed)
            .build();
    }
}
