package src.softies.board;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

// the in-game quit button (bottom-right) and the shared confirmation dialog
// the dialog is also used by the welcome screen quit button via triggerConfirm()
// No (cancel) is always on the LEFT — dark red
// Yes (confirm quit) is always on the RIGHT — dark green
public class QuitWidget {

    private final WorldCalculator world;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    // when false the bottom-right quit button is hidden (welcome screen)
    private boolean visible = false;
    private boolean showConfirm = false;

    private Rectangle quitBounds;
    private Rectangle noBounds;   // LEFT — cancel
    private Rectangle yesBounds;  // RIGHT — confirm quit

    private final float offsetZ = 300f;

    // quit button: blue fill, gold border
    private static final Color BTN_IDLE    = new Color(0.14f, 0.24f, 0.62f, 1f);
    private static final Color BTN_HOVER   = new Color(0.22f, 0.36f, 0.80f, 1f);
    private static final Color GOLD        = new Color(0.82f, 0.67f, 0.12f, 1f);

    // dialog
    private static final Color DIALOG_BG   = new Color(0.10f, 0.18f, 0.52f, 0.97f);

    // No button — dark red
    private static final Color NO_IDLE     = new Color(0.38f, 0.07f, 0.07f, 1f);
    private static final Color NO_HOVER    = new Color(0.58f, 0.13f, 0.13f, 1f);

    // Yes button — dark green
    private static final Color YES_IDLE    = new Color(0.07f, 0.30f, 0.07f, 1f);
    private static final Color YES_HOVER   = new Color(0.13f, 0.48f, 0.13f, 1f);

    /**
     * @param world    for dialog centring coordinates
     * @param viewport for world-size queries and mouse unprojecting
     * @param camera   for the ShapeRenderer projection matrix
     */
    public QuitWidget(WorldCalculator world, Viewport viewport, OrthographicCamera camera) {
        this.world    = world;
        this.viewport = viewport;
        this.camera   = camera;
    }

    /** call with true once the game starts so the bottom-right quit button appears */
    public void setVisible(boolean visible) { this.visible = visible; }

    /** opens the confirmation dialog from outside (e.g. welcome screen quit button) */
    public void triggerConfirm() { showConfirm = true; }

    /**
     * recomputes button rectangles — call every frame before draw() and handleInput()
     */
    public void updateBounds() {
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;

        quitBounds = visible
            ? new Rectangle(worldRight - 90 - 20, worldBottom + 20, 90, 44)
            : null;

        if (showConfirm) {
            // dialog: 400 wide, 200 tall, centred in the right panel
            float dW = 400, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            // buttons: 150 wide, 52 tall with padding from the dialog edges
            float bW = 150, bH = 52, bY = dY + 36, pad = 24;
            noBounds  = new Rectangle(dX + pad,           bY, bW, bH);
            yesBounds = new Rectangle(dX + dW - bW - pad, bY, bW, bH);
        } else {
            noBounds = yesBounds = null;
        }
    }

    /**
     * draws the quit button and, when open, the full confirmation dialog overlay
     * @param shapeRenderer for backgrounds / borders
     * @param batch         for text (not active when called — managed internally)
     * @param font          font for all button and dialog text
     */
    public void draw(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);

        // --- quit button ---
        if (quitBounds != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(quitBounds.contains(mouse.x, mouse.y) ? BTN_HOVER : BTN_IDLE);
            shapeRenderer.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(GOLD);
            shapeRenderer.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);
            shapeRenderer.end();
        }

        // --- confirmation dialog ---
        if (showConfirm) {
            float dW = 400, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 150, bH = 52, bY = dY + 36, pad = 24;
            float noX  = dX + pad;
            float yesX = dX + dW - bW - pad;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // dim overlay behind dialog
            float wl = camera.position.x - viewport.getWorldWidth()  / 2;
            float wb = camera.position.y - viewport.getWorldHeight() / 2;
            shapeRenderer.setColor(0f, 0f, 0f, 0.70f);
            shapeRenderer.rect(wl, wb, viewport.getWorldWidth(), viewport.getWorldHeight());

            // dialog background
            shapeRenderer.setColor(DIALOG_BG);
            shapeRenderer.rect(dX, dY, dW, dH);

            // No button (left) — dark red
            boolean noH = noBounds != null && noBounds.contains(mouse.x, mouse.y);
            shapeRenderer.setColor(noH ? NO_HOVER : NO_IDLE);
            shapeRenderer.rect(noX, bY, bW, bH);

            // Yes button (right) — dark green
            boolean yH = yesBounds != null && yesBounds.contains(mouse.x, mouse.y);
            shapeRenderer.setColor(yH ? YES_HOVER : YES_IDLE);
            shapeRenderer.rect(yesX, bY, bW, bH);
            shapeRenderer.end();

            // borders: gold on dialog, white on buttons
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(GOLD);
            shapeRenderer.rect(dX, dY, dW, dH);
            shapeRenderer.setColor(1f, 1f, 1f, 0.50f);
            shapeRenderer.rect(noX,  bY, bW, bH);
            shapeRenderer.rect(yesX, bY, bW, bH);
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- text ---
        batch.begin();

        if (quitBounds != null) {
            font.setColor(Color.WHITE);
            drawCentred(batch, font, "Quit", quitBounds);
        }

        if (showConfirm) {
            float dW = 400, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 150, bH = 52, bY = dY + 36, pad = 24;
            float noX  = dX + pad;
            float yesX = dX + dW - bW - pad;

            // dialog title — centred
            font.setColor(Color.WHITE);
            String title = "Quit the game?";
            GlyphLayout tl = new GlyphLayout(font, title);
            font.draw(batch, title, dX + (dW - tl.width) / 2f, dY + dH - 30f);

            // button labels — centred in their respective buttons
            font.setColor(Color.WHITE);
            drawCentredXY(batch, font, "Keep Playing", noX,  bY, bW, bH);
            drawCentredXY(batch, font, "Yes, Quit",    yesX, bY, bW, bH);
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * processes a click — returns true if this widget consumed it
     * the dialog eats ALL clicks while open
     * @param touchPos world-space click position
     * @return true if consumed
     */
    public boolean handleInput(Vector3 touchPos) {
        if (showConfirm) {
            if (yesBounds != null && yesBounds.contains(touchPos.x, touchPos.y)) {
                Gdx.app.exit();
                return true;
            }
            if (noBounds != null && noBounds.contains(touchPos.x, touchPos.y)) {
                showConfirm = false;
                return true;
            }
            return true; // swallow all clicks while dialog is open
        }
        if (quitBounds != null && quitBounds.contains(touchPos.x, touchPos.y)) {
            showConfirm = true;
            return true;
        }
        return false;
    }

    // helper: centre text inside a Rectangle
    private void drawCentred(SpriteBatch batch, BitmapFont font, String text, Rectangle rect) {
        drawCentredXY(batch, font, text, rect.x, rect.y, rect.width, rect.height);
    }

    private void drawCentredXY(SpriteBatch batch, BitmapFont font, String text,
                               float x, float y, float w, float h) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text, x + (w - gl.width) / 2f, y + (h + gl.height) / 2f);
    }
}
