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

/**
 * Handles the quit button and its confirmation dialog.
 * Manages bounds, rendering, and input for these UI elements.
 */
public class UIController {
    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final WorldCalculator world;

    private boolean showQuitConfirm = false;
    private Rectangle quitButtonBounds;
    private Rectangle yesButtonBounds;
    private Rectangle noButtonBounds;

    public UIController(Viewport viewport, OrthographicCamera camera, WorldCalculator world) {
        this.viewport = viewport;
        this.camera = camera;
        this.world = world;
    }

    /**
     * Updates button positions based on current camera view.
     * Must be called each frame before input handling.
     */
    public void updateBounds() {
        float worldLeft   = camera.position.x - viewport.getWorldWidth() / 2;
        float worldRight  = camera.position.x + viewport.getWorldWidth() / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;

        // quit button anchored at bottom‑right corner
        float buttonWidth = 80;
        float buttonHeight = 40;
        float quitX = worldRight - buttonWidth - 20;
        float quitY = worldBottom + 20;
        quitButtonBounds = new Rectangle(quitX, quitY, buttonWidth, buttonHeight);

        // if confirmation dialog is active, compute Yes/No button rectangles
        if (showQuitConfirm) {
            float dialogWidth = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX - dialogWidth / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;
            float btnW = 80;
            float btnH = 40;
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX = dialogX + 170;
            float noY = dialogY + 40;
            yesButtonBounds = new Rectangle(yesX, yesY, btnW, btnH);
            noButtonBounds  = new Rectangle(noX,  noY,  btnW, btnH);
        } else {
            yesButtonBounds = null;
            noButtonBounds  = null;
        }
    }

    /**
     * Draws the quit button and, if active, the confirmation dialog.
     * Must be called between batch.end() and batch.begin() because it uses ShapeRenderer.
     */
    public void draw(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // quit button background (filled)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (quitButtonBounds != null) {
            // hover effect
            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(mousePos);
            boolean hover = quitButtonBounds.contains(mousePos.x, mousePos.y);
            shapeRenderer.setColor(hover ? 0.3f : 0.2f, 0.2f, 0.2f, 1);
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }
        shapeRenderer.end();

        // quit button border (outline)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 1);
        if (quitButtonBounds != null) {
            shapeRenderer.rect(quitButtonBounds.x, quitButtonBounds.y,
                quitButtonBounds.width, quitButtonBounds.height);
        }
        shapeRenderer.end();

        // confirmation dialog if active
        if (showQuitConfirm) {
            // semi‑transparent overlay
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 0.7f);
            float worldLeft = camera.position.x - viewport.getWorldWidth() / 2;
            float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
            shapeRenderer.rect(worldLeft, worldBottom,
                viewport.getWorldWidth(), viewport.getWorldHeight());

            // dialog background
            float dialogWidth = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX - dialogWidth / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1);
            shapeRenderer.rect(dialogX, dialogY, dialogWidth, dialogHeight);

            // Yes and No buttons
            float btnW = 80;
            float btnH = 40;
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX = dialogX + 170;
            float noY = dialogY + 40;
            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
            shapeRenderer.rect(yesX, yesY, btnW, btnH);
            shapeRenderer.rect(noX, noY, btnW, btnH);
            shapeRenderer.end();

            // button borders
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1, 1, 1, 1);
            shapeRenderer.rect(yesX, yesY, btnW, btnH);
            shapeRenderer.rect(noX, noY, btnW, btnH);
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // restart batch to draw text on top
        batch.begin();
        if (quitButtonBounds != null) {
            font.draw(batch, "Quit", quitButtonBounds.x + 20, quitButtonBounds.y + 28);
        }
        if (showQuitConfirm) {
            float dialogWidth = 300;
            float dialogHeight = 150;
            float dialogX = world.boardCenterX - dialogWidth / 2;
            float dialogY = world.boardCenterY - dialogHeight / 2;
            float btnW = 80;
            float btnH = 40;
            float yesX = dialogX + 50;
            float yesY = dialogY + 40;
            float noX = dialogX + 170;
            float noY = dialogY + 40;

            font.draw(batch, "Quit game?", dialogX + 90, dialogY + 120);
            font.draw(batch, "Yes", yesX + 25, yesY + 28);
            font.draw(batch, "No",  noX  + 28, noY  + 28);
        }
        batch.end();
    }

    /**
     * Handles input for the UI.
     * @param touchPos world coordinates of the touch
     * @return true if the input was consumed (quit button or dialog), false otherwise
     */
    public boolean handleInput(Vector3 touchPos) {
        if (showQuitConfirm) {
            if (yesButtonBounds != null && yesButtonBounds.contains(touchPos.x, touchPos.y)) {
                Gdx.app.exit();   // quit game
                return true;
            } else if (noButtonBounds != null && noButtonBounds.contains(touchPos.x, touchPos.y)) {
                showQuitConfirm = false;
                return true;
            }
            return true; // any click while dialog is open is consumed (except we already handled Yes/No)
        }

        if (quitButtonBounds != null && quitButtonBounds.contains(touchPos.x, touchPos.y)) {
            showQuitConfirm = true;
            return true;
        }
        return false;
    }
}
