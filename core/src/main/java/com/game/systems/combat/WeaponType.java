package com.game.systems.combat;

/**
 * Enum defining different weapon categories.
 * Each weapon type has different attack behaviors and patterns.
 */
public enum WeaponType {
    /**
     * Swords - Medium speed arc swings with balanced stats.
     * Examples: Short sword, longsword, katana
     */
    SWORD,

    /**
     * Spears - Fast thrust attacks with longer range but narrow hitbox.
     * Examples: Spear, lance, javelin
     */
    SPEAR,

    /**
     * Hammers - Slow overhead slams with high damage and knockback.
     * Examples: War hammer, mace, club
     */
    HAMMER,

    /**
     * Bows - Ranged projectile attacks.
     * Examples: Short bow, longbow, crossbow
     */
    BOW,

    /**
     * Daggers - Very fast, short-range stab attacks with low damage.
     * Examples: Dagger, knife, shiv
     */
    DAGGER,

    /**
     * Axes - Wide arc attacks with good damage.
     * Examples: Hand axe, battle axe, hatchet
     */
    AXE,

    /**
     * Staffs - Quick sweeping attacks with low damage but good range.
     * Examples: Wooden staff, magic staff
     */
    STAFF,

    /**
     * Whips - Very long range arc attacks with low damage.
     * Examples: Whip, flail, chain
     */
    WHIP
}
