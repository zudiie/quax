package src.softies.board;

import src.softies.PlayerColour;
import src.softies.GameMode;

// holds the live state of the game — right now that's just whose turn it is
// anything that needs to be shared across renderers, input handlers, etc. lives here
public class GameState {

    private PlayerColour currentPlayer;
    private boolean firstMoveMade;
    private boolean pieRuleUsed;
    private GameMode gameMode;
    /**
     * initialises the game state with BLACK going first, as per the rules
     */
    public GameState() {
        // black always goes first in Quax
        currentPlayer = PlayerColour.BLACK;
        firstMoveMade = false;
        pieRuleUsed = false;
        gameMode = GameMode.HUMAN_VS_HUMAN;
    }


    /**
     * @return whichever player's turn it currently is
     */
    public PlayerColour getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isFirstMoveMade() {
        return firstMoveMade;
    }

    public void setFirstMoveMade() {
        this.firstMoveMade = true;
    }

    public boolean isPieRuleUsed() {
        return pieRuleUsed;
    }

    public void setPieRuleUsed() {
        this.pieRuleUsed = true;
    }


    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    /**
     * applies the pie rule after the first move, if it has not already been used
     * swaps the turn once and marks the pie rule as used
     */
    public void applyPieRule() {
        if (firstMoveMade && !pieRuleUsed) {
            currentPlayer = (currentPlayer == PlayerColour.BLACK)
                ? PlayerColour.WHITE
                : PlayerColour.BLACK;

            pieRuleUsed = true;
            System.out.println("Pie rule applied. Players swapped.");
        }
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

