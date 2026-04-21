package src.softies.board;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

import src.softies.BotPlayer;
import src.softies.GameMode;
import src.softies.QuaxBoard;

import java.util.Map;

// toggleable heat-map showing how the bot rates every empty cell at the current turn
// only visible in Human-vs-Bot mode; button sits left of the pie rule button (when up) or left of quit
// ratings come from BotPlayer.rateAllMoves() and are cached until invalidate() is called
public class BotStrategyWidget {

    private final BotPlayer botPlayer;
    private final GameState gameState;
    private final TiledMap map;
    private final TiledMapTileLayer octagonLayer;
    private final MapLayer diamondLayer;
    private final float unitScale;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    private boolean enabled = false;
    private Rectangle bounds = null;
    private Map<String, Double> cachedRatings = null;
    private boolean dirty = true;

    // must match HoverDetector / MoveHandler
    private static final int GID_EMPTY_OCT = 5;
    private static final float OCT_CUT = 0.25f;

    // button theme matches QuitWidget / PieRuleWidget
    private static final Color BTN_IDLE  = new Color(0.14f, 0.24f, 0.62f, 1f);
    private static final Color BTN_HOVER = new Color(0.22f, 0.36f, 0.80f, 1f);
    private static final Color GOLD      = new Color(0.82f, 0.67f, 0.12f, 1f);

    private static final float OVERLAY_ALPHA = 0.28f;

    private static final float BTN_W   = 190f;
    private static final float BTN_H   = 44f;
    private static final float BTN_GAP = 14f;

    public BotStrategyWidget(BotPlayer botPlayer, GameState gameState,
                             TiledMap map, TiledMapTileLayer octagonLayer, MapLayer diamondLayer,
                             float unitScale, Viewport viewport, OrthographicCamera camera) {
        this.botPlayer    = botPlayer;
        this.gameState    = gameState;
        this.map          = map;
        this.octagonLayer = octagonLayer;
        this.diamondLayer = diamondLayer;
        this.unitScale    = unitScale;
        this.viewport     = viewport;
        this.camera       = camera;
    }

    /** called after every board placement so the cache is recomputed on the next draw */
    public void invalidate() {
        dirty = true;
    }

    /**
     * recomputes the button rectangle — call every frame before draw()
     * the button is hidden while the welcome screen is up, in Human-vs-Human mode,
     * or once the game is over
     */
    public void updateBounds(boolean showWelcome) {
        if (showWelcome
         || gameState.getGameMode() != GameMode.HUMAN_VS_BOT
         || gameState.isGameOver()) {
            bounds = null;
            return;
        }
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
        // quit button left edge (quit is 90 wide, 20px from right)
        float quitLeft = worldRight - 90 - 20;
        // if pie rule button is up, the bot-strategy button sits to its left; otherwise beside quit
        float anchorLeft = gameState.isPieRuleAvailable()
            ? quitLeft - BTN_W - BTN_GAP
            : quitLeft;
        bounds = new Rectangle(anchorLeft - BTN_W - BTN_GAP, worldBottom + 20, BTN_W, BTN_H);
    }

    /** draws the toggle button — manages batch begin/end internally */
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (bounds == null) return;

        Vector3 mouse = getMouseWorldPos();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(camera.combined);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(bounds.contains(mouse.x, mouse.y) ? BTN_HOVER : BTN_IDLE);
        sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD);
        sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        String label = enabled ? "Hide Strategy" : "Show Strategy";
        GlyphLayout gl = new GlyphLayout(font, label);
        font.draw(batch, label,
            bounds.x + (bounds.width  - gl.width)  / 2f,
            bounds.y + (bounds.height + gl.height) / 2f);
        batch.end();
    }

    /**
     * draws the translucent heat-map overlay over every non-occupied cell
     * no-op when the toggle is off
     */
    public void drawOverlay(ShapeRenderer sr) {
        if (!enabled) return;

        if (dirty || cachedRatings == null) {
            cachedRatings = botPlayer.rateAllMoves();
            dirty = false;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(camera.combined);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        drawOctagonOverlays(sr);
        drawRhombusOverlays(sr);
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** processes a click — flips the toggle if the button was hit */
    public boolean handleInput(Vector3 touchPos) {
        if (bounds == null) return false;
        if (!bounds.contains(touchPos.x, touchPos.y)) return false;
        enabled = !enabled;
        return true;
    }

    // -------------------------------------------------------------------------
    // overlay rendering
    // -------------------------------------------------------------------------

    private void drawOctagonOverlays(ShapeRenderer sr) {
        int mapH = map.getProperties().get("height", Integer.class);
        float tileW = octagonLayer.getTileWidth()  * unitScale;
        float tileH = octagonLayer.getTileHeight() * unitScale;

        for (int cellX = 0; cellX < octagonLayer.getWidth(); cellX++) {
            for (int cellY = 0; cellY < octagonLayer.getHeight(); cellY++) {
                TiledMapTileLayer.Cell cell = octagonLayer.getCell(cellX, cellY);
                if (cell == null || cell.getTile() == null) continue;
                if (cell.getTile().getId() != GID_EMPTY_OCT) continue;

                // inverse of MoveHandler.placeBotOctagon tile→label mapping
                int boardCol = cellX - 5;
                int boardRow = cellY - mapH + 16;
                if (boardCol < 0 || boardCol >= 11 || boardRow < 1 || boardRow > 11) continue;

                String label = QuaxBoard.generateLabel(boardCol, boardRow);
                Double rating = cachedRatings.get(label);
                if (rating == null) continue;

                float wx = cellX * tileW;
                float wy = cellY * tileH;
                float[] verts = buildOctagonVertices(wx, wy, tileW, tileH);
                setGradientColor(sr, rating);
                fillPolygon(sr, verts);
            }
        }
    }

    private void drawRhombusOverlays(ShapeRenderer sr) {
        int mapH    = map.getProperties().get("height", Integer.class);
        int tileHpx = map.getProperties().get("tileheight", Integer.class);
        float mapHeightWorld = mapH * tileHpx * unitScale;

        float dOffX = diamondLayer.getOffsetX() * unitScale;
        float dOffY = diamondLayer.getOffsetY() * unitScale;

        for (MapObject obj : diamondLayer.getObjects()) {
            if (!(obj instanceof TextureMapObject)) continue;
            TextureMapObject tmo = (TextureMapObject) obj;
            if (tmo.getProperties().containsKey("occupied")) continue;

            // MoveHandler.buildRhombusKeyMap — the canonical key formula
            int colGap = Math.round((tmo.getX() + 40) / 128f) - 6;
            int row    = 17 - Math.round((tmo.getY() - 40) / 128f);
            if (colGap < 0 || colGap > 9 || row < 2 || row > 11) continue;
            String key = "R-" + (char)('A' + colGap) + row;

            Double rating = cachedRatings.get(key);
            if (rating == null) continue;

            float w  = tmo.getProperties().get("width",  Float.class) * unitScale;
            float h  = tmo.getProperties().get("height", Float.class) * unitScale;
            float wx = tmo.getX() * unitScale + dOffX;
            // y-flip + alignment offset — matches HoverDetector.checkDiamondHover
            float wy = mapHeightWorld - (tmo.getY() * unitScale) - dOffY + 2 * h + 4f;

            float[] verts = buildDiamondVertices(wx, wy, w, h);
            setGradientColor(sr, rating);
            fillPolygon(sr, verts);
        }
    }

    /** triangle-fan fill from the polygon centroid — mirrors Main.renderHoverOverlay */
    private void fillPolygon(ShapeRenderer sr, float[] verts) {
        int n = verts.length / 2;
        if (n < 3) return;
        float cx = 0, cy = 0;
        for (int i = 0; i < verts.length; i += 2) { cx += verts[i]; cy += verts[i + 1]; }
        cx /= n; cy /= n;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            sr.triangle(cx, cy,
                verts[i * 2],     verts[i * 2 + 1],
                verts[j * 2],     verts[j * 2 + 1]);
        }
    }

    /** translucent red→yellow→green gradient; rating is clamped to [0, 1] */
    private void setGradientColor(ShapeRenderer sr, double rating) {
        float t = (float) Math.max(0.0, Math.min(1.0, rating));
        float r, g;
        if (t < 0.5f) { r = 1f;          g = t * 2f; }
        else          { r = 2f - t * 2f; g = 1f;     }
        sr.setColor(r, g, 0f, OVERLAY_ALPHA);
    }

    /** same 8-point octagon as HoverDetector.buildOctagonVertices */
    private static float[] buildOctagonVertices(float x, float y, float w, float h) {
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

    /** same 4-point diamond as HoverDetector.buildDiamondVertices */
    private static float[] buildDiamondVertices(float x, float y, float w, float h) {
        return new float[]{
            x + w / 2, y,
            x + w,     y + h / 2,
            x + w / 2, y + h,
            x,         y + h / 2
        };
    }

    private Vector3 getMouseWorldPos() {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);
        return mouse;
    }
}
