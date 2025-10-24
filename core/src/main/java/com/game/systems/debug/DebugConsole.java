package com.game.systems.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.game.components.HealthComponent;
import com.game.entity.EnemyEntity;
import com.game.entity.enemies.Axolot;
import com.game.entity.enemies.CatEnemy;
import com.game.entity.enemies.LizardEnemy;
import com.game.integration.WorldItemManager;
import com.game.integration.WorldManager;
import com.game.main.GameScreen;
import com.game.systems.entity.Transform;
import com.game.systems.item.ItemFactory;
import com.game.systems.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game debug console with command support.
 * Press F4 (or backtick) to toggle visibility.
 *
 * Supported commands:
 * - /spawn item <id> [qty] - Spawn items at player position
 * - /spawn enemy <type> - Spawn enemy at player position
 * - /damage <amount> - Damage player
 * - /heal <amount> - Heal player
 * - /debug <feature> - Toggle debug visualization (colliders, navmesh, fps, all, none)
 * - /help - List all commands
 * - /clear - Clear console output
 */
public class DebugConsole extends Window {
    private final GameScreen gameScreen;
    private final DebugManager debugManager;

    private final TextField inputField;
    private final Label outputLabel;
    private final ScrollPane scrollPane;
    private final StringBuilder output;

    private final List<String> commandHistory;
    private int historyIndex;

    public DebugConsole(Skin skin, GameScreen gameScreen, DebugManager debugManager) {
        super("Debug Console", skin);

        this.gameScreen = gameScreen;
        this.debugManager = debugManager;
        this.output = new StringBuilder();
        this.commandHistory = new ArrayList<>();
        this.historyIndex = -1;

        // Make window resizable and movable
        setResizable(true);
        setMovable(true);

        // Output area
        outputLabel = new Label("Type /help for commands\n", skin);
        outputLabel.setWrap(true);
        outputLabel.setAlignment(Align.topLeft);
        output.append("Type /help for commands\n");

        scrollPane = new ScrollPane(outputLabel, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // Input field
        inputField = new TextField("", skin);
        inputField.setMessageText("Enter command...");

        // Layout
        add(scrollPane).expand().fill().pad(5).row();
        add(inputField).fillX().pad(5);

        // Setup input handling
        setupInputHandling();

        // Initial visibility
        setVisible(false);
    }

    private void setupInputHandling() {
        // Submit command on Enter
        inputField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    submitCommand();
                    return true;
                } else if (keycode == Input.Keys.UP) {
                    navigateHistory(-1);
                    return true;
                } else if (keycode == Input.Keys.DOWN) {
                    navigateHistory(1);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Toggle console visibility.
     */
    public void toggle() {
        setVisible(!isVisible());

        if (isVisible()) {
            // Focus input field
            getStage().setKeyboardFocus(inputField);
            inputField.setText("");
        } else {
            // Clear keyboard focus
            getStage().setKeyboardFocus(null);
        }
    }

    /**
     * Navigate command history.
     */
    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;

        historyIndex += direction;

        if (historyIndex < 0) {
            historyIndex = 0;
        } else if (historyIndex >= commandHistory.size()) {
            historyIndex = commandHistory.size();
            inputField.setText("");
            return;
        }

        if (historyIndex < commandHistory.size()) {
            inputField.setText(commandHistory.get(historyIndex));
            inputField.setCursorPosition(inputField.getText().length());
        }
    }

    /**
     * Submit the current command.
     */
    private void submitCommand() {
        String command = inputField.getText().trim();

        if (command.isEmpty()) {
            return;
        }

        // Add to history
        commandHistory.add(command);
        historyIndex = commandHistory.size();

        // Log command
        log("> " + command);

        // Execute
        executeCommand(command);

        // Clear input
        inputField.setText("");

        // Scroll to bottom
        scrollPane.layout();
        scrollPane.setScrollPercentY(1f);
    }

    /**
     * Execute a command string.
     */
    private void executeCommand(String commandStr) {
        String[] parts = commandStr.trim().split("\\s+");

        if (parts.length == 0 || !parts[0].startsWith("/")) {
            error("Commands must start with /");
            return;
        }

        String command = parts[0].substring(1).toLowerCase();

        try {
            switch (command) {
                case "help":
                    executeHelp();
                    break;
                case "clear":
                    executeClear();
                    break;
                case "spawn":
                    executeSpawn(parts);
                    break;
                case "damage":
                    executeDamage(parts);
                    break;
                case "heal":
                    executeHeal(parts);
                    break;
                case "debug":
                    executeDebug(parts);
                    break;
                default:
                    error("Unknown command: " + command);
                    log("Type /help for a list of commands");
            }
        } catch (Exception e) {
            error("Error executing command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== COMMAND IMPLEMENTATIONS ====================

    private void executeHelp() {
        log("Available Commands:");
        log("  /spawn item <id> [qty] - Spawn items at player");
        log("      Item IDs: wood, bag, bag2, bag3");
        log("      Example: /spawn item wood 10");
        log("  /spawn enemy <type> - Spawn enemy at player");
        log("      Types: slime, frog, cat");
        log("      Example: /spawn enemy slime");
        log("  /damage <amount> - Damage player (4 HP = 1 heart)");
        log("      Example: /damage 4");
        log("  /heal <amount> - Heal player");
        log("      Example: /heal 8");
        log("  /debug <feature> - Toggle debug view");
        log("      Features: colliders, navmesh, fps, all, none");
        log("      Example: /debug colliders");
        log("  /clear - Clear console output");
        log("  /help - Show this help");
    }

    private void executeClear() {
        output.setLength(0);
        outputLabel.setText("");
        log("Console cleared");
    }

    private void executeSpawn(String[] parts) {
        if (parts.length < 3) {
            error("Usage: /spawn <item|enemy> <id> [quantity]");
            return;
        }

        String type = parts[1].toLowerCase();

        if ("item".equals(type)) {
            spawnItem(parts);
        } else if ("enemy".equals(type)) {
            spawnEnemy(parts);
        } else {
            error("Unknown spawn type: " + type);
            log("Use: /spawn item <id> [qty] or /spawn enemy <type>");
        }
    }

    private void spawnItem(String[] parts) {
        if (parts.length < 3) {
            error("Usage: /spawn item <id> [quantity]");
            return;
        }

        String itemId = parts[2].toLowerCase();
        int quantity = 1;

        if (parts.length > 3) {
            try {
                quantity = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                error("Invalid quantity: " + parts[3]);
                return;
            }
        }

        // Get player position
        Transform playerTransform = gameScreen.player.getTransform();
        float x = playerTransform.getX();
        float y = playerTransform.getY();

        try {
            // Create and spawn item
            ItemStack stack = ItemFactory.create(itemId, quantity);
            WorldItemManager itemManager = gameScreen.worldItemManager;
            itemManager.spawnItem(stack, x, y, 0.5f);

            log("Spawned " + quantity + "x " + itemId + " at (" + (int)x + ", " + (int)y + ")");
        } catch (Exception e) {
            error("Failed to spawn item '" + itemId + "': " + e.getMessage());
            log("Valid item IDs: wood, bag, bag2, bag3");
        }
    }

    private void spawnEnemy(String[] parts) {
        if (parts.length < 3) {
            error("Usage: /spawn enemy <type>");
            return;
        }

        String enemyType = parts[2].toLowerCase();

        // Get player position
        Transform playerTransform = gameScreen.player.getTransform();
        float x = playerTransform.getX() + 20; // Offset slightly
        float y = playerTransform.getY();

        WorldManager world = gameScreen.world;
        EnemyEntity enemy = null;

        switch (enemyType) {
            case "slime":
            case "lizard":
                enemy = new LizardEnemy(world, x, y);
                break;
            case "frog":
            case "axolot":
                enemy = new Axolot(world, x, y);
                break;
            case "cat":
                enemy = new CatEnemy(world, x, y);
                break;
            default:
                error("Unknown enemy type: " + enemyType);
                log("Valid types: slime, frog, cat");
                return;
        }

        world.addGameObject(enemy);
        log("Spawned " + enemyType + " at (" + (int)x + ", " + (int)y + ")");
    }

    private void executeDamage(String[] parts) {
        if (parts.length < 2) {
            error("Usage: /damage <amount>");
            return;
        }

        try {
            int amount = Integer.parseInt(parts[1]);

            HealthComponent health = gameScreen.player.getHealthComponent();
            health.damage(amount);

            int hearts = amount / 4;
            int quarters = amount % 4;
            String heartStr = hearts > 0 ? hearts + " heart" + (hearts > 1 ? "s" : "") : "";
            String quarterStr = quarters > 0 ? quarters + "/4 heart" : "";
            String combined = heartStr + (hearts > 0 && quarters > 0 ? " and " : "") + quarterStr;

            log("Player damaged for " + amount + " HP (" + combined + ")");
            log("Current health: " + health.getCurrentHealth() + "/" + health.getMaxHealth() + " HP");
        } catch (NumberFormatException e) {
            error("Invalid amount: " + parts[1]);
        }
    }

    private void executeHeal(String[] parts) {
        if (parts.length < 2) {
            error("Usage: /heal <amount>");
            return;
        }

        try {
            int amount = Integer.parseInt(parts[1]);

            HealthComponent health = gameScreen.player.getHealthComponent();
            int beforeHeal = health.getCurrentHealth();
            health.heal(amount);
            int actualHealed = health.getCurrentHealth() - beforeHeal;

            int hearts = actualHealed / 4;
            int quarters = actualHealed % 4;
            String heartStr = hearts > 0 ? hearts + " heart" + (hearts > 1 ? "s" : "") : "";
            String quarterStr = quarters > 0 ? quarters + "/4 heart" : "";
            String combined = heartStr + (hearts > 0 && quarters > 0 ? " and " : "") + quarterStr;

            log("Player healed for " + actualHealed + " HP (" + combined + ")");
            log("Current health: " + health.getCurrentHealth() + "/" + health.getMaxHealth() + " HP");
        } catch (NumberFormatException e) {
            error("Invalid amount: " + parts[1]);
        }
    }

    private void executeDebug(String[] parts) {
        if (parts.length < 2) {
            error("Usage: /debug <feature>");
            log("Features: " + String.join(", ", debugManager.getFeatures()));
            return;
        }

        String feature = parts[1].toLowerCase();
        debugManager.toggle(feature);

        if ("all".equals(feature) || "none".equals(feature)) {
            log("Debug visualization: " + feature);
        } else {
            boolean enabled = debugManager.isEnabled(feature);
            log("Debug '" + feature + "': " + (enabled ? "ENABLED" : "DISABLED"));
        }
    }

    // ==================== LOGGING ====================

    private void log(String message) {
        output.append(message).append("\n");
        outputLabel.setText(output.toString());
    }

    private void error(String message) {
        output.append("[ERROR] ").append(message).append("\n");
        outputLabel.setText(output.toString());
    }
}
