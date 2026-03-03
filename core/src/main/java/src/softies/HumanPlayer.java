package src.softies;

// represents a human player in the game
public class HumanPlayer {
    // stores the colour assigned to this player
    private PlayerColour colour;

    // constructs a human player with a specific colour
    public HumanPlayer(PlayerColour colour) {
        this.colour = colour;
    }

    // gets the player's colour
    public PlayerColour getColour() {
        return this.colour;
    }
}
