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
        // create octagon cells (11x11 grid)
        octagonCells.clear();
        rhombusCells.clear();

        for (int row = 1; row <= BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                // generate labels A1, B1 etc.
                Point p = new Point(col, row);
                String label = generateLabel(col, row);
                octagonCells.put(label, new OctagonalCell(p, PlayerColour.EMPTY, CellType.OCTAGON));

                if (col < BOARD_SIZE - 1 && row > 1) { // Logic adjusted to match visual gap
                    // we label the rhombus based on the octagon to its top-left or similar logic
                    // here R-A1 is the rhombus connected to A1
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

        // print letters
        System.out.print("      ");
        for (char c = 'A'; c < 'A' + BOARD_SIZE; c++){
            System.out.print(c + "     ");
        }
        System.out.println("\n");

        // print rows (top to bottom)
        for (int i = BOARD_SIZE; i >= 1; i--) {

            // row number formatting
            if (i < 10) System.out.print(" ");
            System.out.print(i + "    ");

            // octagonal row
            for (int j = 0; j < BOARD_SIZE; j++) {
                // retrieve cell from Map
                String key = generateLabel(j, i);
                OctagonalCell cell = octagonCells.get(key);

                // print cell symbol
                System.out.print(cell.getDisplaySymbol("X") + "  ");

                if (j !=  BOARD_SIZE - 1) System.out.print("-  ");
            }
            System.out.println();

            // rhombic row
            // only print rhombuses between rows
            if (i > 1) {
                System.out.print("         ");
                for (int k = 0; k < BOARD_SIZE - 1; k++) {

                    // retrieve rhombus from Map
                    // we access the rhombus associated with the row we are currently processing
                    String key = "R-" + generateLabel(k, i);
                    RhombicCell cell = rhombusCells.get(key);

                    // safety check in case map logic varies, default to O
                    String symbol = (cell != null) ? cell.getDisplaySymbol("o") : "o";

                    System.out.print(symbol + "  ");

                    if (k != BOARD_SIZE - 2) System.out.print("-  ");
                }
                System.out.println();
            }
        }
    }

    public static void main(String[] args) {

        QuaxBoard quaxBoard = new QuaxBoard();

        displayBoard();

    }
}
