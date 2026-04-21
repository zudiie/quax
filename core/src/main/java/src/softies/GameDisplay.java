package src.softies;

// handles everything printed to the terminal — menus, headers, and the board itself
// this is purely a view class, it doesn't change any game state
public class GameDisplay {

    /**
     * prints the ASCII welcome banner shown at startup
     */
    public void displayStartScreen() {
        System.out.println("\n\n****************************************");
        System.out.println("*                                      *");
        System.out.println("*         Welcome to Quax Board!       *");
        System.out.println("*                                      *");
        System.out.println("*       Press Enter Key to Start!      *");
        System.out.println("*                                      *");
        System.out.println("*      To exit the game, type quit     *");
        System.out.println("*                                      *");
        System.out.println("****************************************\n\n");
    }

    /**
     * prints the mode selection menu so the player can pick human or bot mode
     */
    public void displayModeSelection() {
        // big newline gap to visually separate from whatever was shown before
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        System.out.println("****************************************");
        System.out.println("*                                      *");
        System.out.println("*           Choose Game Mode:          *");
        System.out.println("*                                      *");
        System.out.println("*      Human vs Human  (type human)    *");
        System.out.println("*        Human vs Bot  (type bot)      *");
        System.out.println("*                                      *");
        System.out.println("****************************************\n\n");
    }

    /**
     * prints the in-game header bar showing the current mode and player directions
     * @param mode the selected game mode (HUMAN_VS_HUMAN or HUMAN_VS_BOT)
     */
    public void printHeader(GameMode mode) {
        // different label depending on whether it's human vs human or human vs bot
        if (mode == GameMode.HUMAN_VS_HUMAN) {
            System.out.print("-----------------      Quax (Human vs Human)      -----------------\n\n");
        } else {
            System.out.print("-----------------       Quax (Human vs Bot)       -----------------\n\n");
        }
        System.out.println("BLACK plays first (Moves Top to Bottom). WHITE plays second (Left to Right).");
        System.out.println("Enter type of cell or 'quit' to exit.");
    }

    /**
     * prints a prefixed message to the terminal — used for prompts and errors
     * @param message the text to display, prefixed with ">>"
     */
    public void showMessage(String message) {
        System.out.print(">> " + message);
    }

    /**
     * prints a message followed by three dots with short delays - gives a "loading" feel
     * @param message the text to show before the dots appear
     */
    public void showLoading(String message) {
        System.out.print(message);
        try {
            // print one dot every 300ms for a simple animated effect
            for (int i = 0; i < 3; i++) {
                Thread.sleep(300);
                System.out.print(".");
            }
        } catch (InterruptedException e) {
            // restore the interrupt flag if something cuts the sleep short
            Thread.currentThread().interrupt();
        }
        System.out.print("\n");
    }

    /**
     * renders the full board state to the terminal including octagonal and rhombic cells
     * rows are printed top-to-bottom (11 down to 1) with column headers A-K
     * @param board the current board to read cell state from
     * @param currentPlayer whose turn it is - shown above the board
     */
    public void renderBoard(QuaxBoard board, PlayerColour currentPlayer) {
        System.out.println("\nCurrent Player: " + currentPlayer + "\n");

        int size = board.getBoardSize();

        // print column letters A through K along the top
        System.out.print("      ");
        for (char c = 'A'; c < 'A' + size; c++) {
            System.out.print(c + "     ");
        }
        System.out.println("\n");

        // iterate rows from top (11) to bottom (1) so the board reads naturally
        for (int i = size; i >= 1; i--) {
            // single-digit rows get an extra space so numbers line up nicely
            if (i < 10) System.out.print(" ");
            System.out.print(i + "    ");

            // print each octagonal cell in this row, joined by dashes
            for (int col = 0; col < size; col++) {
                String label = (char) ('A' + col) + "" + i;
                OctagonalCell cell = board.getOctagonalCell(label);

                String symbol = (cell != null) ? cell.getDisplaySymbol() : ".";
                System.out.print(symbol + "  ");
                // don't print a trailing dash after the last column
                if (col != size - 1) System.out.print("-  ");
            }
            System.out.println();

            // rhombus row sits between octagon rows — skip it below the bottom row
            if (i > 1) {
                System.out.print("         ");
                for (int col = 0; col < size - 1; col++) {
                    String rKey = "R-" + (char) ('A' + col) + i;
                    RhombicCell cell = board.getRhombicCell(rKey);

                    String symbol = (cell != null) ? cell.getDisplaySymbol() : "x";
                    System.out.print(symbol + "  ");
                    // same trailing dash logic as the octagon row
                    if (col != size - 2) System.out.print("-  ");
                }
                System.out.println();
            }
        }
        System.out.println();
    }
}
