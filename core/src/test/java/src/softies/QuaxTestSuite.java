package src.softies;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Combined Test Suite for Quax Board Game.
 * Covers functional and boundary testing for cells, board, and tile geometry.
 */
public class QuaxTestSuite {

    @Nested
    @DisplayName("Cell Implementation Tests")
    class CellTests {
        @Test
        @DisplayName("Octagonal Cell: Display symbols and occupancy")
        void testOctagonalCell() {
            OctagonalCell cell = new OctagonalCell(new Point(0, 0), PlayerColour.EMPTY, CellType.OCTAGON);
            assertEquals("X", cell.getDisplaySymbol(), "New cells should display 'X'");

            cell.setColour(PlayerColour.BLACK);
            cell.setOccupied(true);
            assertEquals("B", cell.getDisplaySymbol(), "Occupied black cells should display 'B'");
        }

        @Test
        @DisplayName("Rhombic Cell: Static display symbol")
        void testRhombicCell() {
            RhombicCell cell = new RhombicCell(new Point(1, 1), PlayerColour.EMPTY, CellType.RHOMBUS);
            assertEquals("o", cell.getDisplaySymbol(), "Rhombus should display 'o'");
        }

        @Test
        @DisplayName("Cell: display correct symbol")
        void testCellDisplayCorrectSymbol() {
            RhombicCell cell = new RhombicCell(new Point(1, 1), PlayerColour.BLACK, CellType.RHOMBUS);
            cell.setOccupied(true);
            assertEquals("B", cell.getDisplaySymbol(), "Black Rhombus should display 'B'");
            cell.setColour(PlayerColour.WHITE);
            assertEquals("W", cell.getDisplaySymbol(), "White Rhombus should display 'W'");
        }
    }

    @Nested
    @DisplayName("Board Logic & Boundary Tests")
    class BoardTests {
        private QuaxBoard board;

        @BeforeEach
        void setUp() {
            board = new QuaxBoard();
        }

        @Test
        @DisplayName("Functionality: Stone placement and overlap prevention")
        void testStonePlacement() {
            assertTrue(board.placeStone("B5", PlayerColour.WHITE));
            assertEquals(PlayerColour.WHITE, board.getOctagonalCell("B5").getColour());

            assertFalse(board.placeStone("B5", PlayerColour.BLACK), "Cannot place stone on occupied cell");
        }

        @Test
        @DisplayName("Mapping: Rhombic cell presence")
        void testRhombicCoordinates() {
            assertNotNull(board.getRhombicCell("R-B2"));
            assertNull(board.getRhombicCell("R-A1"), "Rhombus shouldn't exist on bottom edge");
        }
    }

    @Nested
    @DisplayName("Tile Location Tests")
    class TileLocationTests {
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
            assertThrows(IndexOutOfBoundsException.class, () -> new Point(-1, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> new Point(100, 100));
        }
    }
}
