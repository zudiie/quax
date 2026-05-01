package src.softies;

// an octagonal cell is where players place their stones
// extends Cell with no additional state since geometry is handled by WorldCalculator
public class OctagonalCell extends Cell {

    OctagonalCell(Point coordinates, PlayerColour colour, CellType cellType) {
        super(coordinates, colour, cellType);
    }
}
