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

// application entry point - owns the render loop and wires all subsystems together
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
    private InputHandler      inputHandler;
    private QuitWidget        quitWidget;
    private PieRuleWidget     pieRuleWidget;
    private BotStrategyWidget botStrategyWidget;

    // bot
    private BotPlayer botPlayer;

    // stores the original (empty) TextureRegion for every diamond TMO so restartGame()
    // can restore them without a GID lookup that can fail across libgdx versions
    private final Map<com.badlogic.gdx.maps.objects.TextureMapObject,
        com.badlogic.gdx.graphics.g2d.TextureRegion> initialDiamondTextures
        = new HashMap<>();

    // app-level state
    private boolean showWelcome   = true;
    private String  statusMessage = "";
    private float   statusTimer   = 0f;

    // bot think timer: -1 = not pending; >= 0 = counting down to bot move
    private float botThinkTimer = -1f;
    private static final float BOT_THINK_DELAY = 1.0f;

    private static final float UNIT_SCALE = 0.25f;
    private static final float MARGIN     = 150f;
    private static final float OFFSET_Z   = 300f;

    private static final Color HOVER_FILL    = new Color(1f, 0.95f, 0.4f, 0.20f);
    private static final Color HOVER_OUTLINE = new Color(1f, 0.88f, 0.2f, 0.65f);

    // -------------------------------------------------------------------------
    // lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void create() {
        initRendering();
        if (!loadMap()) return;
        initWorldAndCamera();
        initSubsystems();
        loadFonts();
        snapshotDiamondTextures();
    }

    private void initRendering() {
        camera        = new OrthographicCamera();
        viewport      = new FitViewport(2000, 2000, camera);
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
    }

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

    private void initSubsystems() {
        gameState  = new GameState();
        boardLogic = new QuaxBoard();
        botPlayer  = new BotPlayer(boardLogic, gameState);

        TiledMapTileLayer octagonLayer = (TiledMapTileLayer) map.getLayers().get("Octagons");
        MapLayer          diamondLayer = map.getLayers().get("Diamonds");

        inputHandler      = new InputHandler(map, octagonLayer, diamondLayer, UNIT_SCALE, gameState, viewport, boardLogic);
        boardRenderer     = new BoardRenderer(world, gameState, viewport, camera);
        diamondRenderer   = new DiamondLayerRenderer(map, UNIT_SCALE);
        // WelcomeScreen accepts gameState for source-level compatibility (not used internally)
        welcomeScreen     = new WelcomeScreen(world, viewport, camera, gameState);
        quitWidget        = new QuitWidget(world, viewport, camera);
        pieRuleWidget     = new PieRuleWidget(gameState, world, viewport, camera);
        botStrategyWidget = new BotStrategyWidget(botPlayer, gameState, map, octagonLayer,
            diamondLayer, UNIT_SCALE, viewport, camera);
    }

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
        botStrategyWidget.updateBounds(showWelcome);

        if (Gdx.input.justTouched()) handleInput();

        if (showWelcome) {
            // pass mouse position so WelcomeScreen can compute hover states
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(mouse);
            welcomeScreen.draw(batch, font, welcomeFont, shapeRenderer, mouse);
        } else {
            renderGame();
        }

        // widgets always draw last so they appear over the board
        quitWidget.draw(shapeRenderer, batch, font);
        pieRuleWidget.draw(shapeRenderer, batch, font);
        botStrategyWidget.draw(shapeRenderer, batch, font);
    }

    private void clearScreen() {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    /**
     * ticks all timers: status message, pie rule banner, and the bot think timer
     * when the bot think timer reaches zero the bot either activates the pie rule
     * (if available and it is the bot's turn) or places a strategic move
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
                // bot has a 50/50 chance of activating the pie rule
                if (gameState.isPieRuleAvailable() && gameState.isBotTurn()) {
                    // 50/50 chance: bot may activate the pie rule or decline and play normally
                    if (new java.util.Random().nextBoolean()) {
                        System.out.println("Bot chose to activate the pie rule.");
                        gameState.activatePieRule();
                        botStrategyWidget.invalidate();
                        pieRuleWidget.showBanner(); // show the same banner the human would see
                        // turn passes to human after activation - no further scheduling needed
                    } else {
                        System.out.println("Bot declined the pie rule - playing a normal move.");
                        executeBotMove();
                    }
                } else {
                    executeBotMove();
                }
            }
        }
    }

    private void renderGame() {
        renderTiles();
        renderDiamonds();
        botStrategyWidget.drawOverlayShapes(shapeRenderer); // heat map colours
        renderHoverOverlay();
        renderBoardText();
    }

    private void renderTiles() {
        tileRenderer.setView(camera);
        tileRenderer.render();
    }

    private void renderDiamonds() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        diamondRenderer.render(batch);
        batch.end();
    }

    private void renderHoverOverlay() {
        inputHandler.updateHover(Gdx.input.getX(), Gdx.input.getY());

        if (inputHandler.getHoverShape() == HoverDetector.HoverShape.NONE) return;
        float[] verts = inputHandler.getHoverVertices();
        if (verts.length < 6) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);

        int n = verts.length / 2;
        float cx = 0, cy = 0;
        for (int i = 0; i < verts.length; i += 2) { cx += verts[i]; cy += verts[i+1]; }
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

    /**
     * draws all board text in a single batch pass:
     *   - heat map score labels (if strategy overlay is on)
     *   - strategy explanation text in the right panel (if strategy overlay is on)
     *   - board labels, turn indicator, side panel (always)
     *   - win overlay (when game is over)
     */
    private void renderBoardText() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        botStrategyWidget.drawOverlayLabels(batch, font);    // score numbers on each cell
        botStrategyWidget.drawExplanation(batch, font);      // text legend in right panel
        boardRenderer.render(batch, font, shapeRenderer, statusMessage);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // bot logic
    // -------------------------------------------------------------------------

    /**
     * starts the bot think timer if it is the bot's turn and no timer is already running
     * also starts the timer if the pie rule window is open and the bot should activate it
     */
    private void scheduleBotMoveIfNeeded() {
        // only schedule when it is genuinely the bot's turn - removing the
        // isPieRuleAvailable() secondary trigger which was causing the bot timer to
        // fire on the human's turn and always activate the pie rule unexpectedly
        if (!showWelcome && !gameState.isGameOver()
            && gameState.isBotTurn()
            && botThinkTimer < 0f) {
            botThinkTimer = BOT_THINK_DELAY;
        }
    }

    /**
     * asks BotPlayer for the best move and applies it via InputHandler.placeBotMove()
     * if the bot accidentally selects an invalid cell, retries after a short delay
     */
    private void executeBotMove() {
        if (gameState.isGameOver()) return;
        if (!gameState.isBotTurn())  return;

        String label = botPlayer.selectMove();
        if (label == null) {
            System.err.println("Bot: no valid move found - skipping turn.");
            return;
        }

        InputHandler.MoveResult result = inputHandler.placeBotMove(label);
        botStrategyWidget.invalidate();

        if (result == InputHandler.MoveResult.OCCUPIED
            || result == InputHandler.MoveResult.NOT_A_CELL) {
            System.err.println("Bot selected invalid cell: " + label + " - retrying");
            botThinkTimer = 0.5f;
        }
        // WIN result: win overlay displays automatically via BoardRenderer
    }

    // -------------------------------------------------------------------------
    // input handling
    // -------------------------------------------------------------------------

    private void handleInput() {
        Vector3 tp = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(tp);

        // win overlay buttons (Play Again / Quit) - checked first
        if (!showWelcome) {
            BoardRenderer.InputResult overlayResult = boardRenderer.handleInput(tp);
            if (overlayResult == BoardRenderer.InputResult.RESTART) { restartGame(); return; }
            if (overlayResult == BoardRenderer.InputResult.QUIT)    { Gdx.app.exit(); return; }
        }

        // quit confirmation dialog eats all input while open
        if (quitWidget.handleInput(tp)) return;

        // bot strategy toggle
        if (botStrategyWidget.handleInput(tp)) return;

        // pie rule button
        if (pieRuleWidget.handleInput(tp)) {
            botStrategyWidget.invalidate();
            scheduleBotMoveIfNeeded();
            return;
        }

        if (showWelcome) {
            handleWelcomeInput(tp);
        } else {
            handleBoardInput();
        }
    }

    /**
     * handles a click on the welcome screen
     * mode selection removed - only START and QUIT_CONFIRM remain
     */
    private void handleWelcomeInput(Vector3 tp) {
        WelcomeScreen.WelcomeAction action = welcomeScreen.handleInput(tp);
        if (action == WelcomeScreen.WelcomeAction.START) {
            showWelcome = false;
            quitWidget.setVisible(true);
            botStrategyWidget.invalidate();
            scheduleBotMoveIfNeeded(); // start bot timer immediately if bot is BLACK
        } else if (action == WelcomeScreen.WelcomeAction.QUIT_CONFIRM) {
            quitWidget.triggerConfirm();
        }
    }

    /** handles a board click during gameplay - ignored if it is the bot's turn */
    private void handleBoardInput() {
        if (gameState.isBotTurn() || botThinkTimer >= 0f) return;

        InputHandler.MoveResult result = inputHandler.handleBoardClick(
            Gdx.input.getX(), Gdx.input.getY());

        switch (result) {
            case OCCUPIED:
                statusMessage = "Invalid move. Select an empty cell.";
                statusTimer   = 1.6f;
                break;
            case SUCCESS:
                botStrategyWidget.invalidate();
                scheduleBotMoveIfNeeded();
                break;
            case WIN:
                botStrategyWidget.invalidate();
                break;
            default:
                break;
        }
    }

    // -------------------------------------------------------------------------
    // game restart
    // -------------------------------------------------------------------------

    /**
     * snapshots every diamond TMO's original (empty) texture during create()
     * used by resetDiamondTiles() to restore visuals without a GID lookup
     */
    private void snapshotDiamondTextures() {
        com.badlogic.gdx.maps.MapLayer dl = map.getLayers().get("Diamonds");
        if (dl == null) return;
        for (com.badlogic.gdx.maps.MapObject obj : dl.getObjects()) {
            if (!(obj instanceof com.badlogic.gdx.maps.objects.TextureMapObject)) continue;
            com.badlogic.gdx.maps.objects.TextureMapObject tmo =
                (com.badlogic.gdx.maps.objects.TextureMapObject) obj;
            initialDiamondTextures.put(tmo, tmo.getTextureRegion());
        }
        System.out.println("Captured " + initialDiamondTextures.size() + " diamond textures.");
    }

    /**
     * resets all game state and tile graphics so a fresh game can begin without
     * reloading the TMX map or recreating the window
     *
     * FIX - gameMode is explicitly set to HUMAN_VS_BOT so BotStrategyWidget
     * button remains visible after restart (GameState.gameMode already defaults
     * to HUMAN_VS_BOT, but being explicit avoids any future regression)
     */
    private void restartGame() {
        gameState  = new GameState();
        gameState.setGameMode(GameMode.HUMAN_VS_BOT); // ensure button visibility after restart
        boardLogic = new QuaxBoard();
        botPlayer  = new BotPlayer(boardLogic, gameState);

        TiledMapTileLayer octagonLayer = (TiledMapTileLayer) map.getLayers().get("Octagons");
        MapLayer          diamondLayer = map.getLayers().get("Diamonds");

        // recreate all subsystems that hold the old gameState / boardLogic reference
        inputHandler      = new InputHandler(map, octagonLayer, diamondLayer, UNIT_SCALE, gameState, viewport, boardLogic);
        boardRenderer     = new BoardRenderer(world, gameState, viewport, camera);
        pieRuleWidget     = new PieRuleWidget(gameState, world, viewport, camera);
        botStrategyWidget = new BotStrategyWidget(botPlayer, gameState, map, octagonLayer,
            diamondLayer, UNIT_SCALE, viewport, camera);

        resetOctagonTiles(octagonLayer);
        resetDiamondTiles(diamondLayer);

        botThinkTimer = -1f;
        statusMessage = "";
        statusTimer   = 0f;

        scheduleBotMoveIfNeeded();
        System.out.println("Game restarted - bot is playing as " + gameState.getBotColour());
    }

    /**
     * resets every octagon tile back to the empty octagon graphic (GID 5)
     * tiles with GID 6 (white) or 7 (black) are the only ones that need resetting
     */
    private void resetOctagonTiles(TiledMapTileLayer octagonLayer) {
        com.badlogic.gdx.maps.tiled.TiledMapTile emptyTile = map.getTileSets().getTile(5);
        if (emptyTile == null) return;

        for (int x = 0; x < octagonLayer.getWidth(); x++) {
            for (int y = 0; y < octagonLayer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = octagonLayer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    int gid = cell.getTile().getId();
                    if (gid == 6 || gid == 7) cell.setTile(emptyTile);
                }
            }
        }
    }

    /**
     * restores every diamond TMO to its snapshotted empty texture and removes
     * the "occupied" property so clicks register on them again
     */
    private void resetDiamondTiles(com.badlogic.gdx.maps.MapLayer diamondLayer) {
        for (com.badlogic.gdx.maps.MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof com.badlogic.gdx.maps.objects.TextureMapObject)) continue;
            com.badlogic.gdx.maps.objects.TextureMapObject tmo =
                (com.badlogic.gdx.maps.objects.TextureMapObject) obj;
            com.badlogic.gdx.graphics.g2d.TextureRegion original = initialDiamondTextures.get(tmo);
            if (original != null) tmo.setTextureRegion(original);
            tmo.getProperties().remove("occupied");
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
