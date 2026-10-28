package mdp.solver;

import java.util.List;

/** Пошаговое решение: окно делает по одной итерации и показывает каждую */
public interface Solver {

    Snapshot current();

    List<Snapshot> history();

    boolean converged();

    /** Лимит итераций исчерпан, а решение не сошлось */
    boolean exhausted();

    /** Одна итерация; после конца ничего не делает */
    void step();

    default void solve() {
        while (!converged() && !exhausted()) {
            step();
        }
    }
}
