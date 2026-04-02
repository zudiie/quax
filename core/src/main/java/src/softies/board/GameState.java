package src.softies.board;

import src.softies.PlayerColour;

// holds the live state of the game — current player, player colour assignments, and pie rule state
// the pie rule swaps which colour each player plays, the board tiles are never changed
public class GameState {

    // each player's currently assigned colour — these swap when pie rule is activated
    private PlayerColour player1Colour;
    private PlayerColour player2Colour;

    // the colour whose turn it is right now (derived from player colours, not player number)
    private PlayerColour currentPlayer;

    // true once BLACK's very first stone has been placed
    private boolean firstMoveMade = false;

    // true only during WHITE's first turn — the only window when pie rule can be activated
    private boolean pieRuleAvailable = false;

    /**
     * sets up the game: Player 1 starts as BLACK, Player 2 starts as WHITE, BLACK moves first
     */
    public GameState() {
        player1Colour = PlayerColour.BLACK;
        player2Colour = PlayerColour.WHITE;
        currentPlayer = PlayerColour.BLACK;
    }

    /**
     * @return the colour currently assigned to Player 1 (changes if pie rule is activated)
     */
    public PlayerColour getPlayer1Colour() {
        return player1Colour;
    }

    /**
     * @return the colour currently assigned to Player 2 (changes if pie rule is activated)
     */
    public PlayerColour getPlayer2Colour() {
        return player2Colour;
    }

    /**
     * @return the colour of whichever player should move right now
     */
    public PlayerColour getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * @return true once BLACK has placed the first stone on the board
     */
    public boolean isFirstMoveMade() {
        return firstMoveMade;
    }

    /**
     * called after BLACK's first stone is placed — opens the pie rule window for WHITE
     * safe to call multiple times, only takes effect once
     */
    public void setFirstMoveMade() {
        if (!firstMoveMade) {
            firstMoveMade    = true;
            // WHITE now has the option to swap colours on their first turn
            pieRuleAvailable = true;
        }
    }

    /**
     * @return true if the pie rule button should be shown — only during WHITE's first turn
     */
    public boolean isPieRuleAvailable() {
        return pieRuleAvailable;
    }

    /**
     * activates the pie rule: swaps the colour assigned to each player
     * the board tiles are untouched — only player assignments change
     * after activation, WHITE plays next (Player 1 is now WHITE and moves immediately)
     */
    public void activatePieRule() {
        // swap the colour labels so each player now plays the other colour
        PlayerColour tmp = player1Colour;
        player1Colour    = player2Colour;
        player2Colour    = tmp;

        // after the swap, WHITE plays next — Player 1 is now WHITE
        currentPlayer    = PlayerColour.WHITE;

        // close the pie rule window — can only ever be used once
        pieRuleAvailable = false;

        System.out.println("Pie rule activated — Player 1 is now " + player1Colour
            + ", Player 2 is now " + player2Colour + ". WHITE to move.");
    }

    /**
     * advances the turn to the other colour
     * also closes the pie rule window if WHITE makes a normal move instead of using it
     */
    public void togglePlayer() {
        // if WHITE passes on the pie rule by making a normal move, the window closes
        if (pieRuleAvailable && currentPlayer == PlayerColour.WHITE) {
            pieRuleAvailable = false;
        }
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        System.out.println("It is now " + currentPlayer + "'s turn.");
    }
}
