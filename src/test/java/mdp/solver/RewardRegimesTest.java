package mdp.solver;

import mdp.model.Cell;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Move;
import mdp.model.Settings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Как стратегия зависит от R(s): известные режимы для поля 4×3 */
class RewardRegimesTest {

    private static Snapshot solve(Algorithm a, double r, double gamma) {
        Solver s = a.create(new Mdp(GridWorld.pdfExample(), new Settings(r, gamma, 0.8)));
        s.solve();
        assertTrue(s.converged());
        return s.current();
    }

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void painfulLifeRushesIntoMinusOne(Algorithm a) {
        Snapshot s = solve(a, -2, 1);
        assertEquals(Move.RIGHT, s.policy().get(new Cell(3, 2)));
    }

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void cheapLifeAvoidsRisk(Algorithm a) {
        Snapshot s = solve(a, -0.01, 1);
        assertEquals(Move.LEFT, s.policy().get(new Cell(3, 2)));
        assertEquals(Move.DOWN, s.policy().get(new Cell(4, 1)));
    }

    @ParameterizedTest
    @EnumSource(Algorithm.class)
    void pleasantLifeNeverEnds(Algorithm a) {
        Snapshot s = solve(a, 0.5, 0.9);
        // остаться навсегда: 0.5 / (1 − 0.9) = 5
        assertEquals(5.0, s.utilities().get(new Cell(1, 1)), 1e-4);
        assertNotEquals(Move.RIGHT, s.policy().get(new Cell(3, 3)));
    }
}
