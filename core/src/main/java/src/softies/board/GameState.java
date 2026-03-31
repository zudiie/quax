package src.softies.board;

import src.softies.PlayerColour;

// holds the live state of the game — current player, first move flag, and pie rule availability
// anything that needs to be shared across renderers, input handlers, etc. lives here
public class GameState {

    private PlayerColour currentPlayer;
    // true once BLACK has placed their first stone
    private boolean firstMoveMade = false;
    // true during WHITE's first turn only — the window where pie rule can be activated
    private boolean pieRuleAvailable = false;

    /**
     * initialises the game state with BLACK going first, as per the rules
     */
    public GameState() {
        currentPlayer = PlayerColour.BLACK;
    }

    /**
     * @return whichever player's turn it currently is
     */
    public PlayerColour getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * @return true once the very first stone has been placed on the board
     */
    public boolean isFirstMoveMade() {
        return firstMoveMade;
    }

    /**
     * marks that BLACK's first move has been made and opens the pie rule window for WHITE
     * only has an effect the first time it's called — subsequent calls are ignored
     */
    public void setFirstMoveMade() {
        if (!firstMoveMade) {
            firstMoveMade    = true;
            // opening the pie rule window — WHITE now has the option to swap colours
            pieRuleAvailable = true;
        }
    }

    /**
     * @return true if the pie rule button should currently be visible and clickable
     * only true during WHITE's very first turn
     */
    public boolean isPieRuleAvailable() {
        return pieRuleAvailable;
    }

    /**
     * swaps player colours — WHITE becomes BLACK and BLACK becomes WHITE
     * closes the pie rule window immediately so the button disappears
     */
    public void activatePieRule() {
        // swap whoever is currently playing
        currentPlayer    = (currentPlayer == PlayerColour.WHITE) ? PlayerColour.BLACK : PlayerColour.WHITE;
        // pie rule can only ever be used once — close the window
        pieRuleAvailable = false;
        System.out.println("Pie rule activated — colours swapped. It is now " + currentPlayer + "'s turn.");
    }

    /**
     * flips the turn to the other player after a valid move
     * also closes the pie rule window if WHITE just made their first normal move without using it
     */
    public void togglePlayer() {
        // if WHITE makes their first normal move, the pie rule opportunity expires
        if (pieRuleAvailable && currentPlayer == PlayerColour.WHITE) {
            pieRuleAvailable = false;
        }
        // ternary swap — black becomes white and vice versa
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        System.out.println("It is now " + currentPlayer + "'s turn.");
    }
}
