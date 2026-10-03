package com.game.systems.entity.entities.enemies;

import com.game.integration.WorldManager;
import com.game.systems.entity.entities.EnemyEntity;

/**
 * Enemy types by name ("enemy:lizard", ...). The same names are used on the network
 * (networking/ReplicatedEntities) and for enemies placed in levels.
 */
public final class EnemyFactory {

    private EnemyFactory() {
    }

    /** @return the enemy, or null for an unknown type */
    public static EnemyEntity create(String type, WorldManager world, float x, float y) {
        return switch (type) {
            case "enemy:lizard" -> new LizardEnemy(world, x, y);
            case "enemy:axolot" -> new Axolot(world, x, y);
            case "enemy:cat" -> new CatEnemy(world, x, y);
            default -> null;
        };
    }

    /** The type name of an enemy, or null if it isn't one of the named types. */
    public static String typeOf(EnemyEntity enemy) {
        if (enemy instanceof LizardEnemy) return "enemy:lizard";
        if (enemy instanceof Axolot) return "enemy:axolot";
        if (enemy instanceof CatEnemy) return "enemy:cat";
        return null;
    }
}
