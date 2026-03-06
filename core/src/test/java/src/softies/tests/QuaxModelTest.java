package src.softies.tests;

import org.junit.*;
import src.softies.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for the Quax model (board, cells, controller).
 * Covers requirements SR1, SR1.2–SR1.5, SR2, SR5.
 */
public class QuaxModelTest {

    private QuaxBoard board;
    private QuaxController controller;

    @Before
    public void setUp() {
        board = new QuaxBoard();
        controller = new QuaxController();
    }

    // ------------------------------------------------------------------------
    // SR1.2, SR1.3: Board initialization – 11x11 octagons and rhombic cells
    // ------------------------------------------------------------------------
    @Test
    public void testBoardInitialization() {
        // Check all 121 octagonal cells exist
        for (int row = 1; row <= 11; row++) {
            for (int col = 0; col < 11; col++) {
                String label = generateLabel(col, row);
                assertNotNull("Octagonal cell " + label + " should exist (SR1.2)",
                    board.getOctagonalCell(label));
            }
        }

        // Check rhombic cells: exist only where col < 10 and row > 1 (total 100)
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
        // Valid labels
        assertTrue("A1 should be valid", board.isValidLabel("A1"));
        assertTrue("K11 should be valid", board.isValidLabel("K11"));
        assertTrue("F6 should be valid", board.isValidLabel("F6"));

        // Invalid labels
        assertFalse("Empty string invalid", board.isValidLabel(""));
        assertFalse("Single character invalid", board.isValidLabel("A"));
        assertFalse("Numeric only invalid", board.isValidLabel("12"));
        assertFalse("Row 0 invalid", board.isValidLabel("A0"));
        assertFalse("Row >11 invalid", board.isValidLabel("A12"));
        assertFalse("Column L invalid", board.isValidLabel("L1"));
        assertFalse("Reversed format invalid", board.isValidLabel("1A"));
    }

    // ------------------------------------------------------------------------
    // SR1: On launch, BLACK's turn (controller)
    // ------------------------------------------------------------------------
    @Test
    public void testInitialTurn() throws Exception {
        Field currentPlayerField = QuaxController.class.getDeclaredField("currentPlayer");
        currentPlayerField.setAccessible(true);
        Assert.assertEquals("Game starts with BLACK (SR1)", PlayerColour.BLACK,
            currentPlayerField.get(controller));
    }

    // ------------------------------------------------------------------------
    // SR1.1: Title printed by GameDisplay (terminal)
    // ------------------------------------------------------------------------
    @Test
    public void testPrintHeaderHumanVsHuman() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        GameDisplay display = new GameDisplay();
        display.printHeader(GameMode.HUMAN_VS_HUMAN);
        assertTrue(outContent.toString().contains("Quax (Human vs Human)"));
        System.setOut(System.out);
    }

    @Test
    public void testPrintHeaderHumanVsBot() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        GameDisplay display = new GameDisplay();
        display.printHeader(GameMode.HUMAN_VS_BOT);
        assertTrue(outContent.toString().contains("Quax (Human vs Bot)"));
        System.setOut(System.out);
    }

    // ------------------------------------------------------------------------
    // SR2.1, SR2.2: Turn text in renderBoard
    // ------------------------------------------------------------------------
    @Test
    public void testRenderBoardTurnText() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        GameDisplay display = new GameDisplay();

        display.renderBoard(board, PlayerColour.BLACK);
        assertTrue(outContent.toString().contains("Current Player: BLACK"));

        outContent.reset();
        display.renderBoard(board, PlayerColour.WHITE);
        assertTrue(outContent.toString().contains("Current Player: WHITE"));

        System.setOut(System.out);
    }

    // ------------------------------------------------------------------------
    // SR2: Turn switching after a valid move
    // ------------------------------------------------------------------------
    @Test
    public void testTurnSwitching() throws Exception {
        Field currentPlayerField = QuaxController.class.getDeclaredField("currentPlayer");
        currentPlayerField.setAccessible(true);
        Method switchTurnMethod = QuaxController.class.getDeclaredMethod("switchTurn");
        switchTurnMethod.setAccessible(true);

        assertEquals(PlayerColour.BLACK, currentPlayerField.get(controller));

        switchTurnMethod.invoke(controller);
        assertEquals(PlayerColour.WHITE, currentPlayerField.get(controller));

        switchTurnMethod.invoke(controller);
        assertEquals(PlayerColour.BLACK, currentPlayerField.get(controller));
    }

    // ------------------------------------------------------------------------
    // SR5: Stone placement on empty cells, prevention of overlap
    // ------------------------------------------------------------------------
    @Test
    public void testStonePlacement() {
        String cell = "B5";

        // Place black stone
        assertTrue("First placement on empty should succeed (SR5)",
            board.placeStone(cell, PlayerColour.BLACK));
        OctagonalCell placed = board.getOctagonalCell(cell);
        assertEquals(PlayerColour.BLACK, placed.getColour());
        assertTrue(placed.isOccupied());

        // Attempt to place white stone on same cell
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

    // Helper to generate label
    private String generateLabel(int col, int row) {
        char colChar = (char) ('A' + col);
        return "" + colChar + row;
    }
}
