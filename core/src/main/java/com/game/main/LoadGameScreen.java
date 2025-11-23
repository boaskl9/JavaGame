package com.game.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.save.SaveData;
import com.game.save.SaveManager;
import com.game.save.SaveMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Screen for loading saved games.
 * Shows a list of available saves with metadata.
 */
public class LoadGameScreen implements Screen {
    private final Main game;
    private Stage stage;
    private Skin skin;
    private Table mainTable;
    private ScrollPane scrollPane;

    public LoadGameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("assets/ui/wood-theme.json"));

        // Main table
        mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // Title
        Label titleLabel = new Label("Load Save", skin);
        titleLabel.setFontScale(1.5f);
        mainTable.add(titleLabel).padTop(20).padBottom(30).row();

        // Create saves list
        Table savesTable = new Table();
        savesTable.top();

        List<SaveMetadata> saves = SaveManager.getInstance().listSaves();

        if (saves.isEmpty()) {
            Label noSavesLabel = new Label("No saves found", skin);
            savesTable.add(noSavesLabel).pad(20);
        } else {
            for (SaveMetadata save : saves) {
                Table saveRow = createSaveRow(save);
                savesTable.add(saveRow).width(700).height(120).padBottom(10).row();
            }
        }

        // Scroll pane for saves list
        scrollPane = new ScrollPane(savesTable, skin);
        scrollPane.setFadeScrollBars(false);
        mainTable.add(scrollPane).width(750).height(400).padBottom(20).row();

        // Back button
        TextButton backButton = new TextButton("Back to Menu", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        mainTable.add(backButton).width(300).height(50);
    }

    /**
     * Create a UI row for a single save.
     */
    private Table createSaveRow(SaveMetadata save) {
        Table row = new Table(skin);
        // row.setBackground("default-rect"); // Not available in all skins
        row.pad(10);

        // Left side: Save info
        Table infoTable = new Table();
        infoTable.left();

        Label nameLabel = new Label(save.saveName, skin);
        nameLabel.setFontScale(1.2f);
        infoTable.add(nameLabel).left().row();

        Label levelLabel = new Label("Level: " + save.currentLevelId, skin);
        infoTable.add(levelLabel).left().padTop(5).row();

        Label healthLabel = new Label("Health: " + save.playerHealth + "/" + save.playerMaxHealth, skin);
        infoTable.add(healthLabel).left().padTop(5).row();

        Label playtimeLabel = new Label("Playtime: " + save.getFormattedPlaytime(), skin);
        infoTable.add(playtimeLabel).left().padTop(5).row();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Label dateLabel = new Label("Last played: " + dateFormat.format(new Date(save.timestamp)), skin);
        dateLabel.setColor(0.7f, 0.7f, 0.7f, 1f);
        infoTable.add(dateLabel).left().padTop(5).row();

        row.add(infoTable).expandX().left().padRight(20);

        // Right side: Action buttons
        Table buttonTable = new Table();
        buttonTable.defaults().width(100).height(40).padBottom(5);

        TextButton loadButton = new TextButton("Load", skin);
        loadButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onLoad(save.saveName);
            }
        });
        buttonTable.add(loadButton).row();

        TextButton deleteButton = new TextButton("Delete", skin);
        deleteButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onDelete(save.saveName);
            }
        });
        buttonTable.add(deleteButton);

        row.add(buttonTable).right();

        return row;
    }

    private void onLoad(String saveName) {
        SaveData saveData = SaveManager.getInstance().load(saveName);
        if (saveData != null) {
            System.out.println("LoadGameScreen: Loading save: " + saveName);
            game.setScreen(new GameScreen(saveData));
        } else {
            System.err.println("LoadGameScreen: Failed to load save: " + saveName);
        }
    }

    private void onDelete(String saveName) {
        System.out.println("LoadGameScreen: Deleting save: " + saveName);
        SaveManager.getInstance().deleteSave(saveName);

        // Refresh the screen
        hide();
        show();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
    }
}
