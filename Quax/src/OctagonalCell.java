
public class OctagonalCell extends Cell {

    private final int radius = 2;
    private Point center;
    private Direction directions;


    OctagonalCell(Point coordinates, PlayerColour colour, CellType cellType) {
        super(coordinates, colour, cellType);
    }

    @Override
    public String getDisplaySymbol() {
        if (!isOccupied) return "O";
        return (colour == PlayerColour.BLACK) ? "B" : "W";
    }


}
