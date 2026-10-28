package mdp.controller;

import mdp.model.Cell;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Settings;
import mdp.robot.Robot;
import mdp.solver.Algorithm;
import mdp.solver.Solver;
import mdp.solver.ValueIteration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextsTest {

    private final Mdp pdf = new Mdp(GridWorld.pdfExample(), Settings.defaults());

    @Test
    void numbersLikeInPdfTable() {
        assertEquals("0.812", Texts.number(0.81163));
        assertEquals("-0.040", Texts.number(-0.04));
        assertEquals("0.000", Texts.number(-0.0001));
        assertEquals("+1", Texts.signed(1));
        assertEquals("-1", Texts.signed(-1));
        assertEquals("+0.5", Texts.signed(0.5));
    }

    @Test
    void plurals() {
        assertEquals("1 итерация", Texts.count(1, "итерация", "итерации", "итераций"));
        assertEquals("3 итерации", Texts.count(3, "итерация", "итерации", "итераций"));
        assertEquals("5 итераций", Texts.count(5, "итерация", "итерации", "итераций"));
        assertEquals("11 итераций", Texts.count(11, "итерация", "итерации", "итераций"));
        assertEquals("12 итераций", Texts.count(12, "итерация", "итерации", "итераций"));
        assertEquals("21 итерация", Texts.count(21, "итерация", "итерации", "итераций"));
        assertEquals("22 итерации", Texts.count(22, "итерация", "итерации", "итераций"));
        assertEquals("111 итераций", Texts.count(111, "итерация", "итерации", "итераций"));
        assertEquals("0 итераций", Texts.count(0, "итерация", "итерации", "итераций"));
    }

    @Test
    void valueIterationLines() {
        Solver vi = Algorithm.VALUE.create(pdf);
        vi.step();
        assertEquals(List.of("Итерация 0 · начало: U = 0, в выходах их награда",
                        "Итерация 1 · наибольшее изменение U 0.760 · стрелок поменялось 2"),
                Texts.journal(Algorithm.VALUE, vi.history()));
    }

    @Test
    void policyIterationLines() {
        Solver pi = Algorithm.POLICY.create(pdf);
        pi.step();
        List<String> lines = Texts.journal(Algorithm.POLICY, pi.history());
        assertEquals("Итерация 0 · начальная стратегия: все стрелки вверх", lines.get(0));
        assertTrue(lines.get(1).matches("Итерация 1 · оценка стратегии за \\d+ проход(а|ов)? · наибольшее изменение U \\d\\.\\d{3} · стрелок поменялось \\d+"),
                lines.get(1));
    }

    @Test
    void tinyChangeShownInScientificForm() {
        Solver vi = Algorithm.VALUE.create(pdf);
        vi.solve();
        List<String> lines = Texts.journal(Algorithm.VALUE, vi.history());
        assertTrue(lines.getLast().matches(".*наибольшее изменение U \\d\\.\\de-0\\d .*"), lines.getLast());
    }

    @Test
    void longJournalIsShortenedInTheMiddle() {
        GridWorld noExits = GridWorld.pdfExample().cycle(new Cell(4, 3)).cycle(new Cell(4, 3)).cycle(new Cell(4, 2));
        Solver vi = Algorithm.VALUE.create(new Mdp(noExits, Settings.defaults()));
        vi.solve();
        List<String> lines = Texts.journal(Algorithm.VALUE, vi.history());
        assertEquals(Texts.JOURNAL_HEAD + 1 + Texts.JOURNAL_TAIL, lines.size());
        assertTrue(lines.get(0).startsWith("Итерация 0 "));
        assertEquals("… пропущено " + (ValueIteration.MAX_ITERATIONS + 1 - Texts.JOURNAL_HEAD - Texts.JOURNAL_TAIL) + " …",
                lines.get(Texts.JOURNAL_HEAD));
        assertTrue(lines.getLast().startsWith("Итерация " + ValueIteration.MAX_ITERATIONS + " "));
    }

    @Test
    void shortJournalIsWhole() {
        Solver vi = Algorithm.VALUE.create(pdf);
        vi.solve();
        assertEquals(vi.history().size(), Texts.journal(Algorithm.VALUE, vi.history()).size());
    }

    @Test
    void solverStatus() {
        Solver vi = Algorithm.VALUE.create(pdf);
        assertEquals("Итерация 0, решение ещё не сошлось", Texts.solverStatus(vi));
        vi.solve();
        assertEquals("Решено за 28 итераций", Texts.solverStatus(vi));
        GridWorld noExits = GridWorld.pdfExample().cycle(new Cell(4, 3)).cycle(new Cell(4, 3)).cycle(new Cell(4, 2));
        Solver bad = Algorithm.VALUE.create(new Mdp(noExits, Settings.defaults()));
        bad.solve();
        assertTrue(Texts.solverStatus(bad).startsWith("Не сошлось за 10000 итераций"));
    }

    @Test
    void robotStatus() {
        Solver vi = Algorithm.VALUE.create(new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 1, 1)));
        vi.solve();
        Robot r = new Robot(new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 1, 1)), vi.current().policy(), 1);
        assertEquals("Робот на старте (1,1)", Texts.robotStatus(r));
        r.step();
        assertEquals("Шаг 1, робот в (1,2), сумма -0.080", Texts.robotStatus(r));
        while (!r.finished()) {
            r.step();
        }
        assertEquals("Дошёл до выхода +1 в (4,3) за 5 шагов, сумма 0.800", Texts.robotStatus(r));
    }

    @Test
    void stoppedRobotStatus() {
        GridWorld noExits = GridWorld.pdfExample().cycle(new Cell(4, 3)).cycle(new Cell(4, 3)).cycle(new Cell(4, 2));
        Mdp mdp = new Mdp(noExits, Settings.defaults());
        Solver vi = Algorithm.VALUE.create(mdp);
        vi.solve();
        Robot r = new Robot(mdp, vi.current().policy(), 1);
        while (!r.stopped()) {
            r.step();
        }
        assertEquals("Остановлен: 1000 шагов, а до выхода не дошёл", Texts.robotStatus(r));
    }
}
