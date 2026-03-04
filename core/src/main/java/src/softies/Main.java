package src.softies;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private TiledMapTileLayer octagonLayer;
    private MapLayer diamondLayer;

    private float unitScale = 0.25f;
    private PlayerColour currentPlayer = PlayerColour.BLACK;
    private QuaxBoard boardLogic;

    // GIDs from your Tiled tileset
    private final int GID_BLACK_RHO = 5;
    private final int GID_WHITE_RHO = 4;
    private final int GID_BLACK_OCT = 8;
    private final int GID_WHITE_OCT = 7;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        boardLogic = new QuaxBoard();

        try {
            map = new TmxMapLoader().load("PolygonalGrid.tmx");
            renderer = new OrthogonalTiledMapRenderer(map, unitScale);
            octagonLayer = (TiledMapTileLayer) map.getLayers().get("Octagons");
            diamondLayer = map.getLayers().get("Diamonds");

            centerCameraOnMap();
        } catch (Exception e) {
            Gdx.app.error("Quax", "FILE ERROR: " + e.getMessage());
        }
    }

    private void centerCameraOnMap() {
        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        float mapWidth = map.getProperties().get("width", Integer.class) * tileWidth * unitScale;
        float mapHeight = map.getProperties().get("height", Integer.class) * tileHeight * unitScale;
        camera.position.set(mapWidth / 2f, mapHeight / 2f, 0);
        camera.update();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.justTouched()) {
            handleInput();
        }

        viewport.apply();
        renderer.setView(camera);
        renderer.render();
    }

    private void handleInput() {
        if (Gdx.input.justTouched()) {
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPos);

            // 1. Get exact map dimensions to fix the Y-offset
            int mapHeightInTiles = map.getProperties().get("height", Integer.class);
            int tileHeightPx = map.getProperties().get("tileheight", Integer.class);
            float worldMapHeight = mapHeightInTiles * tileHeightPx * unitScale;

            // --- CHECK DIAMONDS (Rhombuses) ---
            for (MapObject object : diamondLayer.getObjects()) {
                if (object instanceof TextureMapObject) {
                    TextureMapObject tmo = (TextureMapObject) object;

                    // FIX 1: Check if already occupied using a property check
                    if (tmo.getProperties().containsKey("occupied")) continue;

                    float objW = tmo.getProperties().get("width", Float.class) * unitScale;
                    float objH = tmo.getProperties().get("height", Float.class) * unitScale;

                    float worldX = tmo.getX() * unitScale;

                    // FIX 2: Correct Y-flip formula for LibGDX vs Tiled
                    // We subtract the object height because LibGDX draws from the bottom-left
                    float worldY = worldMapHeight - (tmo.getY() * unitScale) - objH;

                    if (touchPos.x >= worldX && touchPos.x <= worldX + objW &&
                        touchPos.y >= worldY && touchPos.y <= worldY + objH) {

                        // Update graphic
                        int gid = (currentPlayer == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
                        tmo.setTextureRegion(map.getTileSets().getTile(gid).getTextureRegion());

                        // Lock this object so it can't be modified again
                        tmo.getProperties().put("occupied", true);

                        togglePlayer();
                        return; // Stop looking once we hit an object
                    }
                }
            }

            // --- CHECK OCTAGONS (Tile Layer) ---
            int cellX = (int) (touchPos.x / (octagonLayer.getTileWidth() * unitScale));
            int cellY = (int) (touchPos.y / (octagonLayer.getTileHeight() * unitScale));

            TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
            if (cell != null && cell.getTile() != null) {
                int currentGid = cell.getTile().getId();

                // FIX 3: Check if cell is already occupied by a color
                if (currentGid == GID_BLACK_OCT || currentGid == GID_WHITE_OCT ||
                    currentGid == GID_BLACK_RHO || currentGid == GID_WHITE_RHO) {
                    return;
                }

                int targetGid = (currentPlayer == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
                cell.setTile(map.getTileSets().getTile(targetGid));
                togglePlayer();
            }
        }
    }

    private void processMove(TextureMapObject tmo) {
        // Logic to update the Rhombus state in QuaxBoard and visual
        updateObjectGraphic(tmo, currentPlayer);
        togglePlayer();
    }

    private void processMove(TiledMapTileLayer.Cell cell, int x, int y) {
        // Translate Tiled coords to logical board coordinates (A1, B2, etc.)
        String label = generateOctagonLabel(x, y);

        if (!label.equals("OUT_OF_BOUNDS")) {
            // Update the logical board
            // boardLogic.makeMove(label, currentPlayer);

            // Update the visual cell
            int gid = (currentPlayer == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
            cell.setTile(map.getTileSets().getTile(gid));

            togglePlayer();
        }
    }

    private void updateObjectGraphic(TextureMapObject tmo, PlayerColour color) {
        int gid = (color == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
        TiledMapTile stoneTile = map.getTileSets().getTile(gid);
        if (stoneTile != null) {
            tmo.setTextureRegion(stoneTile.getTextureRegion());
        }
    }

    private void togglePlayer() {
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        System.out.println("It is now " + currentPlayer + "'s turn.");
    }

    private String generateOctagonLabel(int x, int y) {
        // Based on your grid offsets
        int logicalX = x - 5;
        int logicalY = y - 4;

        if (logicalX < 0 || logicalX >= 11 || logicalY < 0 || logicalY >= 11) {
            return "OUT_OF_BOUNDS";
        }

        char col = (char) ('A' + logicalX);
        int row = logicalY + 1;
        return "" + col + row;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
    }
}
