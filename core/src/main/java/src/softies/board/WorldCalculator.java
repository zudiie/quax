package src.softies.board;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

// computes and stores all world-space measurements needed to position and render the board
// everything that needs to know where the board is in screen space reads from here
public class WorldCalculator {

    public final float unitScale;

    public float tileWidthWorld;
    public float tileHeightWorld;

    public float mapWidthWorld;
    public float mapHeightWorld;

    public float boardMinX, boardMinY, boardMaxX, boardMaxY;
    public float boardCenterX, boardCenterY;
    public float boardWidth, boardHeight;

    public WorldCalculator(float unitScale) {
        this.unitScale = unitScale;
    }

    /**
     * reads map and layer properties and derives all world-space coordinates
     * call once during setup after the map is loaded
     */
    public void compute(TiledMap map, TiledMapTileLayer octagonLayer) {
        computeTileDimensions(map);
        float[] layerOffset = readLayerOffset(octagonLayer);
        computeBoardBounds(layerOffset[0], layerOffset[1]);
        logBoardDimensions(layerOffset[0], layerOffset[1]);
    }

    private void computeTileDimensions(TiledMap map) {
        int mapWidthTiles  = map.getProperties().get("width",      Integer.class);
        int mapHeightTiles = map.getProperties().get("height",     Integer.class);
        int tileWidthPx    = map.getProperties().get("tilewidth",  Integer.class);
        int tileHeightPx   = map.getProperties().get("tileheight", Integer.class);
        tileWidthWorld  = tileWidthPx  * unitScale;
        tileHeightWorld = tileHeightPx * unitScale;
        mapWidthWorld   = mapWidthTiles  * tileWidthWorld;
        mapHeightWorld  = mapHeightTiles * tileHeightWorld;
    }

    private float[] readLayerOffset(TiledMapTileLayer octagonLayer) {
        return new float[]{
            octagonLayer.getOffsetX() * unitScale,
            octagonLayer.getOffsetY() * unitScale
        };
    }

    private void computeBoardBounds(float offsetX, float offsetY) {
        boardMinX = 5        * tileWidthWorld  + offsetX;
        boardMinY = 4        * tileHeightWorld + offsetY;
        boardMaxX = (5 + 11) * tileWidthWorld  + offsetX;
        boardMaxY = (4 + 11) * tileHeightWorld + offsetY;
        boardWidth   = boardMaxX - boardMinX;
        boardHeight  = boardMaxY - boardMinY;
        boardCenterX = boardMinX + boardWidth  / 2;
        boardCenterY = boardMinY + boardHeight / 2;
    }

    private void logBoardDimensions(float offsetX, float offsetY) {
        System.out.println("Octagon layer offset (world): " + offsetX + ", " + offsetY);
        System.out.println("Board world bounds: (" + boardMinX + ", " + boardMinY
            + ") to (" + boardMaxX + ", " + boardMaxY + ")");
        System.out.println("Board center: (" + boardCenterX + ", " + boardCenterY + ")");
    }
}
