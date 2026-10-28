package mdp.solver;

import mdp.model.Cell;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueIterationTest {

    private final Mdp pdf = new Mdp(GridWorld.pdfExample(), Settings.defaults());

    @Test
    void startsFromZerosAndExitRewards() {
        Snapshot first = new ValueIteration(pdf).current();
        assertEquals(0, first.iteration());
        assertEquals(0.0, first.utilities().get(new Cell(1, 1)));
        assertEquals(1.0, first.utilities().get(new Cell(4, 3)));
        assertEquals(-1.0, first.utilities().get(new Cell(4, 2)));
    }

    @Test
    void firstIterationByHand() {
        ValueIteration vi = new ValueIteration(pdf);
        vi.step();
        Snapshot s = vi.current();
        assertEquals(1, s.iteration());
        // (3,3): −0.04 + 0.8·1
        assertEquals(0.76, s.utilities().get(new Cell(3, 3)), 1e-12);
        // остальные: лучший ход никуда не ведёт, только плата за шаг
        assertEquals(-0.04, s.utilities().get(new Cell(1, 1)), 1e-12);
        assertEquals(-0.04, s.utilities().get(new Cell(3, 2)), 1e-12);
        assertEquals(-0.04, s.utilities().get(new Cell(4, 1)), 1e-12);
        assertEquals(1.0, s.utilities().get(new Cell(4, 3)));
        assertEquals(0.76, s.delta(), 1e-12);
        assertEquals(2, vi.history().size());
    }

    @Test
    void discountMultipliesNeighbourValue() {
        ValueIteration vi = new ValueIteration(new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 0.5, 0.8)));
        vi.step();
        assertEquals(-0.04 + 0.5 * 0.8, vi.current().utilities().get(new Cell(3, 3)), 1e-12);
    }

    @Test
    void stopsWhenChangeIsTiny() {
        ValueIteration vi = new ValueIteration(pdf);
        vi.solve();
        Snapshot last = vi.current();
        assertTrue(last.delta() < ValueIteration.EPSILON);
        assertTrue(vi.history().get(vi.history().size() - 2).delta() >= ValueIteration.EPSILON);
        int size = vi.history().size();
        vi.step();
        assertEquals(size, vi.history().size());
    }

    @Test
    void arrowsChangeCountedBetweenIterations() {
        ValueIteration vi = new ValueIteration(pdf);
        int total = 0;
        while (!vi.converged()) {
            vi.step();
            total += vi.current().changed();
        }
        assertTrue(total > 0);
        assertEquals(0, vi.current().changed());
    }

    @Test
    void neverEndingWorldIsCutOff() {
        GridWorld noExits = GridWorld.pdfExample().cycle(new Cell(4, 3)).cycle(new Cell(4, 3))
                .cycle(new Cell(4, 2));
        ValueIteration vi = new ValueIteration(new Mdp(noExits, Settings.defaults()));
        vi.solve();
        assertFalse(vi.converged());
        assertTrue(vi.exhausted());
        assertEquals(ValueIteration.MAX_ITERATIONS, vi.current().iteration());
    }
}
