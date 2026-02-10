public class QuaxBoard {


    public static void displayBoard(/* boardState */){

        // we will pass the state of the board into this function which is
        // a list of cells. each cell will have a label (X, O, B?, W?) to display
        // so like when the cell is occupied by WHITE it displays W
        // Here now X is an octagonal cells and O is rhombic
        // boardState will return a list of cells that we will print here

        // print letters
        System.out.print("      ");
        for (char c = 65; c <= 75; c++){
            System.out.print(c + "     ");
        }
        System.out.println("\n");

        // print rows
        for (int i = 11; i >= 1; i--){
            // add a space for i < 10 before the number
            if (i < 10){
                System.out.print(" ");
            }
            //print row number
            System.out.print(i + "    ");
            // print octagonal cells
            for (int j = 0; j < 11; j++){
                System.out.print("X  ");
                // does not print '-' for the last element in each row
                if (j != 10){
                    System.out.print("-  ");
                }
            }
            // add rhombic cells
            if (i > 1){
                System.out.println();
                System.out.print("         ");
                for (int k = 0; k < 10; k++){
                    System.out.print("O  ");
                    if (k != 9){
                        System.out.print("-  ");
                    }
                }
            }
            System.out.println();
        }

        // storing chars in boardState im a 2D array
        // have to change to list?
        char[][] boardState = new char[21][21];
        for (int i = 0; i < 21; i++){
            for (int j = 0; j < 11; j++){
                if (i % 2 == 0) {
                    boardState[i][j] = 'X';
                } else if (j != 10) {
                    boardState[i][j] = 'O';
                }
            }
        }

        for (int i =  0; i < 21; i++){
            for (int j = 0; j < 11; j++){
                if (j < 10 || i % 2 == 0) {
                    System.out.print(boardState[i][j]);
                }
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {

        displayBoard();

    }
}
