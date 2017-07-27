package ai;

import model.Move;
import model.State;

public class Aggressive extends MinMaxingAI {
    public Aggressive() {
        super(2);
    }

    @Override
    public int heuristic(State toBoard, int us, int them) {
        return 1000-(toBoard.countPieces(them));
    }
}
