package src.softies;

public class OctagonalCell extends Cell {

    private final int radius = 2;
    private Point center;
    private Direction directions;


    OctagonalCell(Point coordinates, PlayerColour colour, CellType cellType) {
        super(coordinates, colour, cellType);
    }


}
