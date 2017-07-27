package model;
/**
 * A Move in the game.
 */
public class Move {

    public int fromx, fromy, tox, toy;

    public Move(int _fromx, int _fromy, int _tox, int _toy) {
        assert State.inBounds(_fromx,_fromy);
        assert State.inBounds(_tox,_toy);

        fromx = _fromx;
        fromy = _fromy;
        tox = _tox;
        toy = _toy;
    }

    @Override
    public String toString() {
        return fromx + "," + fromy + " -> " + tox + "," + toy;
    }

}
