package src.softies;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/*
    This is a model class representing the Quax game board. Responsible for maintaining
    the state of the 11x11 grid, including Octagonal and Rhombic cells.
 */
public class QuaxBoard {

    // constant for board dimensions
    private final static int BOARD_SIZE = 11;

    // data structures to hold the board state
    private static Map<String, OctagonalCell> octagonCells;
    private static Map<String, RhombicCell> rhombusCells;

    public QuaxBoard() {
        this.octagonCells = new HashMap<>();
        this.rhombusCells = new HashMap<>();
        initializeBoard();
        System.out.println("system: board successfully initialised.");
    }

   // initialise the 11x1 structure with empty cells
    private void initializeBoard() {
        // create octagon cells (11x11 grid)
        octagonCells.clear();
        rhombusCells.clear();

        for (int row = 1; row <= BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Point p = new Point(col, row);
                String label = generateLabel(col, row);
                // creates octagonal cells for stones
                octagonCells.put(label, new OctagonalCell(p, PlayerColour.EMPTY, CellType.OCTAGON));

                // creates rhombic cells for diagonal connections
                if (col < BOARD_SIZE - 1 && row > 1) {
                    String rKey = "R-" + label;
                    rhombusCells.put(rKey, new RhombicCell(p, PlayerColour.EMPTY, CellType.RHOMBUS));
                }
            }
        }
    }

    // converts coordinates to labels like A1 or K11
    private static String generateLabel(int col, int row) {
        char colChar = (char) ('A' + col);
        return "" + colChar + row;
    }

    // enhanced validation to catch invalid formats like "AO" or "1A"
    public boolean isValidLabel(String label) {
        if (label == null || label.length() < 2) return false;
        return octagonCells.containsKey(label.toUpperCase());
    }

    // handles the logic of placing a stone on the board
    public boolean placeStone(String label, PlayerColour colour) {
        if (!isValidLabel(label)){
            System.out.println("error: invalid coordinate format. please use A-K and 1-11 (e.g., A1).");
            return false;
        }

        OctagonalCell cell = octagonCells.get(label.trim().toUpperCase());

        if (cell.isOccupied()) {
            System.out.println("error: cell " + label.toUpperCase() + " is already occupied. choose another cell.");
            return false;
        }

        cell.setColour(colour);
        cell.setOccupied(true);
        return true;
    }

    public OctagonalCell getOctagonalCell(String label) {
        return octagonCells.get(label);
    }

    public RhombicCell getRhombicCell(String label) {
        return rhombusCells.get(label);
    }

    public int getBoardSize(){
        return BOARD_SIZE;
    }

//    private static GameMode getGameMode() {
//        Scanner in = new Scanner(System.in);
//        String input = in.nextLine();
//        while(true) {
//            if (input.equals("human")) {
//                return GameMode.HUMAN_VS_HUMAN;
//            } else if (input.equals("bot")) {
//                return GameMode.HUMAN_VS_BOT ;
//            } else {
//                System.out.println("Invalid Input");
//            }
//        }
//    }

    /*private static boolean pressEnterToContinue() {
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
    }*/

//    public static void main(String[] args) {
//
//        QuaxBoard quaxBoard = new QuaxBoard();
//
//        //displayStartScreen();
//        displayBoard();
//
//    }

}
