package com.game.systems.audio;

/**
 * Registry of all sound effects in the game.
 * Maps sound identifiers to their file paths in the assets folder.
 *
 * Sound files are organized by category:
 * - Combat: Weapon swings, hits, impacts
 * - Breakables: Breaking/shattering sounds
 * - Items: Pickup sounds, bonuses
 * - UI: Menu navigation, button clicks
 * - Enemy: Pain, death sounds
 * - Special: Jingles, level up, achievements
 */
public enum SoundRegistry {
    // === COMBAT SOUNDS ===
    SWORD_SWING("Audio/Sounds/Whoosh & Slash/Whoosh.wav"),
    SWORD_HIT("Audio/Sounds/Hit & Impact/Impact2.wav"),


    // === IMPACT/BREAKABLE SOUNDS ===
    POT_BREAK("Audio/Sounds/Whoosh & Slash/Sword2.wav"),  // Clay pottery

    // === ITEM PICKUP SOUNDS ===
    COIN_PICKUP("Audio/Sounds/Bonus/Coin.wav"),

    // === UI SOUNDS ===
    BUTTON_CLICK("Audio/Sounds/Menu/Menu5.wav"),
    MENU_MOVE("Audio/Sounds/Menu/Move1.wav"),
    MENU_ACCEPT("Audio/Sounds/Menu/Accept1.wav"),
    MENU_CANCEL("Audio/Sounds/Menu/Cancel1.wav"),

    // === ENEMY SOUNDS ===
    ENEMY_HIT("Audio/Sounds/Hit/Hit2.wav"),
    ENEMY_DEATH("Audio/Sounds/Voice/Voice1.wav"),
    ENEMY_PAIN("Audio/Sounds/Voice/Voice2.wav"),

    // === MAGIC/ELEMENTAL SOUNDS ===
    /*FIRE_CAST("Audio/Sounds/Elemental/Fire1.wav"),
    EXPLOSION("Audio/Sounds/Elemental/Explosion 1.wav"),
    HEAL("Audio/Sounds/Magic/Heal 1.wav"),
    MAGIC_CAST("Audio/Sounds/Magic/Magic 1.wav"),

    // === SPECIAL JINGLES ===
    LEVEL_UP("Audio/Jingles/Jingle_Achievement_00.wav"),
    SUCCESS("Audio/Jingles/Jingle_Success_00.wav"),
    GAME_OVER("Audio/Jingles/Jingle_Lose_00.wav"),
    SECRET_FOUND("Audio/Jingles/Jingle_SecretFound_00.wav"),

    // === AMBIENT SOUNDS ===
    FOOTSTEP("Audio/Sounds/Menu/Move 1.wav"),  // Placeholder
    JUMP("Audio/Sounds/Jump/Jump 1.wav"),
    BOUNCE("Audio/Sounds/Jump/Bounce 1.wav")*/;

    private final String filePath;

    /**
     * Creates a sound registry entry with the specified file path.
     * @param filePath Path to the sound file relative to assets folder
     */
    SoundRegistry(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Gets the file path for this sound.
     * @return The path to the sound file
     */
    public String getPath() {
        return filePath;
    }
}
