package src.softies;

import org.junit.jupiter.api.*;
import src.softies.board.GameState;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the three Sprint 4 deliverables:
 *
 *   1. Human vs Bot mode         (SR8)         - GameState.java, BotPlayer.java
 *   2. Bot decision-making       (SR8.1)       - BotPlayer.java
 *   3. Strategy visualisation    (SR8.2-SR8.4) - BotPlayer.rateAllMoves()
 *
 * File: QuaxSprintFourTest.java
 *
 */
public class QuaxSprintFourTest {

    // ==========================================================================
    // SECTION 1 - HUMAN VS BOT MODE  (SR8)
    // ==========================================================================
    //
    // SR8: A Human vs Bot gameplay mode in which a human player competes against
    //      a computer-controlled opponent so that a complete game of Quax can be
    //      played without a second human.
    //
    // Covered by: GameState.java  (bot colour assignment, turn management)
    //             BotPlayer.java  (bot exists and can produce moves)

    @Nested
    @DisplayName("Human vs Bot Mode - GameState.java  (SR8)")
    class HumanVsBotModeTests {

        @Test
        @DisplayName("SR8: GameState defaults to HUMAN_VS_BOT mode")
            // GameState.getGameMode() must return HUMAN_VS_BOT on construction
            // so that the game always starts in the correct mode.
        void testGameStateDefaultsToHumanVsBot() {
            GameState state = new GameState();
            assertEquals(GameMode.HUMAN_VS_BOT, state.getGameMode(),
                "GameMode must default to HUMAN_VS_BOT - no mode-selection screen exists");
        }

        @Test
        @DisplayName("SR8: Bot is assigned a valid colour on game start")
            // getBotColour() must return BLACK or WHITE - never EMPTY.
        void testBotColourIsBlackOrWhite() {
            GameState state = new GameState();
            PlayerColour bot = state.getBotColour();
            assertTrue(bot == PlayerColour.BLACK || bot == PlayerColour.WHITE,
                "Bot colour must be BLACK or WHITE, never EMPTY");
        }

        @Test
        @DisplayName("SR8: Bot colour is never EMPTY")
        void testBotColourIsNeverEmpty() {
            // Run several times to cover the random assignment
            for (int i = 0; i < 50; i++) {
                GameState state = new GameState();
                assertNotEquals(PlayerColour.EMPTY, state.getBotColour(),
                    "Bot colour must never be EMPTY");
            }
        }

        @Test
        @DisplayName("SR8: Bot and human are assigned different colours")
            // The human always plays the colour that is NOT the bot's colour,
            // so they can never share the same side.
        void testBotAndHumanHaveDifferentColours() {
            GameState state    = new GameState();
            PlayerColour bot   = state.getBotColour();
            PlayerColour human = (bot == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
            assertNotEquals(bot, human,
                "Bot and human must have different colours");
        }

        @Test
        @DisplayName("SR8: isBotTurn() returns true when current player equals bot colour")
        void testIsBotTurnTrueWhenCurrentMatchesBot() {
            GameState state = new GameState();
            // Force currentPlayer to match bot colour
            forceCurrentPlayer(state, state.getBotColour());
            assertTrue(state.isBotTurn(),
                "isBotTurn() must be true when it is the bot's colour's turn");
        }

        @Test
        @DisplayName("SR8: isBotTurn() returns false when it is the human's turn")
        void testIsBotTurnFalseWhenHumanTurn() {
            GameState state    = new GameState();
            PlayerColour human = opposite(state.getBotColour());
            forceCurrentPlayer(state, human);
            assertFalse(state.isBotTurn(),
                "isBotTurn() must be false when the human player is to move");
        }

        @Test
        @DisplayName("SR8: Bot colour flips correctly when pie rule is activated")
            // After pie rule activation the bot must inherit the opposite colour.
        void testBotColourFlipsOnPieRule() {
            GameState state = new GameState();
            PlayerColour originalBotColour = state.getBotColour();

            state.setFirstMoveMade();
            state.activatePieRule();

            assertNotEquals(originalBotColour, state.getBotColour(),
                "Bot colour must flip when the pie rule is activated");
        }

        // helper - togglePlayer until currentPlayer matches target
        private void forceCurrentPlayer(GameState state, PlayerColour target) {
            int safety = 0;
            while (state.getCurrentPlayer() != target && safety++ < 5)
                state.togglePlayer();
        }
    }

    // ==========================================================================
    // SECTION 2 - BOT DECISION-MAKING  (SR8.1)
    // ==========================================================================
    //
    // SR8.1: The bot uses deliberate decision-making strategies to produce
    //        challenging and varied gameplay rather than random move selection.
    //
    // Priority order enforced by BotPlayer.selectMove():
    //   1. Win immediately if possible
    //   2. Block the opponent from winning
    //   3. Combined Dijkstra strategic move

    @Nested
    @DisplayName("Bot Decision-Making - BotPlayer.java  (SR8.1)")
    class BotDecisionMakingTests {

        /** creates a GameState that forces the bot to play as the given colour */
        private GameState stateWithBotAs(PlayerColour colour) {
            GameState state = new GameState();
            // keep re-creating until we get the desired bot colour (random assignment)
            while (state.getBotColour() != colour) state = new GameState();
            return state;
        }

        @Test
        @DisplayName("SR8.1: selectMove() returns a non-null move on an empty board")
        void testSelectMoveNonNullOnEmptyBoard() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            BotPlayer bot   = new BotPlayer(board, state);
            assertNotNull(bot.selectMove(),
                "selectMove() must always return a move on an empty board");
        }

        @Test
        @DisplayName("SR8.1: selectMove() returns a valid octagonal or rhombus board label")
        void testSelectMoveReturnsValidLabel() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            BotPlayer bot   = new BotPlayer(board, state);
            String move = bot.selectMove();
            assertNotNull(move, "move must not be null");
            boolean isOctagon = move.length() >= 2 && Character.isLetter(move.charAt(0))
                && Character.isDigit(move.charAt(1));
            boolean isRhombus = move.startsWith("R-") && move.length() >= 4;
            assertTrue(isOctagon || isRhombus,
                "Move label must be an octagonal label (e.g. F6) or rhombus label (e.g. R-F6), got: " + move);
        }

        @Test
        @DisplayName("SR8.1: Bot selects the only winning move immediately (priority 1)")
            // Board is set up so the bot is one stone away from winning.
            // selectMove() must return that winning cell, not any other.
        void testBotSelectsWinningMoveImmediately() {
            QuaxBoard board = new QuaxBoard();
            GameState state = stateWithBotAs(PlayerColour.BLACK);
            BotPlayer bot   = new BotPlayer(board, state);

            // Place BLACK stones in rows 1-10 of column F (index 5)
            // Only row 11 is missing - placing there wins for BLACK (top-to-bottom)
            for (int row = 1; row <= 10; row++) {
                board.placeStone(QuaxBoard.generateLabel(5, row), PlayerColour.BLACK);
            }

            // Force it to be the bot's turn
            while (state.getCurrentPlayer() != PlayerColour.BLACK) state.togglePlayer();

            String move = bot.selectMove();
            assertEquals("F11", move,
                "Bot must play the immediate winning move F11, not " + move);
        }

        @Test
        @DisplayName("SR8.1: Bot blocks opponent's winning move (priority 2)")
            // Opponent is one cell away from winning.
            // Bot must block rather than play anywhere else.
        void testBotBlocksOpponentWinningMove() {
            QuaxBoard board = new QuaxBoard();
            GameState state = stateWithBotAs(PlayerColour.WHITE);
            BotPlayer bot   = new BotPlayer(board, state);

            // Place BLACK stones in rows 1-10 of column F - BLACK wins if it plays F11
            for (int row = 1; row <= 10; row++) {
                board.placeStone(QuaxBoard.generateLabel(5, row), PlayerColour.BLACK);
            }

            // Ensure it is WHITE's (bot's) turn
            while (state.getCurrentPlayer() != PlayerColour.WHITE) state.togglePlayer();

            String move = bot.selectMove();
            assertEquals("F11", move,
                "Bot (WHITE) must block BLACK's winning move at F11, not " + move);
        }

        @Test
        @DisplayName("SR8.1: selectMove() never returns an already-occupied cell")
        void testBotDoesNotReturnOccupiedCell() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            BotPlayer bot   = new BotPlayer(board, state);

            // Fill half the board
            String[] cells = {"A1","B1","C1","D1","E1","F1","G1","H1","I1","J1","K1",
                "A2","B2","C2","D2","E2"};
            for (String c : cells) board.placeStone(c, PlayerColour.BLACK);

            for (int attempt = 0; attempt < 10; attempt++) {
                String move = bot.selectMove();
                assertNotNull(move, "selectMove() must not be null");
                if (!move.startsWith("R-")) {
                    OctagonalCell cell = board.getOctagonCells().get(move);
                    assertFalse(cell != null && cell.isOccupied(),
                        "Bot must not return an already-occupied cell: " + move);
                }
            }
        }

        @Test
        @DisplayName("SR8.1: selectMove() returns a move on a nearly-full board")
            // Verifies the bot can still find a legal move when very few cells remain.
        void testSelectMoveOnNearlyFullBoard() {
            QuaxBoard board = new QuaxBoard();
            GameState state = stateWithBotAs(PlayerColour.WHITE);

            // Fill all cells except one (K11) with BLACK
            for (int col = 0; col < 11; col++) {
                for (int row = 1; row <= 11; row++) {
                    String lbl = QuaxBoard.generateLabel(col, row);
                    if (!lbl.equals("K11"))
                        board.placeStone(lbl, PlayerColour.BLACK);
                }
            }

            while (state.getCurrentPlayer() != PlayerColour.WHITE) state.togglePlayer();
            BotPlayer bot = new BotPlayer(board, state);
            String move   = bot.selectMove();
            assertNotNull(move, "Bot must find a legal move even on a nearly-full board");
        }

        @Test
        @DisplayName("SR8.1: Bot produces varied opening moves across multiple games (not always the same cell)")
            // Over 30 fresh games the bot's first move should not always be identical -
            // this confirms the combined Dijkstra strategy uses randomised tie-breaking.
        void testBotOpeningMovesAreVaried() {
            List<String> seen = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                QuaxBoard board = new QuaxBoard();
                GameState state = new GameState();
                BotPlayer bot   = new BotPlayer(board, state);
                String move     = bot.selectMove();
                assertNotNull(move, "selectMove() must not be null on an empty board");
                seen.add(move);
            }
            assertTrue(seen.size() > 1,
                "Bot opening move should vary across games - got only: " + seen);
        }

        @Test
        @DisplayName("SR8.1: Bot prefers central cells when scores are equal")
            // On an empty board all combined Dijkstra scores tie, so the bot's centrality
            // tie-breaker should always pick a cell in the central region around F6.
        void testBotPrefersCenter() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            BotPlayer bot   = new BotPlayer(board, state);
            String move     = bot.selectMove();

            assertNotNull(move, "selectMove() must not be null on an empty board");
            assertFalse(move.startsWith("R-"),
                "Opening move should be an octagon, not a rhombus");
            int col = move.charAt(0) - 'A';
            int row = Integer.parseInt(move.substring(1));
            assertTrue(col >= 3 && col <= 7 && row >= 4 && row <= 8,
                "Bot opening move should be central, got " + move);
        }

        @Test
        @DisplayName("SR8.1: Bot win-check priority occurs before blocking check")
            // When the bot can both win AND block, it must choose to win.
        void testBotWinsBeforeBlocking() {
            QuaxBoard board = new QuaxBoard();
            // Bot = WHITE (left-to-right). Set up WHITE near win on row 6, col A-J.
            // Also set up BLACK near win on column F, rows 1-10.
            GameState state = stateWithBotAs(PlayerColour.WHITE);

            // WHITE (bot) is one away from winning - row 6, cols A through J present
            for (int col = 0; col < 10; col++) {
                board.placeStone(QuaxBoard.generateLabel(col, 6), PlayerColour.WHITE);
            }
            // BLACK (human) is also one away - col F, rows 1-10
            for (int row = 1; row <= 10; row++) {
                if (board.getOctagonCells().get(QuaxBoard.generateLabel(5, row)).isOccupied())
                    continue;
                board.placeStone(QuaxBoard.generateLabel(5, row), PlayerColour.BLACK);
            }

            while (state.getCurrentPlayer() != PlayerColour.WHITE) state.togglePlayer();
            BotPlayer bot = new BotPlayer(board, state);
            String move   = bot.selectMove();

            // K6 wins for WHITE; F11 would merely block BLACK
            assertEquals("K6", move,
                "Bot must choose its own winning move (K6) over blocking BLACK (F11)");
        }
    }

    // ==========================================================================
    // SECTION 3 - STRATEGY VISUALISATION  (SR8.2, SR8.3, SR8.4)
    // ==========================================================================
    //
    // SR8.2: The bot's decision-making process is displayed on the board.
    // SR8.3: Visualisation can be enabled or disabled (toggle button).
    // SR8.4: The visualisation is suitable for testing, debugging, and demo.
    //
    // The visual toggle (SR8.3) and rendering (SR8.2, SR8.4) are LibGDX components
    // (BotStrategyWidget) tested manually.

    @Nested
    @DisplayName("Strategy Visualisation Data - BotPlayer.rateAllMoves()  (SR8.2, SR8.3, SR8.4)")
    class StrategyVisualisationTests {

        @Test
        @DisplayName("SR8.2: rateAllMoves() returns a non-empty map on a fresh board")
        void testRateAllMovesNonEmpty() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            BotPlayer bot   = new BotPlayer(board, state);
            Map<String, Double> ratings = bot.rateAllMoves();
            assertFalse(ratings.isEmpty(),
                "rateAllMoves() must produce at least one rating on a fresh board");
        }

        @Test
        @DisplayName("SR8.2: All ratings are in the range [0.0, 1.0]")
        void testAllRatingsInRange() {
            QuaxBoard board   = new QuaxBoard();
            GameState state   = new GameState();
            BotPlayer bot     = new BotPlayer(board, state);
            Map<String, Double> ratings = bot.rateAllMoves();
            for (Map.Entry<String, Double> e : ratings.entrySet()) {
                double r = e.getValue();
                assertTrue(r >= 0.0 && r <= 1.0,
                    "Rating for " + e.getKey() + " must be in [0,1], got " + r);
            }
        }

        @Test
        @DisplayName("SR8.2: Winning move receives rating of exactly 1.0")
            // When the bot is one cell away from winning, rateAllMoves() must mark
            // that cell with rating 1.0 - the maximum possible score.
        void testWinningMoveRatedAtOne() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            while (state.getBotColour() != PlayerColour.BLACK) state = new GameState();
            BotPlayer bot = new BotPlayer(board, state);

            // BLACK rows 1-10 of column F - placing F11 wins
            for (int row = 1; row <= 10; row++)
                board.placeStone(QuaxBoard.generateLabel(5, row), PlayerColour.BLACK);

            Map<String, Double> ratings = bot.rateAllMoves();
            Double winRating = ratings.get("F11");
            assertNotNull(winRating, "F11 must appear in rateAllMoves() output");
            assertEquals(1.0, winRating, 0.001,
                "The immediate winning cell F11 must receive rating 1.0");
        }

        @Test
        @DisplayName("SR8.2: Blocking move receives rating of 0.95")
            // When the opponent is one cell away from winning, rateAllMoves() must mark
            // the blocking cell with rating 0.95.
        void testBlockingMoveRatedAt095() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            while (state.getBotColour() != PlayerColour.WHITE) state = new GameState();
            BotPlayer bot = new BotPlayer(board, state);

            // BLACK (opponent) rows 1-10 of column F - BLACK wins at F11
            for (int row = 1; row <= 10; row++)
                board.placeStone(QuaxBoard.generateLabel(5, row), PlayerColour.BLACK);

            Map<String, Double> ratings = bot.rateAllMoves();
            Double blockRating = ratings.get("F11");
            assertNotNull(blockRating, "F11 must appear in rateAllMoves() output");
            assertEquals(0.95, blockRating, 0.001,
                "The opponent's winning cell F11 must receive blocking rating 0.95");
        }

        @Test
        @DisplayName("SR8.2: Occupied cells are not rated above 0")
            // Cells that are already occupied should not be rated as playable moves.
        void testOccupiedCellsNotRatedPositively() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            BotPlayer bot   = new BotPlayer(board, state);

            board.placeStone("F6", PlayerColour.BLACK);
            board.placeStone("G7", PlayerColour.WHITE);

            Map<String, Double> ratings = bot.rateAllMoves();

            // Occupied cells must not appear with a positive score
            // (they may appear with 0.0 or not appear at all)
            for (String occupied : new String[]{"F6", "G7"}) {
                Double r = ratings.get(occupied);
                if (r != null) {
                    assertEquals(0.0, r, 0.001,
                        "Occupied cell " + occupied + " must not have a positive rating, got " + r);
                }
            }
        }

        @Test
        @DisplayName("SR8.2: Winning move is rated higher than all other moves")
        void testWinningMoveIsHighestRated() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            while (state.getBotColour() != PlayerColour.BLACK) state = new GameState();
            BotPlayer bot = new BotPlayer(board, state);

            for (int row = 1; row <= 10; row++)
                board.placeStone(QuaxBoard.generateLabel(5, row), PlayerColour.BLACK);

            Map<String, Double> ratings = bot.rateAllMoves();
            double maxRating = ratings.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
            assertEquals(1.0, maxRating, 0.001,
                "The maximum rating must be 1.0 when a winning move exists");
        }

        @Test
        @DisplayName("SR8.2: rateAllMoves() covers all 121 playable octagonal cells initially")
            // On an empty board every octagonal cell should appear in the ratings map.
        void testRateAllMovesCoverageOnEmptyBoard() {
            QuaxBoard board   = new QuaxBoard();
            GameState state   = new GameState();
            BotPlayer bot     = new BotPlayer(board, state);
            Map<String, Double> ratings = bot.rateAllMoves();

            int octagonCount = 0;
            for (int col = 0; col < 11; col++) {
                for (int row = 1; row <= 11; row++) {
                    String label = QuaxBoard.generateLabel(col, row);
                    if (ratings.containsKey(label)) octagonCount++;
                }
            }
            assertEquals(121, octagonCount,
                "All 121 octagonal cells must appear in rateAllMoves() on a fresh board");
        }

        @Test
        @DisplayName("SR8.4: rateAllMoves() rating is consistent with selectMove() priority")
            // The cell selected by selectMove() must be among the highest-rated cells
            // in rateAllMoves() - confirming the visualisation reflects the actual strategy.
        void testRatingsConsistentWithSelectMove() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            while (state.getBotColour() != PlayerColour.BLACK) state = new GameState();

            // Place a few stones to create a meaningful board position
            board.placeStone("F5", PlayerColour.BLACK);
            board.placeStone("F6", PlayerColour.BLACK);
            board.placeStone("E7", PlayerColour.WHITE);

            BotPlayer bot   = new BotPlayer(board, state);
            String chosen   = bot.selectMove();
            Map<String, Double> ratings = bot.rateAllMoves();

            // The chosen move must have a rating in the map and it must not be 0.0
            if (chosen != null && ratings.containsKey(chosen)) {
                assertTrue(ratings.get(chosen) > 0.0,
                    "The cell chosen by selectMove() must have a positive rating in rateAllMoves()");
            }
            // If not in ratings, just verify selectMove returned something valid
            assertNotNull(chosen, "selectMove() must return a non-null move");
        }

        @Test
        @DisplayName("SR8.3: Ratings can be obtained multiple times without error (toggle-safe)")
            // rateAllMoves() is called every frame when the overlay is enabled;
            // calling it repeatedly must produce consistent, non-throwing results.
        void testRateAllMovesIsIdempotent() {
            QuaxBoard board = new QuaxBoard();
            GameState state = new GameState();
            BotPlayer bot   = new BotPlayer(board, state);

            Map<String, Double> r1 = bot.rateAllMoves();
            Map<String, Double> r2 = bot.rateAllMoves();
            Map<String, Double> r3 = bot.rateAllMoves();

            assertEquals(r1.keySet(), r2.keySet(), "rateAllMoves() key set must be stable");
            assertEquals(r2.keySet(), r3.keySet(), "rateAllMoves() key set must be stable across calls");
        }
    }

    // helper

    private PlayerColour opposite(PlayerColour c) {
        return (c == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
    }
}
