package com.game.testsupport;

import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.input.InputSource;
import com.game.world.GameWorld;
import com.game.world.LevelInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for GameScreen: gives the local player scripted input and records what the
 * simulation asked to be shown.
 */
public class TestPresenter implements GameWorld.Presenter {
    public final ScriptedInput input = new ScriptedInput();
    public final List<String> levelsEntered = new ArrayList<>();
    public final List<Integer> damageNumbers = new ArrayList<>();
    public int deathAnimations = 0;
    public int particles = 0;
    public int inventoryChanges = 0;
    public int dataRestored = 0;
    public String connectionLost = null;

    @Override
    public InputSource createLocalInput(PlayerEntity player) {
        return input;
    }

    @Override
    public void onLevelEntered(LevelInstance instance) {
        levelsEntered.add(instance.getLevelId());
    }

    @Override
    public void onLeavingLevel() {
    }

    @Override
    public void onLocalPlayerCreated(PlayerEntity player) {
    }

    @Override
    public void onLocalPlayerDataRestored(PlayerEntity player) {
        dataRestored++;
    }

    @Override
    public void onInventoryChanged() {
        inventoryChanges++;
    }

    @Override
    public void showDamageNumber(float x, float y, int amount) {
        damageNumbers.add(amount);
    }

    @Override
    public void showDeathAnimation(float x, float y) {
        deathAnimations++;
    }

    @Override
    public void showParticles(float x, float y, String particleType) {
        particles++;
    }

    @Override
    public void onConnectionLost(String reason) {
        connectionLost = reason;
    }
}
