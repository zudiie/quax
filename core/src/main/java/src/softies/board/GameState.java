package src.softies.board;

import src.softies.PlayerColour;

/**
 * Holds the global state of the game, such as which player's turn it is.
 */
public class GameState {
    private PlayerColour currentPlayer;

    public GameState() {
        currentPlayer = PlayerColour.BLACK; // black moves first
    }

    public PlayerColour getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Switches to the other player.
     */
    public void togglePlayer() {
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
        System.out.println("It is now " + currentPlayer + "'s turn.");
    }
}
