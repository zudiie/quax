package src.softies.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Responsible for drawing the game board, row numbers, column letters, turn text, and title.
 */
public class BoardRenderer {
    private final WorldCalculator world;
    private final GameState gameState;
    private final Viewport viewport;

    public BoardRenderer(WorldCalculator world, GameState gameState, Viewport viewport) {
        this.world = world;
        this.gameState = gameState;
        this.viewport = viewport;
    }

    /**
     * Draws all board labels and status messages.
     * @param batch the SpriteBatch to use
     * @param font the font to draw with
     * @param statusMessage optional temporary message (e.g., "Invalid move")
     */
    public void render(SpriteBatch batch, BitmapFont font, String statusMessage) {
        // draw current player's turn text at the top center
        String turnText = "Current Turn: " + gameState.getCurrentPlayer();
        GlyphLayout turnLayout = new GlyphLayout(font, turnText);
        float turnX = (viewport.getWorldWidth() - turnLayout.width) / 2f;
        float turnY = 45;
        font.draw(batch, turnText, turnX, turnY);

        // draw status message if present (just below the turn text)
        if (statusMessage != null && !statusMessage.isEmpty()) {
            GlyphLayout statusLayout = new GlyphLayout(font, statusMessage);
            float statusX = (viewport.getWorldWidth() - statusLayout.width) / 2f;
            float statusY = viewport.getWorldHeight() - 20f;
            font.draw(batch, statusMessage, statusX, statusY);
        }

        // row numbers on the left (1 to 11, bottom to top)
        for (int row = 1; row <= 11; row++) {
            float y = 70f + world.boardMinY + (row - 1) * world.tileHeightWorld + world.tileHeightWorld / 2;
            String number = String.valueOf(row);
            float x = world.boardMinX - 30f;
            font.draw(batch, number, x, y);
        }

        // column letters above the board (A to K)
        for (int col = 0; col < 11; col++) {
            float x = -8f + world.boardMinX + col * world.tileWidthWorld + world.tileWidthWorld / 2;
            char letter = (char) ('A' + col);
            float y = world.boardMaxY + 90f;
            font.draw(batch, String.valueOf(letter), x, y);
        }

        // game title centered above the board
        String title = "Quax: Human vs Human";   // could be dynamic based on mode
        GlyphLayout titleLayout = new GlyphLayout(font, title);
        float titleX = world.boardCenterX - titleLayout.width / 2;
        float titleY = world.boardMaxY + 130f;
        font.draw(batch, title, titleX, titleY);

        // --- Configuration ---
        float rightMarginX = world.boardMaxX + 40f;
        float verticalSpacing = 40f; // Gap between list items
        float headerOffset = 250f;   // How high the list starts above the board center

// --- 1. Draw "OBJECTIVES:" Header ---
        String header = "OBJECTIVES:";
        font.setColor(Color.WHITE); // Standard UI Color
        float headerY = world.boardCenterY + headerOffset;
        font.draw(batch, header, rightMarginX, headerY);

// --- 2. Draw WHITE Objective ---
// Format: "• WHITE: Left to Right"
        String whiteObj = "• WHITE: Left to Right";
        float whiteY = headerY - verticalSpacing;
        font.draw(batch, whiteObj, rightMarginX, whiteY);

// --- 3. Draw BLACK Objective ---
// Format: "• BLACK: Top to Bottom"
        String blackObj = "• BLACK: Top to Bottom";
        float blackY = whiteY - verticalSpacing;

        font.setColor(Color.BLACK); // Match the player color
        font.draw(batch, blackObj, rightMarginX, blackY);

// --- 4. RESET ---
        font.setColor(Color.WHITE); // Reset so other UI elements aren't black
    }
}
