public class QuaxBoard {


    public static void displayBoard(){

        // we will pass the state of the board into this function which is
        // a list of cells. each cell will have a label (X, O, B?, W?) to display
        // so like when the cell is occupied by WHITE it dissplays W
        // Here now X is an octagonal cells and O is rhombic


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
    }

    public static void main(String[] args) {

        displayBoard();

    }
}
