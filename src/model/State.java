package model;

import java.util.ArrayList;

public class State {

    public static final int XSIZE = 10;
    public static final int YSIZE = 10;

    protected final int[][] board;
    protected int whoseTurn;
    protected ArrayList<Move> validMoves;
    protected boolean dirtyBoard;


    /**
     * Awkward way of printing out a state for debugging purposes
     */
    public String toString() {
        String out = "";
        for (int y=0; y<YSIZE; y++) {
            for (int x=0; x<XSIZE;x++) {
                switch(board[x][y]) {
                    case 0: out += "."; break;
                    case 1: out += "1"; break;
                    case 2: out += "2"; break;
                    default: out += "?";
                }
            }
            out += "\n";
        }
        return out;
    }

    /**
     * Constructs a new State with an empty board and the first player's turn.
     */
    public State() {
        board = new int[XSIZE][YSIZE];
        for(int y=0;y<YSIZE;y++)
            for(int x=0;x<XSIZE;x++)
                board[x][y] = 0;

        dirtyBoard = true;
        whoseTurn = 1;
    }

    /**
     * Constructs a new State that is a value copy of another state.
     * @param child The State to copy.
     */
    public State(State child) {
        board = new int[XSIZE][YSIZE];
        for (int x=0; x<XSIZE; x++) {
            System.arraycopy(child.board[x], 0, board[x], 0, YSIZE);
        }
        whoseTurn = child.whoseTurn;
        dirtyBoard = true;
    }

    /**
     * Checks if a given coordinate is within the board.
     * @param x The x coordinate to check.
     * @param y The y coordinate to check.
     * @return True if the coordinate is within the board.
     */
    public static boolean inBounds(int x, int y) {
        if (x < 0) return false;
        if (y < 0) return false;
        return x <= (XSIZE - 1) && y <= (YSIZE - 1);
    }

    /**
     * Gets the piece at a particular location on the board.
     * @param x The x coordinate of the location.
     * @param y The y coordinate of the location.
     * @return 0 if the piece is empty, 1 or 2 for a piece owned by player 1 or 2.
     */
    public int pieceAt(int x,int y) {
        assert State.inBounds(x, y) : "pieceAt called with a location out of bounds " + x + "," + y;
        return board[x][y];
    }

    /**
     * Sets the piece at a particular location on the board.
     * Usually you should not need this as you should use makeMove().
     * @param x The x coordinate of the location.
     * @param y The y coordinate of the location.
     * @param v The player number whose piece should be placed, or 0 for empty.
     */
    public void setBoard(int x, int y, int v) {
        assert State.inBounds(x, y) : "setBoard called with a location out of bounds " + x + ", " + y;
        assert v <= 2 : "setBoard called with an invalid piece type " + v;
        assert v >= 0 : "setBoard called with an invalid piece type " + v;
        board[x][y] = v;
        dirtyBoard = true;
    }

    /**
     * Sets whose turn it is.
     * Usually you should not need this as you should use makeMove().
     * @param t The player number whose turn it should be.
     */
    public void setTurn(int t) {
        assert t <= 2 : "setTurn called with invalid player number " + t;
        assert t >= 1 : "setTurn called with invalid player number " + t;
        whoseTurn = t;
        dirtyBoard = true;
    }

    /**
     * Gets whose turn it is.
     * @return 1 or 2, the number of the player whose turn it is.
     */
    public int whoseTurn() {
        return whoseTurn;
    }


    /**
     * Gives the number of the player opposing the specified one.
     * @param player The number of either player.
     * @return The number of the other player.
     */
    public int enemyOf(int player) {
        assert player < 3 : "enemyOf called with invalid player number " + player;
        assert player > 0 : "enemyOf called with invalid player number " + player;
        if (player == 1) return 2;
        return 1;
    }

    /**
     * Gets whose turn it is NOT.
     * @return 1 or 2, the number of player whose turn it is not.
     */
    public int whoseNotTurn() { return enemyOf(whoseTurn); }

    /**
     * Checks if a move is valid or not.
     * @param move A Move object representing the move to check.
     * @return True if the move is valid, false if it is not.
     */
    public boolean moveIsValid(Move move) {
        assert State.inBounds(move.fromx, move.fromy) : "moveIsValid called with a move from an out of bounds location " + move;
        assert State.inBounds(move.tox, move.toy) : "moveIsValid called with a move to an out of bounds location " + move;
        if (pieceAt(move.fromx,move.fromy) != whoseTurn()) return false;
        if (pieceAt(move.tox,move.toy) != 0) return false;
        if ((move.fromx == move.tox) && (move.fromy == move.toy)) return false;
        if (Math.abs((move.tox - move.fromx)) > 2) return false;
        return Math.abs((move.toy - move.fromy)) <= 2;
    }

    /**
     * Counts the pieces on the board for a given player.
     * @param player The player number whose pieces should be counted, or 0 to count empty squares.
     * @return The number of pieces on the board owned by the specified player.
     */
    public int countPieces(int player) {
        assert player <= 2 : "countPieces called with an invalid piece type " + player;
        assert player >= 0 : "countPieces called with an invalid piece type " + player;
        int count = 0;
        for(int x=0;x<XSIZE;x++) {
            for(int y=0; y<YSIZE; y++) {
               if (pieceAt(x,y) == player) count++;
            }
        }
        return count;
    }

    /**
     * Applies a move to the board and returns the state after the move.
     * @param move The move to apply. Must be a valid move.
     * @return A new State representing the game after the move is played.
     */

    public State afterMove(Move move) {
        assert moveIsValid(move) : "afterMove called with an invalid move " + move;
        State newState = new State(this);
        boolean copy = true;
        if (Math.abs((move.tox - move.fromx)) > 1) copy = false;
        if (Math.abs((move.toy - move.fromy)) > 1) copy = false;
        if (!copy) newState.board[move.fromx][move.fromy] = 0;
        newState.setBoard(move.tox,move.toy, whoseTurn());
        for (int dx=-1; dx<=1; dx++) {
            for (int dy=-1; dy<=1; dy++) {
                if (State.inBounds(move.tox + dx, move.toy + dy)) {
                    if (newState.pieceAt(move.tox+dx,move.toy+dy) != 0) {
                       newState.setBoard(move.tox+dx,move.toy+dy,whoseTurn());
                    }
                }
            }
        }
        if (whoseTurn == 1) newState.setTurn(2); else newState.setTurn(1);
        return newState;
    }

    /**
     * Gets a list of all valid moves from this state.
     * This is cached for efficiency.
     * @return ArrayList of Move objects representing all valid moves.
     */
    public ArrayList<Move> validMoves() {
        if (dirtyBoard) {
            validMoves = new ArrayList<>();
            for (int x=0; x<XSIZE; x++) {
                for (int y=0; y<YSIZE; y++) {
                    if (pieceAt(x,y) == whoseTurn()) {
                        for (int dx=-2; dx<=2; dx++) {
                            for (int dy=-2; dy<=2; dy++) {
                                if (State.inBounds(x + dx, y + dy)) {
                                    if (pieceAt(x+dx,y+dy) == 0) {
                                        validMoves.add(new Move(x,y,x+dx,y+dy));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            dirtyBoard = false;
        }
        return validMoves;
    }


}
