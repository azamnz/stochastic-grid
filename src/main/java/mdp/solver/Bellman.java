package mdp.solver;

import mdp.model.Cell;
import mdp.model.Mdp;
import mdp.model.Move;
import mdp.model.Outcome;

import java.util.HashMap;
import java.util.Map;

/** Части уравнения Беллмана */
public final class Bellman {

    /** Лучший ход должен выигрывать больше этого, иначе остаётся прежний: без дрожания на равенствах */
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

    /** Лучший ход; при равенстве остаётся keep (или первый по порядку, если keep нет) */
    static Move best(Mdp mdp, Cell s, Utilities u, Move keep) {
        Move best = keep == null ? Move.values()[0] : keep;
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
    static Policy improve(Mdp mdp, Utilities u, Policy current) {
        Map<Cell, Move> moves = new HashMap<>();
        for (Cell s : mdp.states()) {
            if (!mdp.terminal(s)) {
                moves.put(s, best(mdp, s, u, current == null ? null : current.get(s)));
            }
        }
        return new Policy(moves);
    }
}
