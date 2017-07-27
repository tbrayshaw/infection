package ai;

import model.Move;
import model.State;

import java.util.ArrayList;
import java.util.Random;

public class Dumbass implements AI {

    Random random;

    public Dumbass() {
        random = new Random();
    }


    @Override
    public Move nextMove(State board) {
        /* Get list of valid moves. */
        ArrayList<Move> moves = board.validMoves();

        /* Pick one at random. */
        int index;
        if (moves.size() == 1) index = 0;
            else index = random.nextInt(moves.size()-1);
        return moves.get(index);
    }
}
