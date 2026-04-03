package src.softies.board;

import src.softies.PlayerColour;

// holds the live state of the game — current player, player colour assignments,
// pie rule availability, and the game-over / winner state
public class GameState {

    // each player's currently assigned colour — these swap when the pie rule is activated
    private PlayerColour player1Colour;
    private PlayerColour player2Colour;

    // the colour whose turn it is right now
    private PlayerColour currentPlayer;

    // true once BLACK's very first stone has been placed
    private boolean firstMoveMade = false;

    // true only during WHITE's first turn — the only window when pie rule can be used
    private boolean pieRuleAvailable = false;

    // set to the winning colour as soon as a win is detected; null while the game is ongoing
    private PlayerColour winner = null;

    /**
     * sets up the game: Player 1 starts as BLACK, Player 2 starts as WHITE, BLACK moves first
     */
    public GameState() {
        player1Colour = PlayerColour.BLACK;
        player2Colour = PlayerColour.WHITE;
        currentPlayer = PlayerColour.BLACK;
    }

    /** @return the colour currently assigned to Player 1 */
    public PlayerColour getPlayer1Colour() { return player1Colour; }

    /** @return the colour currently assigned to Player 2 */
    public PlayerColour getPlayer2Colour() { return player2Colour; }

    /** @return the colour of whichever player should move right now */
    public PlayerColour getCurrentPlayer() { return currentPlayer; }

    /** @return true once BLACK has placed the first stone */
    public boolean isFirstMoveMade() { return firstMoveMade; }

    /**
     * called after BLACK's first stone is placed — opens the pie rule window for WHITE
     * safe to call multiple times; only takes effect once
     */
    public void setFirstMoveMade() {
        if (!firstMoveMade) {
            firstMoveMade    = true;
            // WHITE now has the option to swap colours before making their first move
            pieRuleAvailable = true;
        }
    }

    /** @return true if the pie rule button should be visible (only during WHITE's first turn) */
    public boolean isPieRuleAvailable() { return pieRuleAvailable; }

    /**
     * activates the pie rule — swaps the colour label assigned to each player
     * the board tiles are never touched; only the player assignments change
     * after activation WHITE plays next (whoever is now WHITE goes immediately)
     *
     * why the tile stays BLACK: the first stone was placed as BLACK; after the swap
     * Player 2 is now BLACK, so they "own" that stone — no visual change is needed
     */
    public void activatePieRule() {
        // swap colour assignments between the two players
        PlayerColour tmp = player1Colour;
        player1Colour    = player2Colour;
        player2Colour    = tmp;

        // WHITE plays next after the swap
        currentPlayer    = PlayerColour.WHITE;

        // close the window — pie rule can only be used once
        pieRuleAvailable = false;

        System.out.println("Pie rule activated — Player 1 is now " + player1Colour
            + ", Player 2 is now " + player2Colour + ". WHITE to move.");
    }

    /**
     * advances the turn to the other colour
     * also closes the pie rule window if WHITE makes a normal move without using it
     */
    public void togglePlayer() {
        // if WHITE skips the pie rule by just playing, close the window
        if (pieRuleAvailable && currentPlayer == PlayerColour.WHITE) {
            pieRuleAvailable = false;
        }
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        System.out.println("It is now " + currentPlayer + "'s turn.");
    }

    /**
     * records the winner and freezes the game — called by InputHandler when win is detected
     * @param colour the colour that just completed a winning path
     */
    public void setWinner(PlayerColour colour) {
        this.winner = colour;
    }

    /** @return the winning colour, or null if the game is still in progress */
    public PlayerColour getWinner() { return winner; }

    /** @return true once a winner has been set — used to block further moves */
    public boolean isGameOver() { return winner != null; }
}
