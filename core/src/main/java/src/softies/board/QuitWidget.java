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
// the dialog can be opened from within the game or from the welcome screen via triggerConfirm()
// Keep Playing (cancel) is on the LEFT in dark red; Yes, Quit is on the RIGHT
public class QuitWidget {

    private final WorldCalculator world;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    private boolean visible     = false;
    private boolean showConfirm = false;

    private Rectangle quitBounds;
    private Rectangle noBounds;
    private Rectangle yesBounds;
    private Rectangle dialogBounds;

    private static final float OFFSET_Z   = 300f;

    private static final float DIALOG_W      = 400f;
    private static final float DIALOG_H      = 200f;
    private static final float BUTTON_W      = 150f;
    private static final float BUTTON_H      =  52f;
    private static final float BUTTON_PAD    =  24f;
    private static final float BUTTON_Y_OFFSET = 36f;

    private static final Color BTN_IDLE  = new Color(0.243f, 0.145f, 0.063f, 1f);
    private static final Color BTN_HOVER = new Color(0.361f, 0.227f, 0.094f, 1f);
    private static final Color GOLD      = new Color(0.753f, 0.471f, 0.251f, 1f);
    private static final Color DIALOG_BG = new Color(0.133f, 0.082f, 0.039f, 0.97f);
    private static final Color NO_IDLE   = new Color(0.227f, 0.071f, 0.047f, 1f);
    private static final Color NO_HOVER  = new Color(0.345f, 0.125f, 0.078f, 1f);
    private static final Color YES_IDLE  = new Color(0.243f, 0.145f, 0.063f, 1f);
    private static final Color YES_HOVER = new Color(0.361f, 0.227f, 0.094f, 1f);

    public QuitWidget(WorldCalculator world, Viewport viewport, OrthographicCamera camera) {
        this.world    = world;
        this.viewport = viewport;
        this.camera   = camera;
    }

    /** call with true once the game starts so the bottom-right quit button appears */
    public void setVisible(boolean visible) { this.visible = visible; }

    /** opens the confirmation dialog from outside (e.g. the welcome screen quit button) */
    public void triggerConfirm() { showConfirm = true; }

    /** recomputes all button and dialog rectangles - call every frame before draw() */
    public void updateBounds() {
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
        quitBounds = visible
            ? new Rectangle(worldRight - 90 - 20, worldBottom + 20, 90, 44)
            : null;
        if (showConfirm) {
            float dX = world.boardCenterX + (OFFSET_Z - DIALOG_W) / 2f;
            float dY = world.boardCenterY - DIALOG_H / 2f;
            float bY = dY + BUTTON_Y_OFFSET;
            dialogBounds = new Rectangle(dX, dY, DIALOG_W, DIALOG_H);
            noBounds  = new Rectangle(dX + BUTTON_PAD, bY, BUTTON_W, BUTTON_H);
            yesBounds = new Rectangle(dX + DIALOG_W - BUTTON_W - BUTTON_PAD, bY, BUTTON_W, BUTTON_H);
        } else {
            dialogBounds = noBounds = yesBounds = null;
        }
    }

    /** draws the quit button and confirmation dialog - manages batch begin/end internally */
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        updateBounds();

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
        font.setColor(new Color(0.910f, 0.835f, 0.690f, 1f));
        batch.end();
    }

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

    private void drawDialog(ShapeRenderer sr, Vector3 mouse) {
        if (dialogBounds == null || noBounds == null || yesBounds == null) return;
        drawDimOverlay(sr);
        drawDialogFills(sr, mouse);
        drawDialogBorders(sr);
    }

    private void drawDimOverlay(ShapeRenderer sr) {
        float wl = camera.position.x - viewport.getWorldWidth()  / 2;
        float wb = camera.position.y - viewport.getWorldHeight() / 2;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.102f, 0.067f, 0.031f, 0.82f);
        sr.rect(wl, wb, viewport.getWorldWidth(), viewport.getWorldHeight());
        sr.end();
    }

    private void drawDialogFills(ShapeRenderer sr, Vector3 mouse) {
        if (dialogBounds == null || noBounds == null || yesBounds == null) return;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(DIALOG_BG);
        sr.rect(dialogBounds.x, dialogBounds.y, dialogBounds.width, dialogBounds.height);
        sr.setColor(noBounds.contains(mouse.x, mouse.y) ? NO_HOVER : NO_IDLE);
        sr.rect(noBounds.x, noBounds.y, noBounds.width, noBounds.height);
        sr.setColor(yesBounds.contains(mouse.x, mouse.y) ? YES_HOVER : YES_IDLE);
        sr.rect(yesBounds.x, yesBounds.y, yesBounds.width, yesBounds.height);
        sr.end();
    }

    private void drawDialogBorders(ShapeRenderer sr) {
        if (dialogBounds == null || noBounds == null || yesBounds == null) return;
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD);
        sr.rect(dialogBounds.x, dialogBounds.y, dialogBounds.width, dialogBounds.height);
        sr.setColor(0.910f, 0.835f, 0.690f, 0.45f);
        sr.rect(noBounds.x,  noBounds.y,  noBounds.width,  noBounds.height);
        sr.rect(yesBounds.x, yesBounds.y, yesBounds.width, yesBounds.height);
        sr.end();
    }

    private void drawQuitButtonLabel(SpriteBatch batch, BitmapFont font) {
        if (quitBounds == null) return;
        font.setColor(new Color(0.910f, 0.835f, 0.690f, 1f));
        drawCentred(batch, font, "Quit", quitBounds);
    }

    private void drawDialogLabels(SpriteBatch batch, BitmapFont font) {
        if (dialogBounds == null || noBounds == null || yesBounds == null) return;
        font.setColor(Color.WHITE);
        GlyphLayout titleLayout = new GlyphLayout(font, "Quit the game?");
        font.draw(batch, "Quit the game?",
            dialogBounds.x + (dialogBounds.width - titleLayout.width) / 2f,
            dialogBounds.y + dialogBounds.height - 30f);
        drawCentred(batch, font, "Keep Playing", noBounds);
        drawCentred(batch, font, "Yes, Quit",    yesBounds);
    }

    /** processes a click - returns true if this widget consumed the input */
    public boolean handleInput(Vector3 touchPos) {
        if (showConfirm) {
            if (yesBounds != null && yesBounds.contains(touchPos.x, touchPos.y)) { Gdx.app.exit(); return true; }
            if (noBounds  != null && noBounds.contains(touchPos.x, touchPos.y))  { showConfirm = false; return true; }
            return true;
        }
        if (quitBounds != null && quitBounds.contains(touchPos.x, touchPos.y)) {
            showConfirm = true;
            return true;
        }
        return false;
    }

    private Vector3 getMouseWorldPos() {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);
        return mouse;
    }

    private void drawCentred(SpriteBatch batch, BitmapFont font, String text, Rectangle r) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text, r.x + (r.width - gl.width) / 2f, r.y + (r.height + gl.height) / 2f);
    }
}
