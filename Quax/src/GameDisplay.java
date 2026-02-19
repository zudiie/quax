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
                Thread.sleep(500);
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

        //print column headers from A to K
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
                String label = getLabel(col, i);
                OctagonalCell cell = board.getOctagonalCell(label);

                String symbol = (cell != null) ? cell.getDisplaySymbol() : "0";
                System.out.print(symbol + "  ");

                if (col != size - 1) System.out.print("-  ");
            }
            System.out.println();

            //rhombic row
            if (i > 1) {
                System.out.print("         ");
                for (int col = 0; col < size - 1; col++) {
                    String rKey = "R-" + getLabel(col, i);
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

    private String getLabel(int col, int row) {
        char colChar = (char) ('A' + col);
        return "" + colChar + row;
    }

//    public void displayBoard(QuaxBoard board, PlayerColour currentPlayer) {
//        System.out.println();
//        System.out.println("Current Player: " + currentPlayer);
//        System.out.println();
//
//        // Board printing is currently static in QuaxBoard
//        QuaxBoard.displayBoard();
//        System.out.println();
//    }
//
//        public void showMessage(String message) {
//        System.out.println(message);
//        }

    }
