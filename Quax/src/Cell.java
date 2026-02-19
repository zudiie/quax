public class Cell {

    protected Point coordinates;
    protected PlayerColour colour;
    protected boolean isOccupied;
    protected CellType cellType;
    private String label;

    Cell(Point coordinates, PlayerColour colour, CellType cellType) {
        this.coordinates = coordinates;
        this.colour = colour;
        this.cellType = cellType;
        this.isOccupied = false;
    }

    public Point getCoordinates() {
        return coordinates;
    }

    public PlayerColour getColour() {
        return colour;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public CellType getCellType() {
        return cellType;
    }

    public void setColour(PlayerColour colour) {
        this.colour = colour;
    }

    public void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
    }

    public String getLabel(){return label;}

    public String getDisplaySymbol(String x) {
            if (colour == PlayerColour.BLACK) return "B";
            if (colour == PlayerColour.WHITE) return "W";
            return x;
        }

}
