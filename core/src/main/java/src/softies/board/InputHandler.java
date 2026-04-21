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

    private final HoverDetector hoverDetector;
    private final MoveHandler   moveHandler;

    // re-export the result enum so callers only need to import InputHandler
    public enum MoveResult {
        SUCCESS, WIN, OCCUPIED, NOT_A_CELL, INVALID_PLACEMENT
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
        hoverDetector = new HoverDetector(map, octagonLayer, diamondLayer, unitScale, viewport);
        moveHandler   = new MoveHandler(map, octagonLayer, diamondLayer, unitScale, gameState, viewport, boardLogic);
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
        return mapResult(moveHandler.handleClick(screenX, screenY));
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
