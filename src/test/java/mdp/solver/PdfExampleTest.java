package mdp.solver;

import mdp.model.Cell;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Move;
import mdp.model.Settings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static mdp.model.Move.LEFT;
import static mdp.model.Move.RIGHT;
import static mdp.model.Move.UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Оба алгоритма дают таблицу и стрелки из задания */
class PdfExampleTest {

    /** Таблица из задания, сверху вниз; в стене NaN */
    private static final double[][] TABLE = {
            {0.812, 0.868, 0.918, 1},
            {0.762, Double.NaN, 0.660, -1},
            {0.705, 0.655, 0.611, 0.388}};

    private static final Map<Cell, Move> ARROWS = Map.of(
            new Cell(1, 3), RIGHT, new Cell(2, 3), RIGHT, new Cell(3, 3), RIGHT,
            new Cell(1, 2), UP, new Cell(3, 2), UP,
            new Cell(1, 1), UP, new Cell(2, 1), LEFT, new Cell(3, 1), LEFT, new Cell(4, 1), LEFT);

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void tableAndArrowsMatchPdf(Algorithm algorithm) {
        Solver solver = algorithm.create(new Mdp(GridWorld.pdfExample(), Settings.defaults()));
        solver.solve();
        assertTrue(solver.converged());
        assertFalse(solver.exhausted());
        Snapshot last = solver.current();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                Cell cell = new Cell(c + 1, 3 - r);
                if (!Double.isNaN(TABLE[r][c])) {
                    assertEquals(TABLE[r][c], Math.round(last.utilities().get(cell) * 1000) / 1000.0, cell.toString());
                }
            }
        }
        ARROWS.forEach((cell, move) -> assertEquals(move, last.policy().get(cell), cell.toString()));
        assertNull(last.policy().get(new Cell(4, 3)));
        assertNull(last.policy().get(new Cell(4, 2)));
    }

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void fixedPointSatisfiesStartEquationFromPdf(Algorithm algorithm) {
        Mdp mdp = new Mdp(GridWorld.pdfExample(), Settings.defaults());
        Solver solver = algorithm.create(mdp);
        solver.solve();
        Utilities u = solver.current().utilities();
        double up = 0.8 * u.get(new Cell(1, 2)) + 0.1 * u.get(new Cell(2, 1)) + 0.1 * u.get(new Cell(1, 1));
        double left = 0.9 * u.get(new Cell(1, 1)) + 0.1 * u.get(new Cell(1, 2));
        double down = 0.9 * u.get(new Cell(1, 1)) + 0.1 * u.get(new Cell(2, 1));
        double right = 0.8 * u.get(new Cell(2, 1)) + 0.1 * u.get(new Cell(1, 2)) + 0.1 * u.get(new Cell(1, 1));
        double max = Math.max(Math.max(up, left), Math.max(down, right));
        assertEquals(-0.04 + max, u.get(new Cell(1, 1)), 1e-5);
        assertEquals(up, Bellman.expected(mdp, new Cell(1, 1), UP, u), 1e-12);
        assertEquals(left, Bellman.expected(mdp, new Cell(1, 1), LEFT, u), 1e-12);
    }
}
