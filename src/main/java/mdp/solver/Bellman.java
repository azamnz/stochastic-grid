package mdp.solver;

import mdp.model.Cell;
import mdp.model.Mdp;
import mdp.model.Move;
import mdp.model.Outcome;

import java.util.HashMap;
import java.util.Map;

/** Части уравнения Беллмана */
public final class Bellman {

    /** Ход лучше, только если выигрывает больше этого: на равенствах без дрожания из-за округлений */
    static final double TIE = 1e-12;

    private Bellman() {
    }

    /** Σ T(s,a,s')·U(s') */
    public static double expected(Mdp mdp, Cell s, Move a, Utilities u) {
        double sum = 0;
        for (Outcome o : mdp.outcomes(s, a)) {
            sum += o.probability() * u.get(o.to());
        }
        return sum;
    }

    /** Лучший ход; при равенстве первый по порядку вверх, налево, вниз, направо */
    static Move best(Mdp mdp, Cell s, Utilities u) {
        Move best = Move.values()[0];
        double bestValue = expected(mdp, s, best, u);
        for (Move a : Move.values()) {
            double v = expected(mdp, s, a, u);
            if (v > bestValue + TIE) {
                best = a;
                bestValue = v;
            }
        }
        return best;
    }

    /** Улучшение стратегии: в каждой клетке лучший ход по U */
    static Policy improve(Mdp mdp, Utilities u) {
        Map<Cell, Move> moves = new HashMap<>();
        for (Cell s : mdp.states()) {
            if (!mdp.terminal(s)) {
                moves.put(s, best(mdp, s, u));
            }
        }
        return new Policy(moves);
    }
}
