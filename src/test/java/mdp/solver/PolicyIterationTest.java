package mdp.solver;

import mdp.model.Cell;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Move;
import mdp.model.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyIterationTest {

    private final Mdp pdf = new Mdp(GridWorld.pdfExample(), Settings.defaults());

    @Test
    void startsWithAllArrowsUp() {
        Snapshot first = new PolicyIteration(pdf).current();
        assertEquals(0, first.iteration());
        for (Cell s : pdf.states()) {
            assertEquals(pdf.terminal(s) ? null : Move.UP, first.policy().get(s), s.toString());
        }
    }

    @Test
    void firstEvaluationIsUtilityOfAllUp() {
        PolicyIteration pi = new PolicyIteration(pdf);
        pi.step();
        Snapshot s = pi.current();
        // (4,1) при «всё вверх»: U = −0.04 + 0.8·(−1) + 0.1·U(3,1) + 0.1·U(4,1)
        double u41 = s.utilities().get(new Cell(4, 1));
        double u31 = s.utilities().get(new Cell(3, 1));
        assertEquals(-0.04 - 0.8 + 0.1 * u31 + 0.1 * u41, u41, 1e-6);
        assertTrue(s.sweeps() > 1);
        assertTrue(s.changed() > 0);
    }

    @Test
    void endsWhenNoArrowChangesInFewIterations() {
        PolicyIteration pi = new PolicyIteration(pdf);
        pi.solve();
        assertTrue(pi.converged());
        assertEquals(0, pi.current().changed());
        assertTrue(pi.current().iteration() < 10);
        int size = pi.history().size();
        pi.step();
        assertEquals(size, pi.history().size());
    }

    @Test
    void neverEndingWorldIsCutOff() {
        GridWorld noExits = GridWorld.pdfExample().cycle(new Cell(4, 3)).cycle(new Cell(4, 3))
                .cycle(new Cell(4, 2));
        PolicyIteration pi = new PolicyIteration(new Mdp(noExits, Settings.defaults()));
        pi.solve();
        // стрелки перестают меняться, но оценка не сходится: это не решение
        assertFalse(pi.converged());
        assertTrue(pi.exhausted());
        assertEquals(PolicyIteration.MAX_ITERATIONS, pi.current().iteration());
    }
}
