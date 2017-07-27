
package ai;

import model.State;


public class Advanced extends MinMaxingAI {

    public Advanced() {
        super(3);
    }

    @Override
    public int heuristic(State toBoard, int us, int them) {
        // If less than 11 pieces and most of the board is free, play defensive.
        if ((toBoard.countPieces(us) <= 10) && (toBoard.countPieces(0) > 80)) {
            return 5000 + toBoard.countPieces(us);
        }
        // Aggressive with less/same pieces.
        if (toBoard.countPieces(them) >= toBoard.countPieces(us)) {
            return 1000 - toBoard.countPieces(them) - toBoard.countPieces(0);
        }
        // Defensive with more pieces.
        if ((toBoard.countPieces(them) < toBoard.countPieces(us)) 
        || (toBoard.countPieces(them) == toBoard.countPieces(us))) {
            return 1000 + toBoard.countPieces(us) - toBoard.countPieces(0) - toBoard.countPieces(them);
        }
        else return 1000;
    }
}
