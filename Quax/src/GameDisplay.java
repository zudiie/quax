
public class GameDisplay {
    public void displayBoard(QuaxBoard board, PlayerColour currentPlayer) {
        System.out.println();
        System.out.println("Current Player: " + currentPlayer);
        System.out.println();
        final int BOARD_SIZE = 11;

        // Board printing is currently static in QuaxBoard
        displayBoard(board, currentPlayer);
        System.out.println();
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void renderBoard(QuaxBoard board, PlayerColour currentPlayer) {
        System.out.println();
        System.out.println("Current Player: " + currentPlayer);
        System.out.println();
        displayBoard(board, currentPlayer);
    }
}
