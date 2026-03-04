package src.softies;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.PlayerColour;
import src.softies.QuaxBoard;

public class Main extends ApplicationAdapter {
    // game assets and logic objects
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private TiledMapTileLayer octagonLayer;
    private MapLayer diamondLayer;
    private SpriteBatch batch;
    private BitmapFont font;

    private float unitScale = 0.25f;               // conversion factor from pixels to world units
    private PlayerColour currentPlayer = PlayerColour.BLACK;   // whose turn it is
    private QuaxBoard boardLogic;                   // underlying game logic (not fully integrated)

    // tile IDs from the Tiled tileset
    private final int GID_BLACK_RHO = 5;
    private final int GID_WHITE_RHO = 4;
    private final int GID_BLACK_OCT = 8;
    private final int GID_WHITE_OCT = 7;

    // world dimensions (computed from map)
    private float mapWidthWorld;
    private float mapHeightWorld;
    private float tileWidthWorld;
    private float tileHeightWorld;
    private float margin = 150f;                    // empty space around the board

    // board bounds (playable area: columns 5‑15, rows 4‑14)
    private float boardMinX, boardMinY, boardMaxX, boardMaxY;
    private float boardCenterX, boardCenterY;
    private float boardWidth, boardHeight;

    // camera offsets for fine‑tuning the view
    private float offsetX = 20f;    // positive moves right, negative left
    private float offsetY = 2000f;  // positive moves view down (world up)

    // quit button and confirmation dialog state
    private boolean showQuitConfirm = false;
    private Rectangle quitButtonBounds;
    private Rectangle yesButtonBounds;
    private Rectangle noButtonBounds;
    private ShapeRenderer shapeRenderer;   // used for drawing overlays and buttons

    /**
     * Called once when the application starts.
     * Loads the map, computes dimensions, sets up the camera, and creates rendering objects.
     */
    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);   // initial size, will be updated
        boardLogic = new QuaxBoard();

        // 1. load the Tiled map
        try {
            map = new TmxMapLoader().load("PolygonalGrid.tmx");
        } catch (Exception e) {
            Gdx.app.error("Quax", "FILE ERROR: " + e.getMessage());
            return;
        }

        // 2. get the layers by name (must match Tiled file)
        octagonLayer = (TiledMapTileLayer) map.getLayers().get("Octagons");
        diamondLayer = map.getLayers().get("Diamonds");

        // safety check: print available layers if required ones are missing
        if (octagonLayer == null || diamondLayer == null) {
            System.out.println("Layer names in map:");
            for (int i = 0; i < map.getLayers().getCount(); i++) {
                System.out.println(" - " + map.getLayers().get(i).getName());
            }
            Gdx.app.error("Quax", "Required layers not found! Check layer names.");
            return;
        }

        // 3. read map properties (tile counts and pixel sizes)
        int mapWidthTiles  = map.getProperties().get("width", Integer.class);
        int mapHeightTiles = map.getProperties().get("height", Integer.class);
        int tileWidthPx    = map.getProperties().get("tilewidth", Integer.class);
        int tileHeightPx   = map.getProperties().get("tileheight", Integer.class);

        tileWidthWorld  = tileWidthPx * unitScale;
        tileHeightWorld = tileHeightPx * unitScale;
        mapWidthWorld   = mapWidthTiles * tileWidthWorld;
        mapHeightWorld  = mapHeightTiles * tileHeightWorld;

        // 4. get layer offsets (if any) and convert to world units
        float layerOffsetX = octagonLayer.getOffsetX() * unitScale;
        float layerOffsetY = octagonLayer.getOffsetY() * unitScale;

        // 5. compute the actual board area (where cells A1‑K11 are)
        //    board occupies columns 5‑15 and rows 4‑14 (0‑based indices)
        boardMinX = 5 * tileWidthWorld + layerOffsetX;
        boardMinY = 4 * tileHeightWorld + layerOffsetY;
        boardMaxX = (5 + 11) * tileWidthWorld + layerOffsetX;   // after 11 tiles
        boardMaxY = (4 + 11) * tileHeightWorld + layerOffsetY;
        boardWidth  = boardMaxX - boardMinX;
        boardHeight = boardMaxY - boardMinY;
        boardCenterX = boardMinX + boardWidth / 2;
        boardCenterY = boardMinY + boardHeight / 2;

        // debug output
        System.out.println("Octagon layer offset (world): " + layerOffsetX + ", " + layerOffsetY);
        System.out.println("Board world bounds: (" + boardMinX + ", " + boardMinY + ") to (" + boardMaxX + ", " + boardMaxY + ")");
        System.out.println("Board center: (" + boardCenterX + ", " + boardCenterY + ")");

        // 6. set up camera with margins around the board
        setupCamera();

        // 7. create the map renderer
        renderer = new OrthogonalTiledMapRenderer(map, unitScale);

        // 8. create sprite batch and shape renderer
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // 9. generate a bitmap font from a .ttf file
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

    /**
     * Configures the camera to show the board plus a margin around it.
     * Applies any offsets and updates the viewport.
     */
    private void setupCamera() {
        // world size = board size + margins on both sides
        float worldWidth  = boardWidth + 2 * margin;
        float worldHeight = boardHeight + 2 * margin;
        viewport.setWorldSize(worldWidth, worldHeight);
        // camera looks at the board center, shifted by offsets
        camera.position.set(boardCenterX + offsetX, boardCenterY + offsetY, 0);
        System.out.println("Camera position set to: (" + camera.position.x + ", " + camera.position.y + ")");
        camera.update();
    }

    /**
     * Main rendering loop – called every frame.
     * Clears the screen, renders the map, draws UI elements (turn text, row/column labels,
     * title, quit button, confirmation dialog), and handles input.
     */
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();                // update the viewport to the current window size
        renderer.setView(camera);        // set the camera for the map renderer
        renderer.render();                // draw the map layers

        computeButtonBounds();            // update button positions based on current camera

        if (Gdx.input.justTouched()) {    // handle a new click
            handleInput();
        }

        // --- Draw board and labels with batch ---
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // draw current player's turn text at the top center
        String turnText = "Current Turn: " + currentPlayer;
        GlyphLayout turnLayout = new GlyphLayout(font, turnText);
        float turnX = (viewport.getWorldWidth() - turnLayout.width) / 2f;
        float turnY = 45;
        font.draw(batch, turnText, turnX, turnY);

        // row numbers on the left (1 to 11, bottom to top)
        for (int row = 1; row <= 11; row++) {
            float y = 70f + boardMinY + (row - 1) * tileHeightWorld + tileHeightWorld / 2;
            String number = String.valueOf(row);
            float x = boardMinX - 30f;
            font.draw(batch, number, x, y);
        }

        // column letters above the board (A to K)
        for (int col = 0; col < 11; col++) {
            float x = -8f + boardMinX + col * tileWidthWorld + tileWidthWorld / 2;
            char letter = (char) ('A' + col);
            float y = boardMaxY + 90f;
            font.draw(batch, String.valueOf(letter), x, y);
        }

        // game title centered above the board
        String title = "Quax: Human vs Human";   // could be dynamic based on mode
        GlyphLayout titleLayout = new GlyphLayout(font, title);
        float titleX = boardCenterX - titleLayout.width / 2;
        float titleY = boardMaxY + 130f;
        font.draw(batch, title, titleX, titleY);

        // --- End batch to draw shapes ---
        batch.end();

        // --- Draw button backgrounds and dialog with ShapeRenderer ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // draw quit button background (filled rectangle)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (quitButtonBounds != null) {
            // check if mouse is hovering over the button
            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(mousePos);
            boolean hover = quitButtonBounds.contains(mousePos.x, mousePos.y);
            shapeRenderer.setColor(hover ? 0.3f : 0.2f, 0.2f, 0.2f, 1);
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }
        shapeRenderer.end();

        // draw quit button border (outline)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (quitButtonBounds != null) {
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }
        shapeRenderer.end();

        // if confirmation dialog is active, draw overlay and buttons
        if (showQuitConfirm) {
            // semi‑transparent overlay over the whole screen
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 0.7f);
            float worldLeft = camera.position.x - viewport.getWorldWidth() / 2;
            float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
            shapeRenderer.rect(worldLeft, worldBottom,
                viewport.getWorldWidth(), viewport.getWorldHeight());

            // dialog background (centered)
            float dialogWidth = 300;
            float dialogHeight = 150;
            float dialogX = boardCenterX - dialogWidth / 2;
            float dialogY = boardCenterY - dialogHeight / 2;
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
            shapeRenderer.rect(dialogX, dialogY, dialogWidth, dialogHeight);

            // Yes and No buttons inside the dialog
            float btnW = 80;
            float btnH = 40;
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX = dialogX + 170;
            float noY = dialogY + 40;
            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
            shapeRenderer.rect(yesX, yesY, btnW, btnH);
            shapeRenderer.rect(noX, noY, btnW, btnH);
            shapeRenderer.end();

            // button borders
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1, 1, 1, 1);
            shapeRenderer.rect(yesX, yesY, btnW, btnH);
            shapeRenderer.rect(noX, noY, btnW, btnH);
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- Restart batch to draw text on top of buttons ---
        batch.begin();

        // draw "Quit" on the quit button
        if (quitButtonBounds != null) {
            font.draw(batch, "Quit",
                quitButtonBounds.x + 20, quitButtonBounds.y + 28);
        }

        // draw dialog text if active
        if (showQuitConfirm) {
            float dialogWidth = 300;
            float dialogHeight = 150;
            float dialogX = boardCenterX - dialogWidth / 2;
            float dialogY = boardCenterY - dialogHeight / 2;
            float btnW = 80;
            float btnH = 40;
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX = dialogX + 170;
            float noY = dialogY + 40;

            font.draw(batch, "Quit game?", dialogX + 90, dialogY + 120);
            font.draw(batch, "Yes", yesX + 25, yesY + 28);
            font.draw(batch, "No",  noX  + 28, noY  + 28);
        }

        batch.end();
    }

    /**
     * Processes mouse/touch input.
     * Checks clicks on the quit button or confirmation dialog, otherwise forwards to board interaction.
     */
    private void handleInput() {
        if (!Gdx.input.justTouched()) return;

        Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchPos);          // convert screen to world coordinates

        // if confirmation dialog is open, only check Yes/No buttons
        if (showQuitConfirm) {
            if (yesButtonBounds != null && yesButtonBounds.contains(touchPos.x, touchPos.y)) {
                Gdx.app.exit();                // quit the application
            } else if (noButtonBounds != null && noButtonBounds.contains(touchPos.x, touchPos.y)) {
                showQuitConfirm = false;       // close dialog
            }
            return;
        }

        // check if quit button was clicked
        if (quitButtonBounds != null && quitButtonBounds.contains(touchPos.x, touchPos.y)) {
            showQuitConfirm = true;
            return;
        }

        // --- board interaction: diamond (rhombus) and octagon cells ---
        int mapHeightInTiles = map.getProperties().get("height", Integer.class);
        int tileHeightPx = map.getProperties().get("tileheight", Integer.class);
        float worldMapHeight = mapHeightInTiles * tileHeightPx * unitScale;

        // check each object in the diamond layer (TextureMapObjects)
        for (MapObject object : diamondLayer.getObjects()) {
            if (object instanceof TextureMapObject) {
                TextureMapObject tmo = (TextureMapObject) object;
                // skip if already occupied (custom property)
                if (tmo.getProperties().containsKey("occupied")) continue;

                float objW = tmo.getProperties().get("width", Float.class) * unitScale;
                float objH = tmo.getProperties().get("height", Float.class) * unitScale;
                float worldX = tmo.getX() * unitScale;
                // adjust Y because Tiled uses top‑left origin, LibGDX uses bottom‑left
                float worldY = worldMapHeight - (tmo.getY() * unitScale) - objH;

                if (touchPos.x >= worldX && touchPos.x <= worldX + objW &&
                    touchPos.y >= worldY && touchPos.y <= worldY + objH) {

                    // set the correct stone tile based on current player
                    int gid = (currentPlayer == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
                    tmo.setTextureRegion(map.getTileSets().getTile(gid).getTextureRegion());
                    tmo.getProperties().put("occupied", true);   // mark as occupied
                    togglePlayer();
                    return;
                }
            }
        }

        // check octagon layer (tile grid)
        int cellX = (int) (touchPos.x / (octagonLayer.getTileWidth() * unitScale));
        int cellY = (int) (touchPos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell != null && cell.getTile() != null) {
            int currentGid = cell.getTile().getId();
            // if the cell is already occupied by any stone, do nothing
            if (currentGid == GID_BLACK_OCT || currentGid == GID_WHITE_OCT ||
                currentGid == GID_BLACK_RHO || currentGid == GID_WHITE_RHO) {
                return;
            }

            int targetGid = (currentPlayer == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
            cell.setTile(map.getTileSets().getTile(targetGid));
            togglePlayer();
        }
    }

    /**
     * Recalculates the screen‑relative positions of buttons based on current camera.
     * Called every frame before input handling.
     */
    private void computeButtonBounds() {
        float worldLeft   = camera.position.x - viewport.getWorldWidth() / 2;
        float worldRight  = camera.position.x + viewport.getWorldWidth() / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
        float worldTop    = camera.position.y + viewport.getWorldHeight() / 2;

        // quit button anchored at bottom‑right corner of the visible world
        float buttonWidth = 80;
        float buttonHeight = 40;
        float quitX = worldRight - buttonWidth - 20;
        float quitY = worldBottom + 20;
        quitButtonBounds = new Rectangle(quitX, quitY, buttonWidth, buttonHeight);

        // if confirmation dialog is active, compute Yes/No button rectangles
        if (showQuitConfirm) {
            float dialogWidth = 300;
            float dialogHeight = 150;
            float dialogX = boardCenterX - dialogWidth / 2;
            float dialogY = boardCenterY - dialogHeight / 2;
            float btnW = 80;
            float btnH = 40;
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX = dialogX + 170;
            float noY = dialogY + 40;
            yesButtonBounds = new Rectangle(yesX, yesY, btnW, btnH);
            noButtonBounds  = new Rectangle(noX,  noY,  btnW, btnH);
        } else {
            yesButtonBounds = null;
            noButtonBounds  = null;
        }
    }

    /**
     * Switches the current player between BLACK and WHITE.
     */
    private void togglePlayer() {
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        System.out.println("It is now " + currentPlayer + "'s turn.");
    }

    /**
     * Called when the window is resized. Updates the viewport and the batch projection matrix.
     */
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        batch.setProjectionMatrix(camera.combined);
    }

    /**
     * Cleans up resources when the application exits.
     */
    @Override
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
