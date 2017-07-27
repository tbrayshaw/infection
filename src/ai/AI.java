package ai;

import model.Move;
import model.State;

public interface AI {

    /**
     * Called when your AI should decide on its move.
     * @param board The current state of the board.
     * @return The move the AI wants to make.
     */
    Move nextMove(State board);


}
