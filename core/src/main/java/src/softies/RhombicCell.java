package src.softies;

public class RhombicCell extends Cell {

    private Point center;
    private Direction directions;

    public RhombicCell(Point coordinates, PlayerColour colour, CellType cellType) {
        super(coordinates, colour, cellType);
    }

    @Override
    public String getDisplaySymbol() {
        if (isOccupied()) {
            if (colour == PlayerColour.BLACK) return "B";
            if (colour == PlayerColour.WHITE) return "W";
        }
        return "o"; // empty rhombus
    }
}
