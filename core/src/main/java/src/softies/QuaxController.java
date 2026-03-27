package src.softies;

import java.util.Scanner;

// the controller that ties together the board, display and player input
// it owns the game loop, manages turn order and handles the start/mode screens
public class QuaxController {

    private final QuaxBoard board;
    private final GameDisplay display;
    private final Scanner scanner;

    private PlayerColour currentPlayer;
    private boolean running;
    private GameMode gamemode;

    /**
     * sets up the controller with fresh board, display and scanner instances
     * black always goes first as per the quax rules
     */
    public QuaxController() {
        this.scanner = new Scanner(System.in);
        this.board = new QuaxBoard();
        this.display = new GameDisplay();
        // black kicks things off
        this.currentPlayer = PlayerColour.BLACK;
        this.running = true;
    }

    /**
     * kicks off the full game sequence — welcome screen, mode selection, then the loop
     * returns early if the player quits at any of the setup stages
     */
    public void launch() {
        display.displayStartScreen();
        // bail out if the player types quit on the start screen
        if (!waitForEnterOrQuit()) return;

        display.displayModeSelection();
        this.gamemode = getGameModeInput();

        display.showLoading("Launching " + gamemode);

        display.printHeader(gamemode);
        runGameLoop();
    }

    /**
     * the core game loop — renders the board, reads a move, validates it and switches turn
     * keeps going until the player types quit or running is set to false
     */
    private void runGameLoop() {
        while (running) {
            display.renderBoard(board, currentPlayer);

            System.out.print("Move for " + currentPlayer + ": ");
            String input = scanner.nextLine().trim();

            // let the player exit gracefully mid-game
            if (input.equalsIgnoreCase("quit")) {
                quitGame();
                break;
            }

            // only switch turns if the move actually went through
            if (board.placeStone(input, currentPlayer)) {
                switchTurn();
            } else {
                // invalid or occupied — same player tries again
                System.out.println("system: turn remains with " + currentPlayer + " due to invalid move.");
            }
        }
        scanner.close();
    }

    /**
     * swaps the current player between BLACK and WHITE
     */
    private void switchTurn() {
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
    }

    /**
     * waits on the start screen until the player hits Enter or types quit
     * @return true if Enter was pressed (continue), false if quit was typed
     */
    private boolean waitForEnterOrQuit() {
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("quit")) {
                quitGame();
                return false;
            }
            // empty string means Enter was pressed
            if (input.isEmpty()) return true;
        }
    }

    /**
     * loops until the player types a valid mode — "human" or "bot"
     * exits the whole program if they type quit here
     * @return the chosen GameMode
     */
    private GameMode getGameModeInput() {
        while (true) {
            System.out.print(">> ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("human")) return GameMode.HUMAN_VS_HUMAN;
            if (input.equals("bot")) return GameMode.HUMAN_VS_BOT;
            // quit at this stage kills the process entirely
            if (input.equals("quit")) {
                quitGame();
                System.exit(0);
            }
            display.showMessage("Invalid input. Type 'human' or 'bot'.\n");
        }
    }

    /**
     * stops the game loop and prints an exit message
     */
    public void quitGame() {
        running = false;
        display.showMessage("Exiting game...");
    }

    /**
     * @return the current player's colour as a string
     */
    public String getCurrentPlayer() {
        return currentPlayer.toString();
    }

    /**
     * re-renders the board to the terminal using the display class
     */
    public void render() {
        display.renderBoard(board, currentPlayer);
    }

    /**
     * tries to place a stone for the current player at the given label
     * @param input the cell label to target (e.g. "C4")
     * @return true if the move was valid and placed
     */
    public boolean attemptMove(String input) {
        return board.placeStone(input, currentPlayer);
    }

    /**
     * flips the current player — same as switchTurn but public
     * useful for the GUI layer to trigger a turn change directly
     */
    public void changePlayer() {
        if (currentPlayer == PlayerColour.WHITE) {
            currentPlayer = PlayerColour.BLACK;
        } else if (currentPlayer == PlayerColour.BLACK) {
            currentPlayer = PlayerColour.WHITE;
        }
    }

    /**
     * @return the underlying QuaxBoard so other classes can inspect cell state
     */
    public QuaxBoard getBoard() {
        return board;
    }
}
