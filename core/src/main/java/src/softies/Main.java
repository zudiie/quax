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
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import src.softies.board.*;

// the entry point for the libgdx application — owns the render loop and ties all subsystems together
// responsible for loading the map, setting up the camera, and delegating input/rendering to helpers
public class Main extends ApplicationAdapter {

    // --- core libgdx rendering objects ---
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont welcomeFont;

    // --- game logic and state ---
    private GameState gameState;
    private QuaxBoard boardLogic;

    // --- world coordinate calculations ---
    private WorldCalculator world;

    // --- ui and input subsystems ---
    private UIController uiController;
    private InputHandler inputHandler;
    private BoardRenderer boardRenderer;

    // maps are rendered at 25% of their pixel size so tiles don't fill the whole screen
    private final float unitScale = 0.25f;
    // padding around the board so it doesn't press against the viewport edges
    private final float margin = 150f;
    // fine-tune the camera centre position if needed
    private final float offsetX = 0f;
    private final float offsetY = 0f;
    // horizontal space added to the right of the board for the UI panel
    private final float offsetZ = 300f;

    // layer offsets read from the tmx file to align diamonds with octagons
    private float layerOffsetX, layerOffsetY;

    // whether to show the welcome screen instead of the actual game
    private boolean showWelcome = true;
    private Rectangle startButtonBounds;

    // temporary status message shown after invalid moves, and a countdown timer
    private String statusMessage = "";
    private float statusMessageUntil = 0f;

    /**
     * called once by libgdx when the application starts
     * loads the map, sets up all subsystems and generates fonts
     */
    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(2000, 2000, camera);
        gameState = new GameState();
        boardLogic = new QuaxBoard();

        // load the tiled map — log the error and bail if the file isn't found
        try {
            map = new TmxMapLoader().load("PolygonalGrid.tmx");
        } catch (Exception e) {
            Gdx.app.error("Quax", "FILE ERROR: " + e.getMessage());
            return;
        }

        // grab the two layers we need — log all layer names if either is missing
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

        // compute all the world-space board bounds so other classes can reference them
        world = new WorldCalculator(unitScale);
        world.compute(map, octagonLayer);

        // cache the octagon layer offset - used when rendering diamonds to align them correctly
        layerOffsetX = octagonLayer.getOffsetX() * unitScale;
        layerOffsetY = octagonLayer.getOffsetY() * unitScale;

        System.out.println("Board bounds: (" + world.boardMinX + ", " + world.boardMinY + ") to ("
            + world.boardMaxX + ", " + world.boardMaxY + ")");
        System.out.println("Board centre: (" + world.boardCenterX + ", " + world.boardCenterY + ")");
        System.out.println("World size: " + (world.boardWidth + 2 * margin) + " x " + (world.boardHeight + 2 * margin));

        setupCamera();

        // create the tiled map renderer and the two drawing primitives
        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // wire up all the subsystem helpers
        uiController  = new UIController(viewport, camera, world);
        inputHandler  = new InputHandler(map, octagonLayer, diamondLayer, unitScale, gameState, viewport, boardLogic);
        boardRenderer = new BoardRenderer(world, gameState, viewport);

        generateFont();
    }

    /**
     * positions the camera so the board fits the viewport with the UI panel offset accounted for
     * adds the margin and offsetZ so the right-side panel doesn't get cropped
     */
    private void setupCamera() {
        // make the world a bit wider to accommodate the objectives panel on the right
        float worldWidth  = world.boardWidth  + 2 * margin + offsetZ;
        float worldHeight = world.boardHeight + 2 * margin;
        viewport.setWorldSize(worldWidth, worldHeight);
        // centre on the board, not the full world width
        camera.position.set(world.boardCenterX + offsetX, world.boardCenterY + offsetY, 0);
        System.out.println("Camera position set to: (" + camera.position.x + ", " + camera.position.y + ")");
        camera.update();
    }

    /**
     * generates the two fonts used in the game — a normal UI font and a larger welcome screen font
     * both load from the same TTF file with different size and border settings
     */
    private void generateFont() {
        // main UI font — small, white with a thin black border for readability over tiles
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("SF-Pro-Display-Bold.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 20;
        parameter.color = Color.WHITE;
        parameter.kerning = true;
        parameter.spaceX = -1;
        parameter.borderColor = Color.BLACK;
        parameter.borderWidth = 1;
        font = generator.generateFont(parameter);
        // dispose immediately after generating — the font atlas is already on the GPU
        generator.dispose();

        // welcome screen font — bigger with a thicker border so it stands out
        FreeTypeFontGenerator welcomeGen = new FreeTypeFontGenerator(Gdx.files.internal("SF-Pro-Display-Bold.ttf"));
        FreeTypeFontParameter welcomeParam = new FreeTypeFontParameter();
        welcomeParam.size = 30;
        welcomeParam.color = Color.WHITE;
        welcomeParam.borderColor = Color.BLACK;
        welcomeParam.borderWidth = 2;
        welcomeFont = welcomeGen.generateFont(welcomeParam);
        welcomeGen.dispose();
    }

    /**
     * called every frame by libgdx — clears the screen, ticks the status timer and draws everything
     */
    @Override
    public void render() {
        // grey background so empty space around the board doesn't look stark
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // count down the status message timer and clear it when it expires
        if (statusMessageUntil > 0f) {
            statusMessageUntil -= Gdx.graphics.getDeltaTime();
            if (statusMessageUntil <= 0f) {
                statusMessageUntil = 0f;
                statusMessage = "";
            }
        }

        viewport.apply();
        // button bounds must be refreshed each frame in case the window was resized
        uiController.updateBounds();

        if (Gdx.input.justTouched()) {
            handleInput();
        }

        // allow the second player to activate the pie rule with the P key
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.P)) {
            if (gameState.isFirstMoveMade() && !gameState.isPieRuleUsed()) {
                gameState.applyPieRule();
                showStatusMessage("Pie rule applied.", 1.6f);
            }
        }


        // short-circuit to the welcome screen if the game hasn't started yet
        if (showWelcome) {
            drawWelcomeScreen();
            return;
        }

        // render the tiled map (octagons layer renders automatically via the map renderer)
        renderer.setView(camera);
        renderer.render();

        // diamonds are a separate object layer so they need their own draw pass
        renderDiamondLayer();

        // draw all the text labels on top of the tiles
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        boardRenderer.render(batch, font, statusMessage);
        batch.end();

        // draw the quit button and any dialog on top of everything else
        uiController.draw(shapeRenderer, batch, font);
    }

    /**
     * manually draws each diamond TextureMapObject because the object layer doesn't auto-render
     * applies the same y-flip correction used in InputHandler to keep visual and hit positions in sync
     */
    private void renderDiamondLayer() {
        int mapHeightInTiles = map.getProperties().get("height", Integer.class);
        int tileHeightPx     = map.getProperties().get("tileheight", Integer.class);
        // total map height in world units - needed to flip tiled's top-down y to libgdx's bottom-up
        float worldMapHeight = mapHeightInTiles * tileHeightPx * unitScale;

        MapLayer diamondLayer = map.getLayers().get("Diamonds");
        if (diamondLayer == null) return;

        float dOffsetX = diamondLayer.getOffsetX() * unitScale;
        float dOffsetY = diamondLayer.getOffsetY() * unitScale;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (MapObject object : diamondLayer.getObjects()) {
            if (object instanceof TextureMapObject) {
                TextureMapObject tmo = (TextureMapObject) object;
                float objW = tmo.getProperties().get("width",  Float.class) * unitScale;
                float objH = tmo.getProperties().get("height", Float.class) * unitScale;
                float worldX = tmo.getX() * unitScale + dOffsetX;
                // the +2*objH+4f is a deliberate correction to align diamonds visually with the grid
                float worldY = worldMapHeight - (tmo.getY() * unitScale) - dOffsetY + 2 * objH + 4f;
                batch.draw(tmo.getTextureRegion(), worldX, worldY, objW, objH);
            }
        }
        batch.end();
    }

    /**
     * draws the welcome screen — title text, subtitle, and the start button with hover highlight
     */
    private void drawWelcomeScreen() {
        // make sure the button bounds are up to date before we draw or test them
        computeStartButtonBounds();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (startButtonBounds != null) {
            // lighten the button slightly when the mouse is over it
            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(mousePos);
            boolean hover = startButtonBounds.contains(mousePos.x, mousePos.y);
            shapeRenderer.setColor(hover ? 0.3f : 0.2f, 0.2f, 0.2f, 0.9f);
            shapeRenderer.rect(startButtonBounds.x, startButtonBounds.y,
                startButtonBounds.width, startButtonBounds.height);
        }
        shapeRenderer.end();

        // white border around the button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (startButtonBounds != null) {
            shapeRenderer.rect(startButtonBounds.x, startButtonBounds.y,
                startButtonBounds.width, startButtonBounds.height);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // centre the "QUAX" title above the button using glyph layout for accurate width
        String welcomeTitle = "QUAX";
        GlyphLayout wlTitle = new GlyphLayout(welcomeFont, welcomeTitle);
        float titleX = world.boardCenterX + (offsetZ - wlTitle.width) / 2f;
        float titleY = world.boardCenterY + 120f;
        welcomeFont.draw(batch, welcomeTitle, titleX, titleY);

        // subtitle sits just below the title
        String subTitle = "Human vs Human";
        GlyphLayout wlSub = new GlyphLayout(welcomeFont, subTitle);
        float subX = world.boardCenterX + (offsetZ - wlSub.width) / 2f;
        float subY = world.boardCenterY + 80f;
        welcomeFont.draw(batch, subTitle, subX, subY);

        // centre the button label inside the button rectangle
        if (startButtonBounds != null) {
            GlyphLayout btnLayout = new GlyphLayout(font, "Start Game");
            float btnTextX = startButtonBounds.x + (startButtonBounds.width  - btnLayout.width)  / 2f;
            float btnTextY = startButtonBounds.y + (startButtonBounds.height + btnLayout.height) / 2f;
            font.draw(batch, "Start Game", btnTextX, btnTextY);
        }
        batch.end();
    }

    /**
     * calculates the position and size of the start button, centred in the right-side panel
     */
    private void computeStartButtonBounds() {
        float startW = 220;
        float startH = 60;
        // the button lives in the offsetZ panel area to the right of the board
        float startX = world.boardCenterX + (offsetZ - startW) / 2f;
        float startY = world.boardCenterY - 30f;
        startButtonBounds = new Rectangle(startX, startY, startW, startH);
    }

    /**
     * routes a touch event to whichever system should handle it — welcome screen, UI, or board
     * called when Gdx.input.justTouched() is true
     */
    private void handleInput() {
        Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchPos);

        // on the welcome screen only the start button matters
        if (showWelcome) {
            if (startButtonBounds != null && startButtonBounds.contains(touchPos.x, touchPos.y)) {
                showWelcome = false;
            }
            return;
        }

        // give the ui controller first dibs — quit button and dialog take priority
        if (uiController.handleInput(touchPos)) {
            return;
        }

        // pass the click through to the board and react to what came back
        InputHandler.MoveResult result = inputHandler.handleBoardClick(Gdx.input.getX(), Gdx.input.getY());
        switch (result) {
            case OCCUPIED:
                // let the player know they clicked a taken cell
                showStatusMessage("Invalid move. Select an empty cell.", 1.6f);
                break;
            case NOT_A_CELL:
                // clicked on blank space — silently ignore
                break;
            case SUCCESS:
                // move went through fine — nothing extra to do here
                break;
        }
    }

    /**
     * sets a temporary status message that disappears after the given number of seconds
     * @param message the text to display
     * @param seconds how long to keep it on screen
     */
    private void showStatusMessage(String message, float seconds) {
        statusMessage = message;
        statusMessageUntil = seconds;
    }

    /**
     * called by libgdx when the window is resized - updates the viewport and projection matrix
     * @param width new window width in pixels
     * @param height new window height in pixels
     */
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        // keep the batch projection in sync so text doesn't appear in the wrong place
        batch.setProjectionMatrix(camera.combined);
    }

    /**
     * called by libgdx on shutdown - releases all native resources to avoid memory leaks
     */
    @Override
    public void dispose() {
        // null checks guard against a partial create() that returned early
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (welcomeFont != null) welcomeFont.dispose();
    }
}
