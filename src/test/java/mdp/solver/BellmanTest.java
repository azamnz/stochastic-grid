package mdp.solver;

import mdp.model.Cell;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Move;
import mdp.model.Settings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BellmanTest {

    private final Mdp pdf = new Mdp(GridWorld.pdfExample(), Settings.defaults());

    private static double round3(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    @Test
    void evaluationOfOptimalPolicyGivesPdfTable() {
        Solver vi = Algorithm.VALUE.create(pdf);
        vi.solve();
        Evaluation e = Bellman.evaluate(pdf, vi.current().policy());
        assertTrue(e.converged());
        assertEquals(0.705, round3(e.utilities().get(new Cell(1, 1))));
        assertEquals(0.918, round3(e.utilities().get(new Cell(3, 3))));
        assertEquals(0.660, round3(e.utilities().get(new Cell(3, 2))));
        assertEquals(0.388, round3(e.utilities().get(new Cell(4, 1))));
        assertEquals(1.0, e.utilities().get(new Cell(4, 3)));
    }

    @Test
    void evaluationSolvesEquationForGivenArrows() {
        // «всё вверх» не оптимальна: если оценка возьмёт лучший ход вместо стрелки, уравнение не сойдётся
        Policy up = new PolicyIteration(pdf).current().policy();
        Evaluation e = Bellman.evaluate(pdf, up);
        assertTrue(e.converged());
        assertTrue(e.sweeps() > 1);
        for (Cell s : pdf.states()) {
            double expected = pdf.terminal(s) ? pdf.reward(s)
                    : pdf.reward(s) + pdf.settings().gamma() * Bellman.expected(pdf, s, Move.UP, e.utilities());
            assertEquals(expected, e.utilities().get(s), 1e-6, s.toString());
        }
    }

    @Test
    void endlessPolicyIsNotConverged() {
        GridWorld noExits = GridWorld.pdfExample().cycle(new Cell(4, 3)).cycle(new Cell(4, 3))
                .cycle(new Cell(4, 2));
        Mdp mdp = new Mdp(noExits, Settings.defaults());
        Evaluation e = Bellman.evaluate(mdp, new PolicyIteration(mdp).current().policy());
        assertFalse(e.converged());
        assertEquals(Bellman.MAX_SWEEPS, e.sweeps());
    }

    /** Ходы без сноса: значение хода — U клетки, куда он ведёт */
    private final Mdp sure = new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 1, 1));

    private Utilities with(double up, double right) {
        Map<Cell, Double> u = new HashMap<>();
        sure.states().forEach(s -> u.put(s, 0.0));
        u.put(new Cell(1, 2), up);
        u.put(new Cell(2, 1), right);
        return new Utilities(u);
    }

    @Test
    void tieGoesToFirstMoveInPdfOrder() {
        assertEquals(Move.UP, Bellman.best(sure, new Cell(1, 1), with(0.5, 0.5)));
    }

    @Test
    void laterMoveWinsWhenClearlyBetter() {
        assertEquals(Move.RIGHT, Bellman.best(sure, new Cell(1, 1), with(0.5, 0.6)));
        assertEquals(Move.UP, Bellman.best(sure, new Cell(1, 1), with(0.6, 0.5)));
    }

    @Test
    void roundingNoiseIsNotABetterMove() {
        assertEquals(Move.UP, Bellman.best(sure, new Cell(1, 1), with(0.5, 0.5 + 1e-14)));
    }

    @Test
    void allEqualAtStartMeansUpEverywhere() {
        Snapshot first = new ValueIteration(new Mdp(GridWorld.pdfExample(), Settings.defaults())).current();
        assertEquals(Move.UP, first.policy().get(new Cell(1, 1)));
    }
}
