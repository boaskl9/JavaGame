package com.game.networking;

import com.game.components.AnimationComponent;
import com.game.components.VelocityComponent;
import com.game.integration.WorldManager;
import com.game.systems.breakable.BreakableObjectFactory;
import com.game.systems.entity.Entity;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;
import com.game.systems.entity.entities.BreakableEntity;
import com.game.systems.entity.entities.EnemyEntity;
import com.game.systems.entity.entities.ItemPickupEntity;
import com.game.systems.entity.entities.enemies.Axolot;
import com.game.systems.entity.entities.enemies.CatEnemy;
import com.game.systems.entity.entities.enemies.LizardEnemy;

/**
 * Converts between world objects and their network description.
 * Add new replicated entity types here (both directions).
 */
public final class ReplicatedEntities {

    private ReplicatedEntities() {
    }

    /**
     * Whether the host should replicate this world object to clients.
     * Players are handled separately (owner-authoritative); gateways and furniture are static.
     */
    public static boolean isReplicated(GameObject obj) {
        return obj instanceof EnemyEntity || obj instanceof BreakableEntity;
    }

    /**
     * Describe a world object (enemy or breakable) as a spawn packet.
     */
    public static Packets.EntitySpawn describe(GameObject obj) {
        Packets.EntitySpawn spawn = new Packets.EntitySpawn();
        spawn.netId = obj.getNetId();
        spawn.type = typeOf(obj);

        Transform transform = obj.getComponent(Transform.class);
        if (transform != null) {
            spawn.x = transform.getX();
            spawn.y = transform.getY();
        }
        if (obj instanceof Entity entity) {
            spawn.hp = entity.getHealth();
            spawn.maxHp = entity.getMaxHealth();
        }
        return spawn;
    }

    public static Packets.EntitySpawn describeItem(ItemPickupEntity item) {
        Packets.EntitySpawn spawn = new Packets.EntitySpawn();
        spawn.netId = item.getNetId();
        spawn.type = Packets.TYPE_ITEM;
        Transform transform = item.getComponent(Transform.class);
        spawn.x = transform.getX();
        spawn.y = transform.getY();
        spawn.itemId = item.getItemStack().getDefinition().getId();
        spawn.quantity = item.getItemStack().getQuantity();
        return spawn;
    }

    private static String typeOf(GameObject obj) {
        if (obj instanceof LizardEnemy) return "enemy:lizard";
        if (obj instanceof Axolot) return "enemy:axolot";
        if (obj instanceof CatEnemy) return "enemy:cat";
        if (obj instanceof BreakableEntity breakable) return "breakable:" + breakable.getObjectType();
        throw new IllegalArgumentException("Not a replicated type: " + obj.getClass().getSimpleName());
    }

    /**
     * Create a client-side puppet for a spawned enemy or breakable (not items).
     * @return the puppet, or null for unknown types
     */
    public static GameObject createPuppet(Packets.EntitySpawn spawn, WorldManager world) {
        GameObject obj;
        if (spawn.type.startsWith("enemy:")) {
            EnemyEntity enemy = switch (spawn.type) {
                case "enemy:lizard" -> new LizardEnemy(world, spawn.x, spawn.y);
                case "enemy:axolot" -> new Axolot(world, spawn.x, spawn.y);
                case "enemy:cat" -> new CatEnemy(world, spawn.x, spawn.y);
                default -> null;
            };
            if (enemy == null) {
                System.err.println("ReplicatedEntities: Unknown enemy type " + spawn.type);
                return null;
            }
            enemy.setNetworkControlled(true);
            obj = enemy;
        } else if (spawn.type.startsWith("breakable:")) {
            obj = BreakableObjectFactory.create(spawn.type.substring("breakable:".length()), spawn.x, spawn.y);
            if (obj == null) {
                return null;
            }
        } else {
            System.err.println("ReplicatedEntities: Unknown entity type " + spawn.type);
            return null;
        }

        if (obj instanceof Entity entity && spawn.maxHp > 0) {
            entity.setMaxHealth(spawn.maxHp);
            entity.setHealth(spawn.hp);
        }
        obj.setNetId(spawn.netId);
        return obj;
    }

    /**
     * Current animation state of an enemy, for state batches.
     */
    public static void writeState(EnemyEntity enemy, Packets.EntityStateBatch batch, int i) {
        Transform transform = enemy.getTransform();
        VelocityComponent velocity = enemy.getComponent(VelocityComponent.class);
        AnimationComponent animation = enemy.getComponent(AnimationComponent.class);

        batch.netIds[i] = enemy.getNetId();
        batch.x[i] = transform.getX();
        batch.y[i] = transform.getY();
        batch.vx[i] = velocity != null ? velocity.getVelocity().x : 0;
        batch.vy[i] = velocity != null ? velocity.getVelocity().y : 0;
        batch.anims[i] = animation != null ? animation.getCurrentState() : "idle";
        batch.dirs[i] = animation != null ? animation.getCurrentDirection() : 180;
        batch.flips[i] = animation != null && animation.getAnimator().isFlipX();
        batch.hp[i] = enemy.getHealth();
    }
}
