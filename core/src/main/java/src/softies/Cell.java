package src.softies;

// the base class for all cells on the Quax board - both octagonal and rhombic cells extend this
public class Cell {

    protected PlayerColour colour;
    protected boolean isOccupied;
    protected CellType cellType;
    protected Point label;

    /**
     * sets up a cell with its position, colour and shape type
     * @param label the board coordinate (e.g. A1) as a Point
     * @param colour the starting colour - usually EMPTY at first
     * @param cellType whether this is an OCTAGON or RHOMBUS cell
     */
    Cell(Point label, PlayerColour colour, CellType cellType) {
        this.label = label;
        this.colour = colour;
        this.cellType = cellType;
        // all cells start unoccupied, regardless of what colour they get later
        this.isOccupied = false;
    }

    /**
     * @return the current colour of this cell (BLACK, WHITE, or EMPTY)
     */
    public PlayerColour getColour() {
        return colour;
    }

    /**
     * @return true if a stone has been placed here
     */
    public boolean isOccupied() {
        return isOccupied;
    }

    /**
     * @return the shape type of this cell (OCTAGON or RHOMBUS)
     */
    public CellType getCellType() {
        return cellType;
    }

    /**
     * updates the colour of this cell - used when a player places a stone
     * @param colour the new colour to assign
     */
    public void setColour(PlayerColour colour) {
        this.colour = colour;
    }

    /**
     * marks the cell as occupied or free - flipping this to true locks the cell
     * @param isOccupied true to mark as taken, false to clear it
     */
    public void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
    }

    /**
     * @return the Point label representing this cell's board coordinates
     */
    public Point getLabel() {
        return label;
    }

    /**
     * returns the character symbol used when rendering this cell in the terminal
     * subclasses can override this for different visual behaviour
     * @return "B" for black, "W" for white, "X" for empty
     */
    public String getDisplaySymbol() {
        if (colour == PlayerColour.BLACK) return "B";
        if (colour == PlayerColour.WHITE) return "W";
        return "X";
    }
}
