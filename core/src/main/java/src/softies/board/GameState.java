package src.softies.board;

import src.softies.PlayerColour;
import java.util.Random;

// holds the live state of the game — current player, colour assignments, pie rule,
// game-over state, and which colour the bot is playing as
public class GameState {

    // each player's currently assigned colour — these swap when the pie rule is activated
    private PlayerColour player1Colour;
    private PlayerColour player2Colour;

    // the colour whose turn it is right now
    private PlayerColour currentPlayer;

    // the colour the bot is playing as — also swaps when pie rule is activated
    private PlayerColour botColour;

    // true once BLACK's very first stone has been placed
    private boolean firstMoveMade = false;

    // true only during WHITE's first turn — the only window when the pie rule can be used
    private boolean pieRuleAvailable = false;

    // set when a winner is detected; null while the game is still in progress
    private PlayerColour winner = null;

    /**
     * sets up the game with a randomly assigned bot colour
     * BLACK always moves first in Quax, so if the bot is BLACK it also moves first
     */
    public GameState() {
        player1Colour = PlayerColour.BLACK;
        player2Colour = PlayerColour.WHITE;
        currentPlayer = PlayerColour.BLACK;

        // randomly decide whether the bot plays BLACK or WHITE,
        // which effectively randomises who goes first each game
        botColour = new Random().nextBoolean() ? PlayerColour.BLACK : PlayerColour.WHITE;
        System.out.println("Game starting — bot is playing as " + botColour);
    }

    // ----- getters -----

    /** @return colour currently assigned to Player 1 */
    public PlayerColour getPlayer1Colour() { return player1Colour; }

    /** @return colour currently assigned to Player 2 */
    public PlayerColour getPlayer2Colour() { return player2Colour; }

    /** @return the colour whose turn it is right now */
    public PlayerColour getCurrentPlayer() { return currentPlayer; }

    /** @return the colour the bot is currently playing as */
    public PlayerColour getBotColour() { return botColour; }

    /** @return true if it is currently the bot's turn to move */
    public boolean isBotTurn() { return currentPlayer == botColour; }

    /** @return true once BLACK has placed the first stone */
    public boolean isFirstMoveMade() { return firstMoveMade; }

    /**
     * called after BLACK's first stone is placed — opens the pie rule window for WHITE
     * safe to call multiple times; only takes effect the first time
     */
    public void setFirstMoveMade() {
        if (!firstMoveMade) {
            firstMoveMade    = true;
            pieRuleAvailable = true;
        }
    }

    /** @return true if the "Activate Pie Rule" button should be shown */
    public boolean isPieRuleAvailable() { return pieRuleAvailable; }

    /**
     * activates the pie rule:
     * swaps which colour each player is assigned to, swaps the bot's colour accordingly,
     * sets WHITE to play next, and permanently closes the pie rule window
     *
     * the first tile stays BLACK on screen because after the swap
     * the player now assigned BLACK owns that stone — no visual change needed
     */
    public void activatePieRule() {
        PlayerColour tmp = player1Colour;
        player1Colour    = player2Colour;
        player2Colour    = tmp;

        // bot colour flips with the player colours
        botColour = (botColour == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;

        currentPlayer    = PlayerColour.WHITE;
        pieRuleAvailable = false;
        System.out.println("Pie rule activated — bot is now " + botColour);
    }

    /**
     * passes the turn to the other colour
     * also closes the pie rule window if WHITE chose to play instead of swapping
     */
    public void togglePlayer() {
        if (pieRuleAvailable && currentPlayer == PlayerColour.WHITE) {
            pieRuleAvailable = false;
        }
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        System.out.println("Turn → " + currentPlayer);
    }

    /**
     * records the winner and freezes the game
     * @param colour the colour that just completed a winning path
     */
    public void setWinner(PlayerColour colour) {
        this.winner = colour;
    }

    /** @return the winning colour, or null if the game is still in progress */
    public PlayerColour getWinner() { return winner; }

    /** @return true once a winner has been set — all further moves are blocked */
    public boolean isGameOver() { return winner != null; }
}
