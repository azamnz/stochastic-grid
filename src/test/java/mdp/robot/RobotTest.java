package mdp.robot;

import mdp.model.Cell;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Move;
import mdp.model.Outcome;
import mdp.model.Settings;
import mdp.solver.Algorithm;
import mdp.solver.Policy;
import mdp.solver.Solver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotTest {

    private static Policy solved(Mdp mdp) {
        Solver s = Algorithm.VALUE.create(mdp);
        s.solve();
        return s.current().policy();
    }

    private static Robot run(Mdp mdp, Policy policy, long seed) {
        Robot r = new Robot(mdp, policy, seed);
        while (!r.finished() && !r.stopped()) {
            r.step();
        }
        return r;
    }

    @Test
    void withoutWindFollowsArrowsOfPdf() {
        Mdp sure = new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 1, 1));
        Robot r = run(sure, solved(new Mdp(GridWorld.pdfExample(), Settings.defaults())), 1);
        assertEquals(List.of(new Cell(1, 2), new Cell(1, 3), new Cell(2, 3), new Cell(3, 3), new Cell(4, 3)),
                r.journal().stream().map(RobotStep::to).toList());
        assertTrue(r.finished());
        assertEquals(new Cell(4, 3), r.position());
        assertEquals(5, r.steps());
        // R пяти неконечных клеток, начиная со старта, + 1 на выходе
        assertEquals(-0.2 + 1, r.total(), 1e-12);
        assertTrue(r.journal().stream().allMatch(s -> s.wanted() == s.actual()));
    }

    @Test
    void discountWeighsLaterRewardsLess() {
        Mdp sure = new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 0.5, 1));
        Robot r = run(sure, solved(new Mdp(GridWorld.pdfExample(), Settings.defaults())), 1);
        double expected = -0.04 * (1 + 0.5 + 0.25 + 0.125 + 0.0625) + Math.pow(0.5, 5);
        assertEquals(expected, r.total(), 1e-12);
    }

    @Test
    void outcomeIsPickedByCumulativeProbability() {
        List<Outcome> o = List.of(new Outcome(Move.UP, new Cell(1, 2), 0.8),
                new Outcome(Move.LEFT, new Cell(1, 1), 0.1), new Outcome(Move.RIGHT, new Cell(2, 1), 0.1));
        assertEquals(Move.UP, Robot.pick(o, 0.0).move());
        assertEquals(Move.UP, Robot.pick(o, 0.79).move());
        assertEquals(Move.LEFT, Robot.pick(o, 0.8).move());
        assertEquals(Move.LEFT, Robot.pick(o, 0.89).move());
        assertEquals(Move.RIGHT, Robot.pick(o, 0.9).move());
        assertEquals(Move.RIGHT, Robot.pick(o, 0.9999999999).move());
    }

    @Test
    void roundingShortfallFallsToLastOutcome() {
        List<Outcome> o = List.of(new Outcome(Move.UP, new Cell(1, 2), 0.5),
                new Outcome(Move.RIGHT, new Cell(2, 1), 0.4999999));
        assertEquals(Move.RIGHT, Robot.pick(o, 0.99999995).move());
    }

    @Test
    void sameSeedSameWalk() {
        Mdp mdp = new Mdp(GridWorld.pdfExample(), Settings.defaults());
        Policy p = solved(mdp);
        assertEquals(run(mdp, p, 42).journal(), run(mdp, p, 42).journal());
    }

    @Test
    void windBlowsSomewhereAndBumpKeepsPlace() {
        Mdp mdp = new Mdp(GridWorld.pdfExample(), Settings.defaults());
        Policy p = solved(mdp);
        boolean slipped = false;
        for (long seed = 1; seed <= 50; seed++) {
            for (RobotStep s : run(mdp, p, seed).journal()) {
                slipped |= s.wanted() != s.actual();
                Cell next = s.from().move(s.actual());
                boolean blocked = !mdp.world().inside(next) || next.equals(new Cell(2, 2));
                assertEquals(blocked ? s.from() : next, s.to());
            }
        }
        assertTrue(slipped);
    }

    @Test
    void averageWalkEqualsUtilityOfStart() {
        // U(1,1) = 0.705: в среднем путь робота приносит столько же
        Mdp mdp = new Mdp(GridWorld.pdfExample(), Settings.defaults());
        Policy p = solved(mdp);
        double sum = 0;
        int n = 4000;
        for (long seed = 1; seed <= n; seed++) {
            sum += run(mdp, p, seed).total();
        }
        assertEquals(0.705, sum / n, 0.04);
    }

    @Test
    void endlessWalkIsStopped() {
        GridWorld noExits = GridWorld.pdfExample().cycle(new Cell(4, 3)).cycle(new Cell(4, 3))
                .cycle(new Cell(4, 2));
        Mdp mdp = new Mdp(noExits, Settings.defaults());
        Robot r = run(mdp, solved(mdp), 1);
        assertFalse(r.finished());
        assertTrue(r.stopped());
        assertEquals(Robot.MAX_STEPS, r.steps());
        r.step();
        assertEquals(Robot.MAX_STEPS, r.steps());
    }

    @Test
    void finishedRobotDoesNotMove() {
        Mdp sure = new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 1, 1));
        Robot r = run(sure, solved(sure), 1);
        double total = r.total();
        r.step();
        assertEquals(5, r.steps());
        assertEquals(total, r.total());
    }

    @Test
    void startsAtStartWithStartReward() {
        Mdp mdp = new Mdp(GridWorld.pdfExample().withStart(new Cell(3, 1)), Settings.defaults());
        Robot r = new Robot(mdp, solved(mdp), 1);
        assertEquals(new Cell(3, 1), r.position());
        assertEquals(-0.04, r.total(), 1e-12);
        assertEquals(0, r.steps());
        assertFalse(r.finished());
    }

    @Test
    void journalLineReadsLikeStory() {
        RobotStep s = new RobotStep(3, new Cell(1, 2), Move.UP, Move.RIGHT, new Cell(1, 2), -0.04, 0.12);
        assertEquals("Шаг 3 · (1,2) · хочу вверх · снесло направо · упёрся в стену, остался в (1,2) · R = -0.040 · сумма 0.120",
                s.toString());
        RobotStep ok = new RobotStep(1, new Cell(1, 1), Move.UP, Move.UP, new Cell(1, 2), -0.04, -0.08);
        assertEquals("Шаг 1 · (1,1) · хочу вверх · иду вверх · пришёл в (1,2) · R = -0.040 · сумма -0.080", ok.toString());
    }
}
