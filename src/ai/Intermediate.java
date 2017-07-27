
package ai;

import java.util.ArrayList;
import model.Move;
import model.State;


public class Intermediate extends MinMaxingAI {
    
    public Intermediate() {
        super(3);
    }

    @Override
    public int heuristic(State toBoard, int us, int them) {
        int rating = 1000;
        // Defensive with less/same pieces.
        if (toBoard.countPieces(them) >= toBoard.countPieces(us)) {
            rating += (toBoard.countPieces(us) - toBoard.countPieces(0));
        }
        // Aggressive with more pieces.
        else if ((toBoard.countPieces(them) < toBoard.countPieces(us)) 
        || (toBoard.countPieces(them) == toBoard.countPieces(us))) {
            rating -= toBoard.countPieces(them);
        }
        // Aggressive when most of the board is free and holding more pieces.
        if ((toBoard.countPieces(0) >= 50) && 
            (toBoard.countPieces(us) > toBoard.countPieces(them))) {
            rating -= toBoard.countPieces(0);
        }
        // Defensive when most of the board is taken and holding more pieces.
        else if ((toBoard.countPieces(us) > toBoard.countPieces(them)) && 
                (toBoard.countPieces(0) < 50)) {
            rating += toBoard.countPieces(us) - toBoard.countPieces(0);
        }
        else rating = 1000 - toBoard.countPieces(them);
        
        return rating;
    }
}
