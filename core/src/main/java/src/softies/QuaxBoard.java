package src.softies;

import java.util.HashMap;
import java.util.Map;

// the model class for the quax board — owns all cell state and handles stone/rhombus placement
// the board is an 11x11 grid of octagonal cells with rhombic connectors in the diagonal gaps
public class QuaxBoard {

    private final static int BOARD_SIZE = 11;

    // the two maps that store all cells, keyed by their label strings (e.g. "A1" or "R-B3")
    // NOTE: these are static — only one board ever exists at a time
    private static Map<String, OctagonalCell> octagonCells;
    private static Map<String, RhombicCell>   rhombusCells;

    /**
     * creates a fresh board and populates it with empty cells
     */
    public QuaxBoard() {
        octagonCells = new HashMap<>();
        rhombusCells = new HashMap<>();
        initializeBoard();
        System.out.println("system: board successfully initialised.");
    }

    /**
     * fills both cell maps with empty cells
     * rhombus cells only exist above row 1 and to the left of the last column
     */
    private void initializeBoard() {
        octagonCells.clear();
        rhombusCells.clear();

        for (int row = 1; row <= BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Point p = new Point(col, row);
                String label = generateLabel(col, row);
                // every position gets an octagonal cell for stone placement
                octagonCells.put(label, new OctagonalCell(p, PlayerColour.EMPTY, CellType.OCTAGON));
                // rhombus cells sit in the diagonal gaps — only where both neighbours exist
                if (col < BOARD_SIZE - 1 && row > 1) {
                    rhombusCells.put("R-" + label, new RhombicCell(p, PlayerColour.EMPTY, CellType.RHOMBUS));
                }
            }
        }
    }

    /**
     * converts a zero-based column index and one-based row number into a board label
     * e.g. col=1, row=3 → "B3"
     * public and static so WinCheck and InputHandler can use it without a board reference
     * @param col zero-based column index (0 = A, 10 = K)
     * @param row one-based row number (1–11)
     * @return the label string for that cell
     */
    public static String generateLabel(int col, int row) {
        return "" + (char)('A' + col) + row;
    }

    /**
     * checks whether a label maps to a real cell on the board
     * @param label e.g. "B5"
     * @return true if the label exists in the octagon map
     */
    public boolean isValidLabel(String label) {
        if (label == null || label.length() < 2) return false;
        return octagonCells.containsKey(label.toUpperCase());
    }

    /**
     * attempts to place a stone for the given player on the named octagonal cell
     * rejects the move if the label is invalid or the cell is already occupied
     * @param label  the target cell label (e.g. "C4")
     * @param colour the player placing the stone (BLACK or WHITE)
     * @return true if placement succeeded
     */
    public boolean placeStone(String label, PlayerColour colour) {
        if (!isValidLabel(label)) {
            System.out.println("error: invalid coordinate. use A-K and 1-11 (e.g. A1).");
            return false;
        }
        OctagonalCell cell = octagonCells.get(label.trim().toUpperCase());
        if (cell.isOccupied()) {
            System.out.println("error: cell " + label.toUpperCase() + " is already occupied.");
            return false;
        }
        cell.setColour(colour);
        cell.setOccupied(true);
        return true;
    }

    /**
     * attempts to place a rhombus connector for the given player
     * returns false silently if the key is invalid or already occupied
     * @param rhombusKey the rhombus label (e.g. "R-B3")
     * @param colour     the player placing the rhombus
     * @return true if placement succeeded
     */
    public boolean placeRhombus(String rhombusKey, PlayerColour colour) {
        RhombicCell cell = rhombusCells.get(rhombusKey);
        if (cell == null || cell.isOccupied()) return false;
        cell.setColour(colour);
        cell.setOccupied(true);
        return true;
    }

    /**
     * @param label the cell label (e.g. "A1")
     * @return the OctagonalCell, or null if not found
     */
    public OctagonalCell getOctagonalCell(String label) {
        return octagonCells.get(label);
    }

    /**
     * @param label the rhombus key (e.g. "R-B3")
     * @return the RhombicCell, or null if not found
     */
    public RhombicCell getRhombicCell(String label) {
        return rhombusCells.get(label);
    }

    /**
     * used by WinCheck to iterate all cells during path-finding
     * @return the full octagon cell map
     */
    public Map<String, OctagonalCell> getOctagonCells() {
        return octagonCells;
    }

    /**
     * used by WinCheck to look up rhombus cells during path-finding
     * @return the full rhombus cell map
     */
    public Map<String, RhombicCell> getRhombusCells() {
        return rhombusCells;
    }

    /** @return the fixed board dimension (always 11) */
    public int getBoardSize() {
        return BOARD_SIZE;
    }
}
