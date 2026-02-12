public class Player {

    protected PlayerColour colour;
    protected boolean isHuman;

    public Player(PlayerColour colour, boolean isHuman) {
        this.colour = colour;
        this.isHuman = isHuman;
    }

    public PlayerColour getColour() {
        return colour;
    }

    public void setColour(PlayerColour colour) {
        this.colour = colour;
    }

    public boolean isHuman() {
        return isHuman;
    }

//    public String makeMove(board){
//              TODO
//    }
}
