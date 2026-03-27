package src.softies.board;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

// manages the quit button and the "are you sure?" confirmation dialog
// handles positioning, drawing and click detection for both UI elements
public class UIController {

    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final WorldCalculator world;

    private boolean showQuitConfirm = false;
    private Rectangle quitButtonBounds;
    private Rectangle yesButtonBounds;
    private Rectangle noButtonBounds;

    // horizontal offset used to position dialog elements — matches Main's offsetZ
    private final float offsetZ = 300f;

    /**
     * creates the controller with the camera and world info needed to position UI elements
     * @param viewport the active viewport - used to get world dimensions and unproject clicks
     * @param camera the orthographic camera - used to find the visible world bounds
     * @param world provides board centre coordinates for dialog placement
     */
    public UIController(Viewport viewport, OrthographicCamera camera, WorldCalculator world) {
        this.viewport = viewport;
        this.camera = camera;
        this.world = world;
    }

    /**
     * recalculates all button rectangles based on the current camera position
     * call this every frame before doing any input handling or drawing
     */
    public void updateBounds() {
        // work out the visible world edges from the camera position
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;

        // pin the quit button to the bottom-right corner of the visible area
        float buttonWidth  = 80;
        float buttonHeight = 40;
        float quitX = worldRight - buttonWidth - 20;
        float quitY = worldBottom + 20;
        quitButtonBounds = new Rectangle(quitX, quitY, buttonWidth, buttonHeight);

        // only compute dialog button positions if the dialog is actually visible
        if (showQuitConfirm) {
            float dialogWidth  = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX + (offsetZ - dialogWidth)  / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;

            float btnW = 80;
            float btnH = 40;
            // yes sits on the left, no on the right
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX  = dialogX + 170;
            float noY  = dialogY + 40;

            yesButtonBounds = new Rectangle(yesX, yesY, btnW, btnH);
            noButtonBounds  = new Rectangle(noX,  noY,  btnW, btnH);
        } else {
            // clear these so stale rectangles don't accidentally eat clicks
            yesButtonBounds = null;
            noButtonBounds  = null;
        }
    }

    /**
     * draws the quit button and, if the dialog is open, the full confirmation overlay
     * uses the ShapeRenderer for backgrounds and borders, then the batch for text
     * @param shapeRenderer for drawing filled and outlined rectangles
     * @param batch for drawing text on top
     * @param font the font to use for all button labels
     */
    public void draw(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // draw the quit button background — slightly lighter on hover
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (quitButtonBounds != null) {
            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(mousePos);
            boolean hover = quitButtonBounds.contains(mousePos.x, mousePos.y);
            shapeRenderer.setColor(hover ? 0.3f : 0.2f, 0.2f, 0.2f, 1);
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }
        shapeRenderer.end();

        // white outline around the quit button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (quitButtonBounds != null) {
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }
        shapeRenderer.end();

        // everything below only renders when the confirmation dialog is open
        if (showQuitConfirm) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // dim overlay to visually block the game behind the dialog
            shapeRenderer.setColor(0, 0, 0, 0.7f);
            float worldLeft   = camera.position.x - viewport.getWorldWidth()  / 2;
            float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
            shapeRenderer.rect(worldLeft, worldBottom, viewport.getWorldWidth(), viewport.getWorldHeight());

            // dialog box background
            float dialogWidth  = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX + (offsetZ - dialogWidth)  / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
            shapeRenderer.rect(dialogX, dialogY, dialogWidth, dialogHeight);

            // yes and no button fills
            float btnW = 80;
            float btnH = 40;
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX  = dialogX + 170;
            float noY  = dialogY + 40;
            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
            shapeRenderer.rect(yesX, yesY, btnW, btnH);
            shapeRenderer.rect(noX,  noY,  btnW, btnH);
            shapeRenderer.end();

            // button outlines
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1, 1, 1, 1);
            shapeRenderer.rect(yesX, yesY, btnW, btnH);
            shapeRenderer.rect(noX,  noY,  btnW, btnH);
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // draw all text in a fresh batch pass so it renders on top of the shapes
        batch.begin();
        if (quitButtonBounds != null) {
            font.draw(batch, "Quit", quitButtonBounds.x + 20, quitButtonBounds.y + 28);
        }

        if (showQuitConfirm) {
            // re-derive positions here since local vars from above are out of scope
            float dialogWidth  = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX + (offsetZ - dialogWidth)  / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;
            float btnW = 80;
            float btnH = 40;
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX  = dialogX + 170;
            float noY  = dialogY + 40;

            font.draw(batch, "Quit game?", dialogX + 90, dialogY + 120);
            font.draw(batch, "Yes", yesX + 25, yesY + 28);
            font.draw(batch, "No",  noX  + 28, noY  + 28);
        }
        batch.end();
    }

    /**
     * processes a click in world coords and returns whether the UI consumed it
     * if the dialog is open, all clicks are consumed - only yes/no do anything
     * @param touchPos the click position in world coordinates
     * @return true if the click was handled by a UI element (don't pass it to the board)
     */
    public boolean handleInput(Vector3 touchPos) {
        if (showQuitConfirm) {
            if (yesButtonBounds != null && yesButtonBounds.contains(touchPos.x, touchPos.y)) {
                // player confirmed — shut it down
                Gdx.app.exit();
                return true;
            } else if (noButtonBounds != null && noButtonBounds.contains(touchPos.x, touchPos.y)) {
                // player cancelled — close the dialog and resume
                showQuitConfirm = false;
                return true;
            }
            // any click outside yes/no still gets swallowed while the dialog is up
            return true;
        }

        // quit button clicked — show the confirmation dialog next frame
        if (quitButtonBounds != null && quitButtonBounds.contains(touchPos.x, touchPos.y)) {
            showQuitConfirm = true;
            return true;
        }

        // nothing UI-related was clicked
        return false;
    }
}
