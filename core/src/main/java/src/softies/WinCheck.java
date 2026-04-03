package src.softies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// checks whether a player has won by forming a connected path across the board using DFS
// both octagonal and rhombic cells of the same colour are treated as equal connections
// WHITE wins by connecting left (column A) to right (column K)
// BLACK wins by connecting top (row 1) to bottom (row 11)
public class WinCheck {

    private static final int BOARD_SIZE = 11;
    private final QuaxBoard board;

    public WinCheck(QuaxBoard board) {
        this.board = board;
    }

    /**
     * checks if the given player has a winning path across the board
     * @param colour the player to check (BLACK or WHITE)
     * @return true if a connected path exists from one side to the other
     */
    public boolean checkWin(PlayerColour colour) {
        Set<String> visited = new HashSet<>();

        if (colour == PlayerColour.WHITE) {
            // WHITE needs a path from column 0 (A) to column 10 (K)
            for (int row = 1; row <= BOARD_SIZE; row++) {
                String label = QuaxBoard.generateLabel(0, row);
                OctagonalCell cell = board.getOctagonCells().get(label);
                if (cell != null && cell.getColour() == PlayerColour.WHITE) {
                    if (dfs(label, colour, visited, false)) {
                        return true;
                    }
                }
            }
        } else if (colour == PlayerColour.BLACK) {
            // BLACK needs a path from row 1 (top) to row 11 (bottom)
            for (int col = 0; col < BOARD_SIZE; col++) {
                String label = QuaxBoard.generateLabel(col, 1);
                OctagonalCell cell = board.getOctagonCells().get(label);
                if (cell != null && cell.getColour() == PlayerColour.BLACK) {
                    if (dfs(label, colour, visited, false)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * recursive depth-first search through same-coloured cells (both octagonal and rhombic)
     * @param cellKey the label of the current cell (e.g. "A1" or "R-B3")
     * @param colour the player colour we are tracing a path for
     * @param visited set of already-visited cell keys to avoid cycles
     * @param isRhombus true if this cell is a rhombic cell, false if octagonal
     * @return true if the target edge is reachable from this cell
     */
    private boolean dfs(String cellKey, PlayerColour colour, Set<String> visited, boolean isRhombus) {
        if (visited.contains(cellKey)) return false;
        visited.add(cellKey);

        // parse coordinates from the cell key
        String labelPart = isRhombus ? cellKey.substring(2) : cellKey;
        int col = labelPart.charAt(0) - 'A';
        int row = Integer.parseInt(labelPart.substring(1));

        // check if we have reached the target edge (only octagonal cells sit on edges)
        if (!isRhombus) {
            if (colour == PlayerColour.WHITE && col == BOARD_SIZE - 1) return true;
            if (colour == PlayerColour.BLACK && row == BOARD_SIZE) return true;
        }

        // gather all neighbours and recurse into same-coloured ones
        List<String[]> neighbors = isRhombus ? getRhombusNeighbors(col, row) : getOctagonNeighbors(col, row);

        for (String[] neighbor : neighbors) {
            String neighborKey = neighbor[0];
            boolean neighborIsRhombus = neighbor[1].equals("R");

            if (visited.contains(neighborKey)) continue;

            // look up the neighbour cell and check its colour
            Cell neighborCell;
            if (neighborIsRhombus) {
                neighborCell = board.getRhombusCells().get(neighborKey);
            } else {
                neighborCell = board.getOctagonCells().get(neighborKey);
            }

            if (neighborCell != null && neighborCell.getColour() == colour) {
                if (dfs(neighborKey, colour, visited, neighborIsRhombus)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * returns neighbour keys for an octagonal cell at (col, row)
     * includes orthogonal octagon neighbours and diagonal rhombus neighbours
     * each entry is {cellKey, type} where type is "O" (octagon) or "R" (rhombus)
     */
    private List<String[]> getOctagonNeighbors(int col, int row) {
        List<String[]> neighbors = new ArrayList<>();

        // orthogonal octagonal neighbours (share an edge)
        if (col > 0)              neighbors.add(new String[]{QuaxBoard.generateLabel(col - 1, row), "O"});
        if (col < BOARD_SIZE - 1) neighbors.add(new String[]{QuaxBoard.generateLabel(col + 1, row), "O"});
        if (row > 1)              neighbors.add(new String[]{QuaxBoard.generateLabel(col, row - 1), "O"});
        if (row < BOARD_SIZE)     neighbors.add(new String[]{QuaxBoard.generateLabel(col, row + 1), "O"});

        // diagonal rhombic neighbours — each octagon touches up to 4 rhombus cells
        if (col < BOARD_SIZE - 1 && row > 1)
            neighbors.add(new String[]{"R-" + QuaxBoard.generateLabel(col, row), "R"});
        if (col > 0 && row > 1)
            neighbors.add(new String[]{"R-" + QuaxBoard.generateLabel(col - 1, row), "R"});
        if (col < BOARD_SIZE - 1 && row < BOARD_SIZE)
            neighbors.add(new String[]{"R-" + QuaxBoard.generateLabel(col, row + 1), "R"});
        if (col > 0 && row < BOARD_SIZE)
            neighbors.add(new String[]{"R-" + QuaxBoard.generateLabel(col - 1, row + 1), "R"});

        return neighbors;
    }

    /**
     * returns neighbour keys for a rhombic cell at (col, row)
     * a rhombus sits between four octagonal cells and connects them diagonally
     */
    private List<String[]> getRhombusNeighbors(int col, int row) {
        List<String[]> neighbors = new ArrayList<>();

        neighbors.add(new String[]{QuaxBoard.generateLabel(col, row), "O"});
        neighbors.add(new String[]{QuaxBoard.generateLabel(col + 1, row), "O"});
        neighbors.add(new String[]{QuaxBoard.generateLabel(col, row - 1), "O"});
        neighbors.add(new String[]{QuaxBoard.generateLabel(col + 1, row - 1), "O"});

        return neighbors;
    }
}
