package com.game.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.game.entity.Player;
import com.game.world.GameWorld;

/**
 * Main game screen that manages the game loop
 */
public class GameScreen implements Screen {
    private static final int VIEWPORT_WIDTH = 350;
    private static final int VIEWPORT_HEIGHT = 200;

    private SpriteBatch batch;

    // Debug
    private BitmapFont debugFont;
    private boolean debugMode = false;
    private ShapeRenderer shapeRenderer;

    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;     // UI camera (fixed to screen)
    private Viewport viewport;

    private GameWorld world;
    private LevelLoader levelLoader;
    private Player player;

    public GameScreen() {
        // Create camera and viewport
        camera = new OrthographicCamera();
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0);


        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Create debug font

        debugFont = new BitmapFont();
        debugFont.setColor(1, 1, 0, 1); // Yellow text
        debugFont.getData().setScale(0.5f); // Make it bigger

        // Load the level
        levelLoader = new LevelLoader();
        levelLoader.loadLevel("Maps/prototype.tmx");


        // Get world dimensions from the loaded map
        int mapWidth = levelLoader.getMapWidth();
        int mapHeight = levelLoader.getMapHeight();

        // Create world based on map size
        world = new GameWorld(mapWidth, mapHeight);

        levelLoader.loadCollisionData(world);

        // Populate world collision data from the map
        //levelLoader.populateWorldCollision(world);

        // Get player spawn position from map (or use default)
        float[] playerSpawn = levelLoader.getPlayerSpawnPosition();
        int spawnGridX = (int)(playerSpawn[0] / world.getTileSize());
        int spawnGridY = (int)(playerSpawn[1] / world.getTileSize());

        // Create player at spawn position
        player = new Player(world, spawnGridX, spawnGridY);
        world.addEntity(player);

        // Spawn entities from map
        //levelLoader.spawnEntities(world, goblinSpriteSheet);
    }

    @Override
    public void render(float delta) {

        // Check for debug toggle
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugMode = !debugMode;
            System.out.println("Debug mode: " + debugMode);
        }

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update
        world.update(delta);

        // Update camera to follow player with bounds
        updateCamera();

        LevelRenderer.render(batch, levelLoader.getMapRenderer(), world.getEntities(), camera, 2);

        // Render the tiled map
        //levelLoader.render(camera);

        // Render game entities on top of the map
        //batch.setProjectionMatrix(camera.combined);
        //batch.begin();
        //world.render(batch);
        //batch.end();

        // Render debug collision shapes
        if (debugMode) {
            renderCollisionDebug();
            renderDebugStats();
        }
    }

    private void renderCollisionDebug() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 0, 0, 1); // Red

        for (Rectangle rect : world.getCollisionSystem().getCollisionRectangles()) {
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }

        for (Polygon poly : world.getCollisionSystem().getCollisionPolygons()) {
            shapeRenderer.polygon(poly.getTransformedVertices());
        }

        shapeRenderer.end();
    }

    private void renderDebugStats() {
        if (!debugMode) return;

        // Use UI camera (fixed to screen, not world)
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // Get performance stats
        int fps = Gdx.graphics.getFramesPerSecond();
        long memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        long memTotal = Runtime.getRuntime().totalMemory() / 1048576;

        // Get player position
        float playerX = player.getPosition().x;
        float playerY = player.getPosition().y;

        // Render debug text (top-left corner)
        float x = 10;
        float y = VIEWPORT_HEIGHT - 10;
        float lineHeight = 9;

        debugFont.draw(batch, "FPS: " + fps, x, y);
        debugFont.draw(batch, "Memory: " + memUsed + "/" + memTotal + " MB", x, y - lineHeight);
        debugFont.draw(batch, "Player Pos: (" + (int)playerX + ", " + (int)playerY + ")", x, y - lineHeight * 2);
        //debugFont.draw(batch, "Entities: " + world.getEntityCount(), x, y - lineHeight * 3);
        debugFont.draw(batch, "Press F3 to toggle debug", x, y - lineHeight * 4);

        batch.end();
    }



    private void updateCamera() {
        // Get player center position
        float playerCenterX = player.getPosition().x + (world.getTileSize() / 2f);
        float playerCenterY = player.getPosition().y + (world.getTileSize() / 2f);

        // Calculate world bounds (in pixels)
        float worldWidth = world.getWorldWidth() * world.getTileSize();
        float worldHeight = world.getWorldHeight() * world.getTileSize();

        // Calculate camera bounds
        float cameraHalfWidth = camera.viewportWidth * camera.zoom / 2f;
        float cameraHalfHeight = camera.viewportHeight * camera.zoom / 2f;

        // Clamp camera position to world bounds
        float camX = Math.max(cameraHalfWidth, Math.min(playerCenterX, worldWidth - cameraHalfWidth));
        float camY = Math.max(cameraHalfHeight, Math.min(playerCenterY, worldHeight - cameraHalfHeight));

        camera.position.set(camX, camY, 0);
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiCamera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
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
        shapeRenderer.dispose();
        levelLoader.dispose();
        debugFont.dispose();
    }
}
