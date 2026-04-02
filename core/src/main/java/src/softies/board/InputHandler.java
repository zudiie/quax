package src.softies.board;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.PlayerColour;
import src.softies.QuaxBoard;

// translates raw screen clicks into board moves
// checks both the octagon layer and the diamond (rhombus) object layer for hits
public class InputHandler {

    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final GameState gameState;
    private final Viewport viewport;
    private final QuaxBoard boardLogic;

    // references to the first placed tile so pie rule can swap its colour
    private TiledMapTileLayer.Cell firstOctagonCell = null;
    private TextureMapObject firstRhombusTMO = null;

    // tile GIDs from the tileset — these correspond to specific tile images
    private static final int GID_EMPTY_RHO = 2;
    private static final int GID_WHITE_RHO = 3;
    private static final int GID_BLACK_RHO = 4;
    private static final int GID_EMPTY_OCT = 5;
    private static final int GID_WHITE_OCT = 6;
    private static final int GID_BLACK_OCT = 7;

    // the possible outcomes of a move attempt
    public enum MoveResult {
        SUCCESS,            // move went through and turn was toggled
        OCCUPIED,           // the clicked cell was already taken
        NOT_A_CELL,         // the click didn't land on any valid cell
        INVALID_PLACEMENT   // rhombus placement wasn't between two valid stones
    }

    /**
     * creates the input handler with references to the map layers, game state and board logic
     * @param map the loaded TiledMap (used to look up tiles and properties)
     * @param octagonLayer the tile layer where octagonal cells live
     * @param diamondLayer the object layer where rhombus TextureMapObjects are stored
     * @param unitScale the same scale factor used everywhere else (0.25f)
     * @param gameState tracks whose turn it is and lets us toggle after a valid move
     * @param viewport used to convert screen coords to world coords
     * @param boardLogic the board model that actually records placement state
     */
    public InputHandler(TiledMap map, TiledMapTileLayer octagonLayer, MapLayer diamondLayer,
                        float unitScale, GameState gameState, Viewport viewport, QuaxBoard boardLogic) {
        this.map = map;
        this.octagonLayer = octagonLayer;
        this.diamondLayer = diamondLayer;
        this.unitScale = unitScale;
        this.gameState = gameState;
        this.viewport = viewport;
        this.boardLogic = boardLogic;
    }

    /**
     * figures out what (if anything) the player clicked on and applies the move
     * checks the diamond layer first, then falls through to octagon tile detection
     * @param screenX raw screen x from Gdx.input.getX()
     * @param screenY raw screen y from Gdx.input.getY()
     * @return a MoveResult indicating what happened
     */
    public MoveResult handleBoardClick(int screenX, int screenY) {
        // convert screen pixel coords to world coords for hit testing
        Vector3 touchPos = new Vector3(screenX, screenY, 0);
        viewport.unproject(touchPos);

        // we need the map height to flip the y-axis (tiled uses top-down, libgdx uses bottom-up)
        int mapHeightInTiles = map.getProperties().get("height", Integer.class);
        int tileHeightPx     = map.getProperties().get("tileheight", Integer.class);
        float worldMapHeight = mapHeightInTiles * tileHeightPx * unitScale;

        // grab the diamond layer's own offset in case it's shifted relative to the octagon layer
        float dOffsetX = diamondLayer.getOffsetX() * unitScale;
        float dOffsetY = diamondLayer.getOffsetY() * unitScale;

        // --- diamond (rhombus) hit detection ---
        for (MapObject object : diamondLayer.getObjects()) {
            if (!(object instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) object;

            // scale the object's pixel dimensions and position into world space
            float objW   = tmo.getProperties().get("width",  Float.class) * unitScale;
            float objH   = tmo.getProperties().get("height", Float.class) * unitScale;
            float worldX = tmo.getX() * unitScale + dOffsetX;
            // the +2*objH+4f corrects for the y-flip and a small visual offset
            float worldY = worldMapHeight - (tmo.getY() * unitScale) - dOffsetY + 2 * objH + 4f;

            // skip this object if the click landed outside its bounding box
            if (touchPos.x < worldX || touchPos.x > worldX + objW ||
                touchPos.y < worldY || touchPos.y > worldY + objH) continue;

            // already claimed — don't let them place again
            if (tmo.getProperties().containsKey("occupied")) return MoveResult.OCCUPIED;

            // derive the cell key from the object's pixel position rather than a stored property
            int colGap = Math.round((tmo.getX() + 40) / 128f) - 6;
            int row    = 16 - Math.round((tmo.getY() - 40) / 128f);

            // out-of-range coordinates mean the click hit a non-playable diamond
            if (colGap < 0 || colGap > 9 || row < 2 || row > 11) return MoveResult.NOT_A_CELL;
            String cellId = "R-" + (char) ('A' + colGap) + row;

            // try to record the placement in the board model
            if (!boardLogic.placeRhombus(cellId, gameState.getCurrentPlayer()))
                return MoveResult.OCCUPIED;

            // update the tile graphic to show the current player's colour
            int gid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
            tmo.setTextureRegion(map.getTileSets().getTile(gid).getTextureRegion());
            tmo.getProperties().put("occupied", true);

            if (!gameState.isFirstMoveMade()) {
                firstRhombusTMO = tmo;
                gameState.setFirstMoveMade();
            }

            gameState.togglePlayer();
            return MoveResult.SUCCESS;
        }

        // --- octagon tile hit detection ---
        // convert world position to tile grid coordinates
        int cellX = (int) (touchPos.x / (octagonLayer.getTileWidth()  * unitScale));
        int cellY = (int) (touchPos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell != null && cell.getTile() != null) {
            int currentGid = cell.getTile().getId();

            // if any coloured tile is already here, the cell is taken
            if (currentGid == GID_BLACK_OCT || currentGid == GID_WHITE_OCT ||
                currentGid == GID_BLACK_RHO || currentGid == GID_WHITE_RHO) {
                return MoveResult.OCCUPIED;
            }

            // swap the tile to the current player's coloured version and pass the turn
            int targetGid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
            cell.setTile(map.getTileSets().getTile(targetGid));

            if (!gameState.isFirstMoveMade()) {
                firstOctagonCell = cell;
                gameState.setFirstMoveMade();
            }

            gameState.togglePlayer();
            return MoveResult.SUCCESS;
        }

        // nothing was hit
        return MoveResult.NOT_A_CELL;
    }

    /**
     * swaps the colour of the first placed tile — called when the pie rule is activated
     * so the tile visually reflects the new owner
     */
    public void swapFirstTileColour() {
        if (firstOctagonCell != null) {
            int currentGid = firstOctagonCell.getTile().getId();
            int newGid = (currentGid == GID_BLACK_OCT) ? GID_WHITE_OCT : GID_BLACK_OCT;
            firstOctagonCell.setTile(map.getTileSets().getTile(newGid));
        }
        if (firstRhombusTMO != null) {
            int newGid = GID_WHITE_RHO;
            firstRhombusTMO.setTextureRegion(map.getTileSets().getTile(newGid).getTextureRegion());
        }
    }
}
