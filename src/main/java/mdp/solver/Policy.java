package mdp.solver;

import mdp.model.Cell;
import mdp.model.Move;

import java.util.Map;

/** Стратегия π(s): стрелка в каждой неконечной клетке */
public final class Policy {

    private final Map<Cell, Move> moves;

    Policy(Map<Cell, Move> moves) {
        this.moves = Map.copyOf(moves);
    }

    /** Стрелка или null в конечной клетке */
    public Move get(Cell s) {
        return moves.get(s);
    }

    /** Сколько стрелок отличается */
    int changes(Policy other) {
        int n = 0;
        for (Map.Entry<Cell, Move> e : moves.entrySet()) {
            if (e.getValue() != other.get(e.getKey())) {
                n++;
            }
        }
        return n;
    }
}
