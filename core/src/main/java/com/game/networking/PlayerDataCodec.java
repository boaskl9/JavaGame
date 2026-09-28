package com.game.networking;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.game.components.AnimationComponent;
import com.game.components.HealthComponent;
import com.game.save.PlayerData;
import com.game.systems.entity.entities.PlayerEntity;

/**
 * Converts players to and from the formats sent over the network.
 */
public final class PlayerDataCodec {

    private PlayerDataCodec() {
    }

    private static Json json() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        return json;
    }

    /** A player's persistent data (health + inventory) as JSON. */
    public static String toJson(PlayerEntity player) {
        HealthComponent health = player.getHealthComponent();
        PlayerData data = new PlayerData(
            player.getTransform().getX(), player.getTransform().getY(),
            health.getCurrentHealth(), health.getMaxHealth(),
            player.getInventory().exportSaveData()
        );
        return json().toJson(data);
    }

    static String toJson(PlayerData data) {
        return data != null ? json().toJson(data) : null;
    }

    public static PlayerData fromJson(String playerJson) {
        if (playerJson == null) return null;
        try {
            return json().fromJson(PlayerData.class, playerJson);
        } catch (Exception e) {
            System.err.println("PlayerDataCodec: Could not read player data: " + e.getMessage());
            return null;
        }
    }

    /** Restore health and inventory (not position) from saved data. */
    public static void apply(PlayerEntity player, PlayerData data) {
        if (data == null) return;
        if (data.inventory != null) {
            player.getInventory().importSaveData(data.inventory);
        }
        if (data.maxHealth > 0) {
            player.getHealthComponent().setMaxHealth(data.maxHealth);
            player.getHealthComponent().setHealth(Math.max(1, data.currentHealth));
        }
    }

    /** The live state of a locally controlled player. */
    static Packets.PlayerState state(PlayerEntity player, int playerId, String levelId) {
        Packets.PlayerState state = new Packets.PlayerState();
        state.playerId = playerId;
        state.levelId = levelId;
        state.time = System.currentTimeMillis();
        state.x = player.getTransform().getX();
        state.y = player.getTransform().getY();

        com.badlogic.gdx.math.Vector2 velocity =
            player.getComponent(com.game.components.VelocityComponent.class).getVelocity();
        state.vx = velocity.x;
        state.vy = velocity.y;

        AnimationComponent animation = player.getComponent(AnimationComponent.class);
        state.anim = animation.getCurrentState();
        state.dir = animation.getCurrentDirection();
        state.flipX = animation.getAnimator().isFlipX();

        state.hp = player.getHealthComponent().getCurrentHealth();
        state.maxHp = player.getHealthComponent().getMaxHealth();
        state.weaponId = player.getEquippedWeaponId();
        return state;
    }

    /**
     * Feed a received state into a remote player's copy.
     * @return true if this state is the moment that player died
     */
    static boolean applyState(PlayerEntity puppet, Packets.PlayerState state, long localTime) {
        boolean wasAlive = puppet.isAlive();
        if (!wasAlive && state.hp > 0) {
            puppet.clearSnapshotBuffer(); // Respawned elsewhere: jump there instead of sliding across the map
        }
        puppet.enqueueSnapshot(new EntitySnapshot(
            localTime, state.x, state.y, state.vx, state.vy,
            state.anim, state.dir, state.flipX, state.hp, state.maxHp
        ));
        HealthComponent health = puppet.getHealthComponent();
        health.setMaxHealth(state.maxHp);
        health.setHealth(state.hp);
        puppet.setRemoteWeapon(state.weaponId);
        return wasAlive && state.hp <= 0;
    }
}
