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
import src.softies.PlayerColour;

// draws the winner announcement banner when the game ends
// spans the full viewport width so it covers the column letters and board title
//
// sub-message is context-aware:
//   human wins → "Congratulations!"
//   bot wins   → "Better luck next time!"
//
// two buttons are shown:
//   "Play Again" — signals RESTART to Main, which resets the board without exiting
//   "Quit"       — exits the application
public class WinOverlay {

    // what the user clicked — returned from handleInput() so Main can react
    public enum Action { NONE, RESTART, QUIT }

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final GameState gameState; // read to decide the sub-message

    private static final float BANNER_H = 160f;

    // banner
    private static final Color WIN_BG      = new Color(0.06f, 0.12f, 0.40f, 0.95f);
    private static final Color GOLD_BORDER = new Color(0.82f, 0.67f, 0.12f, 1f);
    private static final Color NEAR_BLACK  = new Color(0.10f, 0.10f, 0.10f, 1f);

    // "Play Again" — green
    private static final Color BTN_PLAY_IDLE  = new Color(0.10f, 0.38f, 0.10f, 1f);
    private static final Color BTN_PLAY_HOVER = new Color(0.16f, 0.56f, 0.16f, 1f);

    // "Quit" — dark red
    private static final Color BTN_QUIT_IDLE  = new Color(0.38f, 0.07f, 0.07f, 1f);
    private static final Color BTN_QUIT_HOVER = new Color(0.58f, 0.13f, 0.13f, 1f);

    // button rectangles — set during draw(), used for hit-testing in handleInput()
    private Rectangle playAgainBounds;
    private Rectangle quitBounds;

    /**
     * @param camera    for the ShapeRenderer projection matrix
     * @param viewport  for full-width banner sizing
     * @param gameState read to check botColour for the context-aware sub-message
     */
    public WinOverlay(OrthographicCamera camera, Viewport viewport, GameState gameState) {
        this.camera    = camera;
        this.viewport  = viewport;
        this.gameState = gameState;
    }

    /**
     * draws the full-width win banner centred on the visible screen
     * temporarily ends the active batch to draw ShapeRenderer backgrounds, then restarts it
     *
     * @param batch         active SpriteBatch — ended and restarted internally
     * @param shapeRenderer for backgrounds and borders — must not be mid-begin
     * @param font          font for all banner text
     * @param winner        the colour that won
     */
    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer,
                     BitmapFont font, PlayerColour winner) {

        float bW = viewport.getWorldWidth();
        float bX = camera.position.x - bW / 2f;
        float bY = camera.position.y - BANNER_H / 2f;

        // "Play Again" on the left, "Quit" on the right, both near the bottom of the banner
        float btnW = 180, btnH = 44, btnY = bY + 14f;
        float playX = bX + bW / 2f - btnW - 16f;
        float quitX = bX + bW / 2f + 16f;

        playAgainBounds = new Rectangle(playX, btnY, btnW, btnH);
        quitBounds      = new Rectangle(quitX, btnY, btnW, btnH);

        Vector3 mouse = getMouseWorldPos();

        drawBackground(batch, shapeRenderer, bX, bY, bW, mouse);
        drawText(batch, font, winner, bX, bY, bW);
        drawButtonLabels(batch, font, btnY, btnH);
    }

    /**
     * draws the banner background, gold border, and both button shapes via ShapeRenderer
     * temporarily ends the batch, draws shapes, then restarts the batch
     */
    private void drawBackground(SpriteBatch batch, ShapeRenderer sr,
                                float bX, float bY, float bW, Vector3 mouse) {
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(camera.combined);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        // banner background
        sr.setColor(WIN_BG);
        sr.rect(bX, bY, bW, BANNER_H);
        // Play Again button — green, brightens on hover
        sr.setColor(playAgainBounds.contains(mouse.x, mouse.y) ? BTN_PLAY_HOVER : BTN_PLAY_IDLE);
        sr.rect(playAgainBounds.x, playAgainBounds.y, playAgainBounds.width, playAgainBounds.height);
        // Quit button — dark red, brightens on hover
        sr.setColor(quitBounds.contains(mouse.x, mouse.y) ? BTN_QUIT_HOVER : BTN_QUIT_IDLE);
        sr.rect(quitBounds.x, quitBounds.y, quitBounds.width, quitBounds.height);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(GOLD_BORDER);
        sr.rect(bX, bY, bW, BANNER_H);
        sr.rect(playAgainBounds.x, playAgainBounds.y, playAgainBounds.width, playAgainBounds.height);
        sr.rect(quitBounds.x,      quitBounds.y,      quitBounds.width,      quitBounds.height);
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
    }

    /**
     * draws the winner headline and context-aware sub-message
     * winner name is in its stone colour: near-black for BLACK, white for WHITE
     */
    private void drawText(SpriteBatch batch, BitmapFont font,
                          PlayerColour winner, float bX, float bY, float bW) {
        // headline — e.g. "BLACK wins!"
        String headline = winner + " wins!";
        GlyphLayout hl = new GlyphLayout(font, headline);
        font.setColor(winner == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
        font.draw(batch, headline, bX + (bW - hl.width) / 2f, bY + BANNER_H - 22f);

        // sub-message — differs based on whether the bot or the human won
        boolean botWon = (winner == gameState.getBotColour());
        String sub = botWon ? "Better luck next time!" : "Congratulations!";
        font.setColor(Color.WHITE);
        GlyphLayout sl = new GlyphLayout(font, sub);
        font.draw(batch, sub, bX + (bW - sl.width) / 2f, bY + BANNER_H - 62f);
    }

    /** draws the text labels centred inside each button */
    private void drawButtonLabels(SpriteBatch batch, BitmapFont font, float btnY, float btnH) {
        font.setColor(Color.WHITE);
        drawCentred(batch, font, "Play Again", playAgainBounds);
        drawCentred(batch, font, "Quit",       quitBounds);
    }

    /**
     * processes a click in world space and returns what was clicked
     * called every touch event — returns NONE if neither button was hit
     *
     * @param touchPos world-space position of the click
     * @return RESTART if "Play Again" was clicked, QUIT if "Quit" was clicked, NONE otherwise
     */
    public Action handleInput(Vector3 touchPos) {
        if (playAgainBounds != null && playAgainBounds.contains(touchPos.x, touchPos.y))
            return Action.RESTART;
        if (quitBounds != null && quitBounds.contains(touchPos.x, touchPos.y))
            return Action.QUIT;
        return Action.NONE;
    }

    // --- helpers ---

    private Vector3 getMouseWorldPos() {
        Vector3 m = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(m);
        return m;
    }

    private void drawCentred(SpriteBatch batch, BitmapFont font, String text, Rectangle r) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text,
            r.x + (r.width  - gl.width)  / 2f,
            r.y + (r.height + gl.height) / 2f);
    }
}
