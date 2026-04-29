package src.softies;

import org.junit.jupiter.api.*;
import src.softies.board.GameState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the four Sprint 3 deliverables:
 *
 *   1. Pie Rule            (SR3, SR4)  - GameState.java & PieRuleWidget.java
 *   2. Win Detection       (SR6, SR7)  - WinCheck.java
 *   3. End-of-Game State   (SR6, SR7)  - GameState.java
 *   4. Rhombic Placement   (SR2, SR5)  - QuaxBoard.java
 *
 */
public class QuaxSprintThreeTest {

    // ==========================================================
    // SECTION 1 - PIE RULE  (SR3, SR4)
    // ==========================================================

    /**
     * SR3  : After the first move WHITE is allowed to switch colours.
     * SR3.1: A button will be displayed allowing pie rule activation.
     *          Tested MANUALLY (LibGDX widget - PieRuleWidget.java)
     * SR3.2: On activating Pie Rule, WHITE becomes BLACK and BLACK becomes WHITE.
     * SR4.1: After the Pie Rule button click, the button becomes invisible.
     *          Tested MANUALLY (PieRuleWidget.java visible-flag)
     * SR4.2: After activation the turn indicator reads "WHITE to play".
     *          Partially tested below (currentPlayer field); full display is MANUAL.
     */
    @Nested
    @DisplayName("Pie Rule Tests - GameState.java  (SR3, SR4)")
    class PieRuleTests {

        @Test
        @DisplayName("SR3: Pie rule is NOT available before any move is made")
            // GameState.isPieRuleAvailable() must return false on a fresh game.
        void testPieRuleUnavailableInitially() {
            GameState state = new GameState();
            assertFalse(state.isPieRuleAvailable(),
                "Pie rule must not be available before BLACK's opening move");
        }

        @Test
        @DisplayName("SR3: Pie rule becomes available immediately after the first move")
            // GameState.setFirstMoveMade() must open the pie-rule window.
        void testPieRuleAvailableAfterFirstMove() {
            GameState state = new GameState();
            state.setFirstMoveMade();
            assertTrue(state.isPieRuleAvailable(),
                "Pie rule must become available right after BLACK's first move");
        }

        @Test
        @DisplayName("SR3.2: activatePieRule() swaps Player 1 and Player 2 colour assignments")
            // GameState.activatePieRule() - colours must be exchanged.
        void testPieRuleSwapsColours() {
            GameState state = new GameState();
            // Initial assignments: Player1 = BLACK, Player2 = WHITE
            assertEquals(PlayerColour.BLACK, state.getPlayer1Colour());
            assertEquals(PlayerColour.WHITE, state.getPlayer2Colour());

            state.setFirstMoveMade();
            state.activatePieRule();

            // After swap: Player1 = WHITE, Player2 = BLACK
            assertEquals(PlayerColour.WHITE, state.getPlayer1Colour(),
                "Player 1 should be WHITE after pie rule activation");
            assertEquals(PlayerColour.BLACK, state.getPlayer2Colour(),
                "Player 2 should be BLACK after pie rule activation");
        }

        @Test
        @DisplayName("SR4.2: After pie rule activation the current player is WHITE")
            // GameState.activatePieRule() must set currentPlayer to WHITE.
        void testPieRuleCurrentPlayerIsWhiteAfterActivation() {
            GameState state = new GameState();
            state.setFirstMoveMade();
            state.activatePieRule();
            assertEquals(PlayerColour.WHITE, state.getCurrentPlayer(),
                "WHITE must play immediately after pie rule is activated");
        }

        @Test
        @DisplayName("SR4.1: Pie rule is disabled once it has been activated (button invisible)")
            // GameState.activatePieRule() must close the pie-rule window.
        void testPieRuleDisabledAfterActivation() {
            GameState state = new GameState();
            state.setFirstMoveMade();
            state.activatePieRule();
            assertFalse(state.isPieRuleAvailable(),
                "Pie rule must no longer be available after it has been used");
        }

        @Test
        @DisplayName("SR4: Pie rule window closes if WHITE makes a normal move instead")
            // GameState.togglePlayer() must close the pie-rule window when WHITE moves.
        void testPieRuleClosedWhenWhiteMakesNormalMove() {
            GameState state = new GameState();

            // step 1: BLACK's opening move opens the pie-rule window
            state.setFirstMoveMade();
            assertTrue(state.isPieRuleAvailable(),
                "Pie rule must be open after BLACK's first move");

            // step 2: end BLACK's turn - currentPlayer becomes WHITE, window still open
            state.togglePlayer();
            assertTrue(state.isPieRuleAvailable(),
                "Pie rule must still be available while it is WHITE's turn");
            assertEquals(PlayerColour.WHITE, state.getCurrentPlayer(),
                "It must be WHITE's turn at this point");

            // step 3: WHITE makes a normal move - window must now close
            state.togglePlayer();
            assertFalse(state.isPieRuleAvailable(),
                "Pie rule window must close when WHITE makes a normal move");
        }

        @Test
        @DisplayName("SR3: setFirstMoveMade() is idempotent - safe to call more than once")
            // Calling setFirstMoveMade() twice must not corrupt state
        void testFirstMoveMadeIsIdempotent() {
            GameState state = new GameState();
            state.setFirstMoveMade();
            state.setFirstMoveMade();       // second call should be a no-op
            assertTrue(state.isFirstMoveMade(),
                "firstMoveMade flag must remain true");
            assertTrue(state.isPieRuleAvailable(),
                "Pie rule availability must not be affected by duplicate calls");
        }
    }


    // ==========================================================
    // SECTION 2 - WIN DETECTION  (SR6, SR7)
    // ==========================================================

    /**
     * SR6: When BLACK forms a connected chain top→bottom, "BLACK wins" is shown.
     * SR7: When WHITE forms a connected chain left→right, "WHITE wins" is shown.
     *
     * Connectivity rules (from spec):
     *   - Horizontal/vertical adjacency always connects.
     *   - Diagonal adjacency connects only when a same-colour rhombus is present.
     */
    @Nested
    @DisplayName("Win Detection Tests - WinCheck.java  (SR6, SR7)")
    class WinDetectionTests {

        private QuaxBoard board;
        private WinCheck  winCheck;

        @BeforeEach
        void setUp() {
            board    = new QuaxBoard();
            winCheck = new WinCheck(board);
        }

        @Test
        @DisplayName("SR6 & SR7: No win is detected on an empty board")
            // WinCheck.checkWin() must return false for both colours at game start
        void testNoWinOnEmptyBoard() {
            assertFalse(winCheck.checkWin(PlayerColour.BLACK),
                "BLACK must not win on an empty board");
            assertFalse(winCheck.checkWin(PlayerColour.WHITE),
                "WHITE must not win on an empty board");
        }

        @Test
        @DisplayName("SR6: BLACK wins when a complete vertical column connects rows 1–11")
            // a straight chain down column A satisfies top-to-bottom connectivity.
        void testBlackWinsWithFullVerticalColumn() {
            for (int row = 1; row <= 11; row++) {
                board.placeStone("A" + row, PlayerColour.BLACK);
            }
            assertTrue(winCheck.checkWin(PlayerColour.BLACK),
                "BLACK must win with a complete column from row 1 to row 11");
        }

        @Test
        @DisplayName("SR7: WHITE wins when a complete horizontal row connects columns A–K")
            // A straigh chain across row 1 satisfies left-to-right connectivity
        void testWhiteWinsWithFullHorizontalRow() {
            String[] cols = {"A","B","C","D","E","F","G","H","I","J","K"};
            for (String col : cols) {
                board.placeStone(col + "1", PlayerColour.WHITE);
            }
            assertTrue(winCheck.checkWin(PlayerColour.WHITE),
                "WHITE must win with a complete row from column A to column K");
        }

        @Test
        @DisplayName("SR6: BLACK does NOT win with an incomplete vertical chain (rows 1–10 only)")
            // Chain must reach row 11. stopping at row 10 is insufficient
        void testBlackDoesNotWinIncompleteChain() {
            for (int row = 1; row <= 10; row++) {
                board.placeStone("A" + row, PlayerColour.BLACK);
            }
            assertFalse(winCheck.checkWin(PlayerColour.BLACK),
                "BLACK must not win when the chain does not reach row 11");
        }

        @Test
        @DisplayName("SR7: WHITE does NOT win with an incomplete horizontal chain (cols A–J only)")
            // Chain must reach column K. stopping at J is insufficient
        void testWhiteDoesNotWinIncompleteChain() {
            String[] cols = {"A","B","C","D","E","F","G","H","I","J"};
            for (String col : cols) {
                board.placeStone(col + "1", PlayerColour.WHITE);
            }
            assertFalse(winCheck.checkWin(PlayerColour.WHITE),
                "WHITE must not win when the chain does not reach column K");
        }

        @Test
        @DisplayName("SR6: BLACK wins via a rhombus diagonal connector in the chain")
            // path: A1 -> rhombus R-A2 (diagonal) -> B2 -> B3 -> … -> B11
            // R-A2 connects A1, B1, A2, B2 - giving BLACK a valid diagonal hop.
        void testBlackWinsViaRhombusConnector() {
            board.placeStone("A1", PlayerColour.BLACK);
            board.placeRhombus("R-A2", PlayerColour.BLACK); // diagonal bridge
            board.placeStone("B2", PlayerColour.BLACK);
            for (int row = 3; row <= 11; row++) {
                board.placeStone("B" + row, PlayerColour.BLACK);
            }
            assertTrue(winCheck.checkWin(PlayerColour.BLACK),
                "BLACK must win when a same-colour rhombus bridges the diagonal gap");
        }

        @Test
        @DisplayName("SR6: A rhombus connector of the WRONG colour does NOT complete the chain")
            // an opponent's rhombus between two BLACK stones must NOT connect them
        void testRhombusWrongColourDoesNotBridge() {
            board.placeStone("A1", PlayerColour.BLACK);
            board.placeRhombus("R-A2", PlayerColour.WHITE); // WHITE rhombus - must not help BLACK
            board.placeStone("B2", PlayerColour.BLACK);
            for (int row = 3; row <= 11; row++) {
                board.placeStone("B" + row, PlayerColour.BLACK);
            }
            // Without a BLACK rhombus at R-A2 the diagonal hop is not connected
            assertFalse(winCheck.checkWin(PlayerColour.BLACK),
                "An opponent's rhombus must not bridge a diagonal gap for BLACK");
        }

        @Test
        @DisplayName("SR6 & SR7: Mixed partial stones on the board - no false win for either player")
            // Neither player has completed their crossing; no winner must be reported.
        void testNoFalseWinWithPartialStones() {
            board.placeStone("A1",  PlayerColour.BLACK);
            board.placeStone("A6",  PlayerColour.BLACK);
            board.placeStone("K1",  PlayerColour.WHITE);
            board.placeStone("K6",  PlayerColour.WHITE);
            assertFalse(winCheck.checkWin(PlayerColour.BLACK),
                "BLACK must not win with a disconnected partial chain");
            assertFalse(winCheck.checkWin(PlayerColour.WHITE),
                "WHITE must not win with a disconnected partial chain");
        }

        @Test
        @DisplayName("SR6: WHITE stones filling BLACK's winning column do NOT give BLACK a win")
            // opponent stones must never count towards the other player's win
        void testOpponentStonesDoNotContributeToWin() {
            for (int row = 1; row <= 11; row++) {
                board.placeStone("A" + row, PlayerColour.WHITE);
            }
            assertFalse(winCheck.checkWin(PlayerColour.BLACK),
                "BLACK must not win from a column occupied entirely by WHITE");
        }
    }


    // ==========================================================
    // SECTION 3 - END-OF-GAME STATE  (SR6, SR7)
    // ==========================================================

    /**
     * SR6: After BLACK wins all further board interaction must be disabled.
     * SR7: After WHITE wins all further board interaction must be disabled.
     *
     * InputHandler.handleBoardClick() checks gameState.isGameOver() at the
     * very top and returns NOT_A_CELL immediately if true.  The tests below
     * verify the model state that drives this guard.
     */
    @Nested
    @DisplayName("End-of-Game State Tests - GameState.java  (SR6, SR7)")
    class EndOfGameTests {

        @Test
        @DisplayName("SR6 & SR7: Game is not over at the start; winner is null")
            // gameState.isGameOver() must be false and getWinner() must be null initially.
        void testGameNotOverInitially() {
            GameState state = new GameState();
            assertFalse(state.isGameOver(),
                "isGameOver() must return false before any winner is set");
            assertNull(state.getWinner(),
                "getWinner() must return null before any winner is set");
        }

        @Test
        @DisplayName("SR6: setWinner(BLACK) makes isGameOver() return true and records BLACK")
            // GameState.setWinner() must freeze the game for a BLACK victory
        void testSetWinnerBlack() {
            GameState state = new GameState();
            state.setWinner(PlayerColour.BLACK);
            assertTrue(state.isGameOver(),
                "isGameOver() must return true after BLACK's win is recorded");
            assertEquals(PlayerColour.BLACK, state.getWinner(),
                "getWinner() must return BLACK");
        }

        @Test
        @DisplayName("SR7: setWinner(WHITE) makes isGameOver() return true and records WHITE")
            // GameState.setWinner() must freeze the game for a WHITE victory.
        void testSetWinnerWhite() {
            GameState state = new GameState();
            state.setWinner(PlayerColour.WHITE);
            assertTrue(state.isGameOver(),
                "isGameOver() must return true after WHITE's win is recorded");
            assertEquals(PlayerColour.WHITE, state.getWinner(),
                "getWinner() must return WHITE");
        }

        @Test
        @DisplayName("SR6 & SR7: isGameOver() returns false when winner is null")
            // isGameOver() is purely derived from whether winner != null.
        void testIsGameOverFalseWithNullWinner() {
            GameState state = new GameState();
            assertFalse(state.isGameOver(),
                "isGameOver() must be false while no winner has been set");
        }

        @Test
        @DisplayName("SR6 & SR7: Board interaction guard - isGameOver() true blocks moves in InputHandler")
            // InputHandler.handleBoardClick() guards on gameState.isGameOver().
        void testGameOverFlagPreventsMovesViaInputHandlerGuard() {
            GameState state = new GameState();
            assertFalse(state.isGameOver()); // guard not triggered yet

            state.setWinner(PlayerColour.BLACK);
            assertTrue(state.isGameOver(),
                "isGameOver() must be true so InputHandler returns NOT_A_CELL " +
                    "and no further board moves are processed");
        }

        @Test
        @DisplayName("SR6 & SR7: QuaxBoard still holds correct state after game is frozen")
            // Freezing via GameState must not corrupt the board model.
        void testBoardModelIntactAfterGameOver() {
            QuaxBoard board = new QuaxBoard();
            board.placeStone("A1", PlayerColour.BLACK);

            GameState state = new GameState();
            state.setWinner(PlayerColour.BLACK);

            // The stone placed before the freeze must still be retrievable
            assertEquals(PlayerColour.BLACK,
                board.getOctagonalCell("A1").getColour(),
                "Board model must remain intact after the game is frozen");
        }
    }


    // ==========================================================
    // SECTION 4 - RHOMBIC CONNECTOR PLACEMENT  (SR2, SR5)
    // ==========================================================

    /**
     * SR2 : Players can place a rhombic connector tile on their turn.
     * SR5 : Placement must be on an unoccupied valid rhombic cell.
     *
     * InputHandler validates the click coordinates and derives the rhombus
     * key before calling QuaxBoard.placeRhombus().
     */
    @Nested
    @DisplayName("Rhombic Connector Placement Tests - QuaxBoard.java  (SR2, SR5)")
    class RhombicPlacementTests {

        private QuaxBoard board;

        @BeforeEach
        void setUp() {
            board = new QuaxBoard();
        }

        @Test
        @DisplayName("SR2/SR5: Valid rhombus placement returns true and marks the cell occupied")
            // QuaxBoard.placeRhombus() happy path - cell exists and is empty.
        void testValidRhombusPlacementSucceeds() {
            assertTrue(board.placeRhombus("R-B2", PlayerColour.BLACK),
                "placeRhombus() must return true for a valid empty rhombus cell");

            RhombicCell cell = board.getRhombicCell("R-B2");
            assertNotNull(cell);
            assertTrue(cell.isOccupied(),
                "The rhombus cell must be marked as occupied after placement");
            assertEquals(PlayerColour.BLACK, cell.getColour(),
                "The rhombus cell must carry the placing player's colour");
        }

        @Test
        @DisplayName("SR5: Placing a rhombus on an already-occupied cell is rejected")
            // QuaxBoard.placeRhombus() must refuse double-placement and leave the original owner.
        void testRhombusPlacementOnOccupiedCellIsRejected() {
            board.placeRhombus("R-C3", PlayerColour.BLACK);
            assertFalse(board.placeRhombus("R-C3", PlayerColour.WHITE),
                "placeRhombus() must return false when the cell is already occupied");
            // Original owner must not be overwritten
            assertEquals(PlayerColour.BLACK, board.getRhombicCell("R-C3").getColour(),
                "The original colour must remain unchanged after a rejected placement");
        }

        @Test
        @DisplayName("SR5: Placement with a non-existent rhombus key is rejected")
            // QuaxBoard.placeRhombus() must return false for unknown keys (feedback for invalid attempts).
        void testRhombusPlacementWithInvalidKeyIsRejected() {
            assertFalse(board.placeRhombus("R-Z99", PlayerColour.BLACK),
                "placeRhombus() must return false for a key that does not exist on the board");
        }

        @Test
        @DisplayName("SR2/SR5: WHITE can also place a rhombus connector tile")
            // placeRhombus() must work for PlayerColour.WHITE, not just BLACK.
        void testWhiteRhombusPlacementSucceeds() {
            assertTrue(board.placeRhombus("R-D5", PlayerColour.WHITE));
            assertEquals(PlayerColour.WHITE, board.getRhombicCell("R-D5").getColour(),
                "The rhombus cell must carry WHITE's colour after placement");
        }

        @Test
        @DisplayName("SR5: Rhombus cells do NOT exist on the bottom edge (row 1)")
            // Board initialisation: rhombus cells only exist where row > 1.
        void testRhombusCellAbsentOnBottomEdge() {
            assertNull(board.getRhombicCell("R-A1"),
                "R-A1 must not exist - rhombus cells are not created for row 1");
            assertNull(board.getRhombicCell("R-K1"),
                "R-K1 must not exist - rhombus cells are not created for row 1");
        }

        @Test
        @DisplayName("SR5: Rhombus cells do NOT exist on the rightmost column (col K, index 10)")
            // Board initialisation: rhombus cells only exist where col < BOARD_SIZE - 1.
        void testRhombusCellAbsentOnRightEdge() {
            assertNull(board.getRhombicCell("R-K5"),
                "R-K5 must not exist - rhombus cells are not created for the last column");
            assertNull(board.getRhombicCell("R-K11"),
                "R-K11 must not exist - rhombus cells are not created for the last column");
        }

        @Test
        @DisplayName("SR5: Rhombus cells exist at the minimum valid position (row 2, col A)")
            // The first valid rhombus cell on column A is R-A2.
        void testRhombusCellExistsAtMinimumValidPosition() {
            assertNotNull(board.getRhombicCell("R-A2"),
                "R-A2 must exist - it is the first valid rhombus cell in column A");
        }

        @Test
        @DisplayName("SR5: Rhombus cells exist throughout the interior of the board")
            // Spot-check several interior positions to confirm board initialisation.
        void testRhombusCellsExistInMiddleBoard() {
            assertNotNull(board.getRhombicCell("R-F6"),
                "R-F6 must exist at a centre-board position");
            assertNotNull(board.getRhombicCell("R-J11"),
                "R-J11 must exist at the top-right valid position");
        }

        @Test
        @DisplayName("SR5: Null key is safely rejected without throwing an exception")
            // placeRhombus() must not throw for null or malformed input.
        void testRhombusPlacementNullKeyIsRejected() {
            assertFalse(board.placeRhombus(null, PlayerColour.BLACK),
                "placeRhombus() must return false (not throw) when passed a null key");
        }
    }
}
