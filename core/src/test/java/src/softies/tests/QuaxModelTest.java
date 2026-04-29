package src.softies.tests;

import org.junit.*;
import src.softies.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for the Quax model (board, cells).
 * Covers requirements SR1.2-SR1.5, SR5.
 */
public class QuaxModelTest {

    private QuaxBoard board;

    @Before
    public void setUp() {
        board = new QuaxBoard();
    }

    // ------------------------------------------------------------------------
    // SR1.2, SR1.3: Board initialization - 11x11 octagons and rhombic cells
    // ------------------------------------------------------------------------
    @Test
    public void testBoardInitialization() {
        // all 121 octagonal cells exist
        for (int row = 1; row <= 11; row++) {
            for (int col = 0; col < 11; col++) {
                String label = generateLabel(col, row);
                assertNotNull("Octagonal cell " + label + " should exist (SR1.2)",
                    board.getOctagonalCell(label));
            }
        }

        // rhombic cells exist only where col < 10 and row > 1 (total 100)
        int rhombusCount = 0;
        for (int row = 1; row <= 11; row++) {
            for (int col = 0; col < 11; col++) {
                String rKey = "R-" + generateLabel(col, row);
                if (col < 10 && row > 1) {
                    assertNotNull("Rhombic cell " + rKey + " should exist (SR1.3)",
                        board.getRhombicCell(rKey));
                    rhombusCount++;
                } else {
                    assertNull("Rhombic cell " + rKey + " should not exist (SR1.3)",
                        board.getRhombicCell(rKey));
                }
            }
        }
        assertEquals("There should be exactly 100 rhombic cells", 100, rhombusCount);
    }

    // ------------------------------------------------------------------------
    // SR1.4, SR1.5: Label validation (A1 .. K11)
    // ------------------------------------------------------------------------
    @Test
    public void testLabelValidation() {
        assertTrue("A1 should be valid", board.isValidLabel("A1"));
        assertTrue("K11 should be valid", board.isValidLabel("K11"));
        assertTrue("F6 should be valid", board.isValidLabel("F6"));

        assertFalse("Empty string invalid", board.isValidLabel(""));
        assertFalse("Single character invalid", board.isValidLabel("A"));
        assertFalse("Numeric only invalid", board.isValidLabel("12"));
        assertFalse("Row 0 invalid", board.isValidLabel("A0"));
        assertFalse("Row >11 invalid", board.isValidLabel("A12"));
        assertFalse("Column L invalid", board.isValidLabel("L1"));
        assertFalse("Reversed format invalid", board.isValidLabel("1A"));
    }

    // ------------------------------------------------------------------------
    // SR5: Stone placement on empty cells, prevention of overlap
    // ------------------------------------------------------------------------
    @Test
    public void testStonePlacement() {
        String cell = "B5";

        assertTrue("First placement on empty should succeed (SR5)",
            board.placeStone(cell, PlayerColour.BLACK));
        OctagonalCell placed = board.getOctagonalCell(cell);
        assertEquals(PlayerColour.BLACK, placed.getColour());
        assertTrue(placed.isOccupied());

        assertFalse("Second placement on same cell should fail (occupied)",
            board.placeStone(cell, PlayerColour.WHITE));
        assertEquals(PlayerColour.BLACK, placed.getColour());
    }

    @Test
    public void testPlacementInvalidLabel() {
        String invalid = "Z9";
        assertFalse(board.isValidLabel(invalid));
        assertFalse("Placement on invalid label should fail",
            board.placeStone(invalid, PlayerColour.BLACK));
    }

    private String generateLabel(int col, int row) {
        char colChar = (char) ('A' + col);
        return "" + colChar + row;
    }
}
