
import java.util.Scanner;
public class Main {
    public static void main (String[] args) {

        QuaxController controller = new QuaxController();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Quax (Human v Human)");
        System.out.println("Enter a move like A1, B7, K11. Type 'quit' to exit.");

        while (controller.isRunning()){
            controller.render();

            System.out.print("Move for " + controller.getCurrentPlayer() + ": ");
            String input = scanner.nextLine();

            if(input == null) continue;
            input = input.trim();

            if (input.equalsIgnoreCase("quit")) {
                controller.quitGame();
                break;
            }

            if(controller.attemptMove(input)){
                controller.changePlayer();
            }
        }
        scanner.close();
    }
}
