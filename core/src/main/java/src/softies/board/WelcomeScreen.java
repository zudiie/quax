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

// draws the welcome screen: full-screen dark overlay + centred card
// shows QUAX title, "Human vs Bot" subtitle, and two buttons: Play Game / Quit

public class WelcomeScreen {

    private final WorldCalculator world;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    // card dimensions
    private static final float CARD_W  = 380f;
    private static final float CARD_H  = 310f;
    private static final float BTN_W   = 280f;
    private static final float BTN_H   =  58f;
    private static final float BTN_GAP =  18f;

    // button rectangles recomputed every draw call
    private Rectangle playBounds;
    private Rectangle quitBounds;

    // full-screen dim overlay - very dark navy
    private static final Color OVERLAY    = new Color(0.02f, 0.04f, 0.10f, 0.88f);

    // card - dark blue, gold border
    private static final Color CARD_BG    = new Color(0.06f, 0.10f, 0.22f, 0.98f);
    private static final Color CARD_INNER = new Color(0.08f, 0.14f, 0.28f, 0.98f); // slightly lighter inner
    private static final Color GOLD       = new Color(0.82f, 0.67f, 0.12f, 1f);

    // title / subtitle
    private static final Color TITLE_COL    = new Color(0.96f, 0.83f, 0.25f, 1f); // warm gold
    private static final Color SUBTITLE_COL = new Color(0.55f, 0.87f, 0.97f, 1f); // light cyan

    // Play Game - green
    private static final Color PLAY_IDLE  = new Color(0.08f, 0.38f, 0.14f, 1f);
    private static final Color PLAY_HOVER = new Color(0.13f, 0.54f, 0.22f, 1f);

    // Quit - deep red
    private static final Color QUIT_IDLE  = new Color(0.38f, 0.07f, 0.07f, 1f);
    private static final Color QUIT_HOVER = new Color(0.54f, 0.13f, 0.13f, 1f);

    /**
     * @param world     board bounds - used to compute card centre position
     * @param viewport  for full-screen overlay sizing
     * @param camera    for ShapeRenderer projection and world coordinates
     * @param gameState accepted for source-level compatibility; not used in this class
     */
    public WelcomeScreen(WorldCalculator world, Viewport viewport,
                         OrthographicCamera camera, GameState gameState) {
        this.world    = world;
        this.viewport = viewport;
        this.camera   = camera;
    }

    /**
     * draws the full welcome screen
     * signature matches the original so Main does not need to change
     */
    public void draw(SpriteBatch batch, BitmapFont font,
                     BitmapFont welcomeFont, ShapeRenderer sr, Vector3 mouse) {
        computeButtonBounds();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawBackground(sr, mouse);

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        drawTitleText(batch, welcomeFont);
        drawButtonLabels(batch, font);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // private drawing
    // -------------------------------------------------------------------------

    /**
     * draws the dim overlay, card panel, an inner highlight strip, and both buttons
     */
    private void drawBackground(ShapeRenderer sr, Vector3 mouse) {
        sr.setProjectionMatrix(camera.combined);

        float cx    = camera.position.x;
        float cy    = camera.position.y;
        float cardX = cx - CARD_W / 2f;
        float cardY = cy - CARD_H / 2f;

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // full-screen dark overlay
        float wl = camera.position.x - viewport.getWorldWidth()  / 2f;
        float wb = camera.position.y - viewport.getWorldHeight() / 2f;
        sr.setColor(OVERLAY);
        sr.rect(wl, wb, viewport.getWorldWidth(), viewport.getWorldHeight());

        // card background
        sr.setColor(CARD_BG);
        sr.rect(cardX, cardY, CARD_W, CARD_H);

        // subtle inner highlight strip at the top of the card (title area)
        sr.setColor(CARD_INNER);
        sr.rect(cardX + 4f, cardY + CARD_H - 110f, CARD_W - 8f, 106f);

        // Play Game button
        sr.setColor(playBounds.contains(mouse.x, mouse.y) ? PLAY_HOVER : PLAY_IDLE);
        sr.rect(playBounds.x, playBounds.y, playBounds.width, playBounds.height);

        // Quit button
        sr.setColor(quitBounds.contains(mouse.x, mouse.y) ? QUIT_HOVER : QUIT_IDLE);
        sr.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);

        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        // double gold border: outer (full card) and inner (title area bottom edge)
        sr.setColor(GOLD);
        sr.rect(cardX, cardY, CARD_W, CARD_H);
        // separator between title area and button area - a single horizontal line
        sr.line(cardX + 10f, cardY + CARD_H - 116f, cardX + CARD_W - 10f, cardY + CARD_H - 116f);
        // button borders
        sr.rect(playBounds.x, playBounds.y, playBounds.width, playBounds.height);
        sr.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);
        sr.end();
    }

    /**
     * draws "QUAX" in warm gold and "Human vs Bot" in cyan, centred in the title area
     */
    private void drawTitleText(SpriteBatch batch, BitmapFont welcomeFont) {
        float cx = camera.position.x;
        float cy = camera.position.y;

        welcomeFont.setColor(TITLE_COL);
        String title = "QUAX";
        GlyphLayout tl = new GlyphLayout(welcomeFont, title);
        welcomeFont.draw(batch, title, cx - tl.width / 2f, cy + CARD_H / 2f - 24f);

        welcomeFont.setColor(SUBTITLE_COL);
        String sub = "Human vs Bot";
        GlyphLayout sl = new GlyphLayout(welcomeFont, sub);
        welcomeFont.draw(batch, sub, cx - sl.width / 2f, cy + CARD_H / 2f - 72f);
    }

    /** draws "Play Game" and "Quit" labels centred in their buttons */
    private void drawButtonLabels(SpriteBatch batch, BitmapFont font) {
        font.setColor(Color.WHITE);
        drawCentred(batch, font, "Play Game", playBounds);
        drawCentred(batch, font, "Quit",      quitBounds);
    }

    /**
     * processes a click on the welcome screen
     * WelcomeAction enum contains only NONE / START / QUIT_CONFIRM since mode
     * selection has been removed from this class
     */
    public WelcomeAction handleInput(Vector3 touchPos) {
        if (playBounds != null && playBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.START;
        if (quitBounds != null && quitBounds.contains(touchPos.x, touchPos.y))
            return WelcomeAction.QUIT_CONFIRM;
        return WelcomeAction.NONE;
    }

    // what the user did - SELECT_* removed (only one mode exists)
    public enum WelcomeAction { NONE, START, QUIT_CONFIRM }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /** computes button rectangles centred in the lower half of the card */
    private void computeButtonBounds() {
        float cx   = camera.position.x;
        float cy   = camera.position.y;
        float btnX = cx - BTN_W / 2f;

        // Play Game button sits above centre; Quit button sits below
        playBounds = new Rectangle(btnX, cy - 32f,                 BTN_W, BTN_H);
        quitBounds = new Rectangle(btnX, cy - 32f - BTN_H - BTN_GAP, BTN_W, BTN_H);
    }

    private void drawCentred(SpriteBatch batch, BitmapFont font, String text, Rectangle r) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text,
            r.x + (r.width  - gl.width)  / 2f,
            r.y + (r.height + gl.height) / 2f);
    }
}
