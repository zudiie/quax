package src.softies.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import src.softies.GameMode;

// handles all the text-based drawing on the game board — row numbers, column letters,
// the current turn indicator, status messages and the objectives panel on the right
public class BoardRenderer {

    private final WorldCalculator world;
    private final GameState gameState;
    private final Viewport viewport;

    /**
     * sets up the renderer with the references it needs to draw labels and status text
     * @param world provides board bounds and tile dimensions for positioning
     * @param gameState used to read whose turn it is
     * @param viewport used to get the screen dimensions for centering text
     */
    public BoardRenderer(WorldCalculator world, GameState gameState, Viewport viewport) {
        this.world = world;
        this.gameState = gameState;
        this.viewport = viewport;
    }

    /**
     * draws all the board UI text in a single pass — must be called between batch.begin() and batch.end()
     * covers the turn indicator, status message, row/column labels, title and objectives panel
     * @param batch the active SpriteBatch to draw into
     * @param font the font to use for all text
     * @param statusMessage a temporary message to show (e.g. "Invalid move") — pass empty string if none
     */
    public void render(SpriteBatch batch, BitmapFont font, String statusMessage) {

        // centre the turn text horizontally at the bottom of the screen
        String turnText = "Current Turn: " + gameState.getCurrentPlayer();
        GlyphLayout turnLayout = new GlyphLayout(font, turnText);
        float turnX = (viewport.getWorldWidth() - turnLayout.width) / 2f;
        float turnY = 45;
        font.draw(batch, turnText, turnX, turnY);

        // only draw the status message if there's actually something to show
        if (statusMessage != null && !statusMessage.isEmpty()) {
            GlyphLayout statusLayout = new GlyphLayout(font, statusMessage);
            float statusX = (viewport.getWorldWidth() - statusLayout.width) / 2f;
            // pin it near the top of the screen so it doesn't overlap the board
            float statusY = viewport.getWorldHeight() - 20f;
            font.draw(batch, statusMessage, statusX, statusY);
        }

        // row numbers along the left side, spaced to match the tile height
        for (int row = 1; row <= 11; row++) {
            float y = 70f + world.boardMinY + (row - 1) * world.tileHeightWorld + world.tileHeightWorld / 2;
            String number = String.valueOf(row);
            // nudge slightly left of the board edge
            float x = world.boardMinX - 30f;
            font.draw(batch, number, x, y);
        }

        // column letters along the top, spaced to match tile width
        for (int col = 0; col < 11; col++) {
            float x = -8f + world.boardMinX + col * world.tileWidthWorld + world.tileWidthWorld / 2;
            char letter = (char) ('A' + col);
            float y = world.boardMaxY + 90f;
            font.draw(batch, String.valueOf(letter), x, y);
        }

        // game title centred above the board
        String title = (gameState.getGameMode() == GameMode.HUMAN_VS_BOT)
            ? "Quax: Human vs Bot"
            : "Quax: Human vs Human";
        GlyphLayout titleLayout = new GlyphLayout(font, title);
        float titleX = world.boardCenterX - titleLayout.width / 2;
        float titleY = world.boardMaxY + 130f;
        font.draw(batch, title, titleX, titleY);

        // --- objectives panel on the right side ---
        float rightMarginX  = world.boardMaxX + 40f;
        float verticalSpacing = 40f;
        float headerOffset  = 250f; // how far above the board centre to start the list

        // section header
        String header = "OBJECTIVES:";
        font.setColor(Color.WHITE);
        float headerY = world.boardCenterY + headerOffset;
        font.draw(batch, header, rightMarginX, headerY);

        // white's goal
        String whiteObj = "• WHITE: Left to Right";
        float whiteY = headerY - verticalSpacing;
        font.draw(batch, whiteObj, rightMarginX, whiteY);

        // black's goal — use black colour so it matches the player's stone
        String blackObj = "• BLACK: Top to Bottom";
        float blackY = whiteY - verticalSpacing;
        font.setColor(Color.BLACK);
        font.draw(batch, blackObj, rightMarginX, blackY);

        // reset the font colour so anything drawn after this isn't also black
        font.setColor(Color.WHITE);
    }
}
