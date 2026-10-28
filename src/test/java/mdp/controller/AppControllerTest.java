package mdp.controller;

import mdp.model.Cell;
import mdp.model.CellKind;
import mdp.model.GridSize;
import mdp.model.GridWorld;
import mdp.model.Move;
import mdp.model.Settings;
import mdp.solver.Algorithm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppControllerTest {

    private static final long SECOND = 1_000_000_000L;

    @Test
    void startsWithPdfExampleNotSolved() {
        AppController c = new AppController();
        assertEquals(4, c.world().width());
        assertEquals(3, c.world().height());
        assertEquals(Settings.defaults(), c.settings());
        assertEquals(Algorithm.VALUE, c.algorithm());
        assertEquals(0, c.solver().current().iteration());
        assertEquals(new Cell(1, 1), c.robot().position());
        assertFalse(c.isRunning());
        assertNull(c.message());
    }

    @Test
    void iterateDoesOneIteration() {
        AppController c = new AppController();
        c.iterate();
        c.iterate();
        assertEquals(2, c.solver().current().iteration());
    }

    @Test
    void solveGoesToEnd() {
        AppController c = new AppController();
        c.solve();
        assertTrue(c.solver().converged());
        assertEquals(Move.UP, c.solver().current().policy().get(new Cell(1, 1)));
    }

    @Test
    void restartForgetsIterations() {
        AppController c = new AppController();
        c.solve();
        c.restart();
        assertEquals(0, c.solver().current().iteration());
    }

    @Test
    void algorithmSwitchStartsOver() {
        AppController c = new AppController();
        c.solve();
        c.setAlgorithm(Algorithm.POLICY);
        assertEquals(Algorithm.POLICY, c.algorithm());
        assertEquals(0, c.solver().current().iteration());
        c.solve();
        assertTrue(c.solver().current().iteration() < 10);
    }

    @Test
    void settingsApplyAndStartOver() {
        AppController c = new AppController();
        c.solve();
        c.applySettings(new Settings(-2, 1, 0.8));
        assertEquals(-2, c.settings().stepReward());
        assertEquals(0, c.solver().current().iteration());
        c.solve();
        assertEquals(Move.RIGHT, c.solver().current().policy().get(new Cell(3, 2)));
    }

    @Test
    void settingsSurviveExampleOnlyWhenGridChanges() {
        AppController c = new AppController();
        c.applySettings(new Settings(-2, 1, 0.8));
        c.newGrid(new GridSize(6, 5));
        assertEquals(-2, c.settings().stepReward());
        assertEquals(new GridSize(6, 5), c.size());
        assertEquals(6, c.world().width());
        assertEquals(CellKind.EXIT, c.world().kind(new Cell(6, 5)));
        c.pdfExample();
        assertEquals(Settings.defaults(), c.settings());
        assertEquals(GridSize.defaults(), c.size());
        assertEquals(4, c.world().width());
    }

    @Test
    void clickCyclesCellAndStartsOver() {
        AppController c = new AppController();
        c.solve();
        assertTrue(c.clickCell(new Cell(3, 1)));
        assertEquals(CellKind.WALL, c.world().kind(new Cell(3, 1)));
        assertEquals(0, c.solver().current().iteration());
        assertNull(c.message());
    }

    @Test
    void clickOnStartIsRefusedWithMessage() {
        AppController c = new AppController();
        GridWorld before = c.world();
        assertFalse(c.clickCell(new Cell(1, 1)));
        assertSame(before, c.world());
        assertTrue(c.message().contains("старт"));
        assertTrue(c.clickCell(new Cell(3, 1)));
        assertNull(c.message(), "удачное действие убирает старое сообщение");
    }

    @Test
    void clickOutsideIsIgnored() {
        AppController c = new AppController();
        GridWorld before = c.world();
        assertFalse(c.clickCell(new Cell(5, 1)));
        assertFalse(c.moveStart(new Cell(0, 0)));
        assertSame(before, c.world());
    }

    @Test
    void startMovesOnlyToEmptyCell() {
        AppController c = new AppController();
        assertFalse(c.moveStart(new Cell(2, 2)));
        assertTrue(c.message().contains("пуст"));
        assertFalse(c.moveStart(new Cell(4, 3)));
        assertTrue(c.moveStart(new Cell(3, 1)));
        assertEquals(new Cell(3, 1), c.world().start());
        assertEquals(new Cell(3, 1), c.robot().position());
        assertNull(c.message());
    }

    @Test
    void robotStepsAndFollowsCurrentPolicy() {
        AppController c = new AppController();
        c.solve();
        c.robotStep();
        assertEquals(1, c.robot().steps());
        assertEquals(c.solver().current().policy().get(new Cell(1, 1)), c.robot().journal().getFirst().wanted());
    }

    @Test
    void anyChangeSendsRobotBackToStart() {
        AppController c = new AppController();
        c.solve();
        c.robotStep();
        c.iterate();
        assertEquals(0, c.robot().steps(), "решение уже сошлось, но робот всё равно на старте");
        c.robotStep();
        c.restart();
        assertEquals(0, c.robot().steps());
        c.robotStep();
        c.clickCell(new Cell(3, 1));
        assertEquals(0, c.robot().steps());
        c.robotStep();
        c.solve();
        assertEquals(0, c.robot().steps());
    }

    @Test
    void sameSeedRepeatsWalkAndNewWalkChangesSeed() {
        AppController c = new AppController();
        c.solve();
        for (int i = 0; i < 6; i++) {
            c.robotStep();
        }
        var first = c.robot().journal();
        c.restart();
        c.solve();
        for (int i = 0; i < 6; i++) {
            c.robotStep();
        }
        assertEquals(first, c.robot().journal());
        long seed = c.seed();
        c.newWalk();
        assertEquals(seed + 1, c.seed());
        assertEquals(0, c.robot().steps());
    }

    @Test
    void runningMakesStepsBySpeed() {
        AppController c = new AppController();
        c.solve();
        c.setSpeed(4);
        c.startRobot();
        c.tick(0);
        assertEquals(0, c.robot().steps(), "первый кадр только запоминает время");
        c.tick(SECOND / 2);
        assertEquals(2, c.robot().steps());
    }

    /** Поле без выходов: робот ходит, пока не остановят */
    private static AppController endless() {
        AppController c = new AppController();
        c.clickCell(new Cell(4, 3));
        c.clickCell(new Cell(4, 3));
        c.clickCell(new Cell(4, 2));
        c.solve();
        return c;
    }

    @Test
    void longFreezeCountsAsOneSecond() {
        AppController c = endless();
        c.setSpeed(4);
        c.startRobot();
        c.tick(0);
        c.tick(60 * SECOND);
        assertEquals(4, c.robot().steps());
    }

    @Test
    void timeGoingBackIsIgnored() {
        AppController c = endless();
        c.setSpeed(4);
        c.startRobot();
        c.tick(10 * SECOND);
        c.tick(5_300_000_000L);
        c.tick(5_800_000_000L);
        assertEquals(2, c.robot().steps(), "время назад — как новый отсчёт, без долга");
    }

    @Test
    void pauseTimeIsNotTurnedIntoSteps() {
        AppController c = endless();
        c.setSpeed(4);
        c.startRobot();
        c.tick(0);
        c.tick(SECOND / 2);
        c.pause();
        c.startRobot();
        c.tick(10 * SECOND);
        c.tick(10 * SECOND + SECOND / 4);
        assertEquals(3, c.robot().steps());
    }

    @Test
    void leftoverTimeBeforePauseIsDropped() {
        AppController c = endless();
        c.setSpeed(4);
        c.startRobot();
        c.tick(0);
        c.tick(3 * SECOND / 10);
        assertEquals(1, c.robot().steps());
        c.pause();
        c.startRobot();
        c.tick(10 * SECOND);
        c.tick(10 * SECOND + 2 * SECOND / 10);
        assertEquals(1, c.robot().steps(), "0.2 с до паузы + 0.2 с после — ещё не шаг");
    }

    @Test
    void leftoverTimeBetweenFramesIsKept() {
        AppController c = endless();
        c.setSpeed(4);
        c.startRobot();
        c.tick(0);
        for (int i = 1; i <= 10; i++) {
            c.tick(i * 15 * SECOND / 100);
        }
        assertEquals(6, c.robot().steps(), "кадры по 0.15 с: за 1.5 с шесть шагов");
    }

    @Test
    void stoppedRobotCannotStart() {
        AppController c = endless();
        while (!c.robot().stopped()) {
            c.robotStep();
        }
        c.startRobot();
        assertFalse(c.isRunning());
    }

    @Test
    void runningStopsAtStepLimit() {
        AppController c = endless();
        c.setSpeed(AppController.MAX_SPEED);
        c.startRobot();
        long t = 0;
        for (int i = 0; i < 100 && c.isRunning(); i++) {
            c.tick(t);
            t += SECOND;
        }
        assertTrue(c.robot().stopped());
        assertFalse(c.isRunning());
    }

    @Test
    void runningStopsAtExit() {
        AppController c = new AppController();
        c.solve();
        c.setSpeed(AppController.MAX_SPEED);
        c.startRobot();
        long t = 0;
        for (int i = 0; i < 100 && c.isRunning(); i++) {
            c.tick(t);
            t += SECOND;
        }
        assertTrue(c.robot().finished());
        assertFalse(c.isRunning());
        c.startRobot();
        assertFalse(c.isRunning(), "дошёл — старт ничего не делает");
    }

    @Test
    void buttonsPauseRobot() {
        AppController c = new AppController();
        c.startRobot();
        c.robotStep();
        assertFalse(c.isRunning());
        c.toggleRobot();
        assertTrue(c.isRunning());
        c.iterate();
        assertFalse(c.isRunning());
        c.toggleRobot();
        c.toggleRobot();
        assertFalse(c.isRunning());
    }

    @Test
    void pausedDoesNothing() {
        AppController c = new AppController();
        c.tick(0);
        c.tick(SECOND);
        assertEquals(0, c.robot().steps());
    }

    @Test
    void speedClamped() {
        AppController c = new AppController();
        c.setSpeed(0);
        assertEquals(AppController.MIN_SPEED, c.getSpeed());
        c.setSpeed(Integer.MAX_VALUE);
        assertEquals(AppController.MAX_SPEED, c.getSpeed());
        c.setSpeed(7);
        assertEquals(7, c.getSpeed());
    }

    @Test
    void differentSeedsGiveDifferentWalksSomewhere() {
        AppController c = new AppController();
        c.solve();
        var walks = new java.util.HashSet<Object>();
        for (int i = 0; i < 10; i++) {
            while (!c.robot().finished()) {
                c.robotStep();
            }
            walks.add(c.robot().journal());
            c.newWalk();
        }
        assertNotEquals(1, walks.size());
    }
}
