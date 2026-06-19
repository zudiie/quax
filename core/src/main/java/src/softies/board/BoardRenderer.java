package src.softies.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.PlayerColour;

// coordinates all in-game text drawing:
//   SidePanelRenderer - objectives and You/Bot player panel (right side)
//   WinOverlay        - full-width winner banner drawn on top when the game ends
// also draws board labels, title and the turn indicator directly
//
// TURN INDICATOR POSITION FIX:
//   The indicator was previously drawn at absolute y = 45f.  With typical viewport
//   values, worldBottom ≈ -22, making the button row (at worldBottom+20 ≈ -2) and
//   the indicator almost coincide.  It is now drawn at camera-relative
//   worldBottom + 100f, which keeps it clearly above all bottom-bar buttons.
//
// DRAW ORDER when game is over:
//   1. board labels + side panel (appear behind the overlay)
//   2. win overlay (drawn last - visually covers the column letters and title)
public class BoardRenderer {

    public enum InputResult { NONE, RESTART, QUIT }

    private final WorldCalculator world;
    private final GameState gameState;
    private final Viewport viewport;
    private final OrthographicCamera camera;   // stored so we can compute worldBottom

    private final SidePanelRenderer sidePanel;
    private final WinOverlay        winOverlay;

    private static final Color NEAR_BLACK = new Color(0.11f, 0.063f, 0.024f, 1f);
    private static final Color STATUS_RED = new Color(0.88f, 0.35f, 0.22f, 1f);

    public BoardRenderer(WorldCalculator world, GameState gameState,
                         Viewport viewport, OrthographicCamera camera) {
        this.world     = world;
        this.gameState = gameState;
        this.viewport  = viewport;
        this.camera    = camera;

        this.sidePanel  = new SidePanelRenderer(world, gameState);
        this.winOverlay = new WinOverlay(camera, viewport, gameState);
    }

    /**
     * draws all in-game text for one frame
     * when the game is over the win overlay is drawn last so it covers board labels
     */
    public void render(SpriteBatch batch, BitmapFont font,
                       ShapeRenderer shapeRenderer, String statusMessage) {
        if (gameState.isGameOver()) {
            drawBoardLabels(batch, font);
            sidePanel.render(batch, font);
            winOverlay.draw(batch, shapeRenderer, font, gameState.getWinner());
        } else {
            drawTurnIndicator(batch, font);
            drawStatusMessage(batch, font, statusMessage);
            drawBoardLabels(batch, font);
            sidePanel.render(batch, font);
        }
        font.setColor(new Color(0.910f, 0.835f, 0.690f, 1f));
    }

    /**
     * forwards a world-space click to the WinOverlay (Play Again / Quit buttons)
     * only active after the game has ended
     */
    public InputResult handleInput(Vector3 touchPos) {
        if (!gameState.isGameOver()) return InputResult.NONE;
        WinOverlay.Action action = winOverlay.handleInput(touchPos);
        switch (action) {
            case RESTART: return InputResult.RESTART;
            case QUIT:    return InputResult.QUIT;
            default:      return InputResult.NONE;
        }
    }

    // -------------------------------------------------------------------------
    // private drawing helpers
    // -------------------------------------------------------------------------

    /**
     * draws "Current Turn: BLACK/WHITE" centred just above the bottom-bar buttons
     * y is camera-relative (worldBottom + 100f) so it never overlaps the button row
     * regardless of viewport size - the button row sits at worldBottom + 20 to +64
     */
    private void drawTurnIndicator(SpriteBatch batch, BitmapFont font) {
        PlayerColour current  = gameState.getCurrentPlayer();
        String       prefix   = "Current Turn: ";
        String       suffix   = current.toString();

        GlyphLayout pl = new GlyphLayout(font, prefix);
        GlyphLayout sl = new GlyphLayout(font, suffix);
        float baseX = (viewport.getWorldWidth() - pl.width - sl.width) / 2f;

        // compute y from camera so this is always above the widget buttons
        float worldBottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float y = worldBottom + 100f;

        font.setColor(new Color(0.910f, 0.835f, 0.690f, 1f));
        font.draw(batch, prefix, baseX, y);
        font.setColor(current == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
        font.draw(batch, suffix, baseX + pl.width, y);
    }

    /** draws the temporary status message in red, centred just below the board
     *  horizontally centred over the board (not the full viewport) so it never
     *  overlaps the objectives / players panel on the right side
     */
    private void drawStatusMessage(SpriteBatch batch, BitmapFont font, String msg) {
        if (msg == null || msg.isEmpty()) return;
        font.setColor(STATUS_RED);
        GlyphLayout gl = new GlyphLayout(font, msg);
        float x = world.boardCenterX - gl.width / 2f;
        float y = world.boardMinY - 80f;
        font.draw(batch, msg, x, y);
    }

    /** draws row numbers (1-11), column letters (A-K) and the game title - all white */
    private void drawBoardLabels(SpriteBatch batch, BitmapFont font) {
        font.setColor(Color.WHITE);

        for (int row = 1; row <= 11; row++) {
            float y = 70f + world.boardMinY + (row - 1) * world.tileHeightWorld
                + world.tileHeightWorld / 2;
            font.draw(batch, String.valueOf(row), world.boardMinX - 58f, y);
        }

        for (int col = 0; col < 11; col++) {
            float x = -8f + world.boardMinX + col * world.tileWidthWorld
                + world.tileWidthWorld / 2;
            font.draw(batch, String.valueOf((char)('A' + col)), x, world.boardMaxY + 116f);
        }

        String title = "Quax: Human vs Bot";
        GlyphLayout tl = new GlyphLayout(font, title);
        font.draw(batch, title, world.boardCenterX - tl.width / 2f, world.boardMaxY + 160f);
    }
}
