package mdp.solver;

import mdp.model.Cell;
import mdp.model.CellKind;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Settings;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** На случайных полях оба алгоритма приходят к одним и тем же полезностям */
class AgreementTest {

    @Test
    void bothAlgorithmsAgreeOnRandomWorlds() {
        Random rnd = new Random(7);
        for (int n = 0; n < 150; n++) {
            GridWorld w = GridWorld.blank(2 + rnd.nextInt(5), 2 + rnd.nextInt(5));
            for (int k = 0; k < 6; k++) {
                Cell c = new Cell(1 + rnd.nextInt(w.width()), 1 + rnd.nextInt(w.height()));
                if (!c.equals(w.start())) {
                    for (int t = rnd.nextInt(4); t > 0; t--) {
                        w = w.cycle(c);
                    }
                }
            }
            Settings s = new Settings(-1 + 1.2 * rnd.nextDouble(), 0.5 + 0.45 * rnd.nextDouble(), rnd.nextDouble());
            Mdp mdp = new Mdp(w, s);
            Solver vi = Algorithm.VALUE.create(mdp);
            Solver pi = Algorithm.POLICY.create(mdp);
            vi.solve();
            pi.solve();
            assertTrue(vi.converged() && pi.converged(), "мир " + n);
            for (Cell c : mdp.states()) {
                assertEquals(vi.current().utilities().get(c), pi.current().utilities().get(c), 1e-3, "мир " + n + " " + c);
                if (mdp.world().kind(c) == CellKind.EXIT) {
                    assertEquals(mdp.reward(c), vi.current().utilities().get(c));
                }
            }
        }
    }
}
