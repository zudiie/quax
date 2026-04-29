package src.softies;

// an octagonal cell is where players actually place their stones
// it extends Cell and will eventually hold geometry info for rendering and neighbour lookups
public class OctagonalCell extends Cell {

    // the visual radius of the octagon - used for hit detection or rendering later
    private final int radius = 2;
    private Point center;
    private Direction directions;

    /**
     * creates an octagonal cell at the given coordinates with an initial colour and type
     * @param coordinates the board position as a Point
     * @param colour starting colour (almost always EMPTY on init)
     * @param cellType should be CellType.OCTAGON for these cells
     */
    OctagonalCell(Point coordinates, PlayerColour colour, CellType cellType) {
        // passes everything up to the Cell constructor
        super(coordinates, colour, cellType);
    }
}
