package com.game.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
    private static final int VIEWPORT_WIDTH = 400;
    private static final int VIEWPORT_HEIGHT = 300;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;

    private GameWorld world;
    private LevelLoader levelLoader;
    private Player player;

    private Texture goblinSpriteSheet;

    public GameScreen() {
        // Create camera and viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Load textures
        goblinSpriteSheet = new Texture(Gdx.files.internal("Characters/16x32 Walk Cycle-Sheet.png"));

        // Load the level
        levelLoader = new LevelLoader();
        levelLoader.loadLevel("Maps/StartArea.tmx");


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
        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update
        world.update(delta);

        // Update camera to follow player with bounds
        updateCamera();

        // Render the tiled map
        levelLoader.render(camera);

        // Render game entities on top of the map
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        world.render(batch);
        // Call it in render():
        batch.end();
        renderCollisionDebug(); // Add this line
    }

    // In GameScreen, add to render() method after batch.end():
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
        goblinSpriteSheet.dispose();
        levelLoader.dispose();
    }
}
