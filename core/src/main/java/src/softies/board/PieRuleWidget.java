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

// manages the "Activate Pie Rule" button and the banner shown after activation
// the button is only visible during WHITE's first turn (controlled via GameState)
// after activation a message banner shows at the top of the screen for a few seconds
public class PieRuleWidget {

    private final GameState gameState;
    private final WorldCalculator world;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    private Rectangle bounds;

    // how many seconds the activation banner stays on screen
    private float bannerTimer = 0f;
    private static final float BANNER_DURATION = 3.5f;

    // button: same blue+gold theme as QuitWidget
    private static final Color BTN_IDLE  = new Color(0.14f, 0.24f, 0.62f, 1f);
    private static final Color BTN_HOVER = new Color(0.22f, 0.36f, 0.80f, 1f);
    private static final Color GOLD      = new Color(0.82f, 0.67f, 0.12f, 1f);

    // banner colours
    private static final Color BANNER_BG = new Color(0.10f, 0.18f, 0.52f, 0.92f);

    /**
     * @param gameState consulted each frame to decide whether the button should be visible
     * @param world     for positioning the button next to the quit button
     * @param viewport  for world-size queries and mouse unprojecting
     * @param camera    for the ShapeRenderer projection matrix
     */
    public PieRuleWidget(GameState gameState, WorldCalculator world,
                         Viewport viewport, OrthographicCamera camera) {
        this.gameState = gameState;
        this.world     = world;
        this.viewport  = viewport;
        this.camera    = camera;
    }

    /**
     * recomputes the button rectangle — call every frame before draw() and handleInput()
     * positions the button to the left of the quit button (quit is at worldRight - 110)
     */
    public void updateBounds() {
        if (!gameState.isPieRuleAvailable()) {
            bounds = null;
            return;
        }
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
        float pW = 190, pH = 44;
        float qX = worldRight - 90 - 20; // matches QuitWidget quit button x
        bounds = new Rectangle(qX - pW - 14, worldBottom + 20, pW, pH);
    }

    /**
     * ticks the banner timer — call every frame
     * @param delta Gdx.graphics.getDeltaTime()
     */
    public void update(float delta) {
        if (bannerTimer > 0f) bannerTimer -= delta;
    }

    /**
     * draws the button (when available) and the activation banner (when recently activated)
     * @param shapeRenderer for backgrounds / borders
     * @param batch         for text (not active when called — managed internally)
     * @param font          font for button label and banner text
     */
    public void draw(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);

        // --- pie rule button ---
        if (bounds != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(bounds.contains(mouse.x, mouse.y) ? BTN_HOVER : BTN_IDLE);
            shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(GOLD);
            shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            shapeRenderer.end();
        }

        // --- activation banner ---
        if (bannerTimer > 0f) {
            // banner sits just below the top of the visible world area
            float worldTop  = camera.position.y + viewport.getWorldHeight() / 2;
            float worldLeft = camera.position.x - viewport.getWorldWidth()  / 2;
            float bH = 54f;
            float bY = worldTop - bH - 10f;

            // fade alpha as the timer runs out
            float alpha = Math.min(1f, bannerTimer / 0.6f);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(BANNER_BG.r, BANNER_BG.g, BANNER_BG.b, BANNER_BG.a * alpha);
            shapeRenderer.rect(worldLeft, bY, viewport.getWorldWidth(), bH);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(GOLD.r, GOLD.g, GOLD.b, alpha);
            shapeRenderer.rect(worldLeft, bY, viewport.getWorldWidth(), bH);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

            batch.begin();
            font.setColor(1f, 1f, 1f, alpha);
            String msg = "Pie Rule Activated - Colours Swapped! WHITE to play.";
            GlyphLayout gl = new GlyphLayout(font, msg);
            font.draw(batch, msg,
                worldLeft + (viewport.getWorldWidth() - gl.width)  / 2f,
                bY + (bH + gl.height) / 2f);
            font.setColor(Color.WHITE);
            batch.end();
        } else {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // --- button label ---
        if (bounds != null) {
            batch.begin();
            font.setColor(Color.WHITE);
            GlyphLayout gl = new GlyphLayout(font, "Activate Pie Rule");
            font.draw(batch, "Activate Pie Rule",
                bounds.x + (bounds.width  - gl.width)  / 2f,
                bounds.y + (bounds.height + gl.height) / 2f);
            batch.end();
        }
    }

    /**
     * processes a click — returns true if the button was clicked
     * @param touchPos world-space click position
     * @return true if consumed
     */
    public boolean handleInput(Vector3 touchPos) {
        if (bounds != null && bounds.contains(touchPos.x, touchPos.y)) {
            gameState.activatePieRule();
            bannerTimer = BANNER_DURATION; // start the banner countdown
            return true;
        }
        return false;
    }
}
