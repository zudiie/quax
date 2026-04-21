package src.softies.board;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.QuaxBoard;
import src.softies.WinCheck;

// coordinates the two board-input subsystems:
//   HoverDetector — tracks which cell is under the mouse each frame
//   MoveHandler   — processes clicks, updates the model and tiles, checks for a win
// also exposes placeBotMove() so Main can apply a bot decision programmatically
public class InputHandler {

<<<<<<< HEAD
    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final GameState gameState;
    private final Viewport viewport;
    private final QuaxBoard boardLogic;
    private final WinCheck winCheck;
=======
    private final HoverDetector hoverDetector;
    private final MoveHandler   moveHandler;
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344

    // re-export the result enum so callers only need to import InputHandler
    public enum MoveResult {
<<<<<<< HEAD
        SUCCESS,            // move went through and turn was toggled
        WIN,                // move went through and the player has won
        OCCUPIED,           // the clicked cell was already taken
        NOT_A_CELL,         // the click didn't land on any valid cell
        INVALID_PLACEMENT   // rhombus placement wasn't between two valid stones
=======
        SUCCESS, WIN, OCCUPIED, NOT_A_CELL, INVALID_PLACEMENT
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
    }

    /**
     * @param map          the loaded TiledMap
     * @param octagonLayer tile layer where octagonal cells live
     * @param diamondLayer object layer where rhombus TextureMapObjects are stored
     * @param unitScale    pixel-to-world scale (0.25f)
     * @param gameState    current player, first-move flag and winner
     * @param viewport     converts screen coords to world coords
     * @param boardLogic   the board model
     */
    public InputHandler(TiledMap map, TiledMapTileLayer octagonLayer, MapLayer diamondLayer,
                        float unitScale, GameState gameState, Viewport viewport, QuaxBoard boardLogic) {
<<<<<<< HEAD
        this.map = map;
        this.octagonLayer = octagonLayer;
        this.diamondLayer = diamondLayer;
        this.unitScale = unitScale;
        this.gameState = gameState;
        this.viewport = viewport;
        this.boardLogic = boardLogic;
        this.winCheck = new WinCheck(boardLogic);
=======
        hoverDetector = new HoverDetector(map, octagonLayer, diamondLayer, unitScale, viewport);
        moveHandler   = new MoveHandler(map, octagonLayer, diamondLayer, unitScale, gameState, viewport, boardLogic);
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
    }

    // -------------------------------------------------------------------------
    // hover
    // -------------------------------------------------------------------------

    /**
     * updates the hover polygon from the current mouse position — call once per frame
     * @param screenX raw x from Gdx.input.getX()
     * @param screenY raw y from Gdx.input.getY()
     */
    public void updateHover(int screenX, int screenY) {
        hoverDetector.update(screenX, screenY);
    }

    /** @return the shape type under the mouse (NONE, OCTAGON, or RHOMBUS) */
    public HoverDetector.HoverShape getHoverShape()    { return hoverDetector.getHoverShape(); }

    /** @return polygon vertices for the hovered cell, ready for ShapeRenderer.polygon() */
    public float[]                  getHoverVertices() { return hoverDetector.getHoverVertices(); }

    // -------------------------------------------------------------------------
    // human click
    // -------------------------------------------------------------------------

    /**
     * processes a screen click and attempts to make a board move
     * @param screenX raw x from Gdx.input.getX()
     * @param screenY raw y from Gdx.input.getY()
     * @return a MoveResult indicating what happened
     */
    public MoveResult handleBoardClick(int screenX, int screenY) {
<<<<<<< HEAD
        // convert screen pixel coords to world coords for hit testing
        Vector3 touchPos = new Vector3(screenX, screenY, 0);
        viewport.unproject(touchPos);

        // read map dimensions — used for Y-axis flipping and coordinate conversion
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
            int row    = 17 - Math.round((tmo.getY() - 40) / 128f);

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

            // check for win before toggling the turn
            PlayerColour mover = gameState.getCurrentPlayer();
            if (winCheck.checkWin(mover)) {
                gameState.togglePlayer();
                return MoveResult.WIN;
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

            // convert world position to TMX pixel space to derive the board label
            // this uses the same coordinate system as the rhombus placement code above
            // tiles are square so tileHeightPx works for both dimensions
            int mapHeightPx = mapHeightInTiles * tileHeightPx;
            float tmxPixelX = touchPos.x / unitScale;
            float tmxPixelY = mapHeightPx - (touchPos.y / unitScale);
            int boardCol = (int) (tmxPixelX / tileHeightPx) - 5;
            int boardRow = 15 - (int) (tmxPixelY / tileHeightPx);

            String cellLabel = QuaxBoard.generateLabel(boardCol, boardRow);
            boardLogic.placeStone(cellLabel, gameState.getCurrentPlayer());

            if (!gameState.isFirstMoveMade()) {
                firstOctagonCell = cell;
                gameState.setFirstMoveMade();
            }

            // check for win before toggling the turn
            PlayerColour mover = gameState.getCurrentPlayer();
            if (winCheck.checkWin(mover)) {
                gameState.togglePlayer();
                return MoveResult.WIN;
            }

            gameState.togglePlayer();
            return MoveResult.SUCCESS;
        }

        // nothing was hit
        return MoveResult.NOT_A_CELL;
=======
        return mapResult(moveHandler.handleClick(screenX, screenY));
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
    }

    // -------------------------------------------------------------------------
    // bot move — programmatic placement without a screen click
    // -------------------------------------------------------------------------

    /**
     * places a stone programmatically for the bot at the given board cell label
     * called by Main when the bot's think timer expires
     * @param cellLabel board label such as "F6"
     * @return a MoveResult indicating what happened
     */
    public MoveResult placeBotMove(String cellLabel) {
        return mapResult(moveHandler.placeBotMove(cellLabel));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /** maps MoveHandler.Result → MoveResult so callers use a single enum */
    private MoveResult mapResult(MoveHandler.Result r) {
        switch (r) {
            case WIN:               return MoveResult.WIN;
            case SUCCESS:           return MoveResult.SUCCESS;
            case OCCUPIED:          return MoveResult.OCCUPIED;
            case INVALID_PLACEMENT: return MoveResult.INVALID_PLACEMENT;
            default:                return MoveResult.NOT_A_CELL;
        }
    }
}
