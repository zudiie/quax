import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Combined Test Suite for Quax Board Game.
 * Covers functional and boundary testing for Model, View, and Controller components.
 */
public class QuaxTestSuite {

    // --- CELL COMPONENT TESTS ---
    @Nested
    @DisplayName("Cell Implementation Tests")
    class CellTests {
        @Test
        @DisplayName("Octagonal Cell: Display symbols and occupancy")
        void testOctagonalCell() {
            OctagonalCell cell = new OctagonalCell(new Point(0, 0), PlayerColour.EMPTY, CellType.OCTAGON);
            assertEquals("O", cell.getDisplaySymbol(), "New cells should display 'O'");

            cell.setColour(PlayerColour.BLACK);
            cell.setOccupied(true);
            assertEquals("B", cell.getDisplaySymbol(), "Occupied black cells should display 'B'");
        }

        @Test
        @DisplayName("Rhombic Cell: Static display symbol")
        void testRhombicCell() {
            RhombicCell cell = new RhombicCell(new Point(1, 1), PlayerColour.EMPTY, CellType.RHOMBUS);
            assertEquals("x", cell.getDisplaySymbol(), "Rhombus should display 'x'");
        }

        @Test
        @DisplayName("Cell: display correct symbol")
        void testCellDisplayCorrectSymbol() {
            RhombicCell cell = new RhombicCell(new Point(1, 1), PlayerColour.BLACK, CellType.RHOMBUS);
            assertEquals("B", cell.getDisplaySymbol(), "Black Rhombus should display 'B'");
            cell.setColour(PlayerColour.WHITE);
            assertEquals("W", cell.getDisplaySymbol(), "White Rhombus should display 'W'");
        }
    }

    // --- BOARD LOGIC TESTS ---
    @Nested
    @DisplayName("Board Logic & Boundary Tests")
    class BoardTests {
        private QuaxBoard board;

        @BeforeEach
        void setUp() {
            board = new QuaxBoard();
        }

        @Test
        @DisplayName("Boundary: Valid and Invalid labels")
        void testLabelValidation() {
            assertTrue(board.isValidOctCellLabel("A1"), "A1 is the start boundary"); //instead of isValidOctCellLabel, lets do isValidLabel
            assertTrue(board.isValidOctCellLabel("K11"), "K11 is the end boundary"); // ^ will provide an easier interface
            assertFalse(board.isValidOctCellLabel("L12"), "L12 is out of bounds");
            assertFalse(board.isValidOctCellLabel(""), "Empty string should be invalid");
        }

        @Test
        @DisplayName("Functionality: Stone placement and overlap prevention")
        void testStonePlacement() {
            // First move
            assertTrue(board.placeStone("B5", PlayerColour.WHITE));
            assertEquals(PlayerColour.WHITE, board.getOctagonalCell("B5").getColour());

            // Overlap attempt
            assertFalse(board.placeStone("B5", PlayerColour.BLACK), "Cannot place stone on occupied cell");
        }

        @Test
        @DisplayName("Mapping: Rhombic cell presence")
        void testRhombicCoordinates() {
            // Test a standard middle-board rhombus
            assertNotNull(board.getRhombicCell("R-B2"));
            // Boundary: Row 1 rhombuses shouldn't exist based on your init logic (row > 1)
            assertNull(board.getRhombicCell("R-A1"), "Rhombus shouldn't exist on bottom edge");
        }
    }

    // --- CONTROLLER STATE TESTS ---
    @Nested
    @DisplayName("Controller State & Turn Tests")
    class ControllerTests {
        @Test
        @DisplayName("State: Turn switching logic")
        void testTurnSwitching() throws Exception {
            QuaxController controller = new QuaxController();

            // Accessing private field 'currentPlayer' via reflection for testing
            Field playerField = QuaxController.class.getDeclaredField("currentPlayer");
            playerField.setAccessible(true);

            assertEquals(PlayerColour.BLACK, playerField.get(controller), "Game must start with BLACK");

            // Access and invoke private method 'switchTurn'
            Method switchMethod = QuaxController.class.getDeclaredMethod("switchTurn");
            switchMethod.setAccessible(true);

            switchMethod.invoke(controller);
            assertEquals(PlayerColour.WHITE, playerField.get(controller), "Turn should switch to WHITE");
        }
    }

    // --- VIEW / DISPLAY SMOKE TESTS ---
    @Nested
    @DisplayName("View Rendering Tests")
    class ViewTests {
        @Test
        @DisplayName("Smoke Test: Render should not crash")
        void testRenderStability() {
            GameDisplay display = new GameDisplay();
            QuaxBoard board = new QuaxBoard();
            // We ensure that passing current states doesn't throw exceptions
            assertDoesNotThrow(() -> display.renderBoard(board, PlayerColour.BLACK));
        }
    }

    @Nested
    @DisplayName("Tile Location Tests")
    class  TileLocationTests {
        @Test
        @DisplayName("Proximity test")
        void testProximity() {
            Point p1 = new Point(1, 1);
            Point p2 = new Point(1, 1);
            assertTrue(p1.isAdjacent(p2));

            Point p3 = new Point(1, 3);
            Point p4 = new Point(3, 1);
            assertFalse(p3.isAdjacent(p4));
            assertFalse(p3.isAdjacent(p1));
            assertFalse(p4.isAdjacent(p1));
        }

        void testBoundaries() {
            assertThrows(IndexOutOfBoundsException.class, () -> new Point(-1,-1)); //Out of the scope (negative)
            assertThrows(IndexOutOfBoundsException.class, () -> new Point(100,100)); //out of the scope (too large)
        }
    }
}