package src.softies.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.GameMode;
import src.softies.PlayerColour;

// coordinates all in-game text drawing:
//   SidePanelRenderer — objectives and You/Bot player panel on the right side
//   WinOverlay        — full-width winner banner on top of everything when the game ends
// also draws board labels, title and turn indicator directly
//
// DRAW ORDER when game is over:
//   1. board labels + side panel (appear behind the overlay)
//   2. win overlay drawn last — covers the column letters and title
public class BoardRenderer {

    // what the user clicked in the win overlay — returned by handleInput()
    public enum InputResult { NONE, RESTART, QUIT }

    private final WorldCalculator world;
    private final GameState gameState;
    private final Viewport viewport;

    private final SidePanelRenderer sidePanel;
    private final WinOverlay        winOverlay;

    private static final Color NEAR_BLACK = new Color(0.10f, 0.10f, 0.10f, 1f);
    private static final Color STATUS_RED = new Color(0.95f, 0.35f, 0.35f, 1f);

    /**
     * @param world     board bounds and tile dimensions for label positioning
     * @param gameState current player, colours, bot colour and game-over state
     * @param viewport  screen dimensions for centring text
     * @param camera    passed to WinOverlay for its ShapeRenderer projection matrix
     */
    public BoardRenderer(WorldCalculator world, GameState gameState,
                         Viewport viewport, OrthographicCamera camera) {
        this.world    = world;
        this.gameState = gameState;
        this.viewport = viewport;

        this.sidePanel  = new SidePanelRenderer(world, gameState);
        // WinOverlay receives gameState so it can read botColour for the sub-message
        this.winOverlay = new WinOverlay(camera, viewport, gameState);
    }

    /**
     * draws all in-game text for one frame
     * when the game is over, board labels and side panel are drawn first and
     * the win overlay is drawn last so it visually covers the column letters
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
        font.setColor(Color.WHITE);
    }

    /**
     * forwards a world-space click to the WinOverlay when the game has ended
     * Main uses the returned InputResult to decide whether to restart or quit
     *
     * @param touchPos world-space click position (already unprojected)
     * @return RESTART, QUIT, or NONE
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

    /** draws "Current Turn: BLACK/WHITE" centred at the bottom of the screen */
    private void drawTurnIndicator(SpriteBatch batch, BitmapFont font) {
        PlayerColour current = gameState.getCurrentPlayer();
        String prefix = "Current Turn: ";
        String suffix = current.toString();

        GlyphLayout pl = new GlyphLayout(font, prefix);
        GlyphLayout sl = new GlyphLayout(font, suffix);
        float baseX = (viewport.getWorldWidth() - pl.width - sl.width) / 2f;

        font.setColor(Color.WHITE);
        font.draw(batch, prefix, baseX, 45f);
        font.setColor(current == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
        font.draw(batch, suffix, baseX + pl.width, 45f);
    }

    /** draws the status message near the top in red — only when non-empty */
    private void drawStatusMessage(SpriteBatch batch, BitmapFont font, String msg) {
        if (msg == null || msg.isEmpty()) return;
        font.setColor(STATUS_RED);
        GlyphLayout gl = new GlyphLayout(font, msg);
        font.draw(batch, msg,
            (viewport.getWorldWidth() - gl.width) / 2f,
            viewport.getWorldHeight() - 20f);
    }

    /** draws row numbers, column letters and the game title — all in white */
    private void drawBoardLabels(SpriteBatch batch, BitmapFont font) {
        font.setColor(Color.WHITE);

        for (int row = 1; row <= 11; row++) {
            float y = 70f + world.boardMinY + (row - 1) * world.tileHeightWorld
                + world.tileHeightWorld / 2;
            font.draw(batch, String.valueOf(row), world.boardMinX - 30f, y);
        }

        for (int col = 0; col < 11; col++) {
            float x = -8f + world.boardMinX + col * world.tileWidthWorld
                + world.tileWidthWorld / 2;
            font.draw(batch, String.valueOf((char)('A' + col)), x, world.boardMaxY + 90f);
        }

        String title = (gameState.getGameMode() == GameMode.HUMAN_VS_BOT)
            ? "Quax: Human vs Bot"
            : "Quax: Human vs Human";
        GlyphLayout tl = new GlyphLayout(font, title);
        font.draw(batch, title, world.boardCenterX - tl.width / 2f, world.boardMaxY + 130f);
    }
}
