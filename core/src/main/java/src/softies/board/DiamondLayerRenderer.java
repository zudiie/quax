package src.softies.board;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;

// renders the diamond (rhombus) object layer to the screen
// the object layer does not auto-render like tile layers do, so this must be called each frame
// the y-axis is flipped because Tiled uses top-down coordinates but libgdx uses bottom-up
public class DiamondLayerRenderer {

    private final TiledMap map;
    private final float unitScale;

    /**
     * @param map       the loaded TiledMap — used to read map dimensions and the diamond layer
     * @param unitScale pixel-to-world scale factor (0.25f)
     */
    public DiamondLayerRenderer(TiledMap map, float unitScale) {
        this.map       = map;
        this.unitScale = unitScale;
    }

    /**
     * draws every diamond TextureMapObject using its current texture region
     * the texture region changes when a player places a rhombus connector
     * call this every frame between batch.begin() and batch.end()
     * @param batch the active SpriteBatch
     */
    public void render(SpriteBatch batch) {
        MapLayer dl = map.getLayers().get("Diamonds");
        if (dl == null) return;

        float mapHeightWorld = computeMapHeight();
        float offsetX = dl.getOffsetX() * unitScale;
        float offsetY = dl.getOffsetY() * unitScale;

        for (MapObject obj : dl.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            drawDiamond(batch, (TextureMapObject) obj, mapHeightWorld, offsetX, offsetY);
        }
    }

    /**
     * returns the total map height in world units
     * needed to convert Tiled's top-down y-coordinate into libgdx's bottom-up y
     */
    private float computeMapHeight() {
        int mapH    = map.getProperties().get("height",     Integer.class);
        int tileHpx = map.getProperties().get("tileheight", Integer.class);
        return mapH * tileHpx * unitScale;
    }

    /**
     * draws a single TextureMapObject diamond at its world-space position
     * the +2*h+4f offset corrects the visual position to align with the tile grid
     */
    private void drawDiamond(SpriteBatch batch, TextureMapObject tmo,
                             float mapHeightWorld, float offsetX, float offsetY) {
        float w = tmo.getProperties().get("width",  Float.class) * unitScale;
        float h = tmo.getProperties().get("height", Float.class) * unitScale;
        float x = tmo.getX() * unitScale + offsetX;
        float y = mapHeightWorld - (tmo.getY() * unitScale) - offsetY + 2 * h + 4f;
        batch.draw(tmo.getTextureRegion(), x, y, w, h);
    }
}
