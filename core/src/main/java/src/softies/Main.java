package src.softies;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import java.util.HashMap;
import java.util.Map;

// application entry point — owns the render loop and wires all subsystems together
//
// subsystem map:
//   FontLoader           — generates both BitmapFonts from the TTF file
//   WelcomeScreen        — welcome / start screen
//   QuitWidget           — quit button + confirmation dialog
//   PieRuleWidget        — pie rule button + activation banner
//   InputHandler         — coordinates HoverDetector (hover polygon) and MoveHandler (clicks + win)
//   BotPlayer            — Dijkstra-based bot AI; called when the bot think timer expires
//   BoardRenderer        — all in-game text (delegates to SidePanelRenderer and WinOverlay)
//   DiamondLayerRenderer — renders the rhombus object layer each frame
//
// bot flow:
//   after any move passes the turn to the bot, scheduleBotMoveIfNeeded() starts a 1-second timer
//   when the timer expires, executeBotMove() asks BotPlayer for the best cell and plays it
public class Main extends ApplicationAdapter {

    // libgdx core
    private TiledMap map;
    private OrthogonalTiledMapRenderer tileRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont welcomeFont;

    // game model
    private GameState gameState;
    private QuaxBoard boardLogic;
    private WorldCalculator world;

    // rendering subsystems
    private BoardRenderer        boardRenderer;
    private DiamondLayerRenderer diamondRenderer;
    private WelcomeScreen        welcomeScreen;

    // input / UI subsystems
    private InputHandler  inputHandler;
    private QuitWidget    quitWidget;
    private PieRuleWidget pieRuleWidget;

    // bot
    private BotPlayer botPlayer;



    // stores the original (empty) TextureRegion for every diamond TMO so restartGame()
    // can reliably restore them without looking up GIDs from the tileset
    private final Map<com.badlogic.gdx.maps.objects.TextureMapObject,
        com.badlogic.gdx.graphics.g2d.TextureRegion> initialDiamondTextures
        = new HashMap<>();


    // app-level state
    private boolean showWelcome   = true;
    private String  statusMessage = "";
    private float   statusTimer   = 0f;

    // bot think timer: negative = not pending, >= 0 = counting down to bot move
    private float botThinkTimer = -1f;
    private static final float BOT_THINK_DELAY = 1.0f; // 1-second "thinking" pause

    private static final float UNIT_SCALE = 0.25f;
    private static final float MARGIN     = 150f;
    private static final float OFFSET_Z   = 300f; // width of the right-side UI panel

    // hover highlight — warm yellow fill with a gold outline
    private static final Color HOVER_FILL    = new Color(1f, 0.95f, 0.4f, 0.20f);
    private static final Color HOVER_OUTLINE = new Color(1f, 0.88f, 0.2f, 0.65f);

    // -------------------------------------------------------------------------
    // lifecycle
    // -------------------------------------------------------------------------


    /**
     * called once by libgdx when the application starts
     * loads the map, sets up all subsystems and generates fonts
     */

    @Override
    public void create() {
        initRendering();
        if (!loadMap()) return;
        initWorldAndCamera();
        initSubsystems();
        loadFonts();
        snapshotDiamondTextures(); // capture original empty textures for reliable restart
    }

    /** creates the camera, viewport and the three libgdx drawing primitives */
    private void initRendering() {
        camera        = new OrthographicCamera();
        viewport      = new FitViewport(2000, 2000, camera);
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
    }

    /**
     * loads PolygonalGrid.tmx and verifies the required layers exist
     * @return false if loading fails so create() can bail out cleanly
     */
    private boolean loadMap() {
        try {
            map = new TmxMapLoader().load("PolygonalGrid.tmx");
        } catch (Exception e) {
            Gdx.app.error("Quax", "FILE ERROR: " + e.getMessage());
            return false;
        }
        if (map.getLayers().get("Octagons") == null || map.getLayers().get("Diamonds") == null) {
            Gdx.app.error("Quax", "Required map layers not found!");
            return false;
        }
        return true;
    }

    /** computes world bounds from the map and sizes the viewport to fit board + right panel */
    private void initWorldAndCamera() {
        TiledMapTileLayer octagonLayer = (TiledMapTileLayer) map.getLayers().get("Octagons");
        world = new WorldCalculator(UNIT_SCALE);
        world.compute(map, octagonLayer);

        viewport.setWorldSize(
            world.boardWidth  + 2 * MARGIN + OFFSET_Z,
            world.boardHeight + 2 * MARGIN);
        camera.position.set(world.boardCenterX, world.boardCenterY, 0);
        camera.update();

        tileRenderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE);
    }

    /** creates the game model and all subsystem objects */
    private void initSubsystems() {
        gameState  = new GameState();
        boardLogic = new QuaxBoard();
        botPlayer  = new BotPlayer(boardLogic, gameState);

        TiledMapTileLayer octagonLayer = (TiledMapTileLayer) map.getLayers().get("Octagons");
        MapLayer          diamondLayer = map.getLayers().get("Diamonds");

        inputHandler    = new InputHandler(map, octagonLayer, diamondLayer, UNIT_SCALE, gameState, viewport, boardLogic);
        boardRenderer   = new BoardRenderer(world, gameState, viewport, camera);
        diamondRenderer = new DiamondLayerRenderer(map, UNIT_SCALE);
        welcomeScreen   = new WelcomeScreen(world, viewport, camera, gameState);
        quitWidget      = new QuitWidget(world, viewport, camera);
        pieRuleWidget   = new PieRuleWidget(gameState, world, viewport, camera);
    }

    /** generates both fonts via FontLoader */
    private void loadFonts() {
        font        = FontLoader.loadMainFont();
        welcomeFont = FontLoader.loadWelcomeFont();
    }

    // -------------------------------------------------------------------------
    // render loop
    // -------------------------------------------------------------------------

    @Override
    public void render() {
        clearScreen();
        tickTimers(Gdx.graphics.getDeltaTime());

        viewport.apply();
        quitWidget.updateBounds();
        pieRuleWidget.updateBounds();

        if (Gdx.input.justTouched()) handleInput();

        if (showWelcome) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(mouse);
            welcomeScreen.draw(batch, font, welcomeFont, shapeRenderer, mouse);
        } else {
            renderGame();
        }

        // widgets always draw last so they appear over the board
        quitWidget.draw(shapeRenderer, batch, font);
        pieRuleWidget.draw(shapeRenderer, batch, font);
    }

    /** clears the screen to the neutral grey background each frame */
    private void clearScreen() {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    }

    /**
     * ticks all timers:
     * - status message countdown
     * - pie rule banner animation
     * - bot think timer (fires executeBotMove when it reaches zero)
     */
    private void tickTimers(float dt) {
        pieRuleWidget.update(dt);

        if (statusTimer > 0f) {
            statusTimer -= dt;
            if (statusTimer <= 0f) { statusTimer = 0f; statusMessage = ""; }
        }

        if (botThinkTimer >= 0f) {
            botThinkTimer -= dt;
            if (botThinkTimer <= 0f) {
                botThinkTimer = -1f;
                // if the pie rule is still open and it's the bot's turn, activate it
                // rather than placing a stone — the bot always takes the swap because
                // inheriting the first player's position is always advantageous
                if (gameState.isPieRuleAvailable() && gameState.isBotTurn()) {
                    gameState.activatePieRule();
                    // after the swap currentPlayer = WHITE; if bot is now BLACK that's
                    // the human's colour so no further bot scheduling is needed here
                } else {
                    executeBotMove(); // timer expired — bot places its move
                }
            }
        }
    }

    /** renders the in-game view: tile map, diamonds, hover highlight, board text */
    private void renderGame() {
        renderTiles();
        renderDiamonds();
        renderHoverOverlay();
        renderBoardText();
    }

    /** renders the octagon tile layer via OrthogonalTiledMapRenderer */
    private void renderTiles() {
        tileRenderer.setView(camera);
        tileRenderer.render();
    }

    /** renders the diamond object layer via DiamondLayerRenderer */
    private void renderDiamonds() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        diamondRenderer.render(batch);
        batch.end();
    }

    /** draws a shape-accurate hover highlight using a triangle-fan fill from the polygon centroid */
    private void renderHoverOverlay() {
        inputHandler.updateHover(Gdx.input.getX(), Gdx.input.getY());

        if (inputHandler.getHoverShape() == HoverDetector.HoverShape.NONE) return;
        float[] verts = inputHandler.getHoverVertices();
        if (verts.length < 6) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // compute centroid so the triangle fan fills the exact polygon shape
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

    /** draws all board text — BoardRenderer internally handles the win overlay draw order */
    private void renderBoardText() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        boardRenderer.render(batch, font, shapeRenderer, statusMessage);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // bot logic
    // -------------------------------------------------------------------------

    /**
     * starts the bot think timer if it is currently the bot's turn
     * and no timer is already running — safe to call repeatedly
     */

    private void scheduleBotMoveIfNeeded() {
        if (!showWelcome && !gameState.isGameOver()
            && gameState.isBotTurn() && botThinkTimer < 0f) {
            botThinkTimer = BOT_THINK_DELAY;
        }
    }

    /**
     * asks BotPlayer for the best move and applies it via InputHandler.placeBotMove()
     * called automatically by tickTimers() when the think timer expires
     */
    private void executeBotMove() {
        if (gameState.isGameOver()) return;
        if (!gameState.isBotTurn())  return; // safety check — don't move out of turn

        String label = botPlayer.selectMove();
        if (label == null) {
            System.err.println("Bot could not find any valid move — skipping turn.");
            return;
        }

        InputHandler.MoveResult result = inputHandler.placeBotMove(label);

        // if the bot wins, the win overlay is displayed automatically by BoardRenderer
        // if the move succeeded, it is now the human's turn — don't reschedule the bot
        if (result == InputHandler.MoveResult.OCCUPIED || result == InputHandler.MoveResult.NOT_A_CELL) {
            // bot chose an invalid cell (shouldn't happen with correct BotPlayer logic)
            // reschedule so the bot tries again rather than freezing
            System.err.println("Bot selected invalid cell: " + label + " — retrying");
            botThinkTimer = 0.5f;
        }
    }

    // -------------------------------------------------------------------------
    // input handling
    // -------------------------------------------------------------------------

    /** routes a touch event to the correct subsystem */
    private void handleInput() {
        Vector3 tp = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(tp);

        // win overlay buttons — checked first so they work even when board input is blocked
        if (!showWelcome) {
            BoardRenderer.InputResult overlayResult = boardRenderer.handleInput(tp);
            if (overlayResult == BoardRenderer.InputResult.RESTART) { restartGame(); return; }
            if (overlayResult == BoardRenderer.InputResult.QUIT)    { Gdx.app.exit(); return; }
        }

        // quit dialog eats all clicks while open — always checked first
        if (quitWidget.handleInput(tp)) return;

        // pie rule button — must be checked before board input
        if (pieRuleWidget.handleInput(tp)) {
            // pie rule may have transferred the turn to the bot (e.g. bot was WHITE,
            // human activated pie rule, now bot is BLACK — or vice versa)
            scheduleBotMoveIfNeeded();
            return;
        }

        if (showWelcome) {
            handleWelcomeInput(tp);
        } else {
            handleBoardInput();
        }
    }

    /** handles a click on the welcome screen */
    private void handleWelcomeInput(Vector3 tp) {
        WelcomeScreen.WelcomeAction action = welcomeScreen.handleInput(tp);
        if (action == WelcomeScreen.WelcomeAction.SELECT_HUMAN_VS_HUMAN) {
            gameState.setGameMode(GameMode.HUMAN_VS_HUMAN);
            statusMessage = "Mode selected: Human vs Human";
            statusTimer = 1.2f;
        } else if (action == WelcomeScreen.WelcomeAction.SELECT_HUMAN_VS_BOT) {
            gameState.setGameMode(GameMode.HUMAN_VS_BOT);
            statusMessage = "Mode selected: Human vs Bot";
            statusTimer = 1.2f;
        } else if (action == WelcomeScreen.WelcomeAction.START) {
            showWelcome = false;
            quitWidget.setVisible(true);
            scheduleBotMoveIfNeeded();
        } else if (action == WelcomeScreen.WelcomeAction.QUIT_CONFIRM) {
            quitWidget.triggerConfirm();
        }
    }

    /** handles a click on the board — ignored if it is currently the bot's turn */
    private void handleBoardInput() {
        // don't accept human input while the bot is thinking or it's the bot's turn
        if (gameState.isBotTurn() || botThinkTimer >= 0f) return;

        InputHandler.MoveResult result = inputHandler.handleBoardClick(
            Gdx.input.getX(), Gdx.input.getY());

        switch (result) {
            case OCCUPIED:
                statusMessage = "Invalid move. Select an empty cell.";
                statusTimer   = 1.6f;
                break;
            case SUCCESS:
                // human move succeeded — schedule bot if it is now the bot's turn
                scheduleBotMoveIfNeeded();
                break;
            case WIN:
                // BoardRenderer shows the win overlay automatically
                break;
            default:
                break;
        }
    }

    // -------------------------------------------------------------------------
    // lifecycle (continued)
    // -------------------------------------------------------------------------

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
    }


    // -------------------------------------------------------------------------
    // game restart
    // -------------------------------------------------------------------------

    /**
     * captures the TextureRegion of every diamond TMO while they are still in their
     * initial empty state — used by resetDiamondTiles() to restore them on restart
     * this must be called once during create(), before any moves are made
     */
    private void snapshotDiamondTextures() {
        com.badlogic.gdx.maps.MapLayer dl = map.getLayers().get("Diamonds");
        if (dl == null) return;
        for (com.badlogic.gdx.maps.MapObject obj : dl.getObjects()) {
            if (!(obj instanceof com.badlogic.gdx.maps.objects.TextureMapObject)) continue;
            com.badlogic.gdx.maps.objects.TextureMapObject tmo =
                (com.badlogic.gdx.maps.objects.TextureMapObject) obj;
            // store a reference to the original texture region (immutable, safe to keep)
            initialDiamondTextures.put(tmo, tmo.getTextureRegion());
        }
        System.out.println("Captured " + initialDiamondTextures.size() + " initial diamond textures.");
    }

    /**
     * resets all game state and tile graphics so a fresh game begins
     * without reloading the TMX map or recreating the window
     *
     * what gets reset:
     *   - game model (GameState, QuaxBoard, BotPlayer) — fresh instances
     *   - subsystems that hold references to the old model (InputHandler, BoardRenderer, PieRuleWidget)
     *   - all octagon tile visuals — set back to the empty octagon tile (GID 5)
     *   - all diamond TMO visuals — set back to the empty diamond texture (GID 2) and
     *     have their "occupied" property removed so clicks register again
     *   - app-level timers and status message
     */
    private void restartGame() {
        gameState  = new GameState();
        boardLogic = new QuaxBoard();
        botPlayer  = new BotPlayer(boardLogic, gameState);

        TiledMapTileLayer octagonLayer = (TiledMapTileLayer) map.getLayers().get("Octagons");
        MapLayer          diamondLayer = map.getLayers().get("Diamonds");

        // recreate all subsystems that hold the old gameState / boardLogic reference
        inputHandler  = new InputHandler(map, octagonLayer, diamondLayer, UNIT_SCALE, gameState, viewport, boardLogic);
        boardRenderer = new BoardRenderer(world, gameState, viewport, camera);
        pieRuleWidget = new PieRuleWidget(gameState, world, viewport, camera);

        // reset all tile and object graphics to the empty state
        resetOctagonTiles(octagonLayer);
        resetDiamondTiles(diamondLayer);

        // reset app-level state
        botThinkTimer = -1f;
        statusMessage = "";
        statusTimer   = 0f;

        // if the bot is BLACK it goes first — schedule its opening move
        scheduleBotMoveIfNeeded();
        System.out.println("Game restarted — bot is playing as " + gameState.getBotColour());
    }

    /**
     * resets every octagon tile back to the empty octagon graphic (GID 5)
     * only tiles that already have a tile assigned are touched — blank border tiles are skipped
     */
    private void resetOctagonTiles(TiledMapTileLayer octagonLayer) {
        // GID 5 is the empty octagon tile (must match tileset order in PolygonalGrid.tmx)
        com.badlogic.gdx.maps.tiled.TiledMapTile emptyTile = map.getTileSets().getTile(5);
        if (emptyTile == null) return;

        for (int x = 0; x < octagonLayer.getWidth(); x++) {
            for (int y = 0; y < octagonLayer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = octagonLayer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    int gid = cell.getTile().getId();
                    // only reset tiles that have been changed from empty (GID 6 = white, 7 = black)
                    if (gid == 6 || gid == 7) {
                        cell.setTile(emptyTile);
                    }
                }
            }
        }
    }

    /**
     * resets every diamond TextureMapObject back to its original empty texture
     * uses the snapshot captured during create() rather than a GID lookup —
     * GID-based lookup is unreliable across libgdx versions
     * also clears the "occupied" property so clicks register again
     */
    private void resetDiamondTiles(com.badlogic.gdx.maps.MapLayer diamondLayer) {
        for (com.badlogic.gdx.maps.MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof com.badlogic.gdx.maps.objects.TextureMapObject)) continue;
            com.badlogic.gdx.maps.objects.TextureMapObject tmo =
                (com.badlogic.gdx.maps.objects.TextureMapObject) obj;
            // look up the original texture from our snapshot
            com.badlogic.gdx.graphics.g2d.TextureRegion original = initialDiamondTextures.get(tmo);
            if (original != null) tmo.setTextureRegion(original);
            // clear the occupied flag so this diamond can be clicked again
            tmo.getProperties().remove("occupied");
        }
    }

    @Override
    public void dispose() {
        if (map           != null) map.dispose();
        if (tileRenderer  != null) tileRenderer.dispose();
        if (batch         != null) batch.dispose();
        if (font          != null) font.dispose();
        if (welcomeFont   != null) welcomeFont.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
