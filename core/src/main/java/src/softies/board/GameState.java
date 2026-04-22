package src.softies.board;

import src.softies.PlayerColour;
import src.softies.GameMode;
import java.util.Random;

// holds the live state of the game - current player, colour assignments, pie rule,
// game-over state, and which colour the bot is playing as
//
// gameMode ALWAYS defaults to HUMAN_VS_BOT because the welcome screen no longer
// offers a mode-selection step - the only mode available is Human vs Bot
public class GameState {

    // each player's currently assigned colour - these swap when the pie rule is activated
    private PlayerColour player1Colour;
    private PlayerColour player2Colour;

    // the colour whose turn it is right now
    private PlayerColour currentPlayer;

    // the colour the bot is playing as - also swaps when the pie rule fires
    private PlayerColour botColour;

    private boolean firstMoveMade    = false;
    private boolean pieRuleAvailable = false;

    // null while the game is in progress; set when a winner is detected
    private PlayerColour winner = null;

    // always HUMAN_VS_BOT - fixes the bug where BotStrategyWidget button became
    // invisible after "Play Again" (restartGame creates a new GameState which
    // previously defaulted to HUMAN_VS_HUMAN, hiding the Show Strategy button)
    private GameMode gameMode = GameMode.HUMAN_VS_BOT;

    /**
     * sets up a new game with randomly assigned bot colour
     * BLACK always goes first in Quax, so if the bot draws BLACK it also opens
     */
    public GameState() {
        player1Colour = PlayerColour.BLACK;
        player2Colour = PlayerColour.WHITE;
        currentPlayer = PlayerColour.BLACK;

        // randomise bot colour each game for variety
        botColour = new Random().nextBoolean() ? PlayerColour.BLACK : PlayerColour.WHITE;
        System.out.println("Game starting - bot is playing as " + botColour);
    }

    // ----- colour getters -----

    public PlayerColour getPlayer1Colour()  { return player1Colour; }
    public PlayerColour getPlayer2Colour()  { return player2Colour; }
    public PlayerColour getCurrentPlayer()  { return currentPlayer; }
    public PlayerColour getBotColour()      { return botColour; }
    public boolean      isBotTurn()         { return currentPlayer == botColour; }
    public boolean      isFirstMoveMade()   { return firstMoveMade; }
    public boolean      isPieRuleAvailable(){ return pieRuleAvailable; }
    public GameMode     getGameMode()       { return gameMode; }
    public void         setGameMode(GameMode m) { this.gameMode = m; }

    /**
     * called after BLACK's first stone - opens the pie rule window for WHITE (one use only)
     */
    public void setFirstMoveMade() {
        if (!firstMoveMade) {
            firstMoveMade    = true;
            pieRuleAvailable = true;
        }
    }

    /**
     * activates the pie rule:
     * - swaps which colour each player is assigned
     * - flips the bot's colour accordingly
     * - sets WHITE to play next
     * - closes the pie rule window permanently
     *
     * the first tile stays BLACK on screen - after the swap the player now
     * assigned BLACK owns that stone, so no visual change is needed
     */
    public void activatePieRule() {
        PlayerColour tmp = player1Colour;
        player1Colour    = player2Colour;
        player2Colour    = tmp;

        botColour     = (botColour == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        currentPlayer = PlayerColour.WHITE;
        pieRuleAvailable = false;

        System.out.println("Pie rule activated - bot is now " + botColour);
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
        System.out.println("Turn -> " + currentPlayer);
    }

    /** records the winner and freezes the game - no more moves accepted after this */
    public void setWinner(PlayerColour colour) { this.winner = colour; }

    /** @return the winning colour, or null if the game is still in progress */
    public PlayerColour getWinner()  { return winner; }

    /** @return true once a winner has been set - all further moves are blocked */
    public boolean isGameOver()      { return winner != null; }
}
