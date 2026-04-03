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

// manages the in-game quit button, pie rule button, and the shared quit confirmation dialog
// all interactive buttons use a dark-blue fill with a gold border
// dialog: No (cancel) is on the LEFT in dark red, Yes (quit) is on the RIGHT in dark green
public class UIController {

    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final WorldCalculator world;
    private final GameState gameState;
    private InputHandler inputHandler;

    // false until the welcome screen is dismissed — hides in-game buttons
    private boolean inGame        = false;
    private boolean showQuitConfirm = false;

    private Rectangle quitButtonBounds;
    private Rectangle pieRuleButtonBounds;
    private Rectangle noButtonBounds;   // LEFT — cancel
    private Rectangle yesButtonBounds;  // RIGHT — confirm quit

    private final float offsetZ = 300f;

    // shared button theme: dark blue fill, gold border
    private static final Color BTN_BG       = new Color(0.04f, 0.07f, 0.26f, 1f);
    private static final Color BTN_BG_HOVER = new Color(0.09f, 0.14f, 0.40f, 1f);
    private static final Color GOLD_BORDER  = new Color(0.82f, 0.67f, 0.12f, 1f);

    // dialog background
    private static final Color DIALOG_BG    = new Color(0.03f, 0.05f, 0.20f, 0.97f);

    // No button — dark red
    private static final Color NO_IDLE      = new Color(0.38f, 0.07f, 0.07f, 1f);
    private static final Color NO_HOVER     = new Color(0.58f, 0.12f, 0.12f, 1f);

    // Yes button — dark green
    private static final Color YES_IDLE     = new Color(0.07f, 0.32f, 0.07f, 1f);
    private static final Color YES_HOVER    = new Color(0.12f, 0.50f, 0.12f, 1f);

    /**
     * @param viewport  used for world size and unprojecting clicks
     * @param camera    used to find visible world edges for button anchoring
     * @param world     provides board centre coordinates for dialog placement
     * @param gameState read each frame to show/hide the pie rule button
     */
    public UIController(Viewport viewport, OrthographicCamera camera,
                        WorldCalculator world, GameState gameState) {
        this.viewport  = viewport;
        this.camera    = camera;
        this.world     = world;
        this.gameState = gameState;
    }

    /**
     * call with true once the welcome screen is dismissed so in-game buttons appear
     */
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    /**
     * opens the quit confirmation dialog — callable from outside (e.g. welcome screen quit button)
     */
    public void triggerQuitConfirm() {
        showQuitConfirm = true;
    }

    public void setInputHandler(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    /**
     * recalculates all button rectangles based on the current camera position
     * call every frame before drawing or handling input
     */
    public void updateBounds() {
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;

        if (inGame) {
            // quit button pinned to the bottom-right corner
            float qW = 90, qH = 44;
            float qX = worldRight - qW - 20;
            float qY = worldBottom + 20;
            quitButtonBounds = new Rectangle(qX, qY, qW, qH);

            // pie rule button to the left of quit — only when available
            if (gameState.isPieRuleAvailable()) {
                float pW = 190, pH = 44;
                float pX = qX - pW - 14;
                float pY = qY;
                pieRuleButtonBounds = new Rectangle(pX, pY, pW, pH);
            } else {
                pieRuleButtonBounds = null;
            }
        } else {
            quitButtonBounds    = null;
            pieRuleButtonBounds = null;
        }

        // dialog yes/no bounds — No LEFT, Yes RIGHT
        if (showQuitConfirm) {
            float dW = 380, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 140, bH = 52;
            float bY = dY + 38f;
            float pad = 28f;
            // No on the left
            noButtonBounds  = new Rectangle(dX + pad, bY, bW, bH);
            // Yes on the right
            yesButtonBounds = new Rectangle(dX + dW - bW - pad, bY, bW, bH);
        } else {
            noButtonBounds  = null;
            yesButtonBounds = null;
        }
    }

    /**
     * draws all active buttons and the confirmation dialog
     * safe to call every frame — skips anything not currently needed
     * @param shapeRenderer for backgrounds and borders
     * @param batch         for text labels
     * @param font          font for all button text
     */
    public void draw(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);

        // ---- filled backgrounds ----
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (quitButtonBounds != null) {
            boolean h = quitButtonBounds.contains(mouse.x, mouse.y);
            shapeRenderer.setColor(h ? BTN_BG_HOVER : BTN_BG);
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }

        if (pieRuleButtonBounds != null) {
            boolean h = pieRuleButtonBounds.contains(mouse.x, mouse.y);
            shapeRenderer.setColor(h ? BTN_BG_HOVER : BTN_BG);
            shapeRenderer.rect(pieRuleButtonBounds.x, pieRuleButtonBounds.y,
                pieRuleButtonBounds.width, pieRuleButtonBounds.height);
        }

        shapeRenderer.end();

        // ---- gold borders on in-game buttons ----
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(GOLD_BORDER);
        if (quitButtonBounds != null) {
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }
        if (pieRuleButtonBounds != null) {
            shapeRenderer.rect(pieRuleButtonBounds.x, pieRuleButtonBounds.y,
                pieRuleButtonBounds.width, pieRuleButtonBounds.height);
        }
        shapeRenderer.end();

        // ---- quit confirmation dialog ----
        if (showQuitConfirm) {
            float dW = 380, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 140, bH = 52;
            float bY = dY + 38f;
            float pad = 28f;
            float noX  = dX + pad;
            float yesX = dX + dW - bW - pad;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // dim overlay behind the dialog
            shapeRenderer.setColor(0f, 0f, 0f, 0.72f);
            float wl = camera.position.x - viewport.getWorldWidth()  / 2;
            float wb = camera.position.y - viewport.getWorldHeight() / 2;
            shapeRenderer.rect(wl, wb, viewport.getWorldWidth(), viewport.getWorldHeight());

            // dialog background — dark blue
            shapeRenderer.setColor(DIALOG_BG);
            shapeRenderer.rect(dX, dY, dW, dH);

            // No button (left) — dark red, brightens on hover
            boolean noH = noButtonBounds != null && noButtonBounds.contains(mouse.x, mouse.y);
            shapeRenderer.setColor(noH ? NO_HOVER : NO_IDLE);
            shapeRenderer.rect(noX, bY, bW, bH);

            // Yes button (right) — dark green, brightens on hover
            boolean yH = yesButtonBounds != null && yesButtonBounds.contains(mouse.x, mouse.y);
            shapeRenderer.setColor(yH ? YES_HOVER : YES_IDLE);
            shapeRenderer.rect(yesX, bY, bW, bH);

            shapeRenderer.end();

            // borders: gold on the dialog, white on the buttons
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(GOLD_BORDER);
            shapeRenderer.rect(dX, dY, dW, dH);
            shapeRenderer.setColor(1f, 1f, 1f, 0.55f);
            shapeRenderer.rect(noX,  bY, bW, bH);
            shapeRenderer.rect(yesX, bY, bW, bH);
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ---- text on top of everything ----
        batch.begin();

        if (quitButtonBounds != null) {
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, "Quit");
            font.draw(batch, "Quit",
                quitButtonBounds.x + (quitButtonBounds.width  - gl.width)  / 2f,
                quitButtonBounds.y + (quitButtonBounds.height + gl.height) / 2f);
        }

        if (pieRuleButtonBounds != null) {
            font.setColor(Color.WHITE);
            String lbl = "Activate Pie Rule";
            GlyphLayout gl = new GlyphLayout(font, lbl);
            font.draw(batch, lbl,
                pieRuleButtonBounds.x + (pieRuleButtonBounds.width  - gl.width)  / 2f,
                pieRuleButtonBounds.y + (pieRuleButtonBounds.height + gl.height) / 2f);
        }

        if (showQuitConfirm) {
            float dW = 380, dH = 200;
            float dX = world.boardCenterX + (offsetZ - dW) / 2f;
            float dY = world.boardCenterY - dH / 2f;
            float bW = 140, bH = 52;
            float bY = dY + 38f;
            float pad = 28f;
            float noX  = dX + pad;
            float yesX = dX + dW - bW - pad;

            // centered dialog title
            font.setColor(Color.WHITE);
            String title = "Quit the game?";
            GlyphLayout tl = new GlyphLayout(font, title);
            font.draw(batch, title, dX + (dW - tl.width) / 2f, dY + dH - 28f);

            // No button text — centered
            String noTxt = "No, Keep Playing";
            GlyphLayout noGl = new GlyphLayout(font, noTxt);
            font.draw(batch, noTxt,
                noX  + (bW - noGl.width)  / 2f,
                bY   + (bH + noGl.height) / 2f);

            // Yes button text — centered
            String yesTxt = "Yes, Quit";
            GlyphLayout yesGl = new GlyphLayout(font, yesTxt);
            font.draw(batch, yesTxt,
                yesX  + (bW - yesGl.width)  / 2f,
                bY    + (bH + yesGl.height) / 2f);
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * processes a click in world coordinates and returns true if a UI element consumed it
     * the dialog eats ALL clicks while open — board clicks are only passed through when the dialog is closed
     * @param touchPos click position in world coordinates
     * @return true if consumed — caller must not also pass this to the board
     */
    public boolean handleInput(Vector3 touchPos) {
        if (showQuitConfirm) {
            // Yes (right) — exit the application
            if (yesButtonBounds != null && yesButtonBounds.contains(touchPos.x, touchPos.y)) {
                Gdx.app.exit();
                return true;
            }
            // No (left) — close the dialog and resume
            if (noButtonBounds != null && noButtonBounds.contains(touchPos.x, touchPos.y)) {
                showQuitConfirm = false;
                return true;
            }
            // any click outside the buttons still gets swallowed while the dialog is up
            return true;
        }

        if (pieRuleButtonBounds != null && pieRuleButtonBounds.contains(touchPos.x, touchPos.y)) {
            gameState.activatePieRule();
            if (inputHandler != null) {
                inputHandler.swapFirstTileColour();
            }
            return true;
        }

        if (quitButtonBounds != null && quitButtonBounds.contains(touchPos.x, touchPos.y)) {
            showQuitConfirm = true;
            return true;
        }

        return false;
    }
}
