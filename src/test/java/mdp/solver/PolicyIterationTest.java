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
        assertEquals(Phase.EVALUATE, s.phase());
        assertEquals(1, s.iteration());
    }

    @Test
    void evaluationKeepsArrowsAndImprovementKeepsNumbers() {
        PolicyIteration pi = new PolicyIteration(pdf);
        pi.step();
        Snapshot evaluated = pi.current();
        for (Cell s : pdf.states()) {
            assertEquals(pdf.terminal(s) ? null : Move.UP, evaluated.policy().get(s), "оценка не трогает стрелки " + s);
        }
        assertEquals(0, evaluated.changed());
        pi.step();
        Snapshot improved = pi.current();
        assertEquals(Phase.IMPROVE, improved.phase());
        assertEquals(1, improved.iteration());
        assertEquals(6, improved.changed());
        assertEquals(0, improved.sweeps());
        assertEquals(0.0, improved.delta());
        for (Cell s : pdf.states()) {
            assertEquals(evaluated.utilities().get(s), improved.utilities().get(s), "улучшение не трогает числа " + s);
        }
        assertEquals(Move.RIGHT, improved.policy().get(new Cell(1, 3)));
        pi.step();
        assertEquals(Phase.EVALUATE, pi.current().phase());
        assertEquals(2, pi.current().iteration());
        assertEquals(improved.policy().get(new Cell(1, 3)), pi.current().policy().get(new Cell(1, 3)));
    }

    @Test
    void notDoneRightAfterEvaluation() {
        PolicyIteration pi = new PolicyIteration(pdf);
        pi.solve();
        int iterations = pi.current().iteration();
        PolicyIteration again = new PolicyIteration(pdf);
        for (int n = 0; n < 100 && (again.current().iteration() < iterations || again.current().phase() != Phase.EVALUATE); n++) {
            again.step();
        }
        assertEquals(Phase.EVALUATE, again.current().phase());
        assertFalse(again.converged(), "после оценки ещё нужно проверить стрелки");
        again.step();
        assertTrue(again.converged());
    }

    @Test
    void endsWhenNoArrowChangesInFewIterations() {
        PolicyIteration pi = new PolicyIteration(pdf);
        pi.solve();
        assertTrue(pi.converged());
        assertEquals(0, pi.current().changed());
        assertEquals(Phase.IMPROVE, pi.current().phase());
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
        assertEquals(Phase.IMPROVE, pi.current().phase(), "последняя итерация доведена до улучшения");
    }
}
