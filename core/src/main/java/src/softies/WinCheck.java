package src.softies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// checks whether a player has won by forming a connected path across the board using DFS
<<<<<<< HEAD
// both octagonal and rhombic cells of the same colour are treated as equal connections
// WHITE wins by connecting left (column A) to right (column K)
// BLACK wins by connecting top (row 1) to bottom (row 11)
=======
// both octagonal stones and rhombic connectors of the same colour count as connections
// WHITE wins by connecting column A (left) to column K (right)
// BLACK wins by connecting row 1 (bottom, as stored) to row 11 (top, as stored)
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
public class WinCheck {

    private static final int BOARD_SIZE = 11;
    private final QuaxBoard board;

<<<<<<< HEAD
=======
    /**
     * @param board the live board model — queried for cell colours during DFS
     */
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
    public WinCheck(QuaxBoard board) {
        this.board = board;
    }

    /**
<<<<<<< HEAD
     * checks if the given player has a winning path across the board
     * @param colour the player to check (BLACK or WHITE)
     * @return true if a connected path exists from one side to the other
=======
     * checks whether the given player currently has a winning path across the board
     * starts a DFS from every occupied cell on the player's starting edge
     * @param colour the player to check (BLACK or WHITE)
     * @return true if a connected path exists from one side of the board to the other
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
     */
    public boolean checkWin(PlayerColour colour) {
        Set<String> visited = new HashSet<>();

        if (colour == PlayerColour.WHITE) {
<<<<<<< HEAD
            // WHITE needs a path from column 0 (A) to column 10 (K)
=======
            // WHITE starts from column A (index 0) and needs to reach column K (index 10)
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
            for (int row = 1; row <= BOARD_SIZE; row++) {
                String label = QuaxBoard.generateLabel(0, row);
                OctagonalCell cell = board.getOctagonCells().get(label);
                if (cell != null && cell.getColour() == PlayerColour.WHITE) {
<<<<<<< HEAD
                    if (dfs(label, colour, visited, false)) {
                        return true;
                    }
                }
            }
        } else if (colour == PlayerColour.BLACK) {
            // BLACK needs a path from row 1 (top) to row 11 (bottom)
=======
                    if (dfs(label, colour, visited, false)) return true;
                }
            }
        } else if (colour == PlayerColour.BLACK) {
            // BLACK starts from row 1 (bottom edge) and needs to reach row 11 (top edge)
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
            for (int col = 0; col < BOARD_SIZE; col++) {
                String label = QuaxBoard.generateLabel(col, 1);
                OctagonalCell cell = board.getOctagonCells().get(label);
                if (cell != null && cell.getColour() == PlayerColour.BLACK) {
<<<<<<< HEAD
                    if (dfs(label, colour, visited, false)) {
                        return true;
                    }
=======
                    if (dfs(label, colour, visited, false)) return true;
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
                }
            }
        }

        return false;
    }

    /**
<<<<<<< HEAD
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
=======
     * recursive DFS that walks through same-coloured cells (both octagonal and rhombic)
     * terminates when the target edge is reached or all reachable cells have been visited
     * @param cellKey      the label of the current cell (e.g. "A1" or "R-B3")
     * @param colour       the player colour we are tracing for
     * @param visited      set of already-visited keys — prevents cycles
     * @param isRhombus    true if this cell is rhombic, false if octagonal
     * @return true if the target edge is reachable from here
     */
    private boolean dfs(String cellKey, PlayerColour colour,
                        Set<String> visited, boolean isRhombus) {
        if (visited.contains(cellKey)) return false;
        visited.add(cellKey);

        // strip the "R-" prefix to get the coordinate part for both cell types
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
        String labelPart = isRhombus ? cellKey.substring(2) : cellKey;
        int col = labelPart.charAt(0) - 'A';
        int row = Integer.parseInt(labelPart.substring(1));

<<<<<<< HEAD
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
=======
        // win condition is only checked at octagonal cells (edges of the board are octagonal)
        if (!isRhombus) {
            if (colour == PlayerColour.WHITE && col == BOARD_SIZE - 1) return true; // reached column K
            if (colour == PlayerColour.BLACK && row == BOARD_SIZE)     return true; // reached row 11
        }

        // explore all neighbouring cells and recurse into same-coloured ones
        List<String[]> neighbours = isRhombus
            ? getRhombusNeighbours(col, row)
            : getOctagonNeighbours(col, row);

        for (String[] neighbour : neighbours) {
            String nKey      = neighbour[0];
            boolean nIsRhomb = neighbour[1].equals("R");

            if (visited.contains(nKey)) continue;

            // look up the neighbour in the appropriate map and check its colour
            Cell nCell = nIsRhomb
                ? board.getRhombusCells().get(nKey)
                : board.getOctagonCells().get(nKey);

            if (nCell != null && nCell.getColour() == colour) {
                if (dfs(nKey, colour, visited, nIsRhomb)) return true;
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
            }
        }

        return false;
    }

    /**
<<<<<<< HEAD
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
=======
     * returns all neighbour keys for an octagonal cell at grid position (col, row)
     * includes the four orthogonal octagonal neighbours (shared edges)
     * and up to four diagonal rhombic neighbours (shared corners)
     * each entry is {key, type} where type is "O" (octagon) or "R" (rhombus)
     */
    private List<String[]> getOctagonNeighbours(int col, int row) {
        List<String[]> nb = new ArrayList<>();

        // orthogonal neighbours — cells that share a flat edge
        if (col > 0)              nb.add(new String[]{ QuaxBoard.generateLabel(col - 1, row), "O" });
        if (col < BOARD_SIZE - 1) nb.add(new String[]{ QuaxBoard.generateLabel(col + 1, row), "O" });
        if (row > 1)              nb.add(new String[]{ QuaxBoard.generateLabel(col,     row - 1), "O" });
        if (row < BOARD_SIZE)     nb.add(new String[]{ QuaxBoard.generateLabel(col,     row + 1), "O" });

        // diagonal rhombus neighbours — cells that share a corner via a rhombus tile
        // each rhombus key is "R-" + the label of its bottom-left octagon
        if (col < BOARD_SIZE - 1 && row > 1)
            nb.add(new String[]{ "R-" + QuaxBoard.generateLabel(col,     row),     "R" });
        if (col > 0              && row > 1)
            nb.add(new String[]{ "R-" + QuaxBoard.generateLabel(col - 1, row),     "R" });
        if (col < BOARD_SIZE - 1 && row < BOARD_SIZE)
            nb.add(new String[]{ "R-" + QuaxBoard.generateLabel(col,     row + 1), "R" });
        if (col > 0              && row < BOARD_SIZE)
            nb.add(new String[]{ "R-" + QuaxBoard.generateLabel(col - 1, row + 1), "R" });

        return nb;
    }

    /**
     * returns all neighbour keys for a rhombic cell at grid position (col, row)
     * a rhombus sits between four octagonal cells and provides diagonal connectivity
     * the four octagonal neighbours are the corners of the rhombus bounding box
     */
    private List<String[]> getRhombusNeighbours(int col, int row) {
        List<String[]> nb = new ArrayList<>();
        // the four octagonal cells that share a corner with this rhombus
        nb.add(new String[]{ QuaxBoard.generateLabel(col,     row),     "O" }); // top-left
        nb.add(new String[]{ QuaxBoard.generateLabel(col + 1, row),     "O" }); // top-right
        nb.add(new String[]{ QuaxBoard.generateLabel(col,     row - 1), "O" }); // bottom-left
        nb.add(new String[]{ QuaxBoard.generateLabel(col + 1, row - 1), "O" }); // bottom-right
        return nb;
>>>>>>> 5908d86e1a60d32fa2d636fbcb64a4de8afe9344
    }
}
