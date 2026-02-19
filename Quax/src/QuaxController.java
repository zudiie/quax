
public class QuaxController {

    private final QuaxBoard board;
    private final GameDisplay display;

    private PlayerColour currentPlayer;
    private boolean running;

    public QuaxController() {
        this.board = new QuaxBoard();
        this.display = new GameDisplay();
        this.currentPlayer = PlayerColour.BLACK;
        this.running = true;
    }

    public boolean isRunning() {
        return running;
    }

    public PlayerColour getCurrentPlayer() {
        return currentPlayer;
    }

    public void render() {
        display.displayBoard(board, currentPlayer);
    }

    // Returns true only if the move was placed successfully.
    public boolean attemptMove(String label) {
        if (!board.isValidOctCellLabel(label)) {
            display.showMessage("Invalid input. Use labels like A1, B7, K11.");
            return false;
        }

        boolean placed = board.placeStone(label, currentPlayer);
        if (!placed) {
            display.showMessage("Invalid move. That cell is already occupied.");
            return false;
        }

        // Only switch turn if the move succeeded
        switchTurn();
        return true;
    }

    private void switchTurn() {
        if (currentPlayer == PlayerColour.BLACK) currentPlayer = PlayerColour.WHITE;
        else currentPlayer = PlayerColour.BLACK;
    }

    public void quitGame() {
        this.running = false;
        display.showMessage("Exiting game...");
        System.exit(0);
    }
}
