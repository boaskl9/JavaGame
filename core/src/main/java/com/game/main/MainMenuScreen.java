package com.game.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.save.SaveManager;
import com.game.save.SaveMetadata;

import java.util.List;

/**
 * Main menu screen with Continue, Load, New Game, and Settings options.
 */
public class MainMenuScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Table table;

    private TextButton continueButton;
    private TextButton loadButton;
    private TextButton newGameButton;
    private TextButton multiplayerButton;
    private TextButton settingsButton;
    private TextButton exitButton;

    // Background
    // Uncomment to use a PNG background instead of a solid color
    // private SpriteBatch batch;
    // private Texture backgroundTexture;

    public MainMenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Use the same skin as UIManager
        skin = new Skin(Gdx.files.internal("assets/ui/wood-theme.json"));

        // Uncomment to load PNG background
        // batch = new SpriteBatch();
        // backgroundTexture = new Texture(Gdx.files.internal("assets/ui/menu_background.png"));

        // Create main table
        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);



        // Title
        Label titleLabel = new Label("UNTITLED JAVA GAME", skin);
        titleLabel.setFontScale(2f);
        table.add(titleLabel).padBottom(50).row();

        // Continue button
        continueButton = new TextButton("Continue", skin);
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onContinue();
            }
        });
        table.add(continueButton).width(300).height(60).padBottom(20).row();

        // Load Save button
        loadButton = new TextButton("Load Save", skin);
        loadButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onLoadSave();
            }
        });
        table.add(loadButton).width(300).height(60).padBottom(20).row();

        // New Game button
        newGameButton = new TextButton("New Game", skin);
        newGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onNewGame();
            }
        });
        table.add(newGameButton).width(300).height(60).padBottom(20).row();

        // Multiplayer button
        multiplayerButton = new TextButton("Multiplayer", skin);
        multiplayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onMultiplayer();
            }
        });
        table.add(multiplayerButton).width(300).height(60).padBottom(20).row();

        // Settings button
        settingsButton = new TextButton("Settings", skin);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onSettings();
            }
        });
        table.add(settingsButton).width(300).height(60).padBottom(20).row();

        // Exit button
        exitButton = new TextButton("Exit", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onExit();
            }
        });
        table.add(exitButton).width(300).height(60).row();

        // Update button states based on save availability
        updateButtonStates();
    }

    /**
     * Update button enabled/disabled states based on save availability.
     */
    private void updateButtonStates() {
        List<SaveMetadata> saves = SaveManager.getInstance().listSaves();
        boolean hasSaves = !saves.isEmpty();

        continueButton.setDisabled(!hasSaves);
        loadButton.setDisabled(!hasSaves);

        // Visual feedback for disabled buttons
        if (!hasSaves) {
            continueButton.setColor(0.5f, 0.5f, 0.5f, 1f);
            loadButton.setColor(0.5f, 0.5f, 0.5f, 1f);
        } else {
            continueButton.setColor(1f, 1f, 1f, 1f);
            loadButton.setColor(1f, 1f, 1f, 1f);
        }
    }

    private void onContinue() {
        com.game.save.SaveData saveData = SaveManager.getInstance().loadMostRecent();
        if (saveData != null) {
            System.out.println("MainMenu: Loading most recent save: " + saveData.saveName);
            game.setScreen(new GameScreen(saveData));
        } else {
            System.err.println("MainMenu: No saves found for Continue");
        }
    }

    private void onLoadSave() {
        System.out.println("MainMenu: Opening Load Save screen");
        // TODO: Open LoadGameScreen
        game.setScreen(new LoadGameScreen(game));
    }

    private void onNewGame() {
        System.out.println("MainMenu: Opening New Game dialog");

        com.game.ui.NewGameDialog dialog = new com.game.ui.NewGameDialog(skin);
        dialog.setCallback(new com.game.ui.NewGameDialog.NewGameCallback() {
            @Override
            public void onConfirm(String saveName) {
                System.out.println("MainMenu: Starting new game with save: " + saveName);
                // Start new game with the chosen save name
                GameScreen gameScreen = new GameScreen(saveName);
                game.setScreen(gameScreen);
            }

            @Override
            public void onCancel() {
                System.out.println("MainMenu: New game canceled");
            }
        });

        dialog.show(stage);
    }

    private void onMultiplayer() {
        System.out.println("MainMenu: Opening multiplayer dialog");

        com.game.ui.MultiplayerDialog dialog = new com.game.ui.MultiplayerDialog(skin);
        dialog.setCallback(new com.game.ui.MultiplayerDialog.MultiplayerCallback() {
            @Override
            public void onConnect(String serverIp) {
                System.out.println("MainMenu: Attempting to connect to server: " + serverIp);

                // Test connection FIRST before creating GameScreen
                com.game.networking.GameClient testClient = new com.game.networking.GameClient();
                boolean connected = testClient.connect(serverIp);

                if (!connected) {
                    // Connection failed - show error and stay on menu
                    System.err.println("MainMenu: Failed to connect to server: " + serverIp);
                    testClient.disconnect();

                    com.game.ui.ErrorDialog errorDialog = new com.game.ui.ErrorDialog(
                        "Connection Failed",
                        "Could not connect to server at " + serverIp + ".\n\n" +
                        "Please check:\n" +
                        "- The server is running\n" +
                        "- The IP address is correct\n" +
                        "- Port 25565 is not blocked",
                        skin
                    );
                    errorDialog.show(stage);
                    return;
                }

                System.out.println("MainMenu: Connection successful, creating game screen...");

                // Connection successful - create GameScreen and pass the connected client
                GameScreen gameScreen = new GameScreen(true);  // Pass true for client mode
                gameScreen.setGameClient(testClient);  // Pass the already-connected client
                game.setScreen(gameScreen);

                System.out.println("MainMenu: Game screen created and connected");
            }

            @Override
            public void onCancel() {
                System.out.println("MainMenu: Multiplayer connection canceled");
            }
        });

        dialog.show(stage);
    }

    private void onSettings() {
        System.out.println("MainMenu: Opening settings");
        // TODO: Open settings screen
    }

    private void onExit() {
        System.out.println("MainMenu: Exiting game");
        Gdx.app.exit();
    }

    @Override
    public void render(float delta) {
        // Clear screen with background color (RGB values: 0.1, 0.1, 0.2 = dark blue)
        // You can change these values to set different background colors
        // Format: glClearColor(red, green, blue, alpha) - values from 0.0 to 1.0
        Gdx.gl.glClearColor(0.3f, 0.2f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Uncomment to render PNG background instead of solid color
        // batch.begin();
        // batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // batch.end();

        // Update and draw stage
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();

        // Uncomment when using PNG background
        // if (batch != null) batch.dispose();
        // if (backgroundTexture != null) backgroundTexture.dispose();
    }
}
