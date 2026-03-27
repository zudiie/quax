package src.softies;

// a rhombic cell sits between four octagonal cells and acts as a diagonal connector
// players can place a rhombus to link two of their stones together
public class RhombicCell extends Cell {

    private Point center;
    private Direction directions;

    /**
     * creates a rhombic cell at the given board position
     * @param coordinates the board position as a Point
     * @param colour starting colour (usually EMPTY)
     * @param cellType should be CellType.RHOMBUS for these cells
     */
    public RhombicCell(Point coordinates, PlayerColour colour, CellType cellType) {
        super(coordinates, colour, cellType);
    }

    /**
     * returns the display character for this rhombic cell
     * overrides the base Cell version to show "o" when empty instead of "X"
     * @return "B" for black, "W" for white, "o" when empty
     */
    @Override
    public String getDisplaySymbol() {
        if (isOccupied()) {
            // show which player owns this connector
            if (colour == PlayerColour.BLACK) return "B";
            if (colour == PlayerColour.WHITE) return "W";
        }
        // unoccupied rhombuses show as "o" to visually distinguish them from empty octagons
        return "o";
    }
}
