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
import src.softies.WinCheck;

// translates raw screen clicks into board moves and checks for a winner after each one
// also tracks the hovered cell shape and exposes polygon vertices so Main can draw
// a shape-accurate highlight instead of a bounding-box rect that bleeds into transparent PNG corners
//
// PIE RULE NOTE: this class does NOT visually swap the first tile when the pie rule is activated
// the first tile was placed as BLACK; after the swap, Player 2 is BLACK and "owns" it —
// so the tile correctly stays BLACK in both the model and the visual
public class InputHandler {

    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final GameState gameState;
    private final Viewport viewport;
    private final QuaxBoard boardLogic;

    // win detection — created once and reused every move
    private final WinCheck winCheck;

    // tile GIDs from the tileset — must match the tileset order in PolygonalGrid.tmx
    private static final int GID_EMPTY_RHO = 2;
    private static final int GID_WHITE_RHO = 3;
    private static final int GID_BLACK_RHO = 4;
    private static final int GID_EMPTY_OCT = 5;
    private static final int GID_WHITE_OCT = 6;
    private static final int GID_BLACK_OCT = 7;

    // fraction of each tile dimension clipped at each corner to build the octagon polygon
    // increase this value if the highlight outline doesn't match the tile art
    private static final float OCT_CUT = 0.25f;

    // hover state — updated every frame by updateHover(), read by Main to draw the overlay
    public enum HoverShape { NONE, OCTAGON, RHOMBUS }
    private HoverShape hoverShape    = HoverShape.NONE;
    private float[]    hoverVertices = new float[0];

    // possible outcomes of a move attempt
    public enum MoveResult {
        SUCCESS,          // move placed, turn toggled
        WIN,              // move placed and the moving player has won — game is now frozen
        OCCUPIED,         // the clicked cell was already taken
        NOT_A_CELL,       // the click didn't land on any valid cell
        INVALID_PLACEMENT // rhombus placement wasn't valid
    }

    /**
     * @param map          the loaded TiledMap
     * @param octagonLayer tile layer where octagonal cells live
     * @param diamondLayer object layer where rhombus TextureMapObjects are stored
     * @param unitScale    scale factor applied to all pixel measurements (0.25f)
     * @param gameState    tracks and toggles the current player, stores the winner
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
        this.winCheck     = new WinCheck(boardLogic);
    }

    // -------------------------------------------------------------------------
    // HOVER DETECTION
    // -------------------------------------------------------------------------

    /**
     * determines which cell (if any) is under the mouse and builds its polygon vertex array
     * call every frame so the hover highlight stays current
     * @param screenX raw screen x from Gdx.input.getX()
     * @param screenY raw screen y from Gdx.input.getY()
     */
    public void updateHover(int screenX, int screenY) {
        hoverShape    = HoverShape.NONE;
        hoverVertices = new float[0];

        Vector3 pos = new Vector3(screenX, screenY, 0);
        viewport.unproject(pos);

        int mapH   = map.getProperties().get("height",     Integer.class);
        int tileHpx = map.getProperties().get("tileheight", Integer.class);
        float mapHeightWorld = mapH * tileHpx * unitScale;

        float dOffX = diamondLayer.getOffsetX() * unitScale;
        float dOffY = diamondLayer.getOffsetY() * unitScale;

        // check rhombus diamonds first
        for (MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;
            float w  = tmo.getProperties().get("width",  Float.class) * unitScale;
            float h  = tmo.getProperties().get("height", Float.class) * unitScale;
            float wx = tmo.getX() * unitScale + dOffX;
            float wy = mapHeightWorld - (tmo.getY() * unitScale) - dOffY + 2 * h + 4f;

            if (pos.x >= wx && pos.x <= wx + w && pos.y >= wy && pos.y <= wy + h) {
                hoverShape    = HoverShape.RHOMBUS;
                hoverVertices = buildDiamondVertices(wx, wy, w, h);
                return;
            }
        }

        // fall through to octagon tiles
        int cellX = (int)(pos.x / (octagonLayer.getTileWidth()  * unitScale));
        int cellY = (int)(pos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell != null && cell.getTile() != null) {
            int gid = cell.getTile().getId();
            // only highlight tiles that are part of the playable board (not blank surrounds)
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
     * builds the 8-point octagon polygon by clipping OCT_CUT fraction from each corner
     * @return flat [x0,y0, x1,y1, ...] array for ShapeRenderer.polygon()
     */
    private float[] buildOctagonVertices(float x, float y, float w, float h) {
        float cx = w * OCT_CUT;
        float cy = h * OCT_CUT;
        return new float[]{
            x + cx,     y,
            x + w - cx, y,
            x + w,      y + cy,
            x + w,      y + h - cy,
            x + w - cx, y + h,
            x + cx,     y + h,
            x,          y + h - cy,
            x,          y + cy
        };
    }

    /**
     * builds the 4-point diamond polygon from the midpoints of the bounding box edges
     * @return flat [x0,y0, x1,y1, ...] array for ShapeRenderer.polygon()
     */
    private float[] buildDiamondVertices(float x, float y, float w, float h) {
        return new float[]{
            x + w / 2, y,
            x + w,     y + h / 2,
            x + w / 2, y + h,
            x,         y + h / 2
        };
    }

    /** @return the shape type currently under the mouse */
    public HoverShape getHoverShape()    { return hoverShape; }

    /** @return polygon vertices for the hovered cell, ready for ShapeRenderer.polygon() */
    public float[]    getHoverVertices() { return hoverVertices; }

    // -------------------------------------------------------------------------
    // CLICK / MOVE HANDLING
    // -------------------------------------------------------------------------

    /**
     * processes a screen click, applies the move, and checks for a win
     * returns OCCUPIED or NOT_A_CELL if nothing was placed
     * returns WIN if the move completed a winning path (game is frozen by GameState)
     * returns SUCCESS otherwise
     * @param screenX raw screen x from Gdx.input.getX()
     * @param screenY raw screen y from Gdx.input.getY()
     * @return a MoveResult indicating what happened
     */
    public MoveResult handleBoardClick(int screenX, int screenY) {
        // don't accept any moves after the game is over
        if (gameState.isGameOver()) return MoveResult.NOT_A_CELL;

        Vector3 pos = new Vector3(screenX, screenY, 0);
        viewport.unproject(pos);

        int mapH    = map.getProperties().get("height",     Integer.class);
        int tileHpx = map.getProperties().get("tileheight", Integer.class);
        float mapHeightWorld = mapH * tileHpx * unitScale;

        float dOffX = diamondLayer.getOffsetX() * unitScale;
        float dOffY = diamondLayer.getOffsetY() * unitScale;

        // --- rhombus / diamond hit detection ---
        for (MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;

            float objW   = tmo.getProperties().get("width",  Float.class) * unitScale;
            float objH   = tmo.getProperties().get("height", Float.class) * unitScale;
            float worldX = tmo.getX() * unitScale + dOffX;
            // the +2*objH+4f corrects for the y-flip and a small visual alignment offset
            float worldY = mapHeightWorld - (tmo.getY() * unitScale) - dOffY + 2 * objH + 4f;

            if (pos.x < worldX || pos.x > worldX + objW ||
                pos.y < worldY || pos.y > worldY + objH) continue;

            if (tmo.getProperties().containsKey("occupied")) return MoveResult.OCCUPIED;

            // derive the cell key from the object's pixel position
            int colGap = Math.round((tmo.getX() + 40) / 128f) - 6;
            int row    = 17 - Math.round((tmo.getY() - 40) / 128f);
            if (colGap < 0 || colGap > 9 || row < 2 || row > 11) return MoveResult.NOT_A_CELL;

            String cellId = "R-" + (char)('A' + colGap) + row;
            if (!boardLogic.placeRhombus(cellId, gameState.getCurrentPlayer()))
                return MoveResult.OCCUPIED;

            // update the tile graphic to reflect the current player's colour
            int gid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
            tmo.setTextureRegion(map.getTileSets().getTile(gid).getTextureRegion());
            tmo.getProperties().put("occupied", true);

            // record that the first move has been made so the pie rule window opens
            if (!gameState.isFirstMoveMade()) gameState.setFirstMoveMade();

            // check for a win BEFORE toggling the turn
            PlayerColour mover = gameState.getCurrentPlayer();
            if (winCheck.checkWin(mover)) {
                gameState.setWinner(mover); // freeze the game
                return MoveResult.WIN;
            }

            gameState.togglePlayer();
            return MoveResult.SUCCESS;
        }

        // --- octagon tile hit detection ---
        int cellX = (int)(pos.x / (octagonLayer.getTileWidth()  * unitScale));
        int cellY = (int)(pos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell != null && cell.getTile() != null) {
            int currentGid = cell.getTile().getId();

            // reject if this tile already has a stone or rhombus on it
            if (currentGid == GID_BLACK_OCT || currentGid == GID_WHITE_OCT ||
                currentGid == GID_BLACK_RHO || currentGid == GID_WHITE_RHO) {
                return MoveResult.OCCUPIED;
            }

            // update the visual tile
            int targetGid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
            cell.setTile(map.getTileSets().getTile(targetGid));

            // derive the board label from pixel coordinates
            // the board starts at tile column 5 and row 4 within the full TMX map
            int mapHeightPx = mapH * tileHpx;
            float tmxPixelX = pos.x / unitScale;
            float tmxPixelY = mapHeightPx - (pos.y / unitScale);
            int boardCol = (int)(tmxPixelX / tileHpx) - 5;
            int boardRow = 15 - (int)(tmxPixelY / tileHpx);

            String cellLabel = QuaxBoard.generateLabel(boardCol, boardRow);
            // update the board model so WinCheck can find this stone
            boardLogic.placeStone(cellLabel, gameState.getCurrentPlayer());

            if (!gameState.isFirstMoveMade()) gameState.setFirstMoveMade();

            // check for a win BEFORE toggling the turn
            PlayerColour mover = gameState.getCurrentPlayer();
            if (winCheck.checkWin(mover)) {
                gameState.setWinner(mover); // freeze the game
                return MoveResult.WIN;
            }

            gameState.togglePlayer();
            return MoveResult.SUCCESS;
        }

        return MoveResult.NOT_A_CELL;
    }
}
