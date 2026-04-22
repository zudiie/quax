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

// this class is superseded by WelcomeScreen, QuitWidget and PieRuleWidget
// it is kept only to satisfy compilation - Main.java no longer instantiates it
public class UIController {

    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final WorldCalculator world;
    private final GameState gameState;

    private boolean inGame          = false;
    private boolean showQuitConfirm = false;

    private Rectangle quitButtonBounds;
    private Rectangle pieRuleButtonBounds;
    private Rectangle noButtonBounds;
    private Rectangle yesButtonBounds;

    private final float offsetZ = 300f;

    private static final Color BTN_BG       = new Color(0.14f, 0.24f, 0.62f, 1f);
    private static final Color BTN_BG_HOVER = new Color(0.22f, 0.36f, 0.80f, 1f);
    private static final Color GOLD_BORDER  = new Color(0.82f, 0.67f, 0.12f, 1f);
    private static final Color DIALOG_BG    = new Color(0.10f, 0.18f, 0.52f, 0.97f);
    private static final Color NO_IDLE      = new Color(0.38f, 0.07f, 0.07f, 1f);
    private static final Color NO_HOVER     = new Color(0.58f, 0.12f, 0.12f, 1f);
    private static final Color YES_IDLE     = new Color(0.07f, 0.30f, 0.07f, 1f);
    private static final Color YES_HOVER    = new Color(0.12f, 0.48f, 0.12f, 1f);

    public UIController(Viewport viewport, OrthographicCamera camera,
                        WorldCalculator world, GameState gameState) {
        this.viewport  = viewport;
        this.camera    = camera;
        this.world     = world;
        this.gameState = gameState;
    }

    public void setInGame(boolean inGame)  { this.inGame = inGame; }
    public void triggerQuitConfirm()       { showQuitConfirm = true; }

    // no longer used by Main
    public void setInputHandler(InputHandler inputHandler) { /* no-op */ }

    public void updateBounds() {
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;

        if (inGame) {
            float qW = 90, qH = 44;
            float qX = worldRight - qW - 20;
            float qY = worldBottom + 20;
            quitButtonBounds = new Rectangle(qX, qY, qW, qH);

            if (gameState.isPieRuleAvailable()) {
                float pW = 190, pH = 44;
                pieRuleButtonBounds = new Rectangle(qX - pW - 14, qY, pW, pH);
            } else {
                pieRuleButtonBounds = null;
            }
        } else {
            quitButtonBounds    = null;
            pieRuleButtonBounds = null;
        }

        if (showQuitConfirm) {
            float dW = 400, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 150, bH = 52, bY = dY + 36, pad = 24;
            noButtonBounds  = new Rectangle(dX + pad,           bY, bW, bH);
            yesButtonBounds = new Rectangle(dX + dW - bW - pad, bY, bW, bH);
        } else {
            noButtonBounds  = null;
            yesButtonBounds = null;
        }
    }

    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(camera.combined);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);

        // button backgrounds
        sr.begin(ShapeRenderer.ShapeType.Filled);
        if (quitButtonBounds != null) {
            sr.setColor(quitButtonBounds.contains(mouse.x, mouse.y) ? BTN_BG_HOVER : BTN_BG);
            sr.rect(quitButtonBounds.x, quitButtonBounds.y, quitButtonBounds.width, quitButtonBounds.height);
        }
        if (pieRuleButtonBounds != null) {
            sr.setColor(pieRuleButtonBounds.contains(mouse.x, mouse.y) ? BTN_BG_HOVER : BTN_BG);
            sr.rect(pieRuleButtonBounds.x, pieRuleButtonBounds.y, pieRuleButtonBounds.width, pieRuleButtonBounds.height);
        }
        sr.end();

        // gold button borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD_BORDER);
        if (quitButtonBounds    != null) sr.rect(quitButtonBounds.x,    quitButtonBounds.y,    quitButtonBounds.width,    quitButtonBounds.height);
        if (pieRuleButtonBounds != null) sr.rect(pieRuleButtonBounds.x, pieRuleButtonBounds.y, pieRuleButtonBounds.width, pieRuleButtonBounds.height);
        sr.end();

        // quit confirmation dialog
        if (showQuitConfirm) {
            float dW = 400, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 150, bH = 52, bY = dY + 36, pad = 24;
            float noX  = dX + pad;
            float yesX = dX + dW - bW - pad;

            sr.begin(ShapeRenderer.ShapeType.Filled);
            // dim overlay behind dialog
            sr.setColor(0f, 0f, 0f, 0.70f);
            float wl = camera.position.x - viewport.getWorldWidth()  / 2;
            float wb = camera.position.y - viewport.getWorldHeight() / 2;
            sr.rect(wl, wb, viewport.getWorldWidth(), viewport.getWorldHeight());
            // dialog background
            sr.setColor(DIALOG_BG);
            sr.rect(dX, dY, dW, dH);
            // No button - dark red, left
            sr.setColor(noButtonBounds  != null && noButtonBounds.contains(mouse.x,  mouse.y) ? NO_HOVER  : NO_IDLE);
            sr.rect(noX, bY, bW, bH);
            // Yes button - dark green, right
            sr.setColor(yesButtonBounds != null && yesButtonBounds.contains(mouse.x, mouse.y) ? YES_HOVER : YES_IDLE);
            sr.rect(yesX, bY, bW, bH);
            sr.end();

            // borders
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(GOLD_BORDER);
            sr.rect(dX, dY, dW, dH);
            sr.setColor(1f, 1f, 1f, 0.50f);
            sr.rect(noX,  bY, bW, bH);
            sr.rect(yesX, bY, bW, bH);
            sr.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // text on top
        batch.begin();
        if (quitButtonBounds != null) {
            font.setColor(Color.WHITE);
            drawCentred(batch, font, "Quit", quitButtonBounds);
        }
        if (pieRuleButtonBounds != null) {
            font.setColor(Color.WHITE);
            drawCentred(batch, font, "Activate Pie Rule", pieRuleButtonBounds);
        }
        if (showQuitConfirm) {
            float dW = 400, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 150, bH = 52, bY = dY + 36, pad = 24;
            float noX  = dX + pad;
            float yesX = dX + dW - bW - pad;

            font.setColor(Color.WHITE);
            String title = "Quit the game?";
            GlyphLayout tl = new GlyphLayout(font, title);
            font.draw(batch, title, dX + (dW - tl.width) / 2f, dY + dH - 30f);

            drawCentredXY(batch, font, "Keep Playing", noX,  bY, bW, bH);
            drawCentredXY(batch, font, "Yes, Quit",    yesX, bY, bW, bH);
        }
        font.setColor(Color.WHITE);
        batch.end();
    }

    public boolean handleInput(Vector3 touchPos) {
        if (showQuitConfirm) {
            if (yesButtonBounds != null && yesButtonBounds.contains(touchPos.x, touchPos.y)) {
                Gdx.app.exit();
                return true;
            }
            if (noButtonBounds != null && noButtonBounds.contains(touchPos.x, touchPos.y)) {
                showQuitConfirm = false;
                return true;
            }
            return true; // swallow all clicks while dialog is open
        }
        if (pieRuleButtonBounds != null && pieRuleButtonBounds.contains(touchPos.x, touchPos.y)) {
            // swap player colour assignments only - do NOT touch the tile graphic
            // the first stone stays BLACK because after the swap Player 2 now IS BLACK
            gameState.activatePieRule();
            return true;
        }
        if (quitButtonBounds != null && quitButtonBounds.contains(touchPos.x, touchPos.y)) {
            showQuitConfirm = true;
            return true;
        }
        return false;
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
