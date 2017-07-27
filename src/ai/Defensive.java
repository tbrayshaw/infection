package ai;

import model.Move;
import model.State;


public class Defensive extends MinMaxingAI {
    public Defensive() {
        super(2);
    }

    @Override
    public int heuristic(State toBoard, int us, int them) {
        return (toBoard.countPieces(us));
    }
}
