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

class BellmanTest {

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
