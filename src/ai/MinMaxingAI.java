package ai;

import model.Move;
import model.State;

import java.util.*;

public abstract class MinMaxingAI implements AI {

    /**
     * Inner class used to store a move, its eventual heuristic rating, and the depth to which it was explored.
     */
    class RatedMove {
        Move move;
        int rating;
        int depth;
    }

    /**
     * Cache entry - stores a game state, and the best move we found from that state, to reuse in the future. The
     * "staleness" value is the number of empty squares on the board in this state - since in this game the number of
     * pieces on the board never goes down, we can use this to quickly clear out useless cache entries.
     */
    class CacheEntry {
        State actualState;
        RatedMove foundMove;
        int staleness;
    }

    // For quickly referring to our player ID and opponent's player ID.
    int us, them;

    // For statistics.
    int states, alphas, betas, hits, bumps, overdrives, crashes;

    // Stores the search depth set in the constructor
    int depth;

    // Base bit patterns for Zobrist hashes of game positions.
    int zobristBase[][][]; // [x][y][p] is the bit pattern for a piece of player p being at location x,y.
    int turnZobrist[];     // Zobrist values for whose turn it is in the given state.

    // The actual cache, mapping from zobrist hash to cache entry
    Hashtable<Integer,CacheEntry> cache;

    // Hook for the heuristic
    public abstract int heuristic(State toBoard, int us, int them);

    /**
     * Creates a new MinMaxingAI with the given search depth.
     * @param depth The depth to search to.
     */
    public MinMaxingAI(int depth) {
        super();
        this.depth = depth;
        // Create random zobrist hash values for each possible piece position
        zobristBase = new int[10][10][3];
        Random rnd = new Random();
        for (int x=0; x<10; x++) {
            for (int y=0; y<10; y++) {
                for (int s=0; s<3; s++) {
                    zobristBase[x][y][s] = rnd.nextInt();
                }
            }
        }
        // Create zobrist hash values for it being each player's turn
        turnZobrist = new int[2];
        for (int s=0; s<2; s++) {
            turnZobrist[s] = rnd.nextInt();
        }
        // Set up empty cache
        cache = new Hashtable<>();
    }

    /**
     * Calculate the Zobrist hash for the given board.
     * @param board The board to calculate for.
     * @return The Zobrist hash.
     */
    public int zobristHash(State board) {
        // Start from Zero
        int hash = 0;
        // Xor the hash value for the state of each square
        for (int x=0; x<10; x++) {
            for (int y=0; y<10; y++) {
                hash = hash ^ zobristBase[x][y][board.pieceAt(x,y)];
            }
        }
        // And for whose turn it is
        hash = hash ^ turnZobrist[board.whoseTurn()-1];
        return hash;
    }

    /**
     * Calculates the eventual heuristic rating for a given move.
     * @param fromBoard The board to start from.
     * @param theMove The move to consider making.
     * @param level How many recursive levels to explore after that move.
     * @param inAlpha The alpha cut-off value.
     * @param inBeta The beta cut-off value.
     * @return The integer rating for the move.
     */
    int rateMove(State fromBoard, Move theMove, int level, int inAlpha, int inBeta) {
        State newBoard = fromBoard.afterMove(theMove);

        int ours = newBoard.countPieces(us);
        int theirs = newBoard.countPieces(them);

        // Following are conditions under which we win/lose immediately. We don't put these in the heuristic because
        // if we encounter one, we should stop recursing no matter what (the heuristic is only used when we reach the
        // end of the recursion)
        // Enemy out of pieces, we win.
        if (theirs == 0) return 9998;
        // Board full: we have more pieces, we win; we have less, we lose.
        if (ours+theirs == 100) {
            if (ours > 50) return 9998;
            return 0;
        }
        // Next turn has no moves: other player claims all open spaces.
        // If that gives us more, we win; else, we lose.
        ArrayList<Move> nextMoves = newBoard.validMoves();
        if (nextMoves.size() == 0) {
            int rest = (100 - ours) - theirs;
            if (newBoard.whoseTurn() == us) {
                if ((ours+rest) > theirs) return 9998;
            }   else {
                if ((theirs+rest) > ours) return 0;
            }
        }
        // Ok, we could potentially recurse. Do we have recursive levels left?
        if (level > 0) {
            // Yes, recurse and return result.
            RatedMove next = moveSearch(newBoard,level-1,inAlpha,inBeta);
            if (next != null) {
                return next.rating;
            } else {
                // No future move could be found within the given alpha-beta range, so we shouldn't take this move.
                return -9999;
            }
         } else {
            // Reached recursion limit, use the heuristic.
            return heuristic(newBoard, us, them);
         }
    }

    /**
     * The actual recursive move search function.
     * @param fromBoard The position to start from.
     * @param level How many more levels of recursion to perform.
     * @param inAlpha The alpha value at the root of this subtree.
     * @param inBeta The beta value at the root of this subtree.
     * @return The RatedMove that's the best found so far.
     */
    RatedMove moveSearch(State fromBoard, int level, int inAlpha, int inBeta) {
        int alpha = inAlpha;
        int beta = inBeta;

        Move bestSoFar = null;
        int bestRating, rating;

        // Zobrist hash this board, and check if the hash is already in the cache.
        int zobrist = zobristHash(fromBoard);
        if (cache.containsKey(zobrist)) {
            // Hashes were the same. But Zobrist Hashes are not 100% exact, so see if the boards are really the same.
            CacheEntry cacheResult = cache.get(zobrist);
            State test = cacheResult.actualState;
            boolean ok = true;
            if (test.whoseTurn() != fromBoard.whoseTurn()) ok = false;
            if (ok) {
                for (int x=0; x<10; x++) {
                    for (int y=0; y<10; y++) {
                        if (test.pieceAt(x,y) != fromBoard.pieceAt(x,y)) {
                            ok = false;
                            break;
                        }
                    }
                    if (!ok) break;
                }
            }
            if(!ok) {
                // They are not the same. Flag this as a Zobrist collision and go ahead with regular search.
                crashes++;
            } else {
                // They are the same! We've searched this state before! How did we do?
                if (cacheResult.foundMove.depth > level) {
                    // We've found this before at a high level, so our previous search was actually better than the
                    // one we're about to do!
                    overdrives++;
                    return cacheResult.foundMove;
                }
                if (cacheResult.foundMove.depth == level) {
                    // We've found this before at the same level. No point doing it again.
                    hits++;
                    return cacheResult.foundMove;
                }
                // If we've found this before at a lower level, meh. We need to explore it further, so go ahead
                // with the search we were about to do.
            }
        }

        // If it's our turn, start from the lowest possible rating (worst for us). If it's their turn, start from
        // the highest possible rating (worst for them)
        if (fromBoard.whoseTurn() == us) bestRating = -9999; else bestRating = 9999;

        for (Move theMove : fromBoard.validMoves()) {
            // Count states examined
            states++;
            // If it's our turn..
            if (fromBoard.whoseTurn() == us) {
                // Calculate rating of this move (which may include recursion)
                rating = rateMove(fromBoard, theMove, level - 1, alpha, inBeta);
                // Keep alpha up to date with the best move found so far.
                alpha = Math.max(alpha, rating);
                if ((rating > bestRating)) {
                    bestRating = rating;
                    bestSoFar = theMove;
                }
                // If alpha>beta then it is pointless to continue examining this state. We assume the opponent will not
                // allow us to get into this state in the first place because we have already found a way they can
                // make things worse for us.
                if (alpha >= inBeta) {
                    betas++;
                    break;
                }
            } else { // If it's their turn..
                rating = rateMove(fromBoard,theMove,level-1,inAlpha,beta);
                // Keep beta up to date with the worst (for us, best for opponent) move found so far.
                beta = Math.min(beta,rating);
                if ((rating < bestRating)) {
                    bestRating = rating;
                    bestSoFar = theMove;
                }
                // If alpha>beta then it is pointless to continue examining this state. Getting into this state in the
                // first place is a bad idea because there are other states we've found where the opponent's worst
                // move is better for us.
                if (inAlpha >= beta) {
                    alphas++;
                    break;
                }
            }
        }
        // Store the result in the cache, and return it
        RatedMove result = new RatedMove();
        result.move = bestSoFar;
        result.rating = bestRating;
        result.depth = level;
        if (bestSoFar != null) {
            CacheEntry entry = new CacheEntry();
            entry.actualState = fromBoard;
            entry.foundMove = result;
            entry.staleness = fromBoard.countPieces(0);
            // Note that this will overwrite any previous cache entry with the same Zobrist hash.
            // That's ok - at the start of this method we already checked to see if there was an existing relevant entry.
            cache.put(zobrist,entry);
        }
        return result;
    }

    @Override
    /**
     * Calculates the next move.
     */
    public Move nextMove(State board) {
        // Clear statistic values.
        states = 0;
        alphas = 0;
        betas = 0;
        hits = 0;
        bumps = 0;
        overdrives = 0;
        crashes = 0;

        us = board.whoseTurn();
        them = board.whoseNotTurn();

        // Start the recursive search.
        // Alpha (best maximum at a minimizing node) and beta (best minimum at a maximising node) start at the
        // "worst" possible values.
        RatedMove bestMove = moveSearch(board,depth,-9999,9999);


        System.out.println("MinMaxing examined " + states + " states, " + alphas + " alpha cut-offs, " + betas + " beta cut-offs.");
        System.out.println("Cache size is " + cache.size() + ", " + hits + " hits, " + overdrives + " overdrives, " + crashes + " Zobrist hash collisions.");

        // Cache pruning
        // Since the number of pieces on the board in this game never goes down, only up,
        // Any board with more blank spaces than the current position will never be reached and can be pruned
        ArrayList<Integer> deadlist = new ArrayList<>();
        int threshold = board.countPieces(0);
        for (int key : cache.keySet()) {
            if (cache.get(key).staleness > threshold) deadlist.add(key);
        }
        for (int key : deadlist) {
            cache.remove(key);
        }
        System.out.println("Pruning reduced cache size to " + cache.size());

        return bestMove.move;
    }


}
