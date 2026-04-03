package src.softies.board;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.PlayerColour;

// draws all in-game text: board labels, title, objectives, player panel, turn indicator
// when the game is over it draws a centred win announcement overlay instead of the turn text
// colour rule: text referring to WHITE is drawn white, text referring to BLACK is drawn near-black
public class BoardRenderer {

    private final WorldCalculator world;
    private final GameState gameState;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    // near-black that stays visible on the grey (0.5, 0.5, 0.5) game background
    private static final Color NEAR_BLACK  = new Color(0.10f, 0.10f, 0.10f, 1f);
    // dimmed colour for the waiting player's name row
    private static final Color MUTED       = new Color(0.50f, 0.50f, 0.50f, 1f);
    // gold arrow marking the active player's row
    private static final Color ARROW_GOLD  = new Color(0.90f, 0.72f, 0.12f, 1f);
    // red for invalid-move status messages
    private static final Color STATUS_RED  = new Color(0.95f, 0.35f, 0.35f, 1f);
    // winner banner background — dark blue with gold border (matches the button theme)
    private static final Color WIN_BG      = new Color(0.06f, 0.12f, 0.40f, 0.92f);
    private static final Color GOLD_BORDER = new Color(0.82f, 0.67f, 0.12f, 1f);

    /**
     * @param world     board bounds and tile dimensions for positioning
     * @param gameState current player and colour assignments
     * @param viewport  screen dimensions for centering text
     * @param camera    used to derive visible world bounds for the winner overlay
     */
    public BoardRenderer(WorldCalculator world, GameState gameState,
                         Viewport viewport, OrthographicCamera camera) {
        this.world     = world;
        this.gameState = gameState;
        this.viewport  = viewport;
        this.camera    = camera;
    }

    /**
     * draws all board UI text — call between batch.begin() and batch.end()
     * if the game is over, ends the batch, draws the winner overlay via ShapeRenderer,
     * then restarts the batch for the overlay text
     * @param shapeRenderer used for the winner overlay background — must not be mid-begin
     * @param batch         active SpriteBatch
     * @param font          font for all text
     * @param statusMessage temporary error/info message, empty string if none
     */
    public void render(SpriteBatch batch, BitmapFont font,
                       ShapeRenderer shapeRenderer, String statusMessage) {

        PlayerColour current = gameState.getCurrentPlayer();

        // ---------- winner overlay (game over) ----------
        if (gameState.isGameOver()) {
            PlayerColour winner = gameState.getWinner();

            // draw labels and panel FIRST so the overlay covers them
            drawBoardLabels(batch, font);
            drawSidePanel(batch, font);

            // end active batch to switch to ShapeRenderer for the banner background
            batch.end();

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            float bW = 520, bH = 110;
            float bX = world.boardCenterX - bW / 2f;
            float bY = world.boardCenterY - bH / 2f;

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(WIN_BG);
            shapeRenderer.rect(bX, bY, bW, bH);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(GOLD_BORDER);
            shapeRenderer.rect(bX, bY, bW, bH);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);

            batch.begin();

            // win line
            String winLine = winner.toString() + " wins!";
            GlyphLayout wl = new GlyphLayout(font, winLine);
            font.setColor(winner == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
            font.draw(batch, winLine, world.boardCenterX - wl.width / 2f, bY + bH - 20f);

            // sub-line
            String sub = "Well played! Restart the application to play again.";
            font.setColor(Color.WHITE);
            GlyphLayout sl = new GlyphLayout(font, sub);
            font.draw(batch, sub, world.boardCenterX - sl.width / 2f, bY + 40f);

            font.setColor(Color.WHITE);
            return;
        }

        // ---------- normal in-progress rendering ----------

        // "Current Turn:" in white, the colour name in its stone colour
        String prefix = "Current Turn: ";
        String suffix = current.toString();
        GlyphLayout pl = new GlyphLayout(font, prefix);
        GlyphLayout sl = new GlyphLayout(font, suffix);
        float baseX = (viewport.getWorldWidth() - pl.width - sl.width) / 2f;
        font.setColor(Color.WHITE);
        font.draw(batch, prefix, baseX, 45f);
        font.setColor(current == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
        font.draw(batch, suffix, baseX + pl.width, 45f);

        // status message near the top in red
        if (statusMessage != null && !statusMessage.isEmpty()) {
            font.setColor(STATUS_RED);
            GlyphLayout stl = new GlyphLayout(font, statusMessage);
            font.draw(batch, statusMessage,
                (viewport.getWorldWidth() - stl.width) / 2f,
                viewport.getWorldHeight() - 20f);
        }

        drawBoardLabels(batch, font);
        drawSidePanel(batch, font);
        font.setColor(Color.WHITE);
    }

    // draws row numbers and column letters — always white
    private void drawBoardLabels(SpriteBatch batch, BitmapFont font) {
        font.setColor(Color.WHITE);
        for (int row = 1; row <= 11; row++) {
            float y = 70f + world.boardMinY + (row - 1) * world.tileHeightWorld + world.tileHeightWorld / 2;
            font.draw(batch, String.valueOf(row), world.boardMinX - 30f, y);
        }
        for (int col = 0; col < 11; col++) {
            float x = -8f + world.boardMinX + col * world.tileWidthWorld + world.tileWidthWorld / 2;
            font.draw(batch, String.valueOf((char)('A' + col)), x, world.boardMaxY + 90f);
        }
        String title = "Quax: Human vs Human";
        GlyphLayout tl = new GlyphLayout(font, title);
        font.draw(batch, title, world.boardCenterX - tl.width / 2f, world.boardMaxY + 130f);
    }

    // draws the OBJECTIVES and PLAYERS sections on the right of the board
    private void drawSidePanel(SpriteBatch batch, BitmapFont font) {
        float rX      = world.boardMaxX + 40f;
        float spacing = 40f;
        float objY    = world.boardCenterY + 250f;

        // OBJECTIVES
        font.setColor(Color.WHITE);
        font.draw(batch, "OBJECTIVES:", rX, objY);
        font.setColor(Color.WHITE);
        font.draw(batch, "• WHITE: Left to Right", rX, objY - spacing);
        font.setColor(NEAR_BLACK);
        font.draw(batch, "• BLACK: Top to Bottom", rX, objY - spacing * 2);

        // PLAYERS
        float playersY = objY - spacing * 4.2f;
        font.setColor(Color.WHITE);
        font.draw(batch, "PLAYERS:", rX, playersY);

        PlayerColour current = gameState.getCurrentPlayer();
        PlayerColour p1 = gameState.getPlayer1Colour();
        PlayerColour p2 = gameState.getPlayer2Colour();
        boolean p1Active = (current == p1);

        drawPlayerRow(batch, font, "Player 1", p1, p1Active, rX, playersY - spacing * 1.3f);
        drawPlayerRow(batch, font, "Player 2", p2, !p1Active, rX, playersY - spacing * 2.5f);
    }

    // draws one player row: gold arrow if active, name, colour label in stone colour
    private void drawPlayerRow(SpriteBatch batch, BitmapFont font,
                               String name, PlayerColour colour, boolean active,
                               float x, float y) {
        if (active) {
            font.setColor(ARROW_GOLD);
            font.draw(batch, "->", x, y);
        }
        font.setColor(active ? Color.WHITE : MUTED);
        font.draw(batch, name, x + 28f, y);

        GlyphLayout nl = new GlyphLayout(font, name);
        // colour label in the stone's actual colour — near-black for BLACK, white for WHITE
        font.setColor(colour == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
        font.draw(batch, " (" + colour + ")", x + 28f + nl.width, y);
    }
}
