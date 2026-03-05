//package src.softies;
//
//import org.junit.jupiter.api.*;
//import static org.junit.jupiter.api.Assertions.*;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//
///**
// * Combined Test Suite for Quax Board Game.
// * Covers functional and boundary testing for Model, View, and Controller components.
// */
//public class QuaxTestSuite {
//
//    // --- CELL COMPONENT TESTS ---
//    @Nested
//    @DisplayName("Cell Implementation Tests")
//    class CellTests {
//        @Test
//        @DisplayName("Octagonal Cell: Display symbols and occupancy")
//        void testOctagonalCell() {
//            OctagonalCell cell = new OctagonalCell(new Point(0, 0), PlayerColour.EMPTY, CellType.OCTAGON);
//            assertEquals("O", cell.getDisplaySymbol(), "New cells should display 'O'");
//
//            cell.setColour(PlayerColour.BLACK);
//            cell.setOccupied(true);
//            assertEquals("B", cell.getDisplaySymbol(), "Occupied black cells should display 'B'");
//        }
//
//        @Test
//        @DisplayName("Rhombic Cell: Static display symbol")
//        void testRhombicCell() {
//            RhombicCell cell = new RhombicCell(new Point(1, 1), PlayerColour.EMPTY, CellType.RHOMBUS);
//            assertEquals("x", cell.getDisplaySymbol(), "Rhombus should display 'x'");
//        }
//
//        @Test
//        @DisplayName("Cell: display correct symbol")
//        void testCellDisplayCorrectSymbol() {
//            RhombicCell cell = new RhombicCell(new Point(1, 1), PlayerColour.BLACK, CellType.RHOMBUS);
//            assertEquals("B", cell.getDisplaySymbol(), "Black Rhombus should display 'B'");
//            cell.setColour(PlayerColour.WHITE);
//            assertEquals("W", cell.getDisplaySymbol(), "White Rhombus should display 'W'");
//        }
//    }
//
//    // --- BOARD LOGIC TESTS ---
//    @Nested
//    @DisplayName("Board Logic & Boundary Tests")
//    class BoardTests {
//        private QuaxBoard board;
//
//        @BeforeEach
//        void setUp() {
//            board = new QuaxBoard();
//        }
//
////        @Test
////        @DisplayName("Boundary: Valid and Invalid labels")
////        void testLabelValidation() {
////            assertTrue(board.isValidOctCellLabel("A1"), "A1 is the start boundary"); //instead of isValidOctCellLabel, lets do isValidLabel
////            assertTrue(board.isValidOctCellLabel("K11"), "K11 is the end boundary"); // ^ will provide an easier interface
////            assertFalse(board.isValidOctCellLabel("L12"), "L12 is out of bounds");
////            assertFalse(board.isValidOctCellLabel(""), "Empty string should be invalid");
////        }
//
//        @Test
//        @DisplayName("Functionality: Stone placement and overlap prevention")
//        void testStonePlacement() {
//            // First move
//            assertTrue(board.placeStone("B5", PlayerColour.WHITE));
//            assertEquals(PlayerColour.WHITE, board.getOctagonalCell("B5").getColour());
//
//            // Overlap attempt
//            assertFalse(board.placeStone("B5", PlayerColour.BLACK), "Cannot place stone on occupied cell");
//        }
//
//        @Test
//        @DisplayName("Mapping: Rhombic cell presence")
//        void testRhombicCoordinates() {
//            // Test a standard middle-board rhombus
//            assertNotNull(board.getRhombicCell("R-B2"));
//            // Boundary: Row 1 rhombuses shouldn't exist based on your init logic (row > 1)
//            assertNull(board.getRhombicCell("R-A1"), "Rhombus shouldn't exist on bottom edge");
//        }
//    }
//
//    // --- CONTROLLER STATE TESTS ---
//    @Nested
//    @DisplayName("Controller State & Turn Tests")
//    class ControllerTests {
//        @Test
//        @DisplayName("State: Turn switching logic")
//        void testTurnSwitching() throws Exception {
//            QuaxController controller = new QuaxController();
//
//            // Accessing private field 'currentPlayer' via reflection for testing
//            Field playerField = QuaxController.class.getDeclaredField("currentPlayer");
//            playerField.setAccessible(true);
//
//            assertEquals(PlayerColour.BLACK, playerField.get(controller), "Game must start with BLACK");
//
//            // Access and invoke private method 'switchTurn'
//            Method switchMethod = QuaxController.class.getDeclaredMethod("switchTurn");
//            switchMethod.setAccessible(true);
//
//            switchMethod.invoke(controller);
//            assertEquals(PlayerColour.WHITE, playerField.get(controller), "Turn should switch to WHITE");
//        }
//    }
//
//    // --- VIEW / DISPLAY SMOKE TESTS ---
//    @Nested
//    @DisplayName("View Rendering Tests")
//    class ViewTests {
//        @Test
//        @DisplayName("Smoke Test: Render should not crash")
//        void testRenderStability() {
//            GameDisplay display = new GameDisplay();
//            QuaxBoard board = new QuaxBoard();
//            // We ensure that passing current states doesn't throw exceptions
//            assertDoesNotThrow(() -> display.renderBoard(board, PlayerColour.BLACK));
//        }
//    }
//
//    @Nested
//    @DisplayName("Tile Location Tests")
//    class  TileLocationTests {
//        @Test
//        @DisplayName("Proximity test")
//        void testProximity() {
//            Point p1 = new Point(1, 1);
//            Point p2 = new Point(1, 1);
//            assertTrue(p1.isAdjacent(p2));
//
//            Point p3 = new Point(1, 3);
//            Point p4 = new Point(3, 1);
//            assertFalse(p3.isAdjacent(p4));
//            assertFalse(p3.isAdjacent(p1));
//            assertFalse(p4.isAdjacent(p1));
//        }
//
//        void testBoundaries() {
//            assertThrows(IndexOutOfBoundsException.class, () -> new Point(-1,-1)); //Out of the scope (negative)
//            assertThrows(IndexOutOfBoundsException.class, () -> new Point(100,100)); //out of the scope (too large)
//        }
//    }
//}


package src.softies;

import org.junit.jupiter.api.*;
import java.io.*;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the terminal version of Quax.
 * Covers GameDisplay output and QuaxController input handling.
 */
public class QuaxTestSuite {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    @BeforeEach
    void setUpStreams() {
        outContent.reset();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    // ------------------------------------------------------------------------
    // GameDisplay tests
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("GameDisplay: displayStartScreen prints expected text")
    void testDisplayStartScreen() {
        GameDisplay display = new GameDisplay();
        display.displayStartScreen();
        String output = outContent.toString();
        assertTrue(output.contains("Welcome to Quax Board!"), "Should contain welcome");
        assertTrue(output.contains("Press Enter Key to Start!"), "Should prompt for Enter");
        assertTrue(output.contains("type quit"), "Should mention quit");
    }

    @Test
    @DisplayName("GameDisplay: displayModeSelection prints mode options")
    void testDisplayModeSelection() {
        GameDisplay display = new GameDisplay();
        display.displayModeSelection();
        String output = outContent.toString();
        assertTrue(output.contains("Choose Game Mode:"), "Should show mode selection");
        assertTrue(output.contains("Human vs Human"), "Should list human vs human");
        assertTrue(output.contains("Human vs Bot"), "Should list human vs bot");
    }

    @Test
    @DisplayName("GameDisplay: printHeader shows correct mode")
    void testPrintHeader() {
        GameDisplay display = new GameDisplay();
        display.printHeader(GameMode.HUMAN_VS_HUMAN);
        String output = outContent.toString();
        assertTrue(output.contains("Quax (Human vs Human)"), "Header for human vs human");

        outContent.reset();
        display.printHeader(GameMode.HUMAN_VS_BOT);
        output = outContent.toString();
        assertTrue(output.contains("Quax (Human vs Bot)"), "Header for human vs bot");
    }

    @Test
    @DisplayName("GameDisplay: showMessage prints prefixed message")
    void testShowMessage() {
        GameDisplay display = new GameDisplay();
        display.showMessage("Test message");
        assertEquals(">> Test message", outContent.toString());
    }

    @Test
    @DisplayName("GameDisplay: showLoading prints dots with delay")
    void testShowLoading() {
        GameDisplay display = new GameDisplay();
        display.showLoading("Loading");
        String output = outContent.toString();
        assertTrue(output.startsWith("Loading"), "Should start with message");
        assertTrue(output.endsWith("...\n"), "Should end with three dots and newline");
    }

    @Test
    @DisplayName("GameDisplay: renderBoard prints board structure")
    void testRenderBoard() {
        QuaxBoard board = new QuaxBoard();
        GameDisplay display = new GameDisplay();
        display.renderBoard(board, PlayerColour.BLACK);
        String output = outContent.toString();
        // Check for key elements
        assertTrue(output.contains("Current Player: BLACK"), "Should show current player");
        assertTrue(output.contains("A     B     C"), "Should have column headers");
        assertTrue(output.contains("11"), "Should have row number 11");
        assertTrue(output.contains("1"), "Should have row number 1");
        assertTrue(output.contains("X"), "Should show empty octagons as X");
        assertTrue(output.contains("o"), "Should show rhombic cells as o");
    }

    // ------------------------------------------------------------------------
    // QuaxController tests (require simulated input)
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("QuaxController: start screen – Enter proceeds, quit exits")
    void testStartScreenInput() {
        // Simulate pressing Enter
        String input = "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        QuaxController controller = new QuaxController();
        // Use reflection to call the private waitForEnterOrQuit method
        try {
            java.lang.reflect.Method method = QuaxController.class.getDeclaredMethod("waitForEnterOrQuit");
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(controller);
            assertTrue(result, "Enter should return true");
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }

        // Simulate typing "quit"
        input = "quit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        controller = new QuaxController();
        try {
            java.lang.reflect.Method method = QuaxController.class.getDeclaredMethod("waitForEnterOrQuit");
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(controller);
            assertFalse(result, "quit should return false");
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("QuaxController: mode selection – human and bot are accepted")
    void testModeSelection() {
        // Test "human"
        String input = "human\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        QuaxController controller = new QuaxController();
        try {
            java.lang.reflect.Method method = QuaxController.class.getDeclaredMethod("getGameModeInput");
            method.setAccessible(true);
            GameMode mode = (GameMode) method.invoke(controller);
            assertEquals(GameMode.HUMAN_VS_HUMAN, mode, "human should give HUMAN_VS_HUMAN");
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }

        // Test "bot"
        input = "bot\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        controller = new QuaxController();
        try {
            java.lang.reflect.Method method = QuaxController.class.getDeclaredMethod("getGameModeInput");
            method.setAccessible(true);
            GameMode mode = (GameMode) method.invoke(controller);
            assertEquals(GameMode.HUMAN_VS_BOT, mode, "bot should give HUMAN_VS_BOT");
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("QuaxController: mode selection – invalid input loops until valid")
    void testModeSelectionInvalid() {
        // Simulate invalid then valid input
        String input = "invalid\nhuman\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        QuaxController controller = new QuaxController();
        try {
            java.lang.reflect.Method method = QuaxController.class.getDeclaredMethod("getGameModeInput");
            method.setAccessible(true);
            GameMode mode = (GameMode) method.invoke(controller);
            assertEquals(GameMode.HUMAN_VS_HUMAN, mode, "Should eventually accept human");
            // Check that error message was printed
            String output = outContent.toString();
            assertTrue(output.contains("Invalid input"), "Should show error message");
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("QuaxController: game loop – valid move switches turn")
    void testGameLoopValidMove() throws Exception {
        // Simulate: start screen Enter, mode human, then move "A1", then quit
        String input = "\nhuman\nA1\nquit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        QuaxController controller = new QuaxController();
        controller.launch();

        // Check that after A1, the board cell A1 is occupied by BLACK
        QuaxBoard board = getBoardFromController(controller);
        OctagonalCell cell = board.getOctagonalCell("A1");
        assertNotNull(cell, "Cell A1 should exist");
        assertTrue(cell.isOccupied(), "Cell A1 should be occupied after first move");
        assertEquals(PlayerColour.BLACK, cell.getColour(), "First move should be BLACK");

        // Check that turn switched to WHITE
        PlayerColour current = getCurrentPlayerFromController(controller);
        assertEquals(PlayerColour.WHITE, current, "After move, turn should be WHITE");
    }

    @Test
    @DisplayName("QuaxController: game loop – invalid move does not switch turn")
    void testGameLoopInvalidMove() throws Exception {
        // Simulate: start, human, then invalid "Z9", then valid "A1", then quit
        String input = "\nhuman\nZ9\nA1\nquit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        QuaxController controller = new QuaxController();
        controller.launch();

        // After Z9 (invalid), turn should still be BLACK, and A1 should be placed by BLACK
        QuaxBoard board = getBoardFromController(controller);
        OctagonalCell cell = board.getOctagonalCell("A1");
        assertTrue(cell.isOccupied(), "A1 should be occupied");
        assertEquals(PlayerColour.BLACK, cell.getColour(), "A1 placed by BLACK");

        // Turn should now be WHITE (since only A1 counted)
        PlayerColour current = getCurrentPlayerFromController(controller);
        assertEquals(PlayerColour.WHITE, current, "Turn should be WHITE after valid move");
    }

    @Test
    @DisplayName("QuaxController: game loop – quit command exits")
    void testGameLoopQuit() {
        String input = "\nhuman\nquit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        QuaxController controller = new QuaxController();
        controller.launch();

        // Check that the game stopped – we can't easily check running flag, but output should contain "Exiting game"
        String output = outContent.toString();
        assertTrue(output.contains("Exiting game"), "Should print exit message");
    }

    // ------------------------------------------------------------------------
    // Helper methods to access private fields using reflection
    // ------------------------------------------------------------------------
    private QuaxBoard getBoardFromController(QuaxController controller) throws Exception {
        Field boardField = QuaxController.class.getDeclaredField("board");
        boardField.setAccessible(true);
        return (QuaxBoard) boardField.get(controller);
    }

    private PlayerColour getCurrentPlayerFromController(QuaxController controller) throws Exception {
        Field playerField = QuaxController.class.getDeclaredField("currentPlayer");
        playerField.setAccessible(true);
        return (PlayerColour) playerField.get(controller);
    }
}
