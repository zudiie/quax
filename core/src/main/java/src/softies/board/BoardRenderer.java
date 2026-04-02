package src.softies.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.PlayerColour;

// handles all text-based drawing on the game screen
// board letters/numbers and title stay white; text mentioning BLACK is drawn near-black,
// text mentioning WHITE is drawn white — so the colour always matches the stone
public class BoardRenderer {

    private final WorldCalculator world;
    private final GameState gameState;
    private final Viewport viewport;

    // near-black that stays readable on the grey (0.5, 0.5, 0.5) background
    private static final Color NEAR_BLACK    = new Color(0.08f, 0.08f, 0.08f, 1f);
    // dimmed colour for the waiting player's name
    private static final Color MUTED        = new Color(0.50f, 0.50f, 0.50f, 1f);
    // gold arrow that marks the active player's row
    private static final Color ARROW_GOLD   = new Color(0.90f, 0.72f, 0.12f, 1f);
    // soft red for the status message
    private static final Color STATUS_RED   = new Color(0.95f, 0.35f, 0.35f, 1f);

    /**
     * @param world     provides board bounds and tile dimensions for positioning
     * @param gameState used to read current player and player colour assignments
     * @param viewport  used to get screen dimensions for centering text
     */
    public BoardRenderer(WorldCalculator world, GameState gameState, Viewport viewport) {
        this.world     = world;
        this.gameState = gameState;
        this.viewport  = viewport;
    }

    /**
     * draws all board UI text — call between batch.begin() and batch.end()
     * @param batch         active SpriteBatch
     * @param font          font for all text
     * @param statusMessage temporary error/info message, empty string if none
     */
    public void render(SpriteBatch batch, BitmapFont font, String statusMessage) {

        PlayerColour current = gameState.getCurrentPlayer();

        // "Current Turn:" prefix in white, the player colour word in its matching colour
        String prefix = "Current Turn: ";
        String suffix = current.toString();
        GlyphLayout prefixL = new GlyphLayout(font, prefix);
        GlyphLayout suffixL = new GlyphLayout(font, suffix);
        float totalW = prefixL.width + suffixL.width;
        float baseX  = (viewport.getWorldWidth() - totalW) / 2f;

        font.setColor(Color.WHITE);
        font.draw(batch, prefix, baseX, 45f);
        font.setColor(current == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
        font.draw(batch, suffix, baseX + prefixL.width, 45f);

        // status message near the top in red — only when non-empty
        if (statusMessage != null && !statusMessage.isEmpty()) {
            font.setColor(STATUS_RED);
            GlyphLayout sl = new GlyphLayout(font, statusMessage);
            font.draw(batch, statusMessage,
                (viewport.getWorldWidth() - sl.width) / 2f,
                viewport.getWorldHeight() - 20f);
        }

        // row numbers on the left side — white
        font.setColor(Color.WHITE);
        for (int row = 1; row <= 11; row++) {
            float y = 70f + world.boardMinY + (row - 1) * world.tileHeightWorld + world.tileHeightWorld / 2;
            font.draw(batch, String.valueOf(row), world.boardMinX - 30f, y);
        }

        // column letters above the board — white
        for (int col = 0; col < 11; col++) {
            float x = -8f + world.boardMinX + col * world.tileWidthWorld + world.tileWidthWorld / 2;
            font.draw(batch, String.valueOf((char) ('A' + col)), x, world.boardMaxY + 90f);
        }

        // game title centred above the board — white
        String title = "Quax: Human vs Human";
        GlyphLayout tl = new GlyphLayout(font, title);
        font.draw(batch, title, world.boardCenterX - tl.width / 2f, world.boardMaxY + 130f);

        float rightX  = world.boardMaxX + 40f;
        float spacing = 40f;

        // --- OBJECTIVES section ---
        float objY = world.boardCenterY + 250f;
        font.setColor(Color.WHITE);
        font.draw(batch, "OBJECTIVES:", rightX, objY);

        // WHITE objective in white, BLACK objective in near-black
        font.setColor(Color.WHITE);
        font.draw(batch, "• WHITE: Left to Right", rightX, objY - spacing);
        font.setColor(NEAR_BLACK);
        font.draw(batch, "• BLACK: Top to Bottom", rightX, objY - spacing * 2);

        // --- PLAYERS section ---
        float playersY = objY - spacing * 4.2f;
        font.setColor(Color.WHITE);
        font.draw(batch, "PLAYERS:", rightX, playersY);

        PlayerColour p1col = gameState.getPlayer1Colour();
        PlayerColour p2col = gameState.getPlayer2Colour();
        boolean p1Active   = (current == p1col);

        drawPlayerRow(batch, font, "Player 1", p1col, p1Active, rightX, playersY - spacing * 1.3f);
        drawPlayerRow(batch, font, "Player 2", p2col, !p1Active, rightX, playersY - spacing * 2.5f);

        // always reset to white so nothing after this is tinted unexpectedly
        font.setColor(Color.WHITE);
    }

    /**
     * draws one player row: gold arrow if active, name, then colour label in the stone's actual colour
     * @param batch    active SpriteBatch
     * @param font     font to draw with
     * @param name     "Player 1" or "Player 2"
     * @param colour   their currently assigned PlayerColour
     * @param isActive true if it is this player's turn
     * @param x        left edge x
     * @param y        baseline y
     */
    private void drawPlayerRow(SpriteBatch batch, BitmapFont font,
                               String name, PlayerColour colour, boolean isActive,
                               float x, float y) {
        // gold arrow next to the active player, invisible gap for inactive
        if (isActive) {
            font.setColor(ARROW_GOLD);
            font.draw(batch, "->", x, y);
        }

        // player name — white when active, muted when waiting
        font.setColor(isActive ? Color.WHITE : MUTED);
        font.draw(batch, name, x + 28f, y);

        // colour label in the actual stone colour so it's immediately obvious
        // BLACK label uses near-black (visible on the grey background), WHITE label uses white
        GlyphLayout nameL = new GlyphLayout(font, name);
        float labelX = x + 28f + nameL.width + 8f;
        font.setColor(colour == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
        font.draw(batch, "(" + colour + ")", labelX, y);
    }
}
