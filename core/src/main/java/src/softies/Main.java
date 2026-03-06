package src.softies;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import src.softies.board.*;

public class Main extends ApplicationAdapter {
    // Core objects
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    // Game state and logic
    private GameState gameState;
    private QuaxBoard boardLogic;   // underlying logic (not fully integrated)

    // World dimensions
    private WorldCalculator world;

    // UI and input handlers
    private UIController uiController;
    private InputHandler inputHandler;
    private BoardRenderer boardRenderer;

    // Constants
    private final float unitScale = 0.25f;
    private final float margin = 150f;
    private final float offsetX = 20f;
    private final float offsetY = 2000f;

    // Welcome screen
    private boolean showWelcome = true;
    private Rectangle startButtonBounds;

    // Status message for invalid moves
    private String statusMessage = "";
    private float statusMessageUntil = 0f;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        gameState = new GameState();
        boardLogic = new QuaxBoard();

        // Load map
        try {
            map = new TmxMapLoader().load("PolygonalGrid.tmx");
        } catch (Exception e) {
            Gdx.app.error("Quax", "FILE ERROR: " + e.getMessage());
            return;
        }

        // Get layers
        TiledMapTileLayer octagonLayer = (TiledMapTileLayer) map.getLayers().get("Octagons");
        MapLayer diamondLayer = map.getLayers().get("Diamonds");
        if (octagonLayer == null || diamondLayer == null) {
            System.out.println("Layer names in map:");
            for (int i = 0; i < map.getLayers().getCount(); i++) {
                System.out.println(" - " + map.getLayers().get(i).getName());
            }
            Gdx.app.error("Quax", "Required layers not found!");
            return;
        }

        // Compute world dimensions
        world = new WorldCalculator(unitScale);
        world.compute(map, octagonLayer);

        // Set up camera
        setupCamera();

        // Create renderer and helpers
        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Create handlers
        uiController = new UIController(viewport, camera, world);
        inputHandler = new InputHandler(map, octagonLayer, diamondLayer, unitScale, gameState, viewport);
        boardRenderer = new BoardRenderer(world, gameState, viewport);

        // Generate font
        generateFont();
    }

    private void setupCamera() {
        float worldWidth  = world.boardWidth + 2 * margin;
        float worldHeight = world.boardHeight + 2 * margin;
        viewport.setWorldSize(worldWidth, worldHeight);
        camera.position.set(world.boardCenterX + offsetX, world.boardCenterY + offsetY, 0);
        System.out.println("Camera position set to: (" + camera.position.x + ", " + camera.position.y + ")");
        camera.update();
    }

    private void generateFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("SF-Pro-Display-Bold.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 20;
        parameter.color = Color.WHITE;
        parameter.kerning = true;
        parameter.spaceX = -1;
        parameter.borderColor = Color.BLACK;
        parameter.borderWidth = 1;
        font = generator.generateFont(parameter);
        generator.dispose();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update status message timer
        if (statusMessageUntil > 0f) {
            statusMessageUntil -= Gdx.graphics.getDeltaTime();
            if (statusMessageUntil <= 0f) {
                statusMessageUntil = 0f;
                statusMessage = "";
            }
        }

        viewport.apply();
        uiController.updateBounds();

        // Handle input
        if (Gdx.input.justTouched()) {
            handleInput();
        }

        // --- Welcome screen ---
        if (showWelcome) {
            drawWelcomeScreen();
            return;
        }

        // --- Draw the map (board tiles) ---
        renderer.setView(camera);
        renderer.render();

        // --- Draw board labels and UI ---
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        boardRenderer.render(batch, font, statusMessage);
        batch.end();

        // --- Draw quit button and confirmation dialog ---
        uiController.draw(shapeRenderer, batch, font);
    }

    private void drawWelcomeScreen() {
        // Button background
        computeStartButtonBounds();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (startButtonBounds != null) {
            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(mousePos);
            boolean hover = startButtonBounds.contains(mousePos.x, mousePos.y);
            shapeRenderer.setColor(hover ? 0.3f : 0.2f, 0.2f, 0.2f, 0.9f);
            shapeRenderer.rect(startButtonBounds.x, startButtonBounds.y,
                startButtonBounds.width, startButtonBounds.height);
        }
        shapeRenderer.end();

        // Button border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (startButtonBounds != null) {
            shapeRenderer.rect(startButtonBounds.x, startButtonBounds.y,
                startButtonBounds.width, startButtonBounds.height);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Text
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        String welcomeTitle = "QUAX";
        GlyphLayout wlTitle = new GlyphLayout(font, welcomeTitle);
        float titleX = world.boardCenterX - wlTitle.width / 2f;
        float titleY = world.boardCenterY + 120f;
        font.draw(batch, welcomeTitle, titleX, titleY);

        String subTitle = "Human vs Human";
        GlyphLayout wlSub = new GlyphLayout(font, subTitle);
        float subX = world.boardCenterX - wlSub.width / 2f;
        float subY = world.boardCenterY + 80f;
        font.draw(batch, subTitle, subX, subY);

        if (startButtonBounds != null) {
            GlyphLayout btnLayout = new GlyphLayout(font, "Start Game");
            float btnTextX = startButtonBounds.x + (startButtonBounds.width - btnLayout.width) / 2f;
            float btnTextY = startButtonBounds.y + (startButtonBounds.height + btnLayout.height) / 2f;
            font.draw(batch, "Start Game", btnTextX, btnTextY);
        }
        batch.end();
    }

    private void computeStartButtonBounds() {
        float startW = 220;
        float startH = 60;
        float startX = world.boardCenterX - startW / 2f;
        float startY = world.boardCenterY - 10f;
        startButtonBounds = new Rectangle(startX, startY, startW, startH);
    }

    private void handleInput() {
        Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchPos);

        // Welcome screen
        if (showWelcome) {
            if (startButtonBounds != null && startButtonBounds.contains(touchPos.x, touchPos.y)) {
                showWelcome = false;
            }
            return;
        }

        // UI (quit button / dialog)
        if (uiController.handleInput(touchPos)) {
            return;
        }

        // Board interaction
        InputHandler.MoveResult result = inputHandler.handleBoardClick(Gdx.input.getX(), Gdx.input.getY());
        switch (result) {
            case OCCUPIED:
                showStatusMessage("Invalid move. Select an empty cell.", 1.6f);
                break;
            case NOT_A_CELL:
                // ignore clicks on empty space (no message)
                break;
            case SUCCESS:
                // move made, turn already toggled by InputHandler
                break;
        }
    }

    private void showStatusMessage(String message, float seconds) {
        statusMessage = message;
        statusMessageUntil = seconds;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
