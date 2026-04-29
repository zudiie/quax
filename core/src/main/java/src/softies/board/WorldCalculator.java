package src.softies.board;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

// computes and stores all the world-space measurements needed to position and render the board
// everything that needs to know "where is the board in screen space" reads from here
public class WorldCalculator {

    public final float unitScale;

    // tile dimensions in world units (after scaling)
    public float tileWidthWorld;
    public float tileHeightWorld;

    // full map dimensions in world units
    public float mapWidthWorld;
    public float mapHeightWorld;

    // the playable board area - columns 5-15, rows 4-14 (0-based)
    public float boardMinX, boardMinY, boardMaxX, boardMaxY;
    public float boardCenterX, boardCenterY;
    public float boardWidth, boardHeight;

    /**
     * creates a calculator with the given pixel-to-world unit scale
     * @param unitScale the scale factor applied to all pixel measurements (e.g. 0.25f)
     */
    public WorldCalculator(float unitScale) {
        this.unitScale = unitScale;
    }

    /**
     * reads the map and octagon layer properties and works out all the world-space coordinates
     * call this once during setup after the map has been loaded
     * @param map the loaded TiledMap with width/height/tile size properties
     * @param octagonLayer the octagon tile layer - used to grab any layer-level offsets
     */
    public void compute(TiledMap map, TiledMapTileLayer octagonLayer) {
        // grab the raw pixel dimensions from the map properties
        int mapWidthTiles  = map.getProperties().get("width",      Integer.class);
        int mapHeightTiles = map.getProperties().get("height",     Integer.class);
        int tileWidthPx    = map.getProperties().get("tilewidth",  Integer.class);
        int tileHeightPx   = map.getProperties().get("tileheight", Integer.class);

        // scale everything down to world units
        tileWidthWorld  = tileWidthPx  * unitScale;
        tileHeightWorld = tileHeightPx * unitScale;
        mapWidthWorld   = mapWidthTiles  * tileWidthWorld;
        mapHeightWorld  = mapHeightTiles * tileHeightWorld;

        // account for any offset the octagon layer has baked into the tmx file
        float layerOffsetX = octagonLayer.getOffsetX() * unitScale;
        float layerOffsetY = octagonLayer.getOffsetY() * unitScale;

        // the 11x11 playable area starts at tile column 5 and row 4
        boardMinX = 5  * tileWidthWorld  + layerOffsetX;
        boardMinY = 4  * tileHeightWorld + layerOffsetY;
        boardMaxX = (5 + 11) * tileWidthWorld  + layerOffsetX;
        boardMaxY = (4 + 11) * tileHeightWorld + layerOffsetY;

        boardWidth   = boardMaxX - boardMinX;
        boardHeight  = boardMaxY - boardMinY;
        boardCenterX = boardMinX + boardWidth  / 2;
        boardCenterY = boardMinY + boardHeight / 2;

        // log the results so it's easy to spot alignment issues during dev
        System.out.println("Octagon layer offset (world): " + layerOffsetX + ", " + layerOffsetY);
        System.out.println("Board world bounds: (" + boardMinX + ", " + boardMinY + ") to (" + boardMaxX + ", " + boardMaxY + ")");
        System.out.println("Board center: (" + boardCenterX + ", " + boardCenterY + ")");
    }
}
