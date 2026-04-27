package src.softies.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import src.softies.PlayerColour;

// draws the right-side information panel: objectives and player assignments
// in Human vs Bot mode each colour slot is labelled either "You" or "Bot"
// colour rule: text about WHITE is white, text about BLACK is near-black
public class SidePanelRenderer {

    private final WorldCalculator world;
    private final GameState gameState;

    private static final float PANEL_OFFSET_X = 40f;  // distance to the right of the board
    private static final float SPACING        = 40f;  // vertical gap between lines
    private static final float OBJ_OFFSET_Y   = 320f; // how far above board centre the header sits

    // near-black readable on the grey (0.5, 0.5, 0.5) game background
    private static final Color NEAR_BLACK = new Color(0.11f, 0.063f, 0.024f, 1f);
    // gold arrow marking the active player's row
    private static final Color ARROW_GOLD = new Color(0.753f, 0.471f, 0.251f, 1f);
    // dimmed colour for the waiting player's name
    private static final Color MUTED      = new Color(0.580f, 0.435f, 0.251f, 1f);

    /**
     * @param world     board bounds used to position the panel to the right of the board
     * @param gameState read each frame for current player, colour assignments and bot colour
     */
    public SidePanelRenderer(WorldCalculator world, GameState gameState) {
        this.world     = world;
        this.gameState = gameState;
    }

    /**
     * draws the full right-side panel - call between batch.begin() and batch.end()
     * @param batch active SpriteBatch
     * @param font  font for all panel text
     */
    public void render(SpriteBatch batch, BitmapFont font) {
        float panelX = world.boardMaxX + PANEL_OFFSET_X;
        float objY   = world.boardCenterY + OBJ_OFFSET_Y;

        drawObjectives(batch, font, panelX, objY);
        drawPlayers(batch, font, panelX, objY - SPACING * 4.2f);
    }

    /**
     * draws the OBJECTIVES header and the two win-condition bullet points
     * WHITE bullet is white; BLACK bullet is near-black - matching the stone colours
     */
    private void drawObjectives(SpriteBatch batch, BitmapFont font, float x, float y) {
        font.setColor(new Color(0.910f, 0.835f, 0.690f, 1f));
        font.draw(batch, "OBJECTIVES:", x, y);

        font.setColor(new Color(0.910f, 0.835f, 0.690f, 1f));
        font.draw(batch, "• WHITE: Left to Right", x, y - SPACING);

        font.setColor(NEAR_BLACK);
        font.draw(batch, "• BLACK: Top to Bottom", x, y - SPACING * 2);
    }

    /**
     * draws the PLAYERS header and one row per colour
     * each row shows "You" or "Bot" based on which colour the bot is playing as
     */
    private void drawPlayers(SpriteBatch batch, BitmapFont font, float x, float y) {
        font.setColor(Color.WHITE);
        font.draw(batch, "PLAYERS:", x, y);

        PlayerColour current = gameState.getCurrentPlayer();
        PlayerColour botCol  = gameState.getBotColour();

        // draw BLACK row - label it "Bot" if bot is BLACK, "You" otherwise
        boolean blackActive = (current == PlayerColour.BLACK);
        String  blackLabel  = (botCol == PlayerColour.BLACK) ? "Bot" : "You";
        drawPlayerRow(batch, font, blackLabel, PlayerColour.BLACK, blackActive, x, y - SPACING * 1.3f);

        // draw WHITE row - label it "Bot" if bot is WHITE, "You" otherwise
        boolean whiteActive = (current == PlayerColour.WHITE);
        String  whiteLabel  = (botCol == PlayerColour.WHITE) ? "Bot" : "You";
        drawPlayerRow(batch, font, whiteLabel, PlayerColour.WHITE, whiteActive, x, y - SPACING * 2.5f);
    }

    /**
     * draws one player row:
     * - gold arrow if it is this colour's turn
     * - name in white (active) or muted (waiting)
     * - colour label in the stone's actual colour
     *
     * @param name   "You" or "Bot"
     * @param colour the colour this row represents
     * @param active true if it is currently this colour's turn
     */
    private void drawPlayerRow(SpriteBatch batch, BitmapFont font,
                               String name, PlayerColour colour, boolean active,
                               float x, float y) {
        // gold arrow next to the active row; nothing for the inactive one
        if (active) {
            font.setColor(ARROW_GOLD);
            font.draw(batch, "->", x, y);
        }

        // name - white when active, muted when waiting
        font.setColor(active ? Color.WHITE : MUTED);
        font.draw(batch, name, x + 28f, y);

        // colour label in the stone's actual colour so it's immediately obvious
        GlyphLayout nameLayout = new GlyphLayout(font, name);
        font.setColor(colour == PlayerColour.BLACK ? NEAR_BLACK : Color.WHITE);
        font.draw(batch, " (" + colour + ")", x + 28f + nameLayout.width, y);
    }
}
