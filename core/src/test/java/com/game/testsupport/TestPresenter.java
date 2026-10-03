package com.game.testsupport;

import com.game.networking.Packets;
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
    public Packets.CharacterInfo[] characters = null; // Latest character list from the host
    public String characterMessage = null;
    public int characterLists = 0;
    public int deaths = 0;
    public int respawns = 0;
    public final List<String> messages = new ArrayList<>();

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

    @Override
    public void onLocalPlayerDied() {
        deaths++;
    }

    @Override
    public void onLocalPlayerRespawned() {
        respawns++;
    }

    @Override
    public void showMessage(String text) {
        messages.add(text);
    }

    public String lastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    @Override
    public void chooseCharacter(Packets.CharacterInfo[] characters, String message) {
        this.characters = characters;
        this.characterMessage = message;
        characterLists++;
    }

    /** The listed character with this name, or null. */
    public Packets.CharacterInfo character(String name) {
        if (characters == null) return null;
        for (Packets.CharacterInfo character : characters) {
            if (character.name.equals(name)) return character;
        }
        return null;
    }
}
