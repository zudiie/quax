package src.softies.board;

import src.softies.PlayerColour;

// holds the live state of the game — right now that's just whose turn it is
// anything that needs to be shared across renderers, input handlers, etc. lives here
public class GameState {

    private PlayerColour currentPlayer;

    /**
     * initialises the game state with BLACK going first, as per the rules
     */
    public GameState() {
        // black always goes first in Quax
        currentPlayer = PlayerColour.BLACK;
    }

    /**
     * @return whichever player's turn it currently is
     */
    public PlayerColour getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * flips the turn to the other player after a valid move
     * logs a message so you can see the switch in the console
     */
    public void togglePlayer() {
        // black becomes white and vice versa
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        System.out.println("It is now " + currentPlayer + "'s turn.");
    }
}
