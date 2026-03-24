package src.softies;

// a simple wrapper for a human-controlled player - just holds their assigned colour
// this is separate from the Player class so human-specific logic can live here later on
public class HumanPlayer {

    private PlayerColour colour;

    /**
     * creates a human player and assigns them a colour for the game
     * @param colour BLACK or WHITE depending on turn order
     */
    public HumanPlayer(PlayerColour colour) {
        this.colour = colour;
    }

    /**
     * @return the colour assigned to this human player
     */
    public PlayerColour getColour() {
        return this.colour;
    }
}
