package com.game.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.game.entity.Goblin;
import com.game.entity.Player;

/**
 * Main game screen that manages the game loop
 */
public class GameScreen implements Screen {
    private static final int VIEWPORT_WIDTH = 800;
    private static final int VIEWPORT_HEIGHT = 600;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;

    private GameWorld world;
    private Player player;

    private Texture playerSpriteSheet;
    private Texture goblinSpriteSheet;

    public GameScreen() {
        // Create camera and viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0);

        batch = new SpriteBatch();

        // Load textures
        playerSpriteSheet = new Texture(Gdx.files.internal("Characters/16x32 Walk Cycle-Sheet.png"));
        goblinSpriteSheet = new Texture(Gdx.files.internal("Characters/16x32 Run Cycle-Sheet.png"));

        // Create world (20x15 tiles with 32px tile size = 640x480 world)
        world = new GameWorld(20, 15);

        // Create player at grid position (10, 7) - center of world
        player = new Player(world, 10, 7, playerSpriteSheet);
        world.addEntity(player);

        // Add some goblins
        Goblin goblin1 = new Goblin(world, 5, 5, goblinSpriteSheet);
        world.addEntity(goblin1);

        Goblin goblin2 = new Goblin(world, 15, 10, goblinSpriteSheet);
        world.addEntity(goblin2);
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0.2f, 0.3f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update
        world.update(delta);

        // Center camera on player
        camera.position.set(
            player.getPosition().x + world.getTileSize() / 2f,
            player.getPosition().y + world.getTileSize() / 2f,
            0
        );
        camera.update();

        // Render
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        world.render(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void show() {
        // Called when this screen becomes active
    }

    @Override
    public void hide() {
        // Called when this screen is no longer active
    }

    @Override
    public void pause() {
        // Called when the game is paused
    }

    @Override
    public void resume() {
        // Called when the game is resumed
    }

    @Override
    public void dispose() {
        batch.dispose();
        playerSpriteSheet.dispose();
        goblinSpriteSheet.dispose();
    }
}
