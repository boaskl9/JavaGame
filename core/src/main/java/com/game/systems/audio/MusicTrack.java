package com.game.systems.audio;

/**
 * Registry of all background music tracks in the game.
 * Maps music identifiers to their file paths in the assets folder.
 *
 * Music files are streaming OGG format for memory efficiency.
 * Use these for long-playing background music, ambient loops, and level themes.
 */
public enum MusicTrack {
    // === ADVENTURE / EXPLORATION ===
    ADVENTURE_BEGIN("Audio/Musics/1 - Adventure Begin.ogg"),
    PLAINS("Audio/Musics/2 - Plains.ogg"),
    HIGHLANDS("Audio/Musics/3 - Highlands.ogg"),
    EASTERN_SANDS("Audio/Musics/4 - Eastern Sands.ogg"),
    TROPICS("Audio/Musics/5 - Tropics.ogg"),

    // === TOWN / PEACEFUL ===
    TOWN("Audio/Musics/6 - Town.ogg"),
    VILLAGE("Audio/Musics/7 - Village.ogg"),
    HAMLET("Audio/Musics/8 - Hamlet.ogg"),
    OUTPOST("Audio/Musics/9 - Outpost.ogg"),
    SNOWDIN_TOWN("Audio/Musics/10 - Snowdin Town.ogg"),

    // === DUNGEONS / CAVES ===
    CAVERN("Audio/Musics/11 - Cavern.ogg"),
    CRYPT("Audio/Musics/12 - Crypt.ogg"),
    DUNGEON("Audio/Musics/13 - Dungeon.ogg"),
    TEMPLE("Audio/Musics/14 - Temple.ogg"),
    FORTRESS("Audio/Musics/15 - Fortress.ogg"),

    // === MOUNTAIN / SNOW ===
    MOUNTAIN("Audio/Musics/16 - Mountain.ogg"),
    RUINS("Audio/Musics/17 - Ruins.ogg"),
    SNOWY_SLOPES("Audio/Musics/18 - Snowy Slopes.ogg"),
    FROZEN_KEEP("Audio/Musics/19 - Frozen Keep.ogg"),
    BLIZZARD("Audio/Musics/20 - Blizzard.ogg"),

    // === SWAMP / WETLANDS ===
    SWAMP("Audio/Musics/21 - Swamp.ogg"),
    MARSH("Audio/Musics/22 - Marsh.ogg"),
    BOG("Audio/Musics/23 - Bog.ogg"),

    // === FOREST ===
    FOREST("Audio/Musics/24 - Forest.ogg"),
    ENCHANTED_FOREST("Audio/Musics/25 - Enchanted Forest.ogg"),
    CURSED_WOODS("Audio/Musics/26 - Cursed Woods.ogg"),
    MISTY_WOODS("Audio/Musics/27 - Misty Woods.ogg"),

    // === DESERT ===
    DESERT("Audio/Musics/28 - Desert.ogg"),
    WASTELAND("Audio/Musics/29 - Wasteland.ogg"),
    BADLANDS("Audio/Musics/30 - Badlands.ogg"),

    // === UNDERGROUND / DARK ===
    DEPTHS("Audio/Musics/31 - Depths.ogg"),
    ABYSS("Audio/Musics/32 - Abyss.ogg"),
    UNDERWORLD("Audio/Musics/33 - Underworld.ogg"),
    SHADOWLANDS("Audio/Musics/34 - Shadowlands.ogg"),

    // === BOSS / COMBAT ===
    BOSS_BATTLE("Audio/Musics/35 - Boss Battle.ogg"),
    FINAL_BATTLE("Audio/Musics/36 - Final Battle.ogg"),
    DARK_FOREST("Audio/Musics/37 - Dark Forest.ogg");

    private final String filePath;

    /**
     * Creates a music track registry entry with the specified file path.
     * @param filePath Path to the music file relative to assets folder
     */
    MusicTrack(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Gets the file path for this music track.
     * @return The path to the music file
     */
    public String getPath() {
        return filePath;
    }
}
