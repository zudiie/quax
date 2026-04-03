package src.softies;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

// the model class for the quax board - owns all cell state and handles stone/rhombus placement
// the board is an 11x11 grid of octagonal cells, with rhombic cells sitting in the gaps between rows
public class QuaxBoard {

    // fixed board size - quax is always 11x11
    private final static int BOARD_SIZE = 11;

    // the two maps that store all cells, keyed by their label strings (e.g. "A1" or "R-B3")
    private static Map<String, OctagonalCell> octagonCells;
    private static Map<String, RhombicCell> rhombusCells;

    /**
     * creates a fresh board and populates it with empty cells
     */
    public QuaxBoard() {
        this.octagonCells = new HashMap<>();
        this.rhombusCells = new HashMap<>();
        initializeBoard();
        System.out.println("system: board successfully initialised.");
    }

    /**
     * fills both cell maps with empty octagon and rhombus cells
     * rhombus cells only exist between rows (row > 1) and between columns (col < BOARD_SIZE - 1)
     */
    private void initializeBoard() {
        // wipe any previous state before rebuilding
        octagonCells.clear();
        rhombusCells.clear();

        for (int row = 1; row <= BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Point p = new Point(col, row);
                String label = generateLabel(col, row);

                // every position gets an octagonal cell for stone placement
                octagonCells.put(label, new OctagonalCell(p, PlayerColour.EMPTY, CellType.OCTAGON));

                // rhombus cells only appear between adjacent columns and above the bottom row
                if (col < BOARD_SIZE - 1 && row > 1) {
                    String rKey = "R-" + label;
                    rhombusCells.put(rKey, new RhombicCell(p, PlayerColour.EMPTY, CellType.RHOMBUS));
                }
            }
        }
    }

    /**
     * converts a column index and row number into a label string like "A1" or "K11"
     * @param col zero-based column index
     * @param row one-based row number
     * @return the label string for that position
     */
    public static String generateLabel(int col, int row) {
        // shift 'A' by col to get the right letter
        char colChar = (char) ('A' + col);
        return "" + colChar + row;
    }

    /**
     * checks whether a given label string actually maps to a cell on the board
     * catches obvious bad inputs like "AO" or "1A" before anything tries to use them
     * @param label the label to validate (e.g. "B5")
     * @return true if the label exists in the octagon cell map
     */
    public boolean isValidLabel(String label) {
        // quick length check before doing a map lookup
        if (label == null || label.length() < 2) return false;
        return octagonCells.containsKey(label.toUpperCase());
    }

    /**
     * attempts to place a stone for the given player on the named cell
     * will reject the move if the label is invalid or the cell is already taken
     * @param label the target cell label (e.g. "C4")
     * @param colour the player placing the stone (BLACK or WHITE)
     * @return true if placement succeeded, false if it was rejected
     */
    public boolean placeStone(String label, PlayerColour colour) {
        if (!isValidLabel(label)) {
            System.out.println("error: invalid coordinate format. please use A-K and 1-11 (e.g., A1).");
            return false;
        }

        // trim and uppercase so inputs like "b5 " still work
        OctagonalCell cell = octagonCells.get(label.trim().toUpperCase());

        if (cell.isOccupied()) {
            System.out.println("error: cell " + label.toUpperCase() + " is already occupied. choose another cell.");
            return false;
        }

        // mark the cell with the player's colour and lock it
        cell.setColour(colour);
        cell.setOccupied(true);
        return true;
    }

    /**
     * attempts to place a rhombus for the given player at the named rhombus key
     * silently returns false if the cell doesn't exist or is already occupied
     * @param rhombusKey the rhombus label (e.g. "R-B3")
     * @param colour the player placing the rhombus
     * @return true if placement succeeded, false otherwise
     */
    public boolean placeRhombus(String rhombusKey, PlayerColour colour) {
        RhombicCell cell = rhombusCells.get(rhombusKey);
        // null check covers keys that fall outside the valid rhombus area
        if (cell == null || cell.isOccupied()) {
            return false;
        }
        cell.setColour(colour);
        cell.setOccupied(true);
        return true;
    }

    /**
     * retrieves the octagonal cell at the given label, or null if it doesn't exist
     * @param label the cell label (e.g. "A1")
     * @return the OctagonalCell, or null
     */
    public OctagonalCell getOctagonalCell(String label) {
        return octagonCells.get(label);
    }

    /**
     * retrieves the rhombic cell at the given key, or null if it doesn't exist
     * @param label the rhombus key (e.g. "R-B3")
     * @return the RhombicCell, or null
     */
    public RhombicCell getRhombicCell(String label) {
        return rhombusCells.get(label);
    }

    /**
     * @return the fixed size of the board (always 11)
     */
    public int getBoardSize() {
        return BOARD_SIZE;
    }

    public Map<String, OctagonalCell> getOctagonCells() {
        return octagonCells;
    }

    public Map<String, RhombicCell> getRhombusCells() {
        return rhombusCells;
    }

    /**
     * converts a label string like "B2" back into a {col, row} pair
     * handy for any logic that needs to do positional maths on a label
     * @param label the label to parse
     * @return int array where [0] is the column index and [1] is the row number
     */
    private int[] parseLabel(String label) {
        char colChar = label.charAt(0);
        int row = Integer.parseInt(label.substring(1));
        // convert the letter back to a zero-based column index
        int col = colChar - 'A';
        return new int[]{col, row};
    }
}
