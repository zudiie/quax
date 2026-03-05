package src.softies.board;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * Calculates and stores world dimensions, tile sizes, and the playable board area.
 */
public class WorldCalculator {
    public final float unitScale;

    public float tileWidthWorld;
    public float tileHeightWorld;
    public float mapWidthWorld;
    public float mapHeightWorld;

    // board bounds (playable area: columns 5-15, rows 4-14)
    public float boardMinX, boardMinY, boardMaxX, boardMaxY;
    public float boardCenterX, boardCenterY;
    public float boardWidth, boardHeight;

    public WorldCalculator(float unitScale) {
        this.unitScale = unitScale;
    }

    /**
     * Reads map properties and layer offsets, then computes all world coordinates.
     */
    public void compute(TiledMap map, TiledMapTileLayer octagonLayer) {
        // map dimensions in tiles
        int mapWidthTiles  = map.getProperties().get("width", Integer.class);
        int mapHeightTiles = map.getProperties().get("height", Integer.class);
        int tileWidthPx    = map.getProperties().get("tilewidth", Integer.class);
        int tileHeightPx   = map.getProperties().get("tileheight", Integer.class);

        tileWidthWorld  = tileWidthPx * unitScale;
        tileHeightWorld = tileHeightPx * unitScale;
        mapWidthWorld   = mapWidthTiles * tileWidthWorld;
        mapHeightWorld  = mapHeightTiles * tileHeightWorld;

        // layer offsets (if any)
        float layerOffsetX = octagonLayer.getOffsetX() * unitScale;
        float layerOffsetY = octagonLayer.getOffsetY() * unitScale;

        // board occupies columns 5‑15 and rows 4‑14 (0‑based indices)
        boardMinX = 5 * tileWidthWorld + layerOffsetX;
        boardMinY = 4 * tileHeightWorld + layerOffsetY;
        boardMaxX = (5 + 11) * tileWidthWorld + layerOffsetX;
        boardMaxY = (4 + 11) * tileHeightWorld + layerOffsetY;
        boardWidth  = boardMaxX - boardMinX;
        boardHeight = boardMaxY - boardMinY;
        boardCenterX = boardMinX + boardWidth / 2;
        boardCenterY = boardMinY + boardHeight / 2;

        // debug output
        System.out.println("Octagon layer offset (world): " + layerOffsetX + ", " + layerOffsetY);
        System.out.println("Board world bounds: (" + boardMinX + ", " + boardMinY + ") to (" + boardMaxX + ", " + boardMaxY + ")");
        System.out.println("Board center: (" + boardCenterX + ", " + boardCenterY + ")");
    }
}
