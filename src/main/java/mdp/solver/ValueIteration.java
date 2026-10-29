package mdp.solver;

import mdp.model.Cell;
import mdp.model.Mdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Итерация по значениям: U(s) = R(s) + γ·max_a Σ T(s,a,s')·U(s') для всех клеток разом */
public final class ValueIteration implements Solver {

    public static final double EPSILON = 1e-6;
    public static final int MAX_ITERATIONS = 10_000;

    private final Mdp mdp;
    private final List<Snapshot> history = new ArrayList<>();

    public ValueIteration(Mdp mdp) {
        this.mdp = mdp;
        Utilities u = Utilities.initial(mdp);
        history.add(new Snapshot(0, u, Bellman.improve(mdp, u), Double.NaN, 0, 0, Phase.START));
    }

    @Override
    public void step() {
        if (converged() || exhausted()) {
            return;
        }
        Snapshot last = current();
        Utilities u = last.utilities();
        double gamma = mdp.settings().gamma();
        Map<Cell, Double> next = new HashMap<>();
        for (Cell s : mdp.states()) {
            next.put(s, mdp.terminal(s) ? mdp.reward(s)
                    : mdp.reward(s) + gamma * Bellman.expected(mdp, s, Bellman.best(mdp, s, u), u));
        }
        Utilities nu = new Utilities(next);
        Policy policy = Bellman.improve(mdp, nu);
        history.add(new Snapshot(last.iteration() + 1, nu, policy, nu.maxDiff(u), policy.changes(last.policy()), 1, Phase.VALUE));
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
        return current().delta() < EPSILON;
    }

    @Override
    public boolean exhausted() {
        return !converged() && current().iteration() >= MAX_ITERATIONS;
    }
}
