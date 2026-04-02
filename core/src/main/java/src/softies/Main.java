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

    private final float unitScale = 0.25f;
    private final float margin    = 150f;
    private final float offsetX   = 0f;
    private final float offsetY   = 0f;
    private final float offsetZ   = 300f;

    private float layerOffsetX, layerOffsetY;

    private boolean showWelcome = true;
    private Rectangle startButtonBounds;
    private Rectangle welcomeQuitButtonBounds;

    // hover overlay: semi-transparent warm yellow, matches the tone of the gold theme
    private static final Color HOVER_FILL    = new Color(1f, 0.95f, 0.4f, 0.22f);
    private static final Color HOVER_OUTLINE = new Color(1f, 0.88f, 0.2f, 0.70f);

    // welcome screen panel theme: dark blue background, gold border
    private static final Color PANEL_BG      = new Color(0.03f, 0.06f, 0.22f, 0.93f);
    private static final Color GOLD_BORDER   = new Color(0.82f, 0.67f, 0.12f, 1f);
    private static final Color BTN_BG        = new Color(0.04f, 0.07f, 0.26f, 1f);
    private static final Color BTN_BG_HOVER  = new Color(0.09f, 0.14f, 0.40f, 1f);
    private static final Color QUIT_BG       = new Color(0.30f, 0.06f, 0.06f, 1f);
    private static final Color QUIT_BG_HOVER = new Color(0.48f, 0.10f, 0.10f, 1f);

    private String statusMessage      = "";
    private float  statusMessageUntil = 0f;

    /**
     * called once by libgdx at startup — loads the map, wires all subsystems, generates fonts
     */
    @Override
    public void create() {
        camera    = new OrthographicCamera();
        viewport  = new FitViewport(2000, 2000, camera);
        gameState = new GameState();
        boardLogic = new QuaxBoard();

        try {
            map = new TmxMapLoader().load("PolygonalGrid.tmx");
        } catch (Exception e) {
            Gdx.app.error("Quax", "FILE ERROR: " + e.getMessage());
            return;
        }

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

        world = new WorldCalculator(unitScale);
        world.compute(map, octagonLayer);

        layerOffsetX = octagonLayer.getOffsetX() * unitScale;
        layerOffsetY = octagonLayer.getOffsetY() * unitScale;

        setupCamera();

        renderer      = new OrthogonalTiledMapRenderer(map, unitScale);
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // UIController gets gameState so it can show/hide the pie rule button
        uiController  = new UIController(viewport, camera, world, gameState);
        inputHandler  = new InputHandler(map, octagonLayer, diamondLayer, unitScale, gameState, viewport, boardLogic);
        boardRenderer = new BoardRenderer(world, gameState, viewport);

        generateFont();
    }

    /**
     * sizes the viewport to fit the board plus the right-side panel, then centres the camera
     */
    private void setupCamera() {
        float worldWidth  = world.boardWidth  + 2 * margin + offsetZ;
        float worldHeight = world.boardHeight + 2 * margin;
        viewport.setWorldSize(worldWidth, worldHeight);
        camera.position.set(world.boardCenterX + offsetX, world.boardCenterY + offsetY, 0);
        camera.update();
    }

    /**
     * generates both fonts at high point size then scales down for crisp rendering
     * the bullet character is explicitly included so "•" renders correctly
     */
    private void generateFont() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("SF-Pro-Display-Bold.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size        = 100;
        // include • so objective bullets render correctly
        parameter.characters  = FreeTypeFontGenerator.DEFAULT_CHARS + "•";
        parameter.color       = Color.WHITE;
        parameter.kerning     = true;
        parameter.spaceX      = -1;
        parameter.borderColor = Color.BLACK;
        parameter.borderWidth = 5;
        font = generator.generateFont(parameter);
        font.getData().setScale(0.2f);
        generator.dispose();

        FreeTypeFontGenerator welcomeGen = new FreeTypeFontGenerator(Gdx.files.internal("SF-Pro-Display-Bold.ttf"));
        FreeTypeFontParameter welcomeParam = new FreeTypeFontParameter();
        welcomeParam.size        = 150;
        // include • for the welcome font too in case it's ever needed
        welcomeParam.characters  = FreeTypeFontGenerator.DEFAULT_CHARS + "•";
        welcomeParam.color       = Color.WHITE;
        welcomeParam.borderColor = Color.BLACK;
        welcomeParam.borderWidth = 10;
        welcomeFont = welcomeGen.generateFont(welcomeParam);
        welcomeFont.getData().setScale(0.2f);
        welcomeGen.dispose();
    }

    /**
     * main render loop — clears screen, ticks timers, draws everything, draws UI on top
     */
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (statusMessageUntil > 0f) {
            statusMessageUntil -= Gdx.graphics.getDeltaTime();
            if (statusMessageUntil <= 0f) {
                statusMessageUntil = 0f;
                statusMessage = "";
            }
        }

        viewport.apply();
        uiController.updateBounds();

        if (Gdx.input.justTouched()) {
            handleInput();
        }

        if (showWelcome) {
            drawWelcomeScreen();
        } else {
            renderer.setView(camera);
            renderer.render();

            renderDiamondLayer();

            // update hover polygons then draw the shape-accurate highlight
            inputHandler.updateHover(Gdx.input.getX(), Gdx.input.getY());
            renderHoverOverlay();

            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            boardRenderer.render(batch, font, statusMessage);
            batch.end();
        }

        // UIController always draws last so dialogs appear over everything
        uiController.draw(shapeRenderer, batch, font);
    }

    /**
     * manually renders the diamond object layer — it doesn't auto-render like the tile layers
     */
    private void renderDiamondLayer() {
        int mapHeightInTiles = map.getProperties().get("height", Integer.class);
        int tileHeightPx     = map.getProperties().get("tileheight", Integer.class);
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
                float worldY = worldMapHeight - (tmo.getY() * unitScale) - dOffsetY + 2 * objH + 4f;
                batch.draw(tmo.getTextureRegion(), worldX, worldY, objW, objH);
            }
        }
        batch.end();
    }

    /**
     * draws a shape-accurate hover highlight using triangle-fan fill + polygon outline
     * uses the polygon vertices from InputHandler so the highlight matches the actual tile art
     * (a bounding-box rect would bleed into transparent PNG corners)
     */
    private void renderHoverOverlay() {
        if (inputHandler.getHoverShape() == InputHandler.HoverShape.NONE) return;

        float[] verts = inputHandler.getHoverVertices();
        if (verts.length < 6) return; // need at least 3 vertices

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // compute centroid so we can fan-triangulate the polygon from the inside
        int n = verts.length / 2;
        float cx = 0, cy = 0;
        for (int i = 0; i < verts.length; i += 2) {
            cx += verts[i];
            cy += verts[i + 1];
        }
        cx /= n;
        cy /= n;

        // filled triangle fan — each pair of adjacent vertices forms a triangle with the centroid
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(HOVER_FILL);
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            shapeRenderer.triangle(
                cx,               cy,
                verts[i * 2],     verts[i * 2 + 1],
                verts[next * 2],  verts[next * 2 + 1]
            );
        }
        shapeRenderer.end();

        // sharp outline traces the exact tile shape
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(HOVER_OUTLINE);
        shapeRenderer.polygon(verts);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * draws the welcome screen — dark-blue panel with gold border, title, and two styled buttons
     */
    private void drawWelcomeScreen() {
        computeWelcomeButtonBounds();

        // panel spans from slightly above the quit button to above the title
        float panelPadX = 22f, panelPadY = 20f;
        float panelX = startButtonBounds.x - panelPadX;
        float panelY = welcomeQuitButtonBounds.y - panelPadY;
        float panelW = startButtonBounds.width + panelPadX * 2;
        // top of panel should sit above the "QUAX" title
        float panelTop = world.boardCenterY + 136f;
        float panelH = panelTop - panelY;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);

        // panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(PANEL_BG);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);

        // start button fill
        if (startButtonBounds != null) {
            boolean h = startButtonBounds.contains(mouse.x, mouse.y);
            shapeRenderer.setColor(h ? BTN_BG_HOVER : BTN_BG);
            shapeRenderer.rect(startButtonBounds.x, startButtonBounds.y,
                startButtonBounds.width, startButtonBounds.height);
        }

        // quit button fill — dark red, brightens on hover
        if (welcomeQuitButtonBounds != null) {
            boolean h = welcomeQuitButtonBounds.contains(mouse.x, mouse.y);
            shapeRenderer.setColor(h ? QUIT_BG_HOVER : QUIT_BG);
            shapeRenderer.rect(welcomeQuitButtonBounds.x, welcomeQuitButtonBounds.y,
                welcomeQuitButtonBounds.width, welcomeQuitButtonBounds.height);
        }

        shapeRenderer.end();

        // gold borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(GOLD_BORDER);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        if (startButtonBounds != null) {
            shapeRenderer.rect(startButtonBounds.x, startButtonBounds.y,
                startButtonBounds.width, startButtonBounds.height);
        }
        if (welcomeQuitButtonBounds != null) {
            shapeRenderer.rect(welcomeQuitButtonBounds.x, welcomeQuitButtonBounds.y,
                welcomeQuitButtonBounds.width, welcomeQuitButtonBounds.height);
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // text
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // "QUAX" title — white, centred in the panel
        String welcomeTitle = "QUAX";
        GlyphLayout wlTitle = new GlyphLayout(welcomeFont, welcomeTitle);
        float titleX = world.boardCenterX + (offsetZ - wlTitle.width) / 2f;
        welcomeFont.setColor(Color.WHITE);
        welcomeFont.draw(batch, welcomeTitle, titleX, world.boardCenterY + 120f);

        // subtitle — white
        String subTitle = "Human vs Human";
        GlyphLayout wlSub = new GlyphLayout(welcomeFont, subTitle);
        float subX = world.boardCenterX + (offsetZ - wlSub.width) / 2f;
        welcomeFont.draw(batch, subTitle, subX, world.boardCenterY + 78f);

        // start button label — centred
        if (startButtonBounds != null) {
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, "Start Game");
            font.draw(batch, "Start Game",
                startButtonBounds.x + (startButtonBounds.width  - gl.width)  / 2f,
                startButtonBounds.y + (startButtonBounds.height + gl.height) / 2f);
        }

        // quit button label — centred
        if (welcomeQuitButtonBounds != null) {
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, "Quit");
            font.draw(batch, "Quit",
                welcomeQuitButtonBounds.x + (welcomeQuitButtonBounds.width  - gl.width)  / 2f,
                welcomeQuitButtonBounds.y + (welcomeQuitButtonBounds.height + gl.height) / 2f);
        }

        batch.end();
    }

    /**
     * computes welcome button rectangles — Start on top, Quit directly below it
     */
    private void computeWelcomeButtonBounds() {
        float btnW = 220, btnH = 60, gap = 14;
        float btnX = world.boardCenterX + (offsetZ - btnW) / 2f;
        startButtonBounds       = new Rectangle(btnX, world.boardCenterY - 30f, btnW, btnH);
        welcomeQuitButtonBounds = new Rectangle(btnX, world.boardCenterY - 30f - btnH - gap, btnW, btnH);
    }

    /**
     * routes a touch event to the correct handler
     */
    private void handleInput() {
        Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchPos);

        // UIController always gets first dibs — it owns the quit dialog
        if (uiController.handleInput(touchPos)) return;

        if (showWelcome) {
            if (startButtonBounds != null && startButtonBounds.contains(touchPos.x, touchPos.y)) {
                showWelcome = false;
                uiController.setInGame(true);
            }
            if (welcomeQuitButtonBounds != null && welcomeQuitButtonBounds.contains(touchPos.x, touchPos.y)) {
                uiController.triggerQuitConfirm();
            }
            return;
        }

        InputHandler.MoveResult result = inputHandler.handleBoardClick(Gdx.input.getX(), Gdx.input.getY());
        switch (result) {
            case OCCUPIED:
                showStatusMessage("Invalid move. Select an empty cell.", 1.6f);
                break;
            case NOT_A_CELL:
                break;
            case SUCCESS:
                break;
        }
    }

    /**
     * @param message the text to display
     * @param seconds how long before it disappears
     */
    private void showStatusMessage(String message, float seconds) {
        statusMessage      = message;
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
        if (welcomeFont != null) welcomeFont.dispose();
    }
}
