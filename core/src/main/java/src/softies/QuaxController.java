package src.softies;

import java.util.Scanner;

// the controller maanages the game flow, user input and turn logics
public class QuaxController {

    private final QuaxBoard board;
    private final GameDisplay display;
    private final Scanner scanner;

    private PlayerColour currentPlayer;
    private boolean running;
    private GameMode gamemode;

    public QuaxController() {
        this.scanner = new Scanner(System.in);
        this.board = new QuaxBoard();
        this.display = new GameDisplay();
        this.currentPlayer = PlayerColour.BLACK; // black moves first per rules
        this.running = true;
    }

    // launches the game sequence
    public void launch() {
        display.displayStartScreen();
        if (!waitForEnterOrQuit()) return;

        display.displayModeSelection();
        this.gamemode = getGameModeInput();

        display.showLoading("Launching " + gamemode);

        display.printHeader(gamemode);
        runGameLoop();
    }

    // main game loop
    private void runGameLoop() {
        while (running) {
            display.renderBoard(board, currentPlayer);

            System.out.print("Move for " + currentPlayer + ": ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                quitGame();
                break;
            }

            // check move validity and occupancy
            if (board.placeStone(input, currentPlayer)) {
                switchTurn();
            } else {
                // clear error message indicating turn does not change
                System.out.println("system: turn remains with " + currentPlayer + " due to invalid move.");
            }
        }
        scanner.close();
    }

    // switches turn between black and white
    private void switchTurn() {
        currentPlayer = (currentPlayer == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
    }


    // utility to handle start screen input
    private boolean waitForEnterOrQuit() {
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("quit")) {
                quitGame();
                return false;
            }
            if (input.isEmpty()) return true;

        }
    }

    // handles mode selection with validation
    private GameMode getGameModeInput() {
        while (true) {
            System.out.print(">> ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("human")) return GameMode.HUMAN_VS_HUMAN;
            if (input.equals("bot")) return GameMode.HUMAN_VS_BOT;
            if (input.equals("quit")) { quitGame(); System.exit(0); }
            display.showMessage("Invalid input. Type 'human' or 'bot'.\n");
        }
    }

    public void quitGame() {
        running = false;
        display.showMessage("Exiting game...");
    }

<<<<<<< HEAD:Quax/src/QuaxController.java
    public String getCurrentPlayer() {
        return currentPlayer.toString();
    }


    public void render() {
        display.renderBoard(board, currentPlayer);
    }

    public boolean attemptMove(String input) {
        return board.placeStone(input, currentPlayer);
    }

    public void changePlayer(){
        if (currentPlayer == PlayerColour.WHITE) {
            currentPlayer = PlayerColour.BLACK;
        }
        else if (currentPlayer == PlayerColour.BLACK) {
            currentPlayer = PlayerColour.WHITE;
        }
    }

    public QuaxBoard getBoard() {
        return board;
    }
}
=======
}
>>>>>>> 0bd1067f4dfabb8f693ecef440b728743658350e:core/src/main/java/src/softies/QuaxController.java
