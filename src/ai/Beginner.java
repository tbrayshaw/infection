
package ai;

import java.util.ArrayList;
import model.Move;
import model.State;


public class Beginner extends MinMaxingAI {

    public Beginner() {
        super(3);
    }
    
    @Override
    public int heuristic(State toBoard, int us, int them) {
        // Start defensive.
        if (toBoard.countPieces(0) > 75) return 1000+toBoard.countPieces(us);
        // Become aggressive (and careless).
        else if (toBoard.countPieces(0) < 75) return 1000+toBoard.countPieces(0)-toBoard.countPieces(them);
        else return 1000;
    }
    
}
