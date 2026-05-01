package src.softies;

// a rhombic cell sits between four octagonal cells and acts as a diagonal connector
// players can place a rhombus to link two of their diagonally adjacent stones
public class RhombicCell extends Cell {

    public RhombicCell(Point coordinates, PlayerColour colour, CellType cellType) {
        super(coordinates, colour, cellType);
    }

    /**
     * returns "B" for black, "W" for white, or "o" when empty
     * overrides the base Cell version to distinguish empty rhombuses from empty octagons
     */
    @Override
    public String getDisplaySymbol() {
        if (!isOccupied()) return "o";
        return (colour == PlayerColour.BLACK) ? "B" : "W";
    }
}
