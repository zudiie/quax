package src.softies;

// the base player class - holds whether this player is human or a bot, plus their colour
// HumanPlayer extends behaviour from this, and a bot player would too eventually
public class Player {

    protected PlayerColour colour;
    protected boolean isHuman;

    /**
     * sets up a player with their colour and whether they're human-controlled
     * @param colour BLACK or WHITE
     * @param isHuman true for a human, false for a bot
     */
    public Player(PlayerColour colour, boolean isHuman) {
        this.colour = colour;
        this.isHuman = isHuman;
    }

    /**
     * @return this player's colour
     */
    public PlayerColour getColour() {
        return colour;
    }

    /**
     * updates this player's colour - mostly useful if reassigning sides mid-setup
     * @param colour the new colour to assign
     */
    public void setColour(PlayerColour colour) {
        this.colour = colour;
    }

    /**
     * @return true if this player is a human (not a bot)
     */
    public boolean isHuman() {
        return isHuman;
    }
}
