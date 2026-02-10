public class QuaxBoard {


    public static void displayBoard(){
        // print letters
        System.out.print("      ");
        for (char c = 65; c <= 75; c++){
            System.out.print(c + "   ");
        }
        System.out.println("\n");

        // print rows
        for (int i = 1; i <= 11; i++){
            // add a space for i < 10 before the number
            if (i < 10){
                System.out.print(" ");
            }
            //print row number
            System.out.print(i + "    ");
            // print octagonal cells
            for (int j = 0; j < 11; j++){
                System.out.print("X ");
                // does not print '-' for the last element in each row
                if (j != 10){
                    System.out.print("- ");
                }
            }
            // add rhombic cells
            if (i < 11){
                System.out.println();
                System.out.print("        ");
                for (int k = 0; k < 10; k++){
                    System.out.print("O ");
                    if (k != 9){
                        System.out.print("- ");
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
