import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


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
                System.out.print(cell.getDisplaySymbol("O") + "  ");

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
                    String symbol = (cell != null) ? cell.getDisplaySymbol("x") : "x";

                    System.out.print(symbol + "  ");

                    if (k != BOARD_SIZE - 2) System.out.print("-  ");
                }
                System.out.println();
            }
        }
    }


    private static void displayStartScreen() {
        while (true) {
            System.out.println("\n\n****************************************");
            System.out.println("*                                      *");
            System.out.println("*         Welcome to Quax Board!       *");
            System.out.println("*                                      *");
            System.out.println("*       Press Enter Key to Start!      *");
            System.out.println("*                                      *");
            System.out.println("*      To exit the game, type quit     *");
            System.out.println("*                                      *");
            System.out.println("****************************************\n\n");

            if (!pressEnterToContinue()) {
                break;
            }

            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
            System.out.println("****************************************");
            System.out.println("*                                      *");
            System.out.println("*           Choose Game Mode:          *");
            System.out.println("*                                      *");
            System.out.println("*      Human vs Human  (type human)    *");
            System.out.println("*        Human vs Bot  (type bot)      *");
            System.out.println("*                                      *");
            System.out.println("****************************************\n\n");

            GameMode gamemode = getGameMode();

            if (gamemode == GameMode.HUMAN_VS_HUMAN) {
                System.out.print("Launching Human vs Human mode.");

            } else {
                System.out.print("Launching Human vs Bot mode.");
            }
            delayOutput();
            System.out.print(".");
            delayOutput();
            System.out.print(".");
            delayOutput();
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

            printHeader(gamemode);
            break;
        }
    }

    private static void delayOutput() {
        try {
            Thread.sleep(1000); // 1000 ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printHeader(GameMode gamemode) {
        if  (gamemode == GameMode.HUMAN_VS_HUMAN) {
            System.out.println("\n\n-----------------      Quax (Human vs Human)      -----------------\n");
        } else  {
            System.out.println("\n\n-----------------       Quax (Human vs Bot)       -----------------\n");
        }
    }

    private static GameMode getGameMode() {
        Scanner in = new Scanner(System.in);
        String input = in.nextLine();
        while(true) {
            if (input.equals("human")) {
                return GameMode.HUMAN_VS_HUMAN;
            } else if (input.equals("bot")) {
                return GameMode.HUMAN_VS_BOT ;
            } else {
                System.out.println("Invalid Input");
            }
        }
    }

    private static boolean pressEnterToContinue() {
        Scanner in = new Scanner(System.in);
        String input;
        while(true) {
            input =  in.nextLine();
            if (input.equals("quit")) {
                return false;
            } else if (input.equals("")) {
                return true;
            } else {
                System.out.println("Invalid Input");
            }
        }
    }

    public static void main(String[] args) {

        QuaxBoard quaxBoard = new QuaxBoard();

        displayStartScreen();
        displayBoard();

    }

}
