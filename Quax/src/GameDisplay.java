public class GameDisplay {
    public void displayBoard(QuaxBoard board, PlayerColour currentPlayer) {
        System.out.println();
        System.out.println("Current Player: " + currentPlayer);
        System.out.println();

        // Board printing is currently static in QuaxBoard
        QuaxBoard.displayBoard();
        System.out.println();
    }

        public void showMessage(String message) {
        System.out.println(message);
        }

    }
