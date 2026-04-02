package src.softies.board;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.PlayerColour;
import src.softies.QuaxBoard;

// translates raw screen clicks into board moves and tracks the hovered cell shape
// exposes polygon vertices for the hover highlight so Main can draw the actual tile shape
// instead of a bounding-box rect which would cover transparent PNG corners
public class InputHandler {

    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final GameState gameState;
    private final Viewport viewport;
    private final QuaxBoard boardLogic;

    // label and type of the last successfully placed cell — read by Main for logging
    private String lastCellLabel = "";
    private String lastCellType  = "";

    // what shape is currently under the mouse — drives which polygon is built
    public enum HoverShape { NONE, OCTAGON, RHOMBUS }
    private HoverShape hoverShape = HoverShape.NONE;

    // flat [x0,y0, x1,y1, ...] vertex array ready for ShapeRenderer.polygon()
    // updated every frame by updateHover()
    private float[] hoverVertices = new float[0];

    // tile GIDs matching the tileset
    private static final int GID_EMPTY_RHO = 2;
    private static final int GID_WHITE_RHO = 3;
    private static final int GID_BLACK_RHO = 4;
    private static final int GID_EMPTY_OCT = 5;
    private static final int GID_WHITE_OCT = 6;
    private static final int GID_BLACK_OCT = 7;

    // fraction of each tile dimension to clip at each corner when building the octagon polygon
    // tune this value if the highlight outline doesn't match the tile art
    private static final float OCT_CUT = 0.25f;

    public enum MoveResult {
        SUCCESS,
        OCCUPIED,
        NOT_A_CELL,
        INVALID_PLACEMENT
    }

    /**
     * @param map          the loaded TiledMap
     * @param octagonLayer tile layer where octagonal cells live
     * @param diamondLayer object layer where rhombus TextureMapObjects are stored
     * @param unitScale    scale factor applied to all pixel measurements (0.25f)
     * @param gameState    tracks and toggles the current player
     * @param viewport     converts screen coords to world coords
     * @param boardLogic   the board model that records placement state
     */
    public InputHandler(TiledMap map, TiledMapTileLayer octagonLayer, MapLayer diamondLayer,
                        float unitScale, GameState gameState, Viewport viewport, QuaxBoard boardLogic) {
        this.map          = map;
        this.octagonLayer = octagonLayer;
        this.diamondLayer = diamondLayer;
        this.unitScale    = unitScale;
        this.gameState    = gameState;
        this.viewport     = viewport;
        this.boardLogic   = boardLogic;
    }

    /**
     * determines which cell (if any) is under the mouse and builds its polygon vertex array
     * call every frame so the hover highlight stays current
     * @param screenX raw screen x from Gdx.input.getX()
     * @param screenY raw screen y from Gdx.input.getY()
     */
    public void updateHover(int screenX, int screenY) {
        hoverShape    = HoverShape.NONE;
        hoverVertices = new float[0];

        Vector3 touchPos = new Vector3(screenX, screenY, 0);
        viewport.unproject(touchPos);

        int mapHeightInTiles = map.getProperties().get("height", Integer.class);
        int tileHeightPx     = map.getProperties().get("tileheight", Integer.class);
        float worldMapHeight = mapHeightInTiles * tileHeightPx * unitScale;

        float dOffsetX = diamondLayer.getOffsetX() * unitScale;
        float dOffsetY = diamondLayer.getOffsetY() * unitScale;

        // check rhombus diamonds first — diamond polygon from bounding-box midpoints
        for (MapObject object : diamondLayer.getObjects()) {
            if (!(object instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) object;

            float objW = tmo.getProperties().get("width",  Float.class) * unitScale;
            float objH = tmo.getProperties().get("height", Float.class) * unitScale;
            float wx   = tmo.getX() * unitScale + dOffsetX;
            float wy   = worldMapHeight - (tmo.getY() * unitScale) - dOffsetY + 2 * objH + 4f;

            // bounding-box hit test to find which diamond we're over
            if (touchPos.x >= wx && touchPos.x <= wx + objW &&
                touchPos.y >= wy && touchPos.y <= wy + objH) {
                hoverShape    = HoverShape.RHOMBUS;
                // diamond vertices: midpoints of each edge of the bounding box
                hoverVertices = buildDiamondVertices(wx, wy, objW, objH);
                return;
            }
        }

        // fall through to octagon tile check
        int cellX = (int) (touchPos.x / (octagonLayer.getTileWidth()  * unitScale));
        int cellY = (int) (touchPos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell != null && cell.getTile() != null) {
            int gid = cell.getTile().getId();
            // only highlight tiles that are part of the playable board
            if (gid == GID_EMPTY_OCT || gid == GID_BLACK_OCT || gid == GID_WHITE_OCT) {
                float wx = cellX * octagonLayer.getTileWidth()  * unitScale;
                float wy = cellY * octagonLayer.getTileHeight() * unitScale;
                float w  = octagonLayer.getTileWidth()  * unitScale;
                float h  = octagonLayer.getTileHeight() * unitScale;
                hoverShape    = HoverShape.OCTAGON;
                hoverVertices = buildOctagonVertices(wx, wy, w, h);
            }
        }
    }

    /**
     * builds the 8-point octagon polygon by clipping OCT_CUT fraction of each corner
     * @param x  world-space left edge
     * @param y  world-space bottom edge
     * @param w  tile width in world units
     * @param h  tile height in world units
     * @return flat float array [x0,y0, x1,y1, ...] for ShapeRenderer.polygon()
     */
    private float[] buildOctagonVertices(float x, float y, float w, float h) {
        float cx = w * OCT_CUT;
        float cy = h * OCT_CUT;
        return new float[]{
            x + cx,     y,          // bottom-left edge
            x + w - cx, y,          // bottom-right edge
            x + w,      y + cy,     // right-bottom edge
            x + w,      y + h - cy, // right-top edge
            x + w - cx, y + h,      // top-right edge
            x + cx,     y + h,      // top-left edge
            x,          y + h - cy, // left-top edge
            x,          y + cy      // left-bottom edge
        };
    }

    /**
     * builds the 4-point diamond polygon from the midpoints of the bounding box edges
     * @param x  world-space left edge of the bounding box
     * @param y  world-space bottom edge of the bounding box
     * @param w  bounding box width in world units
     * @param h  bounding box height in world units
     * @return flat float array [x0,y0, x1,y1, ...] for ShapeRenderer.polygon()
     */
    private float[] buildDiamondVertices(float x, float y, float w, float h) {
        return new float[]{
            x + w / 2, y,          // bottom point
            x + w,     y + h / 2,  // right point
            x + w / 2, y + h,      // top point
            x,         y + h / 2   // left point
        };
    }

    /** @return the shape type currently under the mouse */
    public HoverShape getHoverShape() { return hoverShape; }

    /** @return polygon vertices for the hovered cell, ready for ShapeRenderer.polygon() */
    public float[] getHoverVertices() { return hoverVertices; }

    /**
     * processes a screen click and applies the move if valid
     * checks the diamond layer first, then falls through to octagon tiles
     * @param screenX raw screen x from Gdx.input.getX()
     * @param screenY raw screen y from Gdx.input.getY()
     * @return a MoveResult indicating what happened
     */
    public MoveResult handleBoardClick(int screenX, int screenY) {
        Vector3 touchPos = new Vector3(screenX, screenY, 0);
        viewport.unproject(touchPos);

        int mapHeightInTiles = map.getProperties().get("height", Integer.class);
        int tileHeightPx     = map.getProperties().get("tileheight", Integer.class);
        float worldMapHeight = mapHeightInTiles * tileHeightPx * unitScale;

        float dOffsetX = diamondLayer.getOffsetX() * unitScale;
        float dOffsetY = diamondLayer.getOffsetY() * unitScale;

        // --- diamond / rhombus hit detection ---
        for (MapObject object : diamondLayer.getObjects()) {
            if (!(object instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) object;

            float objW   = tmo.getProperties().get("width",  Float.class) * unitScale;
            float objH   = tmo.getProperties().get("height", Float.class) * unitScale;
            float worldX = tmo.getX() * unitScale + dOffsetX;
            float worldY = worldMapHeight - (tmo.getY() * unitScale) - dOffsetY + 2 * objH + 4f;

            if (touchPos.x < worldX || touchPos.x > worldX + objW ||
                touchPos.y < worldY || touchPos.y > worldY + objH) continue;

            if (tmo.getProperties().containsKey("occupied")) return MoveResult.OCCUPIED;

            // derive the cell key from the object's pixel position
            int colGap = Math.round((tmo.getX() + 40) / 128f) - 6;
            int row    = 17 - Math.round((tmo.getY() - 40) / 128f);
            if (colGap < 0 || colGap > 9 || row < 2 || row > 11) return MoveResult.NOT_A_CELL;
            String cellId = "R-" + (char) ('A' + colGap) + row;

            if (!boardLogic.placeRhombus(cellId, gameState.getCurrentPlayer()))
                return MoveResult.OCCUPIED;

            int gid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
            tmo.setTextureRegion(map.getTileSets().getTile(gid).getTextureRegion());
            tmo.getProperties().put("occupied", true);

            lastCellLabel = cellId;
            lastCellType  = "Rhombus";

            if (!gameState.isFirstMoveMade()) gameState.setFirstMoveMade();
            gameState.togglePlayer();
            return MoveResult.SUCCESS;
        }

        // --- octagon tile hit detection ---
        int cellX = (int) (touchPos.x / (octagonLayer.getTileWidth()  * unitScale));
        int cellY = (int) (touchPos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell != null && cell.getTile() != null) {
            int currentGid = cell.getTile().getId();
            if (currentGid == GID_BLACK_OCT || currentGid == GID_WHITE_OCT ||
                currentGid == GID_BLACK_RHO || currentGid == GID_WHITE_RHO) {
                return MoveResult.OCCUPIED;
            }

            int targetGid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
            cell.setTile(map.getTileSets().getTile(targetGid));

            // subtract board tile origin (col 5, row 4) to get A1–K11 labels
            int boardCol = cellX - 5;
            int boardRow = cellY - 4;
            lastCellLabel = "" + (char) ('A' + boardCol) + boardRow;
            lastCellType  = "Octagon";

            if (!gameState.isFirstMoveMade()) gameState.setFirstMoveMade();
            gameState.togglePlayer();
            return MoveResult.SUCCESS;
        }

        return MoveResult.NOT_A_CELL;
    }

    /** @return the label of the cell from the most recent successful move (e.g. "C4" or "R-B3") */
    public String getLastCellLabel() { return lastCellLabel; }

    /** @return the cell type from the most recent successful move — "Octagon" or "Rhombus" */
    public String getLastCellType() { return lastCellType; }
}
