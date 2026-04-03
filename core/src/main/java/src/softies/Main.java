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
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import src.softies.board.*;

// application entry point — owns the render loop and wires all subsystems together
// drawing and input logic is delegated to specialised classes:
//   WelcomeScreen — welcome / start screen
//   QuitWidget    — quit button + confirmation dialog
//   PieRuleWidget — pie rule button + activation banner
//   InputHandler  — board clicks, hover polygon, win detection
//   BoardRenderer — all in-game text and the winner overlay
public class Main extends ApplicationAdapter {

    // --- libgdx core ---
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont welcomeFont;

    // --- game model ---
    private GameState gameState;
    private QuaxBoard boardLogic;
    private WorldCalculator world;

    // --- subsystems ---
    private InputHandler  inputHandler;
    private BoardRenderer boardRenderer;
    private WelcomeScreen welcomeScreen;
    private QuitWidget    quitWidget;
    private PieRuleWidget pieRuleWidget;

    // --- state ---
    private boolean showWelcome   = true;
    private String  statusMessage = "";
    private float   statusTimer   = 0f;

    private final float unitScale = 0.25f;
    private final float margin    = 150f;
    private final float offsetZ   = 300f;

    // hover highlight — warm yellow fill with a gold outline
    private static final Color HOVER_FILL    = new Color(1f, 0.95f, 0.4f, 0.20f);
    private static final Color HOVER_OUTLINE = new Color(1f, 0.88f, 0.2f, 0.65f);

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
            Gdx.app.error("Quax", "Required layers not found!");
            return;
        }

        world = new WorldCalculator(unitScale);
        world.compute(map, octagonLayer);

        setupCamera();

        renderer      = new OrthogonalTiledMapRenderer(map, unitScale);
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // wire up subsystems — BoardRenderer now also receives camera for the win overlay
        inputHandler  = new InputHandler(map, octagonLayer, diamondLayer, unitScale, gameState, viewport, boardLogic);
        boardRenderer = new BoardRenderer(world, gameState, viewport, camera);
        welcomeScreen = new WelcomeScreen(world, viewport, camera);
        quitWidget    = new QuitWidget(world, viewport, camera);
        pieRuleWidget = new PieRuleWidget(gameState, world, viewport, camera);

        generateFont();
    }

    private void setupCamera() {
        viewport.setWorldSize(
            world.boardWidth  + 2 * margin + offsetZ,
            world.boardHeight + 2 * margin);
        camera.position.set(world.boardCenterX, world.boardCenterY, 0);
        camera.update();
    }

    /**
     * generates both fonts at a high pixel size then scales them down
     * the bullet character "•" is explicitly included so objectives render correctly
     */
    private void generateFont() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("SF-Pro-Display-Bold.ttf"));

        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size        = 100;
        p.characters  = FreeTypeFontGenerator.DEFAULT_CHARS + "•";
        p.color       = Color.WHITE;
        p.kerning     = true;
        p.spaceX      = -1;
        p.borderColor = Color.BLACK;
        p.borderWidth = 5;
        font = gen.generateFont(p);
        font.getData().setScale(0.2f);
        gen.dispose();

        // welcome font — slightly smaller than before for a less overwhelming title
        FreeTypeFontGenerator wGen = new FreeTypeFontGenerator(Gdx.files.internal("SF-Pro-Display-Bold.ttf"));
        FreeTypeFontParameter wp = new FreeTypeFontParameter();
        wp.size        = 110;
        wp.characters  = FreeTypeFontGenerator.DEFAULT_CHARS + "•";
        wp.color       = Color.WHITE;
        wp.borderColor = Color.BLACK;
        wp.borderWidth = 7;
        welcomeFont = wGen.generateFont(wp);
        welcomeFont.getData().setScale(0.2f);
        wGen.dispose();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float dt = Gdx.graphics.getDeltaTime();

        // tick timers
        if (statusTimer > 0f) {
            statusTimer -= dt;
            if (statusTimer <= 0f) { statusTimer = 0f; statusMessage = ""; }
        }
        pieRuleWidget.update(dt);

        viewport.apply();

        // recalculate widget button bounds every frame
        quitWidget.updateBounds();
        pieRuleWidget.updateBounds();

        if (Gdx.input.justTouched()) handleInput();

        if (showWelcome) {
            // welcome screen owns its own drawing
            welcomeScreen.draw(shapeRenderer, batch, font, welcomeFont);
        } else {
            // render the tiled map layers
            renderer.setView(camera);
            renderer.render();
            renderDiamondLayer();

            // update hover polygon then draw the shape-accurate highlight
            inputHandler.updateHover(Gdx.input.getX(), Gdx.input.getY());
            renderHoverOverlay();

            // board text + winner overlay (BoardRenderer manages batch begin/end internally when game over)
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            boardRenderer.render(batch, font, shapeRenderer, statusMessage);
            batch.end();
        }

        // widgets always draw last so they appear over everything including the board
        quitWidget.draw(shapeRenderer, batch, font);
        pieRuleWidget.draw(shapeRenderer, batch, font);
    }

    // renders the diamond (rhombus) object layer — it doesn't auto-render like tile layers
    private void renderDiamondLayer() {
        MapLayer dl = map.getLayers().get("Diamonds");
        if (dl == null) return;

        int mH   = map.getProperties().get("height",     Integer.class);
        int tHpx = map.getProperties().get("tileheight", Integer.class);
        float mapH = mH * tHpx * unitScale;

        float dX = dl.getOffsetX() * unitScale;
        float dY = dl.getOffsetY() * unitScale;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (MapObject obj : dl.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;
            float w = tmo.getProperties().get("width",  Float.class) * unitScale;
            float h = tmo.getProperties().get("height", Float.class) * unitScale;
            float x = tmo.getX() * unitScale + dX;
            float y = mapH - (tmo.getY() * unitScale) - dY + 2 * h + 4f;
            batch.draw(tmo.getTextureRegion(), x, y, w, h);
        }
        batch.end();
    }

    // draws a shape-accurate hover highlight using a triangle-fan fill and a polygon outline
    // uses polygon vertices from InputHandler so the highlight follows the actual tile shape —
    // not a bounding-box rect that would bleed into transparent PNG corners
    private void renderHoverOverlay() {
        if (inputHandler.getHoverShape() == InputHandler.HoverShape.NONE) return;
        float[] verts = inputHandler.getHoverVertices();
        if (verts.length < 6) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // compute the centroid so we can fan-triangulate from the centre outward
        int n = verts.length / 2;
        float cx = 0, cy = 0;
        for (int i = 0; i < verts.length; i += 2) { cx += verts[i]; cy += verts[i + 1]; }
        cx /= n; cy /= n;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(HOVER_FILL);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            shapeRenderer.triangle(cx, cy, verts[i*2], verts[i*2+1], verts[j*2], verts[j*2+1]);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(HOVER_OUTLINE);
        shapeRenderer.polygon(verts);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void handleInput() {
        Vector3 tp = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(tp);

        // quit dialog eats all clicks while open — always check first
        if (quitWidget.handleInput(tp)) return;
        // pie rule button — must be checked before board clicks
        if (pieRuleWidget.handleInput(tp)) return;

        if (showWelcome) {
            WelcomeScreen.WelcomeAction action = welcomeScreen.handleInput(tp);
            if (action == WelcomeScreen.WelcomeAction.START) {
                showWelcome = false;
                quitWidget.setVisible(true); // show the in-game quit button
            } else if (action == WelcomeScreen.WelcomeAction.QUIT_CONFIRM) {
                quitWidget.triggerConfirm();
            }
            return;
        }

        // pass the click to the board
        InputHandler.MoveResult result = inputHandler.handleBoardClick(Gdx.input.getX(), Gdx.input.getY());
        switch (result) {
            case OCCUPIED:
                statusMessage = "Invalid move. Select an empty cell.";
                statusTimer   = 1.6f;
                break;
            case WIN:
                // the winner is stored in GameState — BoardRenderer will display the overlay
                // no status message needed; the overlay is more prominent
                break;
            case SUCCESS:
            case NOT_A_CELL:
            case INVALID_PLACEMENT:
                break;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void dispose() {
        if (map          != null) map.dispose();
        if (renderer     != null) renderer.dispose();
        if (batch        != null) batch.dispose();
        if (font         != null) font.dispose();
        if (welcomeFont  != null) welcomeFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
