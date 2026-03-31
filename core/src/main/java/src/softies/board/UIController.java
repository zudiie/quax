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

// manages the quit button, the pie rule button, and the quit confirmation dialog
// the pie rule button only appears during WHITE's very first turn and vanishes after that
public class UIController {

    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final WorldCalculator world;
    private final GameState gameState;

    private boolean showQuitConfirm   = false;
    private Rectangle quitButtonBounds;
    private Rectangle pieRuleButtonBounds;
    private Rectangle yesButtonBounds;
    private Rectangle noButtonBounds;

    // matches Main's offsetZ so dialog placement lines up with the rest of the right-side UI
    private final float offsetZ = 300f;

    // blue tint for the pie rule button to make it visually distinct from the quit button
    private static final Color PIE_IDLE  = new Color(0.2f, 0.4f, 0.8f, 1f);
    private static final Color PIE_HOVER = new Color(0.3f, 0.55f, 1.0f, 1f);

    /**
     * creates the controller — now also needs gameState to check pie rule availability each frame
     * @param viewport  used to get world dimensions and unproject clicks
     * @param camera    used to find visible world bounds for button anchoring
     * @param world     provides board centre coordinates for dialog placement
     * @param gameState read each frame to decide whether to show the pie rule button
     */
    public UIController(Viewport viewport, OrthographicCamera camera,
                        WorldCalculator world, GameState gameState) {
        this.viewport   = viewport;
        this.camera     = camera;
        this.world      = world;
        this.gameState  = gameState;
    }

    /**
     * recalculates all button rectangles based on the current camera position
     * call this every frame before doing any input handling or drawing
     */
    public void updateBounds() {
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;

        // quit button anchored to the bottom-right corner
        float quitW = 80, quitH = 40;
        float quitX = worldRight - quitW - 20;
        float quitY = worldBottom + 20;
        quitButtonBounds = new Rectangle(quitX, quitY, quitW, quitH);

        // pie rule button sits just to the left of the quit button, same height
        // only compute it when it's actually going to be shown
        if (gameState.isPieRuleAvailable()) {
            float pieW = 180, pieH = 40;
            float pieX = quitX - pieW - 40;
            float pieY = quitY;
            pieRuleButtonBounds = new Rectangle(pieX, pieY, pieW, pieH);
        } else {
            // clear the bounds so stale rectangles don't catch clicks when the button is hidden
            pieRuleButtonBounds = null;
        }

        // yes/no dialog bounds only matter when the quit dialog is open
        if (showQuitConfirm) {
            float dialogWidth  = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX + (offsetZ - dialogWidth)  / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;
            float btnW = 80, btnH = 40;
            yesButtonBounds = new Rectangle(dialogX + 50,  dialogY + 40, btnW, btnH);
            noButtonBounds  = new Rectangle(dialogX + 170, dialogY + 40, btnW, btnH);
        } else {
            yesButtonBounds = null;
            noButtonBounds  = null;
        }
    }

    /**
     * draws the quit button, the pie rule button (when available), and the quit confirmation dialog
     * @param shapeRenderer for drawing filled and outlined rectangles
     * @param batch         for drawing text labels on top
     * @param font          the font to use for all button text
     */
    public void draw(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // quit button background
        if (quitButtonBounds != null) {
            boolean hover = quitButtonBounds.contains(mousePos.x, mousePos.y);
            shapeRenderer.setColor(hover ? 0.3f : 0.2f, 0.2f, 0.2f, 1f);
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }

        // pie rule button background — blue, only drawn when available
        if (pieRuleButtonBounds != null) {
            boolean hover = pieRuleButtonBounds.contains(mousePos.x, mousePos.y);
            Color c = hover ? PIE_HOVER : PIE_IDLE;
            shapeRenderer.setColor(c);
            shapeRenderer.rect(pieRuleButtonBounds.x, pieRuleButtonBounds.y,
                pieRuleButtonBounds.width, pieRuleButtonBounds.height);
        }

        shapeRenderer.end();

        // outlines for both buttons
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 1f);
        if (quitButtonBounds != null) {
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }
        if (pieRuleButtonBounds != null) {
            shapeRenderer.rect(pieRuleButtonBounds.x, pieRuleButtonBounds.y,
                pieRuleButtonBounds.width, pieRuleButtonBounds.height);
        }
        shapeRenderer.end();

        // quit confirmation dialog
        if (showQuitConfirm) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // dark overlay behind the dialog
            shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
            float worldLeft   = camera.position.x - viewport.getWorldWidth()  / 2;
            float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
            shapeRenderer.rect(worldLeft, worldBottom, viewport.getWorldWidth(), viewport.getWorldHeight());

            float dialogWidth  = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX + (offsetZ - dialogWidth)  / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
            shapeRenderer.rect(dialogX, dialogY, dialogWidth, dialogHeight);

            float btnW = 80, btnH = 40;
            float yesX = dialogX + 50,  yesY = dialogY + 40;
            float noX  = dialogX + 170, noY  = dialogY + 40;
            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f);
            shapeRenderer.rect(yesX, yesY, btnW, btnH);
            shapeRenderer.rect(noX,  noY,  btnW, btnH);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 1f, 1f, 1f);
            shapeRenderer.rect(yesX, yesY, btnW, btnH);
            shapeRenderer.rect(noX,  noY,  btnW, btnH);
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- text layer ---
        batch.begin();

        if (quitButtonBounds != null) {
            font.setColor(Color.WHITE);
            font.draw(batch, "Quit", quitButtonBounds.x + 20, quitButtonBounds.y + 28);
        }

        // centre "Activate Pie Rule" text inside the button
        if (pieRuleButtonBounds != null) {
            font.setColor(Color.WHITE);
            String pieLabel = "Activate Pie Rule";
            GlyphLayout gl = new GlyphLayout(font, pieLabel);
            float textX = pieRuleButtonBounds.x + (pieRuleButtonBounds.width  - gl.width)  / 2f;
            float textY = pieRuleButtonBounds.y + (pieRuleButtonBounds.height + gl.height) / 2f;
            font.draw(batch, pieLabel, textX, textY);
        }

        if (showQuitConfirm) {
            float dialogWidth  = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX + (offsetZ - dialogWidth)  / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;
            float btnW = 80, btnH = 40;
            float yesX = dialogX + 50,  yesY = dialogY + 40;
            float noX  = dialogX + 170, noY  = dialogY + 40;
            font.draw(batch, "Quit game?", dialogX + 90, dialogY + 120);
            font.draw(batch, "Yes", yesX + 25, yesY + 28);
            font.draw(batch, "No",  noX  + 28, noY  + 28);
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * processes a click and returns true if a UI element consumed it
     * checks the pie rule button first, then quit button, then dialog
     * @param touchPos the click position in world coordinates
     * @return true if consumed — caller should not pass this click to the board
     */
    public boolean handleInput(Vector3 touchPos) {
        // dialog eats all clicks while it's open
        if (showQuitConfirm) {
            if (yesButtonBounds != null && yesButtonBounds.contains(touchPos.x, touchPos.y)) {
                Gdx.app.exit();
                return true;
            } else if (noButtonBounds != null && noButtonBounds.contains(touchPos.x, touchPos.y)) {
                showQuitConfirm = false;
                return true;
            }
            return true;
        }

        // pie rule button — only reachable when pieRuleButtonBounds is non-null (i.e. available)
        if (pieRuleButtonBounds != null && pieRuleButtonBounds.contains(touchPos.x, touchPos.y)) {
            gameState.activatePieRule();
            return true;
        }

        // quit button
        if (quitButtonBounds != null && quitButtonBounds.contains(touchPos.x, touchPos.y)) {
            showQuitConfirm = true;
            return true;
        }

        return false;
    }
}
