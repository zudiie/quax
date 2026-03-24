package src.softies;

// represents a coordinate on the board - stores both numeric x/y and a human-readable label like "B3"
public class Point {

    String title;
    private int x;
    private int y;

    /**
     * creates a board point from column and row integers and auto-generates its label
     * e.g. x=1, y=3 becomes "B3"
     * @param x the column index (0-based, so 0 = A, 1 = B, etc.)
     * @param y the row number (1-based)
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
        // convert the numeric column to a letter for the display label
        char columnLetter = (char) (x + 65);
        this.title = columnLetter + String.valueOf(y);
    }

    /**
     * checks if this point is directly next to another one (including diagonals)
     * used later to validate rhombus placement between two stones
     * @param other the other point to compare against
     * @return true if the two points are within 1 step in any direction
     */
    public boolean isAdjacent(Point other) {
        return Math.abs(this.x - other.x) <= 1 && Math.abs(this.y - other.y) <= 1;
    }

    /**
     * @return the column index (0-based)
     */
    public int getX() {
        return x;
    }

    /**
     * @return the row number (1-based)
     */
    public int getY() {
        return y;
    }
}
