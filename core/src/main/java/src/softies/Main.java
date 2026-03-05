package src.softies;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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

    // World dimensions and board bounds
    private WorldCalculator world;

    // UI and input handlers
    private UIController uiController;
    private InputHandler inputHandler;
    private BoardRenderer boardRenderer;

    // Constants
    private final float unitScale = 0.25f;
    private final float margin = 150f;               // empty space around the board
    private final float offsetX = 20f;                // camera offset right
    private final float offsetY = 2000f;               // camera offset down (positive moves view down)

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        gameState = new GameState();
        boardLogic = new QuaxBoard();

        // 1. Load the map
        try {
            map = new TmxMapLoader().load("PolygonalGrid.tmx");
        } catch (Exception e) {
            Gdx.app.error("Quax", "FILE ERROR: " + e.getMessage());
            return;
        }

        // 2. Get layers
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

        // 3. Compute world dimensions
        world = new WorldCalculator(unitScale);
        world.compute(map, octagonLayer);

        // 4. Set up camera with margins
        setupCamera();

        // 5. Create renderer and helpers
        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // 6. Create UI and input handlers
        uiController = new UIController(viewport, camera, world);
        inputHandler = new InputHandler(map, octagonLayer, diamondLayer, unitScale, gameState, viewport);
        boardRenderer = new BoardRenderer(world, gameState, viewport);

        // 7. Generate font
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

        viewport.apply();
        renderer.setView(camera);
        renderer.render();

        // Update UI bounds and handle input
        uiController.updateBounds();

        if (Gdx.input.justTouched()) {
            // First give UI a chance to consume the click
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);
            if (!uiController.handleInput(touchPos)) {
                // Not consumed by UI → handle board click
                inputHandler.handleBoardClick(Gdx.input.getX(), Gdx.input.getY());
            }
        }

        // Draw board labels (text only)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        boardRenderer.render(batch, font);
        batch.end();

        // Draw UI (buttons, dialog) which uses ShapeRenderer and then text
        uiController.draw(shapeRenderer, batch, font);
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
