import java.util.HashMap;
import java.util.Map;



public class QuaxBoard {

    private static Map<String, OctagonalCell> octagonCells = new HashMap<>();
    private static Map<String, RhombicCell> rhombusCells = new HashMap<>();
    private final static int BOARD_SIZE = 11;

    public QuaxBoard() {
        initializeBoard();
    }

    private void initializeBoard() {
        // Create Octagon Cells (11x11 Grid)
        octagonCells.clear();
        rhombusCells.clear();

        for (int row = 1; row <= BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                // Generate Label: A1, B1 ... K11
                Point p = new Point(col, row);
                String label = generateLabel(col, row);
                octagonCells.put(label, new OctagonalCell(p, PlayerColour.EMPTY, CellType.OCTAGON));

                if (col < BOARD_SIZE - 1 && row > 1) { // Logic adjusted to match visual gap
                    // We label the rhombus based on the octagon to its top-left or similar logic
                    // Here: R-A1 is the rhombus connected to A1
                    String rKey = "R-" + label;
                    rhombusCells.put(rKey, new RhombicCell(p, PlayerColour.EMPTY, CellType.RHOMBUS));
                }
            }
        }
    }

    private static String generateLabel(int col, int row) {
        char colChar = (char) ('A' + col);
        return "" + colChar + row;
    }




    public static void displayBoard(){

        // we will pass the state of the board into this function which is
        // a list of cells. each cell will have a label (X, O, B?, W?) to display
        // so like when the cell is occupied by WHITE it displays W
        // Here now X is an octagonal cells and O is rhombic
        // boardState will return a list of cells that we will print here

        // print letters
        System.out.print("      ");
        for (char c = 'A'; c < 'A' + BOARD_SIZE; c++){
            System.out.print(c + "     ");
        }
        System.out.println("\n");

        // Print Rows (Top to Bottom: 11 -> 1)
        for (int i = BOARD_SIZE; i >= 1; i--) {

            // Row Number Formatting
            if (i < 10) System.out.print(" ");
            System.out.print(i + "    ");

            // --- OCTAGONAL ROW ---
            for (int j = 0; j < BOARD_SIZE; j++) {
                // Retrieve cell from Map
                String key = generateLabel(j, i);
                OctagonalCell cell = octagonCells.get(key);

                // Print cell symbol
                System.out.print(cell.getDisplaySymbol("X") + "  ");

                if (j !=  BOARD_SIZE - 1) System.out.print("-  ");
            }
            System.out.println();

            // --- RHOMBIC ROW ---
            // Only print rhombuses between rows
            if (i > 1) {
                System.out.print("         "); // Indent
                for (int k = 0; k < BOARD_SIZE - 1; k++) {

                    // Retrieve rhombus from Map
                    // Note: We access the rhombus associated with the row we are currently processing
                    String key = "R-" + generateLabel(k, i);
                    RhombicCell cell = rhombusCells.get(key);

                    // Safety check in case map logic varies, default to O
                    String symbol = (cell != null) ? cell.getDisplaySymbol("o") : "o";

                    System.out.print(symbol + "  ");

                    if (k != BOARD_SIZE - 2) System.out.print("-  ");
                }
                System.out.println();
            }
        }

//        // print rows
//        for (int i = 11; i >= 1; i--){
//            // add a space for i < 10 before the number
//            if (i < 10){
//                System.out.print(" ");
//            }
//            //print row number
//            System.out.print(i + "    ");
//            // print octagonal cells
//            for (int j = 0; j < 11; j++){
//                System.out.print("X  ");
//                // does not print '-' for the last element in each row
//                if (j != 10){
//                    System.out.print("-  ");
//                }
//            }
//            // add rhombic cells
//            if (i > 1){
//                System.out.println();
//                System.out.print("      -  ");
//                for (int k = 0; k < 10; k++){
//                    System.out.print("o  -  ");
//                }
//            }
//            System.out.println();
//        }
//
//        // storing chars in boardState im a 2D array
//        // have to change to list?
//        char[][] boardState = new char[21][21];
//        for (int i = 0; i < 21; i++){
//            for (int j = 0; j < 11; j++){
//                if (i % 2 == 0) {
//                    boardState[i][j] = 'X';
//                } else if (j != 10) {
//                    boardState[i][j] = 'O';
//                }
//            }
//        }
//
//        for (int i =  0; i < 21; i++){
//            for (int j = 0; j < 11; j++){
//                if (j < 10 || i % 2 == 0) {
//                    System.out.print(boardState[i][j]);
//                }
//            }
//            System.out.println();
//        }
    }

    public static void main(String[] args) {

        QuaxBoard quaxBoard = new QuaxBoard();

        displayBoard();

    }
}
