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

/**
 * Handles clicks on the game board (octagons and diamonds).
 */
public class InputHandler {
    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final GameState gameState;
    private final Viewport viewport;

    // tile IDs from the tileset
    private static final int GID_BLACK_RHO = 5;
    private static final int GID_WHITE_RHO = 4;
    private static final int GID_BLACK_OCT = 8;
    private static final int GID_WHITE_OCT = 7;

    public enum MoveResult {
        SUCCESS,        // move made, turn toggled
        OCCUPIED,       // cell already occupied
        NOT_A_CELL      // clicked on empty space
    }

    public InputHandler(TiledMap map, TiledMapTileLayer octagonLayer, MapLayer diamondLayer,
                        float unitScale, GameState gameState, Viewport viewport) {
        this.map = map;
        this.octagonLayer = octagonLayer;
        this.diamondLayer = diamondLayer;
        this.unitScale = unitScale;
        this.gameState = gameState;
        this.viewport = viewport;
    }

    /**
     * Processes a touch on the board.
     * @param screenX screen x coordinate
     * @param screenY screen y coordinate
     * @return MoveResult indicating what happened
     */
    public MoveResult handleBoardClick(int screenX, int screenY) {
        Vector3 touchPos = new Vector3(screenX, screenY, 0);
        viewport.unproject(touchPos);

        int mapHeightInTiles = map.getProperties().get("height", Integer.class);
        int tileHeightPx = map.getProperties().get("tileheight", Integer.class);
        float worldMapHeight = mapHeightInTiles * tileHeightPx * unitScale;

        // check diamonds first
        for (MapObject object : diamondLayer.getObjects()) {
            if (object instanceof TextureMapObject) {
                TextureMapObject tmo = (TextureMapObject) object;

                float objW = tmo.getProperties().get("width", Float.class) * unitScale;
                float objH = tmo.getProperties().get("height", Float.class) * unitScale;
                float worldX = tmo.getX() * unitScale;
                // adjust Y because Tiled uses top‑left origin, LibGDX uses bottom‑left
                float worldY = worldMapHeight - (tmo.getY() * unitScale) - objH;

                if (touchPos.x >= worldX && touchPos.x <= worldX + objW &&
                    touchPos.y >= worldY && touchPos.y <= worldY + objH) {

                    // occupied diamond?
                    if (tmo.getProperties().containsKey("occupied")) {
                        return MoveResult.OCCUPIED;
                    }

                    // place stone
                    int gid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_RHO : GID_WHITE_RHO;
                    tmo.setTextureRegion(map.getTileSets().getTile(gid).getTextureRegion());
                    tmo.getProperties().put("occupied", true);
                    gameState.togglePlayer();
                    return MoveResult.SUCCESS;
                }
            }
        }

        // check octagons
        int cellX = (int) (touchPos.x / (octagonLayer.getTileWidth() * unitScale));
        int cellY = (int) (touchPos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell != null && cell.getTile() != null) {
            int currentGid = cell.getTile().getId();
            if (currentGid == GID_BLACK_OCT || currentGid == GID_WHITE_OCT ||
                currentGid == GID_BLACK_RHO || currentGid == GID_WHITE_RHO) {
                return MoveResult.OCCUPIED; // already occupied
            }

            int targetGid = (gameState.getCurrentPlayer() == PlayerColour.BLACK) ? GID_BLACK_OCT : GID_WHITE_OCT;
            cell.setTile(map.getTileSets().getTile(targetGid));
            gameState.togglePlayer();
            return MoveResult.SUCCESS;
        }
        return MoveResult.NOT_A_CELL;
    }
}
