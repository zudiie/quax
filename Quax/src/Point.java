/**
 * Represents a logical coordinate on the game board.
 * Used to map internal grid positions to game rules
 */

public class Point {

    private final int col;
    private final int row;

    public Point(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getCol() {
        return col;
    }
    public int getRow() {
        return row;
    }

}
