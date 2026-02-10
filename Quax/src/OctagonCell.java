public class OctagonCell extends Cell {

    private final int radius = 2;
    private Point center;
    private Direction directions;


    OctagonCell(Point coordinates, PlayerColour colour, CellType cellType) {
        super(coordinates, colour, cellType);
    }

}
