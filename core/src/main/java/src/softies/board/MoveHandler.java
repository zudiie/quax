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

// processes board clicks and bot moves
// updates the board model and the visual tiles, then checks for a win
public class MoveHandler {

    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final GameState gameState;
    private final Viewport viewport;
    private final QuaxBoard boardLogic;
    private final WinCheck winCheck;

    // pre-built lookup: rhombus board key (e.g. "R-F6") -> the diamond TextureMapObject
    // built once in the constructor so bot placement never has to scan the full layer
    private final Map<String, TextureMapObject> rhombusKeyToTmo = new HashMap<>();

    private static final int GID_EMPTY_OCT = 5;
    private static final int GID_WHITE_OCT = 6;
    private static final int GID_BLACK_OCT = 7;
    private static final int GID_WHITE_RHO = 3;
    private static final int GID_BLACK_RHO = 4;

    public enum Result {
        SUCCESS,
        WIN,
        OCCUPIED,
        NOT_A_CELL,
        INVALID_PLACEMENT
    }

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
        buildRhombusKeyMap();
    }

    private void buildRhombusKeyMap() {
        for (MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;
            int colGap = Math.round((tmo.getX() + 40) / 128f) - 6;
            int row    = 17 - Math.round((tmo.getY() - 40) / 128f);
            if (colGap < 0 || colGap > 9 || row < 2 || row > 11) continue;
            String key = "R-" + (char)('A' + colGap) + row;
            rhombusKeyToTmo.put(key, tmo);
        }
        System.out.println("MoveHandler: mapped " + rhombusKeyToTmo.size() + " rhombus TMOs.");
    }

    /** processes a raw screen click - tries diamond layer first, then octagon tiles */
    public Result handleClick(int screenX, int screenY) {
        if (gameState.isGameOver()) return Result.NOT_A_CELL;
        Vector3 pos = unprojectClick(screenX, screenY);
        Result diamondResult = tryPlaceDiamond(pos, getMapHeightWorld());
        if (diamondResult != null) return diamondResult;
        return tryPlaceOctagon(pos);
    }

    /**
     * places a stone or rhombus for the bot using a board label
     * labels starting with "R-" are rhombus placements; all others are octagonal
     */
    public Result placeBotMove(String cellLabel) {
        if (gameState.isGameOver()) return Result.NOT_A_CELL;
        if (cellLabel == null || cellLabel.isEmpty()) return Result.NOT_A_CELL;
        return cellLabel.startsWith("R-") ? placeBotRhombus(cellLabel) : placeBotOctagon(cellLabel);
    }

    private Result placeBotOctagon(String cellLabel) {
        if (cellLabel.startsWith("R-")) {
            System.err.println("placeBotOctagon received rhombus label: " + cellLabel);
            return Result.NOT_A_CELL;
        }
        int[] coords = parseBoardLabelToCoords(cellLabel);
        if (coords == null) return Result.NOT_A_CELL;
        return placeOctagonAtBoardCoords(cellLabel, coords[0], coords[1]);
    }

    private int[] parseBoardLabelToCoords(String cellLabel) {
        try {
            int col = cellLabel.charAt(0) - 'A';
            int row = Integer.parseInt(cellLabel.substring(1));
            return new int[]{col, row};
        } catch (Exception e) {
            System.err.println("parseBoardLabelToCoords: bad label format: " + cellLabel);
            return null;
        }
    }

    private Result placeOctagonAtBoardCoords(String cellLabel, int boardCol, int boardRow) {
        int mapH  = getMapHeight();
        int cellX = boardCol + 5;
        int cellY = mapH - 16 + boardRow;
        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell == null || cell.getTile() == null) return Result.NOT_A_CELL;
        if (isOccupied(cell.getTile().getId()))     return Result.OCCUPIED;
        int gid = currentPlayerOctagonGid();
        cell.setTile(map.getTileSets().getTile(gid));
        boardLogic.placeStone(cellLabel, gameState.getCurrentPlayer());
        return finalisePlacement();
    }

    private Result placeBotRhombus(String rhombusKey) {
        TextureMapObject tmo = rhombusKeyToTmo.get(rhombusKey);
        if (tmo == null) {
            System.err.println("placeBotRhombus: no TMO found for key: " + rhombusKey);
            return Result.NOT_A_CELL;
        }
        return placeDiamond(tmo);
    }

    private Result tryPlaceDiamond(Vector3 pos, float mapHeightWorld) {
        float dOffX = diamondLayer.getOffsetX() * unitScale;
        float dOffY = diamondLayer.getOffsetY() * unitScale;
        for (MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;
            float objW   = tmo.getProperties().get("width",  Float.class) * unitScale;
            float objH   = tmo.getProperties().get("height", Float.class) * unitScale;
            float worldX = tmo.getX() * unitScale + dOffX;
            float worldY = mapHeightWorld - (tmo.getY() * unitScale) - dOffY + 2 * objH + 4f;
            if (!hitTest(pos, worldX, worldY, objW, objH)) continue;
            return placeDiamond(tmo);
        }
        return null;
    }

    private Result placeDiamond(TextureMapObject tmo) {
        if (tmo.getProperties().containsKey("occupied")) return Result.OCCUPIED;
        int colGap = Math.round((tmo.getX() + 40) / 128f) - 6;
        int row    = 17 - Math.round((tmo.getY() - 40) / 128f);
        if (colGap < 0 || colGap > 9 || row < 2 || row > 11) return Result.NOT_A_CELL;
        String cellId = "R-" + (char)('A' + colGap) + row;
        if (!boardLogic.placeRhombus(cellId, gameState.getCurrentPlayer())) return Result.OCCUPIED;
        int gid = currentPlayerRhombusGid();
        tmo.setTextureRegion(map.getTileSets().getTile(gid).getTextureRegion());
        tmo.getProperties().put("occupied", true);
        return finalisePlacement();
    }

    private Result tryPlaceOctagon(Vector3 pos) {
        int cellX = (int)(pos.x / (octagonLayer.getTileWidth()  * unitScale));
        int cellY = (int)(pos.y / (octagonLayer.getTileHeight() * unitScale));
        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell == null || cell.getTile() == null) return Result.NOT_A_CELL;
        if (isOccupied(cell.getTile().getId()))     return Result.OCCUPIED;
        cell.setTile(map.getTileSets().getTile(currentPlayerOctagonGid()));
        boardLogic.placeStone(deriveOctagonLabel(pos), gameState.getCurrentPlayer());
        return finalisePlacement();
    }

    private String deriveOctagonLabel(Vector3 pos) {
        int mapH    = getMapHeight();
        int tileHpx = map.getProperties().get("tileheight", Integer.class);
        float tmxPixelX = pos.x / unitScale;
        float tmxPixelY = (mapH * tileHpx) - (pos.y / unitScale);
        int boardCol = (int)(tmxPixelX / tileHpx) - 5;
        int boardRow = 15 - (int)(tmxPixelY / tileHpx);
        return QuaxBoard.generateLabel(boardCol, boardRow);
    }

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

    private int currentPlayerOctagonGid() {
        return (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
    }

    private int currentPlayerRhombusGid() {
        return (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
    }

    private int getMapHeight() {
        return map.getProperties().get("height", Integer.class);
    }

    private float getMapHeightWorld() {
        int mapH    = getMapHeight();
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
