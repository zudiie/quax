public class Cell {


    protected PlayerColour colour;
    protected boolean isOccupied;
    protected CellType cellType;
    protected Point label;

    Cell(Point label, PlayerColour colour, CellType cellType) {
        this.label = label;
        this.colour = colour;
        this.cellType = cellType;
        this.isOccupied = false;
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

    public Point getLabel(){return label;}

    public String getDisplaySymbol() {
            if (colour == PlayerColour.BLACK) return "B";
            if (colour == PlayerColour.WHITE) return "W";
            return "x";
        }

}
