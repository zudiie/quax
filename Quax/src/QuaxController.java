import java.util.Scanner;

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
        this.currentPlayer = PlayerColour.BLACK;
        this.running = true;
    }

    public void launch() {
        // show start screen
        display.displayStartScreen();
        if (!waitForEnterOrQuit()) return;

        // select mode
        display.displayModeSelection();
        this.gamemode = getGameModeInput();

        display.showLoading("Launching " + gamemode);

        //start game loop
        display.printHeader(gamemode);
        runGameLoop();
    }
    public boolean isRunning() {
        return running;
    }



    private void runGameLoop() {
        while (running) {
            display.renderBoard(board, currentPlayer);

            System.out.print("Move for " + currentPlayer + ": ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                quitGame();
                break;
            }

            // attempt move
            if (board.isValidLabel(input)) {
                boolean success = board.placeStone(input, currentPlayer);
                if (success) {
                    switchTurn();
                } else {
                    display.showMessage("Cell is already occupied.");
                }
            } else {
                display.showMessage("Invalid coordinate. Try format 'A1', 'K11'.");
            }
        }
        scanner.close();
    }

    private void switchTurn() {
        if (currentPlayer == PlayerColour.BLACK) currentPlayer = PlayerColour.WHITE;
        else currentPlayer = PlayerColour.BLACK;
    }

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
}