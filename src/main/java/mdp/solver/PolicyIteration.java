package mdp.solver;

import mdp.model.Cell;
import mdp.model.Mdp;
import mdp.model.Move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Итерация по стратегиям: оценка U_{i+1}(s) = R(s) + γ·Σ T(s,π(s),s')·U_i(s') до схождения,
 * затем улучшение π; конец, когда стрелки не меняются
 */
public final class PolicyIteration implements Solver {

    public static final double EPSILON = 1e-9;
    public static final int MAX_SWEEPS = 10_000;
    public static final int MAX_ITERATIONS = 100;

    private final Mdp mdp;
    private final List<Snapshot> history = new ArrayList<>();
    /** Последняя оценка сошлась */
    private boolean evaluated;

    public PolicyIteration(Mdp mdp) {
        this.mdp = mdp;
        Map<Cell, Move> up = new HashMap<>();
        for (Cell s : mdp.states()) {
            if (!mdp.terminal(s)) {
                up.put(s, Move.UP);
            }
        }
        history.add(new Snapshot(0, Utilities.initial(mdp), new Policy(up), Double.NaN, 0, 0, Phase.START));
    }

    /** Шаги чередуются: оценка, улучшение, оценка… */
    @Override
    public void step() {
        if (converged() || exhausted()) {
            return;
        }
        if (current().phase() == Phase.EVALUATE) {
            improve();
        } else {
            evaluate();
        }
    }

    /** Оценка: числа для текущих стрелок; начинаем с прошлой U, так быстрее */
    private void evaluate() {
        Snapshot last = current();
        Policy policy = last.policy();
        double gamma = mdp.settings().gamma();
        Utilities u = last.utilities();
        int sweeps = 0;
        double delta;
        do {
            Map<Cell, Double> next = new HashMap<>();
            for (Cell s : mdp.states()) {
                next.put(s, mdp.terminal(s) ? mdp.reward(s)
                        : mdp.reward(s) + gamma * Bellman.expected(mdp, s, policy.get(s), u));
            }
            Utilities nu = new Utilities(next);
            delta = nu.maxDiff(u);
            u = nu;
            sweeps++;
        } while (delta >= EPSILON && sweeps < MAX_SWEEPS);
        evaluated = delta < EPSILON;
        history.add(new Snapshot(last.iteration() + 1, u, policy, u.maxDiff(last.utilities()), 0, sweeps,
                Phase.EVALUATE));
    }

    /** Улучшение: в каждой клетке лучший ход по только что посчитанным числам */
    private void improve() {
        Snapshot last = current();
        Policy improved = Bellman.improve(mdp, last.utilities());
        history.add(new Snapshot(last.iteration(), last.utilities(), improved, 0, improved.changes(last.policy()), 0,
                Phase.IMPROVE));
    }

    @Override
    public Snapshot current() {
        return history.getLast();
    }

    @Override
    public List<Snapshot> history() {
        return List.copyOf(history);
    }

    @Override
    public boolean converged() {
        return evaluated && current().phase() == Phase.IMPROVE && current().changed() == 0;
    }

    @Override
    public boolean exhausted() {
        return !converged() && current().phase() == Phase.IMPROVE && current().iteration() >= MAX_ITERATIONS;
    }
}
