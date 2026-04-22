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

import java.util.HashMap;
import java.util.Map;

// processes board clicks and programmatic bot moves
// updates the board model AND the visual tiles, then checks for a win
public class MoveHandler {

    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final GameState gameState;
    private final Viewport viewport;
    private final QuaxBoard boardLogic;
    private final WinCheck winCheck;

    // pre-built lookup: rhombus board key (e.g. "R-F6") → the diamond TextureMapObject
    // built once in the constructor so bot placement never has to scan the full layer
    private final Map<String, TextureMapObject> rhombusKeyToTMO = new HashMap<>();

    // tile GIDs - must match the tileset order in PolygonalGrid.tmx
    private static final int GID_EMPTY_OCT = 5;
    private static final int GID_WHITE_OCT = 6; // white octagonal stone
    private static final int GID_BLACK_OCT = 7; // black octagonal stone
    private static final int GID_WHITE_RHO = 3;
    private static final int GID_BLACK_RHO = 4;

    // possible outcomes of any move attempt
    public enum Result {
        SUCCESS,           // move placed, turn toggled
        WIN,               // move placed and moving player has won
        OCCUPIED,          // target cell was already taken
        NOT_A_CELL,        // location didn't map to a valid board cell
        INVALID_PLACEMENT  // rhombus placement rule was violated
    }

    /**
     * @param map          the loaded TiledMap
     * @param octagonLayer tile layer containing octagonal cells
     * @param diamondLayer object layer containing rhombus TextureMapObjects
     * @param unitScale    pixel-to-world scale factor (0.25f)
     * @param gameState    tracks the current player, first-move flag and winner
     * @param viewport     converts screen coords to world coords
     * @param boardLogic   the board model - updated on every successful placement
     */
    public MoveHandler(TiledMap map, TiledMapTileLayer octagonLayer, MapLayer diamondLayer,
                       float unitScale, GameState gameState, Viewport viewport, QuaxBoard boardLogic) {
        this.map          = map;
        this.octagonLayer = octagonLayer;
        this.diamondLayer = diamondLayer;
        this.unitScale    = unitScale;
        this.gameState    = gameState;
        this.viewport     = viewport;
        this.boardLogic   = boardLogic;
        this.winCheck     = new WinCheck(boardLogic);

        buildRhombusKeyMap(); // pre-compute key → TMO for O(1) bot placement
    }

    /**
     * iterates the diamond layer once and maps each rhombus board key to its TMO
     * uses the same pixel formula as human click detection to derive the key
     * called once in the constructor - never needs to repeat
     */
    private void buildRhombusKeyMap() {
        for (MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;

            // derive the rhombus board key from the TMO's pixel position
            // this is the same formula used by human click detection in tryPlaceDiamond
            int colGap = Math.round((tmo.getX() + 40) / 128f) - 6;
            int row    = 17 - Math.round((tmo.getY() - 40) / 128f);

            // only store valid board positions
            if (colGap < 0 || colGap > 9 || row < 2 || row > 11) continue;

            String key = "R-" + (char)('A' + colGap) + row;
            rhombusKeyToTMO.put(key, tmo);
        }
        System.out.println("MoveHandler: mapped " + rhombusKeyToTMO.size() + " rhombus TMOs.");
    }

    // -------------------------------------------------------------------------
    // human click
    // -------------------------------------------------------------------------

    /**
     * processes a raw screen click - tries diamond layer first, then octagon tiles
     */
    public Result handleClick(int screenX, int screenY) {
        if (gameState.isGameOver()) return Result.NOT_A_CELL;

        Vector3 pos = unprojectClick(screenX, screenY);
        Result  diamondResult = tryPlaceDiamond(pos, getMapHeightWorld());
        if (diamondResult != null) return diamondResult;
        return tryPlaceOctagon(pos);
    }

    // -------------------------------------------------------------------------
    // bot placement - programmatic, no screen click needed
    // -------------------------------------------------------------------------

    /**
     * places a stone or rhombus for the bot using a board label
     *
     * routing:
     *   label starts with "R-"  → rhombus placement via pre-built TMO map
     *   any other label         → octagonal stone placement
     *
     * defensive guard in placeBotOctagon also catches any stale "R-" labels
     * so this can never throw a NumberFormatException regardless of caller
     *
     * @param cellLabel "F6" for an octagon, "R-F6" for a rhombus
     */
    public Result placeBotMove(String cellLabel) {
        if (gameState.isGameOver()) return Result.NOT_A_CELL;
        if (cellLabel == null || cellLabel.isEmpty()) return Result.NOT_A_CELL;

        if (cellLabel.startsWith("R-")) {
            return placeBotRhombus(cellLabel);
        } else {
            return placeBotOctagon(cellLabel);
        }
    }

    /**
     * places a bot octagonal stone at the given board label
     * converts the label to tile-grid coordinates (inverse of deriveOctagonLabel)
     *
     * GUARD: if this method somehow receives an "R-" label it returns NOT_A_CELL
     * rather than throwing NumberFormatException - this makes it crash-safe
     */
    private Result placeBotOctagon(String cellLabel) {
        // defensive guard - rhombus labels must never reach here
        if (cellLabel.startsWith("R-")) {
            System.err.println("placeBotOctagon received rhombus label: " + cellLabel + " - ignoring");
            return Result.NOT_A_CELL;
        }

        int boardCol, boardRow;
        try {
            boardCol = cellLabel.charAt(0) - 'A';
            boardRow = Integer.parseInt(cellLabel.substring(1));
        } catch (Exception e) {
            System.err.println("placeBotOctagon: bad label format: " + cellLabel);
            return Result.NOT_A_CELL;
        }

        // convert board coordinates to libgdx tile-grid coordinates
        // board starts at TMX tile column 5; y-flip: tileY = mapH - 16 + boardRow
        int mapH  = map.getProperties().get("height", Integer.class);
        int cellX = boardCol + 5;
        int cellY = mapH - 16 + boardRow;

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell == null || cell.getTile() == null) return Result.NOT_A_CELL;
        if (isOccupied(cell.getTile().getId()))     return Result.OCCUPIED;

        // update the visual tile and the board model
        int gid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
        cell.setTile(map.getTileSets().getTile(gid));
        boardLogic.placeStone(cellLabel, gameState.getCurrentPlayer());

        return finalisePlacement();
    }

    /**
     * places a bot rhombus using the pre-built key → TMO map
     * O(1) lookup - no pixel math, no iteration, no floating-point drift
     */
    private Result placeBotRhombus(String rhombusKey) {
        TextureMapObject tmo = rhombusKeyToTMO.get(rhombusKey);
        if (tmo == null) {
            System.err.println("placeBotRhombus: no TMO found for key: " + rhombusKey);
            return Result.NOT_A_CELL;
        }
        return placeDiamond(tmo);
    }

    // -------------------------------------------------------------------------
    // diamond (rhombus) helpers - shared by human click and bot placement
    // -------------------------------------------------------------------------

    /** scans the diamond layer for a TMO hit by the given world-space click position */
    private Result tryPlaceDiamond(Vector3 pos, float mapHeightWorld) {
        float dOffX = diamondLayer.getOffsetX() * unitScale;
        float dOffY = diamondLayer.getOffsetY() * unitScale;

        for (MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;

            float objW   = tmo.getProperties().get("width",  Float.class) * unitScale;
            float objH   = tmo.getProperties().get("height", Float.class) * unitScale;
            float worldX = tmo.getX() * unitScale + dOffX;
            // +2*objH+4f corrects for the y-flip and a small visual alignment offset
            float worldY = mapHeightWorld - (tmo.getY() * unitScale) - dOffY + 2 * objH + 4f;

            if (!hitTest(pos, worldX, worldY, objW, objH)) continue;
            return placeDiamond(tmo);
        }
        return null; // no diamond was hit
    }

    /**
     * applies a rhombus placement on the given TMO:
     * marks it occupied, updates the visual texture, updates the board model
     */
    private Result placeDiamond(TextureMapObject tmo) {
        if (tmo.getProperties().containsKey("occupied")) return Result.OCCUPIED;

        // derive the cell key from the TMO's pixel position
        int colGap = Math.round((tmo.getX() + 40) / 128f) - 6;
        int row    = 17 - Math.round((tmo.getY() - 40) / 128f);
        if (colGap < 0 || colGap > 9 || row < 2 || row > 11) return Result.NOT_A_CELL;

        String cellId = "R-" + (char)('A' + colGap) + row;
        if (!boardLogic.placeRhombus(cellId, gameState.getCurrentPlayer())) return Result.OCCUPIED;

        // update the visual texture to show the current player's colour
        int gid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
        tmo.setTextureRegion(map.getTileSets().getTile(gid).getTextureRegion());
        tmo.getProperties().put("occupied", true);

        return finalisePlacement();
    }

    // -------------------------------------------------------------------------
    // octagon helpers
    // -------------------------------------------------------------------------

    /** converts click position to tile-grid coordinates and tries to place a stone */
    private Result tryPlaceOctagon(Vector3 pos) {
        int cellX = (int)(pos.x / (octagonLayer.getTileWidth()  * unitScale));
        int cellY = (int)(pos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell == null || cell.getTile() == null) return Result.NOT_A_CELL;
        if (isOccupied(cell.getTile().getId()))     return Result.OCCUPIED;

        int targetGid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
        cell.setTile(map.getTileSets().getTile(targetGid));
        boardLogic.placeStone(deriveOctagonLabel(pos), gameState.getCurrentPlayer());

        return finalisePlacement();
    }

    /**
     * converts a world-space click position into an A1–K11 board label
     * the board occupies TMX tile columns 5–15 and rows 4–14
     */
    private String deriveOctagonLabel(Vector3 pos) {
        int mapH    = map.getProperties().get("height",     Integer.class);
        int tileHpx = map.getProperties().get("tileheight", Integer.class);
        float tmxPixelX = pos.x / unitScale;
        float tmxPixelY = (mapH * tileHpx) - (pos.y / unitScale);
        int boardCol = (int)(tmxPixelX / tileHpx) - 5;
        int boardRow = 15 - (int)(tmxPixelY / tileHpx);
        return QuaxBoard.generateLabel(boardCol, boardRow);
    }

    // -------------------------------------------------------------------------
    // shared post-placement logic
    // -------------------------------------------------------------------------

    /**
     * called after every successful placement:
     * marks the first move, checks for a win (BEFORE toggling), and toggles the turn
     */
    private Result finalisePlacement() {
        if (!gameState.isFirstMoveMade()) gameState.setFirstMoveMade();

        PlayerColour mover = gameState.getCurrentPlayer();
        if (winCheck.checkWin(mover)) {
            gameState.setWinner(mover);
            return Result.WIN;
        }

        gameState.togglePlayer();
        return Result.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // utilities
    // -------------------------------------------------------------------------

    private float getMapHeightWorld() {
        int mapH    = map.getProperties().get("height",     Integer.class);
        int tileHpx = map.getProperties().get("tileheight", Integer.class);
        return mapH * tileHpx * unitScale;
    }

    private Vector3 unprojectClick(int screenX, int screenY) {
        Vector3 pos = new Vector3(screenX, screenY, 0);
        viewport.unproject(pos);
        return pos;
    }

    private boolean hitTest(Vector3 pos, float x, float y, float w, float h) {
        return pos.x >= x && pos.x <= x + w && pos.y >= y && pos.y <= y + h;
    }

    private boolean isOccupied(int gid) {
        return gid == GID_BLACK_OCT || gid == GID_WHITE_OCT
            || gid == GID_BLACK_RHO || gid == GID_WHITE_RHO;
    }
}
