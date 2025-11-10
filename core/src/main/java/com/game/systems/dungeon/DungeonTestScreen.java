package com.game.systems.dungeon;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.game.systems.collision.SpatialQuery;
import com.game.systems.dungeon.assembly.AssembledDungeon;
import com.game.systems.dungeon.assembly.DungeonAssembler;
import com.game.systems.dungeon.generation.DungeonGenerator;
import com.game.systems.dungeon.generation.DungeonGenerationResult;
import com.game.systems.dungeon.generation.PlacedRoom;

import java.util.List;
import java.util.Random;

/**
 * Test screen for rapidly iterating on dungeon generation parameters.
 * Provides UI controls to adjust parameters and instantly regenerate dungeons.
 */
public class DungeonTestScreen implements Screen {
    private static final int VIEWPORT_WIDTH = 1920;
    private static final int VIEWPORT_HEIGHT = 1080;

    // Rendering
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    // UI
    private Stage stage;
    private Skin skin;
    private Window controlPanel;

    // Dungeon preview
    private OrthogonalTiledMapRenderer mapRenderer;
    private AssembledDungeon currentDungeon;
    private List<PlacedRoom> placedRooms;
    private SpatialQuery collisionSystem;

    // Parameters (mutable for UI binding)
    private String theme = "cave";
    private int budget = 15;
    private long seed = System.currentTimeMillis();
    private int maxRooms = 50;
    private boolean autoRandomizeSeed = true;  // Randomize seed on each generation

    // UI widgets
    private Label budgetValueLabel;
    private Label maxRoomsValueLabel;
    private Label seedLabel;
    private Label statsLabel;
    private TextField themeField;
    private CheckBox autoRandomizeCheckBox;

    public DungeonTestScreen() {
        // Initialize rendering
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0);
        camera.update();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        // Initialize UI
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("assets/ui/wood-theme.json"));

        createUI();

        // Generate initial dungeon
        generateDungeon();

        // Set input processor
        Gdx.input.setInputProcessor(stage);
    }

    private void createUI() {
        controlPanel = new Window("Dungeon Test Controls", skin);
        controlPanel.setMovable(true);
        controlPanel.setResizable(false);

        Table table = new Table(skin);
        table.defaults().pad(5).left();

        table.padTop(15);

        // Theme input
        table.add(new Label("Theme:", skin)).width(120);
        themeField = new TextField(theme, skin);
        themeField.setMessageText("forest, cave, etc.");
        table.add(themeField).width(200).row();

        // Budget slider
        table.add(new Label("Budget:", skin));
        budgetValueLabel = new Label(String.valueOf(budget), skin);
        table.add(budgetValueLabel).width(50);
        Slider budgetSlider = new Slider(5, 50, 1, false, skin);
        budgetSlider.setValue(budget);
        budgetSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                budget = (int) budgetSlider.getValue();
                budgetValueLabel.setText(String.valueOf(budget));
            }
        });
        table.add(budgetSlider).width(200).row();

        // Max rooms slider
        table.add(new Label("Max Rooms:", skin));
        maxRoomsValueLabel = new Label(String.valueOf(maxRooms), skin);
        table.add(maxRoomsValueLabel).width(50);
        Slider maxRoomsSlider = new Slider(10, 200, 10, false, skin);
        maxRoomsSlider.setValue(maxRooms);
        maxRoomsSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                maxRooms = (int) maxRoomsSlider.getValue();
                maxRoomsValueLabel.setText(String.valueOf(maxRooms));
            }
        });
        table.add(maxRoomsSlider).width(200).row();

        // Seed display and randomize button
        table.add(new Label("Seed:", skin));
        seedLabel = new Label(String.valueOf(seed), skin);
        table.add(seedLabel).colspan(2).row();

        // Auto-randomize checkbox
        autoRandomizeCheckBox = new CheckBox(" Auto-randomize seed", skin);
        autoRandomizeCheckBox.setChecked(autoRandomizeSeed);
        autoRandomizeCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                autoRandomizeSeed = autoRandomizeCheckBox.isChecked();
            }
        });
        table.add(autoRandomizeCheckBox).colspan(1).left();

        // Manual randomize button
        TextButton randomizeSeedBtn = new TextButton("Randomize Seed Now", skin);
        randomizeSeedBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                seed = System.currentTimeMillis();
                seedLabel.setText(String.valueOf(seed));
            }
        });
        table.add(randomizeSeedBtn).colspan(3).fillX().row();

        TextButton generateBtn = new TextButton("Generate Dungeon", skin);
        generateBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                theme = themeField.getText();
                generateDungeon();
            }
        });
        table.add(generateBtn).colspan(3).fillX().row();

        TextButton exitBtn = new TextButton("Exit to Game", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // TODO: Return to game screen
                System.out.println("Exit to game (not implemented yet)");
            }
        });
        table.add(exitBtn).colspan(3).fillX().row();

        // Stats label
        table.add(new Label("Stats:", skin)).colspan(3).row();
        statsLabel = new Label("No dungeon generated", skin);
        statsLabel.setWrap(true);
        table.add(statsLabel).colspan(3).width(400).row();

        controlPanel.add(table).pad(10);
        controlPanel.pack();
        controlPanel.setPosition(20, Gdx.graphics.getHeight() - controlPanel.getHeight() - 20);

        stage.addActor(controlPanel);
    }

    private void generateDungeon() {
        System.out.println("\n=== Generating Dungeon ===");

        try {
            // Auto-randomize seed if enabled
            if (autoRandomizeSeed) {
                seed = System.currentTimeMillis();
                seedLabel.setText(String.valueOf(seed));
                System.out.println("Auto-randomized seed to: " + seed);
            }

            // Build parameters
            DungeonGenerationParams params = new DungeonGenerationParams.Builder()
                .theme(theme)
                .budget(budget)
                .seed(seed)
                .maxRooms(maxRooms)
                .build();

            System.out.println("Parameters: " + params);

            // Generate layout (use static method)
            DungeonGenerationResult result = DungeonGenerator.generateWithSeed(theme, budget, seed);

            if (result == null) {
                statsLabel.setText("ERROR: Generation failed!");
                System.err.println("DungeonGenerator.generateWithSeed returned null");
                return;
            }

            placedRooms = result.getPlacedRooms();

            // Load theme for assembly
            DungeonTheme dungeonTheme = DungeonThemeRegistry.getInstance().getTheme(theme);
            if (dungeonTheme == null) {
                dungeonTheme = DungeonThemeRegistry.getInstance().loadTheme(theme);
            }

            if (dungeonTheme == null) {
                statsLabel.setText("ERROR: Theme '" + theme + "' not found!");
                System.err.println("Failed to load theme: " + theme);
                return;
            }

            // Assemble dungeon (use static method)
            currentDungeon = DungeonAssembler.assemble(result, dungeonTheme);

            // Setup renderer
            if (mapRenderer != null) {
                mapRenderer.dispose();
            }
            mapRenderer = new OrthogonalTiledMapRenderer(currentDungeon.getTiledMap());

            // Setup collision system
            collisionSystem = new SpatialQuery();
            for (Rectangle rect : currentDungeon.getCollisionShapes()) {
                collisionSystem.addRectangle(rect);
            }
            System.out.println("Loaded " + collisionSystem.getShapeCount() + " collision shapes");

            // Update stats
            updateStats();

            // Center camera on dungeon
            centerCameraOnDungeon();

            System.out.println("=== Generation Complete ===\n");

        } catch (Exception e) {
            System.err.println("Failed to generate dungeon: " + e.getMessage());
            e.printStackTrace();
            statsLabel.setText("ERROR: " + e.getMessage());
        }
    }

    private void updateStats() {
        if (currentDungeon != null && placedRooms != null) {
            int roomCount = placedRooms.size();
            int connectedDoors = countConnectedDoors();
            int totalDoors = countTotalDoors();
            int collisionCount = collisionSystem != null ? collisionSystem.getShapeCount() : 0;

            String stats = String.format(
                "Rooms: %d\nConnected Doors: %d/%d\nMap Size: %dx%d tiles\nCollision Shapes: %d",
                roomCount,
                connectedDoors,
                totalDoors,
                currentDungeon.getLevelData().getWidth(),
                currentDungeon.getLevelData().getHeight(),
                collisionCount
            );

            statsLabel.setText(stats);
        }
    }

    private int countConnectedDoors() {
        if (placedRooms == null) return 0;
        int count = 0;
        for (PlacedRoom room : placedRooms) {
            for (PlacedRoom.WorldDoor door : room.getWorldDoors()) {
                if (door.isConnected()) {
                    count++;
                }
            }
        }
        return count / 2;  // Each connection counted twice
    }

    private int countTotalDoors() {
        if (placedRooms == null) return 0;
        int count = 0;
        for (PlacedRoom room : placedRooms) {
            count += room.getWorldDoors().size();
        }
        return count;
    }

    private void centerCameraOnDungeon() {
        if (currentDungeon != null) {
            int mapWidth = currentDungeon.getLevelData().getWidth();
            int mapHeight = currentDungeon.getLevelData().getHeight();
            camera.position.set(mapWidth * 16 / 2f, mapHeight * 16 / 2f, 0);
            camera.update();
        }
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Handle camera movement
        handleCameraInput(delta);
        camera.update();

        // Render dungeon
        if (mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }

        // Render debug overlays
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Render collision shapes (RED)
        if (collisionSystem != null) {
            shapeRenderer.setColor(Color.RED);
            for (Rectangle rect : collisionSystem.getRectangles()) {
                shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
            }
            for (Polygon poly : collisionSystem.getPolygons()) {
                shapeRenderer.polygon(poly.getTransformedVertices());
            }
        }

        // Render room bounds (GREEN)
        if (placedRooms != null && currentDungeon != null) {
            // Apply the same offset that was used for tiles and collision
            float pixelOffsetX = currentDungeon.getOffsetX() * 16;
            float pixelOffsetY = currentDungeon.getOffsetY() * 16;

            shapeRenderer.setColor(Color.GREEN);
            for (PlacedRoom room : placedRooms) {
                RoomBounds bounds = room.getTemplate().getBounds();
                float x = room.getWorldX() - pixelOffsetX;
                float y = room.getWorldY() - pixelOffsetY;
                float w = bounds.getWidth() * 16;
                float h = bounds.getHeight() * 16;
                shapeRenderer.rect(x, y, w, h);
            }
        }

        // Render doors (YELLOW for unconnected, CYAN for connected)
        if (placedRooms != null && currentDungeon != null) {
            float pixelOffsetX = currentDungeon.getOffsetX() * 16;
            float pixelOffsetY = currentDungeon.getOffsetY() * 16;

            for (PlacedRoom room : placedRooms) {
                for (PlacedRoom.WorldDoor door : room.getWorldDoors()) {
                    if (door.isConnected()) {
                        shapeRenderer.setColor(Color.CYAN);
                    } else {
                        shapeRenderer.setColor(Color.YELLOW);
                    }
                    float doorX = door.getWorldX() - pixelOffsetX;
                    float doorY = door.getWorldY() - pixelOffsetY;
                    float doorW = door.getDoor().getWidth();
                    float doorH = door.getDoor().getHeight();
                    shapeRenderer.rect(doorX, doorY, doorW, doorH);
                }
            }
        }

        shapeRenderer.end();

        // Render UI
        stage.act(delta);
        stage.draw();

        // Render help text
        batch.begin();
        font.draw(batch, "WASD: Move Camera | +/-: Zoom | ESC: Exit | RED: Colliders | GREEN: Room Bounds | YELLOW: Open Doors | CYAN: Connected Doors", 10, 30);
        batch.end();
    }

    private void handleCameraInput(float delta) {
        float moveSpeed = 300 * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.position.y += moveSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.position.y -= moveSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.position.x -= moveSpeed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.position.x += moveSpeed;
        }

        // Exit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // TODO: Return to game
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        stage.getViewport().update(width, height, true);

        // Reposition control panel
        controlPanel.setPosition(20, height - controlPanel.getHeight() - 20);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        stage.dispose();
        skin.dispose();
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
    }
}
