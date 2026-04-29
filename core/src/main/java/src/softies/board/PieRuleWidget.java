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
// the button is only visible during WHITE's first turn (controlled via GameState.isPieRuleAvailable)
// after activation a banner fades in at the top of the screen for BANNER_DURATION seconds
public class PieRuleWidget {

    private final GameState gameState;
    private final WorldCalculator world;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    private Rectangle bounds; // null when button is not available

    // seconds the activation banner stays on screen
    private float bannerTimer = 0f;
    private static final float BANNER_DURATION = 3.5f;

    // button colours - matches QuitWidget theme
    private static final Color BTN_IDLE  = new Color(0.243f, 0.145f, 0.063f, 1f);
    private static final Color BTN_HOVER = new Color(0.361f, 0.227f, 0.094f, 1f);
    private static final Color GOLD      = new Color(0.753f, 0.471f, 0.251f, 1f);

    // banner background
    private static final Color BANNER_BG = new Color(0.133f, 0.082f, 0.039f, 0.96f);

    public PieRuleWidget(GameState gameState, WorldCalculator world,
                         Viewport viewport, OrthographicCamera camera) {
        this.gameState = gameState;
        this.world     = world;
        this.viewport  = viewport;
        this.camera    = camera;
    }

    /**
     * recomputes the button rectangle from the current camera position
     * positioned to the left of the quit button - call every frame before draw/handleInput
     */
    public void updateBounds() {
        if (!gameState.isPieRuleAvailable()) {
            bounds = null;
            return;
        }
        float worldRight  = camera.position.x + viewport.getWorldWidth()  / 2;
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2;
        // sit 14px to the left of the quit button (quit is at worldRight - 90 - 20)
        float qX = worldRight - 90 - 20;
        bounds = new Rectangle(qX - 190 - 14, worldBottom + 20, 190, 44);
    }

    /**
     * ticks the banner countdown - call every frame with getDeltaTime()
     */
    public void update(float delta) {
        if (bannerTimer > 0f) bannerTimer -= delta;
    }

    /**
     * draws the button (when available) and the activation banner (when recently used)
     * manages batch begin/end internally - do not call with an active batch
     */
    public void draw(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        Vector3 mouse = getMouseWorldPos();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(camera.combined);

        drawButton(sr, mouse);
        drawBanner(sr, batch, font);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // button label is drawn separately (banner already handled its own batch)
        if (bounds != null) {
            boolean botTurn = gameState.isBotTurn();
            float alpha = botTurn ? 0.50f : 1f;
            batch.begin();
            font.setColor(0.910f, 0.835f, 0.690f, alpha);
            GlyphLayout gl = new GlyphLayout(font, "Activate Pie Rule");
            font.draw(batch, "Activate Pie Rule",
                bounds.x + (bounds.width  - gl.width)  / 2f,
                bounds.y + (bounds.height + gl.height) / 2f);
            font.setColor(Color.WHITE);
            batch.end();
        }
    }

    /**
     * draws the blue button background and gold border
     */
    private void drawButton(ShapeRenderer sr, Vector3 mouse) {
        if (bounds == null) return;

        // when it is the bot's turn the button is visible but locked - 50% opacity
        boolean botTurn = gameState.isBotTurn();
        float alpha = botTurn ? 0.50f : 1f;

        Color fill = botTurn ? BTN_IDLE : (bounds.contains(mouse.x, mouse.y) ? BTN_HOVER : BTN_IDLE);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(fill.r, fill.g, fill.b, alpha);
        sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD.r, GOLD.g, GOLD.b, alpha);
        sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        sr.end();
    }

    /**
     * draws the full-width banner that fades in at the top of the screen after activation
     * only shown while bannerTimer is positive; fades out in the last 0.6 seconds
     */
    private void drawBanner(ShapeRenderer sr, SpriteBatch batch, BitmapFont font) {
        if (bannerTimer <= 0f) return;

        float worldTop  = camera.position.y + viewport.getWorldHeight() / 2;
        float worldLeft = camera.position.x - viewport.getWorldWidth()  / 2;
        float bH = 54f;
        float bY = worldTop - bH - 10f;

        // alpha fades to 0 in the last 0.6 seconds of the banner lifetime
        float alpha = Math.min(1f, bannerTimer / 0.6f);

        drawBannerBackground(sr, worldLeft, bY, bH, alpha);
        drawBannerText(batch, font, worldLeft, bY, bH, alpha);
    }

    /** draws the dark-blue background and gold border for the banner */
    private void drawBannerBackground(ShapeRenderer sr, float worldLeft, float bY,
                                      float bH, float alpha) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BANNER_BG.r, BANNER_BG.g, BANNER_BG.b, BANNER_BG.a * alpha);
        sr.rect(worldLeft, bY, viewport.getWorldWidth(), bH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD.r, GOLD.g, GOLD.b, alpha);
        sr.rect(worldLeft, bY, viewport.getWorldWidth(), bH);
        sr.end();
    }

    /** draws the centred banner message with fading alpha */
    private void drawBannerText(SpriteBatch batch, BitmapFont font,
                                float worldLeft, float bY, float bH, float alpha) {
        Gdx.gl.glDisable(GL20.GL_BLEND); // re-enabled inside batch.begin/end via BlendFunc
        batch.begin();
        font.setColor(1f, 1f, 1f, alpha);
        String msg = "Pie Rule Activated - Colours Swapped! WHITE to play.";
        GlyphLayout gl = new GlyphLayout(font, msg);
        font.draw(batch, msg,
            worldLeft + (viewport.getWorldWidth() - gl.width)  / 2f,
            bY + (bH + gl.height) / 2f);
        font.setColor(Color.WHITE);
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
    }

    /**
     * processes a click - activates the pie rule and starts the banner if the button was hit
     * @return true if the button was clicked and consumed the input
     */
    public boolean handleInput(Vector3 touchPos) {
        if (bounds != null && bounds.contains(touchPos.x, touchPos.y)) {
            if (gameState.isBotTurn()) return true; // consume click but do nothing
            gameState.activatePieRule();
            bannerTimer = BANNER_DURATION;
            return true;
        }
        return false;
    }

    /**
     * starts the activation banner without a click - called by Main when the bot
     * activates the pie rule programmatically so the player sees the same visual
     * confirmation that appears when a human clicks the button
     */
    public void showBanner() {
        bannerTimer = BANNER_DURATION;
    }

    private Vector3 getMouseWorldPos() {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);
        return mouse;
    }
}
