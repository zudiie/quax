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

// the in-game quit button (bottom-right corner) and the shared confirmation dialog
// the dialog can also be opened from the welcome screen via triggerConfirm()
// No (cancel) is on the LEFT in dark red; Yes (quit) is on the RIGHT in dark green
public class QuitWidget {

    private final WorldCalculator world;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    // the quit button is hidden until the game starts
    private boolean visible     = false;
    private boolean showConfirm = false;

    private Rectangle quitBounds;
    private Rectangle noBounds;   // LEFT — cancel
    private Rectangle yesBounds;  // RIGHT — confirm quit

    private static final float OFFSET_Z = 300f;

    // button colours
    private static final Color BTN_IDLE  = new Color(0.14f, 0.24f, 0.62f, 1f);
    private static final Color BTN_HOVER = new Color(0.22f, 0.36f, 0.80f, 1f);
    private static final Color GOLD      = new Color(0.82f, 0.67f, 0.12f, 1f);

    // dialog colours
    private static final Color DIALOG_BG = new Color(0.10f, 0.18f, 0.52f, 0.97f);
    private static final Color NO_IDLE   = new Color(0.38f, 0.07f, 0.07f, 1f);
    private static final Color NO_HOVER  = new Color(0.58f, 0.13f, 0.13f, 1f);
    private static final Color YES_IDLE  = new Color(0.07f, 0.30f, 0.07f, 1f);
    private static final Color YES_HOVER = new Color(0.13f, 0.48f, 0.13f, 1f);

    public QuitWidget(WorldCalculator world, Viewport viewport, OrthographicCamera camera) {
        this.world    = world;
        this.viewport = viewport;
        this.camera   = camera;
    }

    /** call with true once the game starts so the bottom-right quit button appears */
    public void setVisible(boolean visible) { this.visible = visible; }

    /** opens the confirmation dialog from outside (e.g. the welcome screen quit button) */
    public void triggerConfirm() { showConfirm = true; }

    /**
     * recomputes all button rectangles from the current camera position
     * call every frame before draw() and handleInput()
     */
    public void updateBounds() {
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;

        quitBounds = visible
            ? new Rectangle(worldRight - 90 - 20, worldBottom + 20, 90, 44)
            : null;

        if (showConfirm) {
            float dW = 400, dH = 200;
            float dX = world.boardCenterX + (OFFSET_Z - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 150, bH = 52, bY = dY + 36, pad = 24;
            noBounds  = new Rectangle(dX + pad,           bY, bW, bH);
            yesBounds = new Rectangle(dX + dW - bW - pad, bY, bW, bH);
        } else {
            noBounds = yesBounds = null;
        }
    }

    /**
     * draws the quit button and the confirmation dialog (if open)
     * manages batch begin/end internally — do not call with an active batch
     */
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        Vector3 mouse = getMouseWorldPos();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(camera.combined);

        drawQuitButton(sr, mouse);
        if (showConfirm) drawDialog(sr, mouse);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        drawQuitButtonLabel(batch, font);
        if (showConfirm) drawDialogLabels(batch, font);
        font.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * draws the quit button background and gold border
     */
    private void drawQuitButton(ShapeRenderer sr, Vector3 mouse) {
        if (quitBounds == null) return;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(quitBounds.contains(mouse.x, mouse.y) ? BTN_HOVER : BTN_IDLE);
        sr.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD);
        sr.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);
        sr.end();
    }

    /**
     * draws the full dialog: dim overlay, dark-blue background, red No button, green Yes button
     */
    private void drawDialog(ShapeRenderer sr, Vector3 mouse) {
        float dW = 400, dH = 200;
        float dX = world.boardCenterX + (OFFSET_Z - dW) / 2f;
        float dY = world.boardCenterY - dH / 2f;
        float bW = 150, bH = 52, bY = dY + 36, pad = 24;
        float noX  = dX + pad;
        float yesX = dX + dW - bW - pad;

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // semi-transparent dim layer behind the dialog
        float wl = camera.position.x - viewport.getWorldWidth()  / 2;
        float wb = camera.position.y - viewport.getWorldHeight() / 2;
        sr.setColor(0f, 0f, 0f, 0.70f);
        sr.rect(wl, wb, viewport.getWorldWidth(), viewport.getWorldHeight());

        sr.setColor(DIALOG_BG);
        sr.rect(dX, dY, dW, dH);

        // No — dark red, left
        sr.setColor(noBounds != null && noBounds.contains(mouse.x, mouse.y) ? NO_HOVER : NO_IDLE);
        sr.rect(noX, bY, bW, bH);

        // Yes — dark green, right
        sr.setColor(yesBounds != null && yesBounds.contains(mouse.x, mouse.y) ? YES_HOVER : YES_IDLE);
        sr.rect(yesX, bY, bW, bH);
        sr.end();

        // gold border on the dialog, white borders on the buttons
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD);
        sr.rect(dX, dY, dW, dH);
        sr.setColor(1f, 1f, 1f, 0.50f);
        sr.rect(noX,  bY, bW, bH);
        sr.rect(yesX, bY, bW, bH);
        sr.end();
    }

    /** draws the "Quit" label centred in the quit button */
    private void drawQuitButtonLabel(SpriteBatch batch, BitmapFont font) {
        if (quitBounds == null) return;
        font.setColor(Color.WHITE);
        drawCentred(batch, font, "Quit", quitBounds);
    }

    /** draws the dialog title and centred button labels */
    private void drawDialogLabels(SpriteBatch batch, BitmapFont font) {
        float dW = 400, dH = 200;
        float dX = world.boardCenterX + (OFFSET_Z - dW) / 2f;
        float dY = world.boardCenterY - dH / 2f;
        float bW = 150, bH = 52, bY = dY + 36, pad = 24;

        font.setColor(Color.WHITE);

        // centred dialog title
        String title = "Quit the game?";
        GlyphLayout tl = new GlyphLayout(font, title);
        font.draw(batch, title, dX + (dW - tl.width) / 2f, dY + dH - 30f);

        drawCentredXY(batch, font, "Keep Playing", dX + pad,           bY, bW, bH);
        drawCentredXY(batch, font, "Yes, Quit",    dX + dW - bW - pad, bY, bW, bH);
    }

    /**
     * processes a click — returns true if this widget consumed it
     * the confirmation dialog eats ALL clicks while open
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
            return true; // swallow everything while the dialog is open
        }
        if (quitBounds != null && quitBounds.contains(touchPos.x, touchPos.y)) {
            showConfirm = true;
            return true;
        }
        return false;
    }

    // --- helpers ---

    private Vector3 getMouseWorldPos() {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);
        return mouse;
    }

    private void drawCentred(SpriteBatch batch, BitmapFont font, String text, Rectangle r) {
        drawCentredXY(batch, font, text, r.x, r.y, r.width, r.height);
    }

    private void drawCentredXY(SpriteBatch batch, BitmapFont font, String text,
                               float x, float y, float w, float h) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text, x + (w - gl.width) / 2f, y + (h + gl.height) / 2f);
    }
}
