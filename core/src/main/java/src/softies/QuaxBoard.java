package src.softies;

import java.util.HashMap;
import java.util.Map;

// the model class for the quax board - owns all cell state and handles stone/rhombus placement
// the board is an 11x11 grid of octagonal cells with rhombic connectors in the diagonal gaps
public class QuaxBoard {

    private final static int BOARD_SIZE = 11;

    // instance fields - each QuaxBoard owns its own maps; safe to recreate on restart
    private final Map<String, OctagonalCell> octagonCells;
    private final Map<String, RhombicCell>   rhombusCells;

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
        for (int row = 1; row <= BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Point p = new Point(col, row);
                String label = generateLabel(col, row);
                octagonCells.put(label, new OctagonalCell(p, PlayerColour.EMPTY, CellType.OCTAGON));
                // rhombus cells sit in the diagonal gaps - only where both neighbours exist
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
     */
    public static String generateLabel(int col, int row) {
        return "" + (char)('A' + col) + row;
    }

    /** @return true if the label exists in the octagon map */
    public boolean isValidLabel(String label) {
        if (label == null || label.length() < 2) return false;
        return octagonCells.containsKey(label.toUpperCase());
    }

    /**
     * attempts to place a stone for the given player on the named octagonal cell
     * rejects the move if the label is invalid or the cell is already occupied
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
     */
    public boolean placeRhombus(String rhombusKey, PlayerColour colour) {
        RhombicCell cell = rhombusCells.get(rhombusKey);
        if (cell == null || cell.isOccupied()) return false;
        cell.setColour(colour);
        cell.setOccupied(true);
        return true;
    }

    /** @return the OctagonalCell at the given label, or null */
    public OctagonalCell getOctagonalCell(String label) {
        return octagonCells.get(label);
    }

    /** @return the RhombicCell at the given key, or null */
    public RhombicCell getRhombicCell(String label) {
        return rhombusCells.get(label);
    }

    /** used by WinCheck and BotPlayer to iterate all octagonal cells */
    public Map<String, OctagonalCell> getOctagonCells() {
        return octagonCells;
    }

    /** used by WinCheck and BotPlayer to iterate all rhombic cells */
    public Map<String, RhombicCell> getRhombusCells() {
        return rhombusCells;
    }

    /** @return the fixed board dimension (always 11) */
    public int getBoardSize() {
        return BOARD_SIZE;
    }
}
