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

    /** Оценка сошлась, когда за проход ни одно U не сдвинулось больше этого */
    public static final double EPSILON = 1e-9;
    /** Предел проходов оценки: стратегия может никогда не дойти до выхода */
    public static final int MAX_SWEEPS = 10_000;

    private Bellman() {
    }

    /** Полезность стратегии U^π с нуля */
    public static Evaluation evaluate(Mdp mdp, Policy policy) {
        return evaluate(mdp, policy, Utilities.initial(mdp));
    }

    /** Оценка стратегии: U_{i+1}(s) = R(s) + γ·Σ T(s,π(s),s')·U_i(s') до схождения, начиная с from */
    static Evaluation evaluate(Mdp mdp, Policy policy, Utilities from) {
        double gamma = mdp.settings().gamma();
        Utilities u = from;
        int sweeps = 0;
        double delta;
        do {
            Map<Cell, Double> next = new HashMap<>();
            for (Cell s : mdp.states()) {
                next.put(s, mdp.terminal(s) ? mdp.reward(s)
                        : mdp.reward(s) + gamma * expected(mdp, s, policy.get(s), u));
            }
            Utilities nu = new Utilities(next);
            delta = nu.maxDiff(u);
            u = nu;
            sweeps++;
        } while (delta >= EPSILON && sweeps < MAX_SWEEPS);
        return new Evaluation(u, sweeps, delta < EPSILON);
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
