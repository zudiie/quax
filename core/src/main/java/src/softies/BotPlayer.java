package src.softies;

import src.softies.board.GameState;

import java.util.*;

// computer-controlled player for Human vs Bot mode
//
// MOVE SELECTION - evaluated in priority order every turn:
//
//   1. Win immediately  - test every empty octagon AND legal rhombus via temporary
//                         placement + WinCheck.  If any cell wins, play it.
//
//   2. Block opponent   - same scan for the opponent's colour.
//
//   3. Combined strategic move - run Dijkstra from both edges for BOTH players,
//                         then score every candidate cell as:
//
//                           combined = botCost + oppCost   (minimise)
//
//                         where botCost  = distStartBot[cell] + distEndBot[cell]
//                               oppCost  = distStartOpp[cell] + distEndOpp[cell]
//
//                         A cell where BOTH scores are low sits on both players'
//                         optimal crossing paths - the most contested spot on the
//                         board and therefore the best move.  This means the bot
//                         proactively blocks the opponent throughout the game, not
//                         only when they are one step from winning.
//
//                         Rhombuses are scored the same way and compete on equal
//                         terms with octagons - they are only chosen when they are
//                         genuinely on the optimal combined path, not reflexively
//                         whenever a legal placement exists.
//
// DIJKSTRA GRAPH mirrors WinCheck connectivity exactly:
//   octagon  → 4 orthogonal octagons
//   octagon  → up to 4 adjacent rhombus cells (shared corner)
//   rhombus  → 4 corner octagonal cells
//
// Cell traversal costs:
//   own-colour cell : 0  (already in chain, free to pass through)
//   empty cell      : 1  (needs to be claimed)
//   opponent cell   : ∞  (impassable - never enqueued)
public class BotPlayer {

    private static final int BOARD_SIZE = 11;

    private final QuaxBoard board;
    private final GameState gameState;
    private final WinCheck  winCheck;
    private final Random    rng = new Random();

    // sentinel for "unreachable" - larger than any valid path cost (max = 121)
    private static final int INF = Integer.MAX_VALUE / 2;

    // number of top-scoring candidates to consider for random tie-breaking
    // set to 1 for deterministic best play; higher values add variety
    private static final int RANDOM_POOL_SIZE = 3;

    public BotPlayer(QuaxBoard board, GameState gameState) {
        this.board     = board;
        this.gameState = gameState;
        this.winCheck  = new WinCheck(board);
    }

    // -------------------------------------------------------------------------
    // public API
    // -------------------------------------------------------------------------

    /**
     * returns the best available move label ("F6" for octagon, "R-F6" for rhombus)
     * returns null only if the board is completely full (should never occur in practice)
     */
    public String selectMove() {
        PlayerColour bot      = gameState.getBotColour();
        PlayerColour opponent = opposite(bot);

        // priority 1 - win if possible
        String win = findImmediateMove(bot);
        if (win != null) { System.out.println("Bot wins at: " + win); return win; }

        // priority 2 - block opponent's winning move
        String block = findImmediateMove(opponent);
        if (block != null) { System.out.println("Bot blocks at: " + block); return block; }

        // priority 3 - combined strategic move
        String move = findBestCombinedMove(bot, opponent);
        System.out.println("Bot combined move: " + move);
        return move;
    }

    /**
     * rates every non-occupied cell in [0,1] for visualisation (BotStrategyWidget)
     * scores mirror the combined strategy used by selectMove() so the heat map
     * accurately reflects what the bot considers important
     *
     * rating bands:
     *   winning move          → 1.00
     *   blocks opponent win   → 0.95
     *   other candidates      → 0.10 – 0.90  (based on combined Dijkstra score)
     *   unreachable / illegal → 0.00
     */
    public Map<String, Double> rateAllMoves() {
        PlayerColour bot      = gameState.getBotColour();
        PlayerColour opponent = opposite(bot);

        Map<String, Double> ratings = new HashMap<>();

        // highest band: immediate win / block
        Set<String> winMoves   = collectImmediateMoves(bot);
        Set<String> blockMoves = collectImmediateMoves(opponent);
        blockMoves.removeAll(winMoves);
        winMoves.forEach(k  -> ratings.put(k, 1.00));
        blockMoves.forEach(k -> ratings.put(k, 0.95));

        // combined Dijkstra scoring for everything else
        Map<String, Integer> dsBot = dijkstraFull(bot,      opponent, true);
        Map<String, Integer> deBot = dijkstraFull(bot,      opponent, false);
        Map<String, Integer> dsOpp = dijkstraFull(opponent, bot,      true);
        Map<String, Integer> deOpp = dijkstraFull(opponent, bot,      false);

        Map<String, Integer> rawScores = new LinkedHashMap<>();

        // score empty octagonal cells
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            String key = e.getKey();
            if (ratings.containsKey(key) || e.getValue().isOccupied()
                || e.getValue().getColour() == opponent) continue;

            int botCost = safeAdd(dsBot.getOrDefault(key, INF), deBot.getOrDefault(key, INF));
            int oppCost = safeAdd(dsOpp.getOrDefault(key, INF), deOpp.getOrDefault(key, INF));
            if (botCost >= INF) { ratings.put(key, 0.0); continue; }
            rawScores.put(key, safeAdd(botCost, oppCost < INF ? oppCost : 0));
        }

        // score legal rhombic cells (same formula, competing fairly with octagons)
        for (Map.Entry<String, RhombicCell> e : board.getRhombusCells().entrySet()) {
            String key = e.getKey();
            if (ratings.containsKey(key) || e.getValue().isOccupied()) continue;

            int[] rhombusCoords = parseRhombusCoords(key);
            if (rhombusCoords == null) continue;

            // illegal rhombus (bot doesn't own the required diagonal pair) → 0
            if (!isRhombusLegal(rhombusCoords[0], rhombusCoords[1], bot)) { ratings.put(key, 0.0); continue; }

            int botCost = rhombusPathScore(dsBot, deBot, rhombusCoords[0], rhombusCoords[1]);
            int oppCost = rhombusPathScore(dsOpp, deOpp, rhombusCoords[0], rhombusCoords[1]);
            if (botCost >= INF) { ratings.put(key, 0.0); continue; }
            rawScores.put(key, safeAdd(botCost, oppCost < INF ? oppCost : 0));
        }

        // normalise raw combined scores to the [0.10, 0.90] band
        applyBand(ratings, rawScores, 0.10, 0.90);

        return ratings;
    }

    // -------------------------------------------------------------------------
    // priority 1 & 2 - immediate win / block
    // -------------------------------------------------------------------------

    /**
     * returns the first cell (octagon or legal rhombus) that immediately wins for `colour`
     * uses temporary placement so the board state is never permanently changed
     */
    private String findImmediateMove(PlayerColour colour) {
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            if (e.getValue().isOccupied()) continue;
            if (testTemporary(e.getKey(), e.getValue(), colour)) return e.getKey();
        }
        for (Map.Entry<String, RhombicCell> e : board.getRhombusCells().entrySet()) {
            if (e.getValue().isOccupied()) continue;
            int[] rhombusCoords = parseRhombusCoords(e.getKey());
            if (rhombusCoords == null
                || !isRhombusLegal(rhombusCoords[0], rhombusCoords[1], colour)) continue;
            if (testTemporary(e.getKey(), e.getValue(), colour)) return e.getKey();
        }
        return null;
    }

    /** temporarily places and checks win, always undoes the placement */
    private boolean testTemporary(String key, Cell cell, PlayerColour colour) {
        cell.setColour(colour);
        cell.setOccupied(true);
        boolean wins = winCheck.checkWin(colour);
        cell.setColour(PlayerColour.EMPTY);
        cell.setOccupied(false);
        return wins;
    }

    // -------------------------------------------------------------------------
    // priority 3 - combined strategic move (bot path + opponent disruption)
    // -------------------------------------------------------------------------

    /**
     * finds the best move by running Dijkstra for BOTH players and minimising
     * the combined path cost through each candidate cell:
     *
     *   combined = botCost + oppCost
     *
     * A low combined score means the cell lies on BOTH players' optimal crossing
     * routes - the most contested position and therefore the strongest move.
     *
     * Rhombuses compete on equal terms with octagons in this ranking.
     * They are not placed reflexively whenever legal; they must earn their place
     * by scoring at least as well as the best available octagon.
     *
     * Ties are broken by Manhattan distance to the board centre (preferring central
     * cells), then randomly among the remaining top candidates for variety.
     */
    private String findBestCombinedMove(PlayerColour bot, PlayerColour opponent) {
        Map<String, Integer> scores = computeCombinedScores(bot, opponent);
        if (scores.isEmpty()) return anyEmptyOctagon(opponent);
        return pickBestScoredCell(scores);
    }

    /**
     * builds a candidate-cell → combined-cost map by running Dijkstra for both players
     * across both edges and scoring every empty octagon and legal rhombus
     */
    private Map<String, Integer> computeCombinedScores(PlayerColour bot, PlayerColour opponent) {
        Map<String, Integer> dsBot = dijkstraFull(bot,      opponent, true);
        Map<String, Integer> deBot = dijkstraFull(bot,      opponent, false);
        Map<String, Integer> dsOpp = dijkstraFull(opponent, bot,      true);
        Map<String, Integer> deOpp = dijkstraFull(opponent, bot,      false);

        Map<String, Integer> scores = new LinkedHashMap<>();
        scoreOctagons(scores, opponent, dsBot, deBot, dsOpp, deOpp);
        scoreRhombuses(scores, bot, dsBot, deBot, dsOpp, deOpp);
        return scores;
    }

    private void scoreOctagons(Map<String, Integer> scores, PlayerColour opponent,
                               Map<String, Integer> dsBot, Map<String, Integer> deBot,
                               Map<String, Integer> dsOpp, Map<String, Integer> deOpp) {
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            OctagonalCell cell = e.getValue();
            if (cell.isOccupied() || cell.getColour() == opponent) continue;

            int botCost = safeAdd(dsBot.getOrDefault(e.getKey(), INF),
                deBot.getOrDefault(e.getKey(), INF));
            if (botCost >= INF) continue; // unreachable for bot - skip

            int oppCost = safeAdd(dsOpp.getOrDefault(e.getKey(), INF),
                deOpp.getOrDefault(e.getKey(), INF));
            // unreachable for opponent → 0 disruption value
            scores.put(e.getKey(), safeAdd(botCost, oppCost < INF ? oppCost : 0));
        }
    }

    private void scoreRhombuses(Map<String, Integer> scores, PlayerColour bot,
                                Map<String, Integer> dsBot, Map<String, Integer> deBot,
                                Map<String, Integer> dsOpp, Map<String, Integer> deOpp) {
        for (Map.Entry<String, RhombicCell> e : board.getRhombusCells().entrySet()) {
            if (e.getValue().isOccupied()) continue;
            int[] rhombusCoords = parseRhombusCoords(e.getKey());
            if (rhombusCoords == null
                || !isRhombusLegal(rhombusCoords[0], rhombusCoords[1], bot)) continue;

            int botCost = rhombusPathScore(dsBot, deBot, rhombusCoords[0], rhombusCoords[1]);
            if (botCost >= INF) continue;

            int oppCost = rhombusPathScore(dsOpp, deOpp, rhombusCoords[0], rhombusCoords[1]);
            scores.put(e.getKey(), safeAdd(botCost, oppCost < INF ? oppCost : 0));
        }
    }

    /**
     * picks the lowest-scoring cell, breaking ties by centrality and then randomly
     * among the top RANDOM_POOL_SIZE candidates for variety
     */
    private String pickBestScoredCell(Map<String, Integer> scores) {
        int minScore = Collections.min(scores.values());

        List<String> best = new ArrayList<>();
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            if (e.getValue() == minScore) best.add(e.getKey());
        }

        best.sort(Comparator.comparingInt(this::centralityPenalty));

        int poolEnd = Math.min(RANDOM_POOL_SIZE, best.size());
        return best.get(rng.nextInt(poolEnd));
    }

    // -------------------------------------------------------------------------
    // Dijkstra - full graph including diagonal rhombus edges
    // -------------------------------------------------------------------------

    /**
     * runs Dijkstra from one goal edge across the full connectivity graph:
     *   octagon  → 4 orthogonal octagons
     *   octagon  → up to 4 adjacent rhombus cells
     *   rhombus  → 4 corner octagonal cells
     *
     * for BLACK: near edge = row 1, far edge = row 11
     * for WHITE: near edge = col A (0), far edge = col K (10)
     *
     * @param traverser  the colour whose chain we are path-finding for
     * @param blocker    the colour that cannot be traversed
     * @param fromStart  true → seed from near edge; false → seed from far edge
     */
    private Map<String, Integer> dijkstraFull(PlayerColour traverser, PlayerColour blocker,
                                              boolean fromStart) {
        Map<String, Integer> dist = new HashMap<>();
        // queue entries: {cost, key, isRhombus}
        PriorityQueue<Object[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> (int) a[0]));

        if (traverser == PlayerColour.BLACK) {
            int seedRow = fromStart ? 1 : BOARD_SIZE;
            for (int col = 0; col < BOARD_SIZE; col++)
                seedOctagon(pq, dist, col, seedRow, traverser, blocker);
        } else {
            int seedCol = fromStart ? 0 : BOARD_SIZE - 1;
            for (int row = 1; row <= BOARD_SIZE; row++)
                seedOctagon(pq, dist, seedCol, row, traverser, blocker);
        }

        while (!pq.isEmpty()) {
            Object[] cur   = pq.poll();
            int     d         = (int)     cur[0];
            String  key       = (String)  cur[1];
            boolean isRhombus = (boolean) cur[2];

            if (d > dist.getOrDefault(key, INF)) continue; // stale entry

            if (isRhombus) {
                int[] rhombusCoords = parseRhombusCoords(key);
                if (rhombusCoords == null) continue;
                int col = rhombusCoords[0], row = rhombusCoords[1];
                relaxOctagon(pq, dist, col,     row,     d, traverser, blocker);
                relaxOctagon(pq, dist, col + 1, row,     d, traverser, blocker);
                relaxOctagon(pq, dist, col,     row - 1, d, traverser, blocker);
                relaxOctagon(pq, dist, col + 1, row - 1, d, traverser, blocker);
            } else {
                int col, row;
                try {
                    col = key.charAt(0) - 'A';
                    row = Integer.parseInt(key.substring(1));
                } catch (Exception e) { continue; }

                // orthogonal octagon neighbours
                relaxOctagon(pq, dist, col - 1, row,     d, traverser, blocker);
                relaxOctagon(pq, dist, col + 1, row,     d, traverser, blocker);
                relaxOctagon(pq, dist, col,     row - 1, d, traverser, blocker);
                relaxOctagon(pq, dist, col,     row + 1, d, traverser, blocker);

                // diagonal rhombus neighbours
                relaxRhombus(pq, dist, col - 1, row,     d, traverser, blocker);
                relaxRhombus(pq, dist, col,     row,     d, traverser, blocker);
                relaxRhombus(pq, dist, col - 1, row + 1, d, traverser, blocker);
                relaxRhombus(pq, dist, col,     row + 1, d, traverser, blocker);
            }
        }
        return dist;
    }

    private void seedOctagon(PriorityQueue<Object[]> pq, Map<String, Integer> dist,
                             int col, int row, PlayerColour traverser, PlayerColour blocker) {
        if (col < 0 || col >= BOARD_SIZE || row < 1 || row > BOARD_SIZE) return;
        String label = QuaxBoard.generateLabel(col, row);
        OctagonalCell cell = board.getOctagonCells().get(label);
        if (cell == null || cell.getColour() == blocker) return;
        int cost = (cell.getColour() == traverser) ? 0 : 1;
        if (cost < dist.getOrDefault(label, INF)) {
            dist.put(label, cost);
            pq.offer(new Object[]{cost, label, false});
        }
    }

    private void relaxOctagon(PriorityQueue<Object[]> pq, Map<String, Integer> dist,
                              int col, int row, int d, PlayerColour traverser, PlayerColour blocker) {
        if (col < 0 || col >= BOARD_SIZE || row < 1 || row > BOARD_SIZE) return;
        String label = QuaxBoard.generateLabel(col, row);
        OctagonalCell cell = board.getOctagonCells().get(label);
        if (cell == null || cell.getColour() == blocker) return;
        int nd  = safeAdd(d, cell.getColour() == traverser ? 0 : 1);
        if (nd < dist.getOrDefault(label, INF)) {
            dist.put(label, nd);
            pq.offer(new Object[]{nd, label, false});
        }
    }

    private void relaxRhombus(PriorityQueue<Object[]> pq, Map<String, Integer> dist,
                              int col, int row, int d, PlayerColour traverser, PlayerColour blocker) {
        if (col < 0 || col >= BOARD_SIZE - 1 || row < 2 || row > BOARD_SIZE) return;
        String key = "R-" + QuaxBoard.generateLabel(col, row);
        RhombicCell cell = board.getRhombusCells().get(key);
        if (cell == null || cell.getColour() == blocker) return;
        int nd = safeAdd(d, cell.getColour() == traverser ? 0 : 1);
        if (nd < dist.getOrDefault(key, INF)) {
            dist.put(key, nd);
            pq.offer(new Object[]{nd, key, true});
        }
    }

    // -------------------------------------------------------------------------
    // helpers for rateAllMoves()
    // -------------------------------------------------------------------------

    private Set<String> collectImmediateMoves(PlayerColour colour) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            if (!e.getValue().isOccupied() && testTemporary(e.getKey(), e.getValue(), colour))
                result.add(e.getKey());
        }
        for (Map.Entry<String, RhombicCell> e : board.getRhombusCells().entrySet()) {
            if (e.getValue().isOccupied()) continue;
            int[] rhombusCoords = parseRhombusCoords(e.getKey());
            if (rhombusCoords != null
                && isRhombusLegal(rhombusCoords[0], rhombusCoords[1], colour)
                && testTemporary(e.getKey(), e.getValue(), colour))
                result.add(e.getKey());
        }
        return result;
    }

    private void applyBand(Map<String, Double> ratings, Map<String, Integer> scores,
                           double bandLow, double bandHigh) {
        if (scores.isEmpty()) return;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int s : scores.values()) { if (s < min) min = s; if (s > max) max = s; }
        double range = max - min;
        double mid   = (bandLow + bandHigh) / 2.0;
        double span  = bandHigh - bandLow;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            // lower raw score → higher rating (better move = greener cell)
            double rating = (range == 0) ? mid : bandHigh - span * ((e.getValue() - min) / range);
            ratings.put(e.getKey(), rating);
        }
    }

    // -------------------------------------------------------------------------
    // rhombus helpers
    // -------------------------------------------------------------------------

    /**
     * a rhombus at (col, row) is legal when the bot owns BOTH stones
     * of at least one of its two diagonal pairs:
     *   pair 1: (col, row)   and (col+1, row-1)
     *   pair 2: (col+1, row) and (col, row-1)
     */
    private boolean isRhombusLegal(int col, int row, PlayerColour colour) {
        boolean pair1 = hasColour(col,     row,     colour)
            && hasColour(col + 1, row - 1, colour);
        boolean pair2 = hasColour(col + 1, row,     colour)
            && hasColour(col,     row - 1, colour);
        return pair1 || pair2;
    }

    /**
     * scores a rhombus position by the minimum combined (start+end) distance
     * among its four corner octagonal cells
     */
    private int rhombusPathScore(Map<String, Integer> distStart,
                                 Map<String, Integer> distEnd,
                                 int col, int row) {
        int[][] corners = {{col, row}, {col+1, row}, {col, row-1}, {col+1, row-1}};
        int minScore = INF;
        for (int[] c : corners) {
            if (c[0] < 0 || c[0] >= BOARD_SIZE || c[1] < 1 || c[1] > BOARD_SIZE) continue;
            String label = QuaxBoard.generateLabel(c[0], c[1]);
            int ds = distStart.getOrDefault(label, INF);
            int de = distEnd.getOrDefault(label, INF);
            if (ds < INF && de < INF) minScore = Math.min(minScore, safeAdd(ds, de));
        }
        return minScore;
    }

    private boolean hasColour(int col, int row, PlayerColour colour) {
        if (col < 0 || col >= BOARD_SIZE || row < 1 || row > BOARD_SIZE) return false;
        OctagonalCell cell = board.getOctagonCells().get(QuaxBoard.generateLabel(col, row));
        return cell != null && cell.getColour() == colour;
    }

    private int[] parseRhombusCoords(String key) {
        if (key == null || !key.startsWith("R-") || key.length() < 4) return null;
        try {
            int col = key.charAt(2) - 'A';
            int row = Integer.parseInt(key.substring(3));
            return new int[]{col, row};
        } catch (Exception e) { return null; }
    }

    private String anyEmptyOctagon(PlayerColour opponent) {
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            if (!e.getValue().isOccupied() && e.getValue().getColour() != opponent)
                return e.getKey();
        }
        return null;
    }

    /** Manhattan distance to board centre (col F = index 5, row 6) - lower is preferred */
    private int centralityPenalty(String label) {
        try {
            int col = label.charAt(0) - 'A';
            int row = Integer.parseInt(label.substring(1));
            return Math.abs(col - 5) + Math.abs(row - 6);
        } catch (Exception e) { return 999; }
    }

    private PlayerColour opposite(PlayerColour c) {
        return (c == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
    }

    private int safeAdd(int a, int b) {
        if (a == INF || b == INF || a >= INF - b) return INF;
        return a + b;
    }
}
