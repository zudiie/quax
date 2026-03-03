package src.softies;

// handles the terminal-based visual representation
public class GameDisplay {

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

    // shows the menu for selecting human or bot mode
    public void displayModeSelection() {
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

    //prints the header indicating the current game mode
    public void printHeader(GameMode mode) {
        if (mode == GameMode.HUMAN_VS_HUMAN) {
            System.out.print("-----------------      Quax (Human vs Human)      -----------------\n\n");

        } else {
            System.out.print("-----------------       Quax (Human vs Bot)       -----------------\n\n");
        }
        System.out.println("BLACK plays first (Moves Top to Bottom). WHITE plays second (Left to Right).");
        System.out.println("Enter type of cell or 'quit' to exit.");
    }

    public void showMessage(String message) {
        System.out.print(">> " + message);
    }

    public void showLoading(String message) {
        System.out.print(message);
        try {
            for (int i = 0; i < 3; i++){
                Thread.sleep(300);
                System.out.print(".");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
    }

    public void renderBoard(QuaxBoard board, PlayerColour currentPlayer) {

        System.out.println("\nCurrent Player: " + currentPlayer + "\n");

        int size = board.getBoardSize();

        // column headers A-K
        System.out.print("      ");
        for (char c = 'A'; c < 'A' + size; c++) {
            System.out.print(c + "     ");
        }
        System.out.println("\n");

        //print rows (top to bottom 11 to 1)
        for (int i = size; i >= 1; i--){
            if (i < 10) System.out.print(" ");
            System.out.print(i + "    ");

            //octagonal row
            for (int col = 0; col < size; col++) {
                String label = (char)('A' + col) + "" + i;
                OctagonalCell cell = board.getOctagonalCell(label);

                String symbol = (cell != null) ? cell.getDisplaySymbol() : ".";
                System.out.print(symbol + "  ");
                if (col != size - 1) System.out.print("-  ");
            }
            System.out.println();

            // renders the rhombic connections between rows
            if (i > 1) {
                System.out.print("         ");
                for (int col = 0; col < size - 1; col++) {
                    String rKey = "R-" + (char)('A' + col) + i;
                    RhombicCell cell = board.getRhombicCell(rKey);

                    String symbol = (cell != null) ? cell.getDisplaySymbol() : "x";
                    System.out.print(symbol + "  ");
                    if (col != size - 2) System.out.print("-  ");
                }
                System.out.println();
            }
        }
        System.out.println();
    }
}
