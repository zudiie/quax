package src.softies.board;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

// detects which board cell the mouse is currently hovering over and builds the polygon
// vertex array for that cell so Main can draw a shape-accurate highlight overlay
// supports two cell shapes: octagonal (8-vertex polygon) and rhombic (4-vertex diamond)
public class HoverDetector {

    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final Viewport viewport;

    // GIDs that represent playable octagon tiles — blank surrounds have different IDs
    private static final int GID_EMPTY_OCT = 5;
    private static final int GID_BLACK_OCT = 6;
    private static final int GID_WHITE_OCT = 7;

    // fraction of each tile dimension clipped at corners to approximate the octagon shape
    // increase if the highlight outline doesn't align with the tile art
    private static final float OCT_CUT = 0.25f;

    // what shape is under the mouse — NONE when not hovering any playable cell
    public enum HoverShape { NONE, OCTAGON, RHOMBUS }
    private HoverShape hoverShape    = HoverShape.NONE;

    // flat [x0,y0, x1,y1, ...] vertex array ready for ShapeRenderer.polygon()
    private float[]    hoverVertices = new float[0];

    /**
     * @param map          the loaded TiledMap — used to read height/tileheight properties
     * @param octagonLayer the octagon tile layer
     * @param diamondLayer the diamond (rhombus) object layer
     * @param unitScale    pixel-to-world scale factor (0.25f)
     * @param viewport     used to convert screen coords to world coords
     */
    public HoverDetector(TiledMap map, TiledMapTileLayer octagonLayer,
                         MapLayer diamondLayer, float unitScale, Viewport viewport) {
        this.map          = map;
        this.octagonLayer = octagonLayer;
        this.diamondLayer = diamondLayer;
        this.unitScale    = unitScale;
        this.viewport     = viewport;
    }

    /**
     * updates the hover state from the current mouse position
     * call once per frame before reading getHoverShape() / getHoverVertices()
     * @param screenX raw screen x from Gdx.input.getX()
     * @param screenY raw screen y from Gdx.input.getY()
     */
    public void update(int screenX, int screenY) {
        hoverShape    = HoverShape.NONE;
        hoverVertices = new float[0];

        Vector3 pos = unprojectMouse(screenX, screenY);
        if (checkDiamondHover(pos)) return; // diamonds checked first — they're on top visually
        checkOctagonHover(pos);
    }

    // --- getters ---

    /** @return the shape type currently under the mouse, or NONE */
    public HoverShape getHoverShape()    { return hoverShape; }

    /** @return polygon vertices for the hovered cell, ready for ShapeRenderer.polygon() */
    public float[]    getHoverVertices() { return hoverVertices; }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    /** converts raw screen coordinates into world space */
    private Vector3 unprojectMouse(int screenX, int screenY) {
        Vector3 pos = new Vector3(screenX, screenY, 0);
        viewport.unproject(pos);
        return pos;
    }

    /**
     * checks whether pos falls inside any diamond object and sets hover state if so
     * @return true if a diamond was hit
     */
    private boolean checkDiamondHover(Vector3 pos) {
        int mapH    = map.getProperties().get("height",     Integer.class);
        int tileHpx = map.getProperties().get("tileheight", Integer.class);
        float mapHeightWorld = mapH * tileHpx * unitScale;

        float dOffX = diamondLayer.getOffsetX() * unitScale;
        float dOffY = diamondLayer.getOffsetY() * unitScale;

        for (MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;

            float w  = tmo.getProperties().get("width",  Float.class) * unitScale;
            float h  = tmo.getProperties().get("height", Float.class) * unitScale;
            float wx = tmo.getX() * unitScale + dOffX;
            // y-flip correction matching the diamond rendering formula
            float wy = mapHeightWorld - (tmo.getY() * unitScale) - dOffY + 2 * h + 4f;

            if (pos.x >= wx && pos.x <= wx + w && pos.y >= wy && pos.y <= wy + h) {
                hoverShape    = HoverShape.RHOMBUS;
                hoverVertices = buildDiamondVertices(wx, wy, w, h);
                return true;
            }
        }
        return false;
    }

    /**
     * checks whether pos falls on a playable octagon tile and sets hover state if so
     */
    private void checkOctagonHover(Vector3 pos) {
        int cellX = (int)(pos.x / (octagonLayer.getTileWidth()  * unitScale));
        int cellY = (int)(pos.y / (octagonLayer.getTileHeight() * unitScale));

        TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
        if (cell == null || cell.getTile() == null) return;

        int gid = cell.getTile().getId();
        // only highlight tiles that are part of the playable board
        if (gid != GID_EMPTY_OCT && gid != GID_BLACK_OCT && gid != GID_WHITE_OCT) return;

        float wx = cellX * octagonLayer.getTileWidth()  * unitScale;
        float wy = cellY * octagonLayer.getTileHeight() * unitScale;
        float w  = octagonLayer.getTileWidth()  * unitScale;
        float h  = octagonLayer.getTileHeight() * unitScale;

        hoverShape    = HoverShape.OCTAGON;
        hoverVertices = buildOctagonVertices(wx, wy, w, h);
    }

    /**
     * builds the 8-point octagon polygon by clipping OCT_CUT of each corner
     * @return flat [x0,y0, x1,y1, ...] for ShapeRenderer.polygon()
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
     * builds the 4-point diamond polygon from the midpoints of the bounding box
     * @return flat [x0,y0, x1,y1, ...] for ShapeRenderer.polygon()
     */
    private float[] buildDiamondVertices(float x, float y, float w, float h) {
        return new float[]{
            x + w / 2, y,
            x + w,     y + h / 2,
            x + w / 2, y + h,
            x,         y + h / 2
        };
    }
}
