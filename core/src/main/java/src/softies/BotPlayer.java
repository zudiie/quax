package src.softies;

import src.softies.board.GameState;

import java.util.*;

// computer-controlled player for Human vs Bot mode
//
// MOVE SELECTION — evaluated in this priority order every turn:
//
//   1. Win immediately   — test every empty octagonal AND rhombic cell via temporary
//                          placement + WinCheck. If any cell wins, play it.
//
//   2. Block opponent    — same scan for the opponent's colour.
//
//   3. Place rhombus     — if any legal rhombus exists (the required diagonal pair of
//                          same-colour stones is already on the board), ALWAYS play it.
//                          A rhombus is free diagonal connectivity — never waste the
//                          opportunity.
//
//   4. Strategic octagon — run Dijkstra from both goal edges using a graph that
//                          includes DIAGONAL edges through rhombus cells (mirroring
//                          the WinCheck connectivity model). Pick the empty octagonal
//                          cell that minimises the total crossing cost.
//                          Ties are broken by preferring cells closest to the board
//                          centre, then randomly among remaining ties.
//
// DIJKSTRA GRAPH:
//   Nodes — octagonal cells (all 121) and rhombic cells (100)
//   Edges — octagon  → 4 orthogonal octagons
//          — octagon  → up to 4 adjacent rhombus cells (sharing a corner)
//          — rhombus  → 4 corner octagonal cells
//   Costs — bot-owned cell: 0   (already in chain)
//          — empty cell:    1   (needs to be claimed)
//          — opponent cell: ∞   (impassable — never enqueued)
//
// This graph matches exactly how WinCheck traces winning paths, so the bot's
// path-finding and win-detection share the same connectivity model.
public class BotPlayer {

    private static final int BOARD_SIZE = 11;

    private final QuaxBoard board;
    private final GameState gameState;
    private final WinCheck  winCheck;
    private final Random    rng = new Random();

    // large sentinel for "unreachable" — must be > any real path cost (max = 121)
    private static final int INF = Integer.MAX_VALUE / 2;

    /**
     * @param board     live board model — read and temporarily mutated for win testing
     * @param gameState queried every turn for the current bot colour
     */
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
     * returns null only if the board is completely full
     */
    public String selectMove() {
        PlayerColour bot      = gameState.getBotColour();
        PlayerColour opponent = opposite(bot);

        // priority 1 — win immediately
        String win = findImmediateMove(bot);
        if (win != null) { System.out.println("Bot wins at: " + win); return win; }

        // priority 2 — block the opponent from winning next turn
        String block = findImmediateMove(opponent);
        if (block != null) { System.out.println("Bot blocks at: " + block); return block; }

        // priority 3 — place any legal rhombus (always strategically free connectivity)
        String rhombus = findBestLegalRhombus(bot, opponent);
        if (rhombus != null) { System.out.println("Bot places rhombus: " + rhombus); return rhombus; }

        // priority 4 — choose the best strategic octagon via full Dijkstra
        String strategic = findStrategicOctagon(bot, opponent);
        System.out.println("Bot strategic move: " + strategic);
        return strategic;
    }

    /**
     * rates every non-occupied cell (octagonal and rhombic) from the bot's perspective
     * in a normalized [0, 1] range where 1.0 = the bot wants this cell most
     *
     * ratings are banded to preserve the four-level priority cascade of selectMove:
     *   winning move          → 1.00
     *   blocks opponent win   → 0.95
     *   legal rhombus         → 0.70 – 0.90  (via rhombusPathScore)
     *   reachable octagon     → 0.10 – 0.60  (via ds + de)
     *   illegal rhombus / unreachable → 0.00
     *
     * within a band, lower raw path cost → higher rating
     */
    public Map<String, Double> rateAllMoves() {
        PlayerColour bot      = gameState.getBotColour();
        PlayerColour opponent = opposite(bot);

        Map<String, Double> ratings = new HashMap<>();

        // priority 1 + 2 — collect EVERY winning / blocking cell, not just the first
        Set<String> winningMoves  = collectImmediateMoves(bot);
        Set<String> blockingMoves = collectImmediateMoves(opponent);
        blockingMoves.removeAll(winningMoves); // winning takes precedence over blocking

        for (String key : winningMoves)  ratings.put(key, 1.00);
        for (String key : blockingMoves) ratings.put(key, 0.95);

        // run Dijkstra once each direction — reused for both rhombus and octagon scoring
        Map<String, Integer> distStart = dijkstraFull(bot, opponent, true);
        Map<String, Integer> distEnd   = dijkstraFull(bot, opponent, false);

        // priority 3 — legal rhombuses get a band score; illegal ones get 0.0
        Map<String, Integer> rhombusRawScores = new LinkedHashMap<>();
        for (Map.Entry<String, RhombicCell> e : board.getRhombusCells().entrySet()) {
            String key = e.getKey();
            if (ratings.containsKey(key) || e.getValue().isOccupied()) continue;

            int[] rc = parseRhombusCoords(key);
            if (rc == null) continue;

            if (!isRhombusLegal(rc[0], rc[1], bot)) {
                ratings.put(key, 0.0);
                continue;
            }
            int raw = rhombusPathScore(distStart, distEnd, rc[0], rc[1]);
            if (raw >= INF) {
                ratings.put(key, 0.0);
                continue;
            }
            rhombusRawScores.put(key, raw);
        }

        // priority 4 — reachable empty octagons get a band score; unreachable get 0.0
        Map<String, Integer> octagonRawScores = new LinkedHashMap<>();
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            String key = e.getKey();
            OctagonalCell cell = e.getValue();
            if (ratings.containsKey(key)) continue;
            if (cell.isOccupied() || cell.getColour() == opponent) continue;

            int ds = distStart.getOrDefault(key, INF);
            int de = distEnd.getOrDefault(key, INF);
            if (ds >= INF || de >= INF) {
                ratings.put(key, 0.0);
                continue;
            }
            octagonRawScores.put(key, ds + de);
        }

        applyBand(ratings, rhombusRawScores, 0.70, 0.90);
        applyBand(ratings, octagonRawScores, 0.10, 0.60);

        return ratings;
    }

    /** collects EVERY cell (octagon + legal rhombus) where `colour` would win if placed */
    private Set<String> collectImmediateMoves(PlayerColour colour) {
        Set<String> result = new HashSet<>();

        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            OctagonalCell cell = e.getValue();
            if (cell.isOccupied()) continue;

            cell.setColour(colour);
            cell.setOccupied(true);
            boolean wins = winCheck.checkWin(colour);
            cell.setColour(PlayerColour.EMPTY);
            cell.setOccupied(false);

            if (wins) result.add(e.getKey());
        }

        for (Map.Entry<String, RhombicCell> e : board.getRhombusCells().entrySet()) {
            RhombicCell cell = e.getValue();
            if (cell.isOccupied()) continue;

            int[] rc = parseRhombusCoords(e.getKey());
            if (rc == null || !isRhombusLegal(rc[0], rc[1], colour)) continue;

            cell.setColour(colour);
            cell.setOccupied(true);
            boolean wins = winCheck.checkWin(colour);
            cell.setColour(PlayerColour.EMPTY);
            cell.setOccupied(false);

            if (wins) result.add(e.getKey());
        }

        return result;
    }

    /**
     * normalises raw path costs into the given rating band
     * lower raw = higher rating; a single-entry tier gets the band midpoint
     */
    private void applyBand(Map<String, Double> ratings, Map<String, Integer> scores,
                           double bandLow, double bandHigh) {
        if (scores.isEmpty()) return;

        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int s : scores.values()) {
            if (s < min) min = s;
            if (s > max) max = s;
        }
        double range = max - min;
        double mid   = (bandLow + bandHigh) / 2.0;
        double span  = bandHigh - bandLow;

        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            double rating = (range == 0)
                ? mid
                : bandHigh - span * ((e.getValue() - min) / range);
            ratings.put(e.getKey(), rating);
        }
    }

    // -------------------------------------------------------------------------
    // priority 1 & 2 — immediate win / block
    // -------------------------------------------------------------------------

    /**
     * scans all empty octagonal and rhombic cells and returns one that immediately
     * produces a winning path for the given colour if claimed
     * uses temporary placement so the board state is never permanently modified
     */
    private String findImmediateMove(PlayerColour colour) {
        // check octagonal cells
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            OctagonalCell cell = e.getValue();
            if (cell.isOccupied()) continue;

            cell.setColour(colour);
            cell.setOccupied(true);
            boolean wins = winCheck.checkWin(colour);
            cell.setColour(PlayerColour.EMPTY);
            cell.setOccupied(false);

            if (wins) return e.getKey();
        }

        // check rhombic cells (only legally placeable ones)
        for (Map.Entry<String, RhombicCell> e : board.getRhombusCells().entrySet()) {
            RhombicCell cell = e.getValue();
            if (cell.isOccupied()) continue;

            int[] rc = parseRhombusCoords(e.getKey());
            if (rc == null || !isRhombusLegal(rc[0], rc[1], colour)) continue;

            cell.setColour(colour);
            cell.setOccupied(true);
            boolean wins = winCheck.checkWin(colour);
            cell.setColour(PlayerColour.EMPTY);
            cell.setOccupied(false);

            if (wins) return e.getKey();
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // priority 3 — place best legal rhombus
    // -------------------------------------------------------------------------

    /**
     * finds the legal rhombus that lies on the best crossing path and returns its key
     * a rhombus is legal when the bot already owns both stones of at least one diagonal pair
     * among all legal rhombuses, picks the one with the best (lowest) Dijkstra crossing score
     * returns null if no legal rhombus exists
     */
    private String findBestLegalRhombus(PlayerColour bot, PlayerColour opponent) {
        // run Dijkstra first so we can score the rhombus positions
        Map<String, Integer> distStart = dijkstraFull(bot, opponent, true);
        Map<String, Integer> distEnd   = dijkstraFull(bot, opponent, false);

        String bestKey   = null;
        int    bestScore = INF;

        for (Map.Entry<String, RhombicCell> e : board.getRhombusCells().entrySet()) {
            if (e.getValue().isOccupied()) continue;

            int[] rc = parseRhombusCoords(e.getKey());
            if (rc == null || !isRhombusLegal(rc[0], rc[1], bot)) continue;

            // score this rhombus by its Dijkstra crossing cost
            int score = rhombusPathScore(distStart, distEnd, rc[0], rc[1]);
            if (score < bestScore) {
                bestScore = score;
                bestKey   = e.getKey();
            }
        }

        return bestKey; // null if no legal rhombus exists
    }

    // -------------------------------------------------------------------------
    // priority 4 — strategic octagon via full Dijkstra
    // -------------------------------------------------------------------------

    /**
     * runs Dijkstra from both goal edges using a graph that includes diagonal
     * connections through rhombus cells, then picks the best empty octagonal cell
     * ties are broken by centrality (closer to board centre is better), then randomly
     */
    private String findStrategicOctagon(PlayerColour bot, PlayerColour opponent) {
        Map<String, Integer> distStart = dijkstraFull(bot, opponent, true);
        Map<String, Integer> distEnd   = dijkstraFull(bot, opponent, false);

        // build candidate map: label → crossing score for all empty octagonal cells
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            OctagonalCell cell = e.getValue();
            if (cell.isOccupied() || cell.getColour() == opponent) continue;

            int ds = distStart.getOrDefault(e.getKey(), INF);
            int de = distEnd.getOrDefault(e.getKey(), INF);
            if (ds == INF || de == INF) continue;

            scores.put(e.getKey(), ds + de);
        }

        if (scores.isEmpty()) return anyEmptyOctagon(opponent);

        // find minimum score
        int minScore = Collections.min(scores.values());

        // collect all cells with the minimum score, then sort by centrality
        List<String> best = new ArrayList<>();
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            if (e.getValue() == minScore) best.add(e.getKey());
        }

        // among tied cells, prefer those closest to the centre of the board (col 5, row 6)
        // this steers the bot toward the middle on an empty board and keeps play varied
        best.sort(Comparator.comparingInt(label -> centralityPenalty(label)));

        // keep only the top-central tier (within 2 of the best centrality penalty)
        int bestCentrality = centralityPenalty(best.get(0));
        List<String> topTier = new ArrayList<>();
        for (String label : best) {
            if (centralityPenalty(label) <= bestCentrality + 2) topTier.add(label);
        }

        return topTier.get(rng.nextInt(topTier.size()));
    }

    /**
     * penalises cells far from the board centre (column F = index 5, row 6)
     * lower value = closer to centre = preferred
     */
    private int centralityPenalty(String label) {
        int col = label.charAt(0) - 'A';
        int row;
        try { row = Integer.parseInt(label.substring(1)); }
        catch (NumberFormatException e) { return 999; }
        return Math.abs(col - 5) + Math.abs(row - 6);
    }

    /** fallback: first empty octagonal cell not owned by opponent */
    private String anyEmptyOctagon(PlayerColour opponent) {
        for (Map.Entry<String, OctagonalCell> e : board.getOctagonCells().entrySet()) {
            if (!e.getValue().isOccupied() && e.getValue().getColour() != opponent)
                return e.getKey();
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // full Dijkstra — includes diagonal edges through rhombus cells
    // -------------------------------------------------------------------------

    /**
     * runs Dijkstra from one goal edge across the board, traversing BOTH orthogonal
     * octagon edges AND diagonal edges through rhombus cells
     *
     * this graph mirrors the WinCheck DFS connectivity model exactly:
     *   octagon → 4 orthogonal octagons
     *   octagon → up to 4 adjacent rhombus cells
     *   rhombus → 4 corner octagonal cells
     *
     * cell costs:
     *   bot-owned: 0  (already claimed, free to pass through)
     *   empty:     1  (needs to be claimed)
     *   opponent:  ∞  (impassable)
     *
     * for BLACK: near edge = row 1, far edge = row 11
     * for WHITE: near edge = col A (0), far edge = col K (10)
     *
     * @param fromStart true → seed from the "near" edge; false → seed from the "far" edge
     * @return map from cell key (octagon label or "R-" rhombus key) → minimum path cost
     */
    private Map<String, Integer> dijkstraFull(PlayerColour bot, PlayerColour opponent,
                                              boolean fromStart) {
        Map<String, Integer> dist = new HashMap<>();
        // priority queue entries: {cost, key, isRhombus}
        PriorityQueue<Object[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> (int)a[0]));

        // seed the starting edge
        if (bot == PlayerColour.BLACK) {
            int seedRow = fromStart ? 1 : BOARD_SIZE;
            for (int col = 0; col < BOARD_SIZE; col++) {
                seedOctagon(pq, dist, col, seedRow, bot, opponent);
            }
        } else {
            int seedCol = fromStart ? 0 : BOARD_SIZE - 1;
            for (int row = 1; row <= BOARD_SIZE; row++) {
                seedOctagon(pq, dist, seedCol, row, bot, opponent);
            }
        }

        while (!pq.isEmpty()) {
            Object[] cur = pq.poll();
            int    d      = (int)     cur[0];
            String key    = (String)  cur[1];
            boolean isRhb = (boolean) cur[2];

            if (d > dist.getOrDefault(key, INF)) continue; // stale entry

            if (isRhb) {
                // rhombus → 4 corner octagonal cells
                int[] rc = parseRhombusCoords(key);
                if (rc == null) continue;
                int col = rc[0], row = rc[1];
                relaxOctagon(pq, dist, col,     row,     d, bot, opponent);
                relaxOctagon(pq, dist, col + 1, row,     d, bot, opponent);
                relaxOctagon(pq, dist, col,     row - 1, d, bot, opponent);
                relaxOctagon(pq, dist, col + 1, row - 1, d, bot, opponent);
            } else {
                // octagon → 4 orthogonal octagon neighbours
                int col = key.charAt(0) - 'A';
                int row;
                try { row = Integer.parseInt(key.substring(1)); } catch (Exception e) { continue; }

                relaxOctagon(pq, dist, col - 1, row,     d, bot, opponent);
                relaxOctagon(pq, dist, col + 1, row,     d, bot, opponent);
                relaxOctagon(pq, dist, col,     row - 1, d, bot, opponent);
                relaxOctagon(pq, dist, col,     row + 1, d, bot, opponent);

                // octagon → up to 4 adjacent rhombus cells
                // a rhombus R-(c,r) touches the octagon at (c,r), (c+1,r), (c,r-1), (c+1,r-1)
                relaxRhombus(pq, dist, col - 1, row,     d, bot, opponent); // R-(col-1, row)
                relaxRhombus(pq, dist, col,     row,     d, bot, opponent); // R-(col,   row)
                relaxRhombus(pq, dist, col - 1, row + 1, d, bot, opponent); // R-(col-1, row+1)
                relaxRhombus(pq, dist, col,     row + 1, d, bot, opponent); // R-(col,   row+1)
            }
        }

        return dist;
    }

    /** seeds one octagonal cell into the Dijkstra queue */
    private void seedOctagon(PriorityQueue<Object[]> pq, Map<String, Integer> dist,
                             int col, int row, PlayerColour bot, PlayerColour opponent) {
        if (col < 0 || col >= BOARD_SIZE || row < 1 || row > BOARD_SIZE) return;
        String label = QuaxBoard.generateLabel(col, row);
        OctagonalCell cell = board.getOctagonCells().get(label);
        if (cell == null || cell.getColour() == opponent) return;
        int cost = (cell.getColour() == bot) ? 0 : 1;
        if (cost < dist.getOrDefault(label, INF)) {
            dist.put(label, cost);
            pq.offer(new Object[]{cost, label, false});
        }
    }

    /** relaxes an octagon edge from a current node at distance d */
    private void relaxOctagon(PriorityQueue<Object[]> pq, Map<String, Integer> dist,
                              int col, int row, int d, PlayerColour bot, PlayerColour opponent) {
        if (col < 0 || col >= BOARD_SIZE || row < 1 || row > BOARD_SIZE) return;
        String label = QuaxBoard.generateLabel(col, row);
        OctagonalCell cell = board.getOctagonCells().get(label);
        if (cell == null || cell.getColour() == opponent) return;
        int nd = d + ((cell.getColour() == bot) ? 0 : 1);
        if (nd < dist.getOrDefault(label, INF)) {
            dist.put(label, nd);
            pq.offer(new Object[]{nd, label, false});
        }
    }

    /** relaxes a rhombus edge from a current octagon node at distance d */
    private void relaxRhombus(PriorityQueue<Object[]> pq, Map<String, Integer> dist,
                              int col, int row, int d, PlayerColour bot, PlayerColour opponent) {
        // rhombuses only exist for col 0..9 and row 2..11
        if (col < 0 || col >= BOARD_SIZE - 1 || row < 2 || row > BOARD_SIZE) return;
        String key = "R-" + QuaxBoard.generateLabel(col, row);
        RhombicCell cell = board.getRhombusCells().get(key);
        if (cell == null || cell.getColour() == opponent) return;
        int nd = d + ((cell.getColour() == bot) ? 0 : 1);
        if (nd < dist.getOrDefault(key, INF)) {
            dist.put(key, nd);
            pq.offer(new Object[]{nd, key, true});
        }
    }

    // -------------------------------------------------------------------------
    // rhombus helpers
    // -------------------------------------------------------------------------

    /**
     * a rhombus at (col, row) is legal when the bot already owns both stones
     * of at least one of its two diagonal pairs:
     *   pair 1: (col, row) and (col+1, row-1)
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
     * scores a rhombus placement by taking the minimum crossing cost of its
     * four corner cells — a lower score means the rhombus sits on a better path
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
            if (ds < INF && de < INF) minScore = Math.min(minScore, ds + de);
        }
        return minScore;
    }

    /** @return true if the octagonal cell at (col, row) belongs to the given colour */
    private boolean hasColour(int col, int row, PlayerColour colour) {
        if (col < 0 || col >= BOARD_SIZE || row < 1 || row > BOARD_SIZE) return false;
        OctagonalCell cell = board.getOctagonCells().get(QuaxBoard.generateLabel(col, row));
        return cell != null && cell.getColour() == colour;
    }

    /**
     * parses a rhombus key like "R-B3" into {col, row}
     * returns null if malformed
     */
    private int[] parseRhombusCoords(String key) {
        if (key == null || !key.startsWith("R-") || key.length() < 4) return null;
        String label = key.substring(2);
        try {
            int col = label.charAt(0) - 'A';
            int row = Integer.parseInt(label.substring(1));
            return new int[]{col, row};
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // utilities
    // -------------------------------------------------------------------------

    private PlayerColour opposite(PlayerColour c) {
        return (c == PlayerColour.BLACK) ? PlayerColour.WHITE : PlayerColour.BLACK;
    }
}
