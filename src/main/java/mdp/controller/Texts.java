package mdp.controller;

import mdp.robot.Robot;
import mdp.solver.Algorithm;
import mdp.solver.Snapshot;
import mdp.solver.Solver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Строки для окна: числа, журнал решения, состояние робота */
public final class Texts {

    public static final int JOURNAL_HEAD = 20;
    public static final int JOURNAL_TAIL = 280;

    private Texts() {
    }

    /** Три знака, как в таблице задания; без «-0.000» */
    public static String number(double v) {
        return String.format(Locale.ROOT, "%.3f", Math.abs(v) < 0.0005 ? 0.0 : v);
    }

    /** Награда выхода со знаком: «+1», «-1», «+0.5» */
    public static String signed(double v) {
        String s = BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
        return v > 0 ? "+" + s : s;
    }

    /** Число со словом в нужной форме: 1 итерация, 3 итерации, 5 итераций */
    public static String count(int n, String one, String few, String many) {
        int tens = n % 100;
        int last = n % 10;
        String word = tens >= 11 && tens <= 14 ? many : last == 1 ? one : last >= 2 && last <= 4 ? few : many;
        return n + " " + word;
    }

    /** Строка на итерацию; длинный журнал сокращается в середине */
    public static List<String> journal(Algorithm algorithm, List<Snapshot> history) {
        List<String> lines = new ArrayList<>();
        int skipped = history.size() - JOURNAL_HEAD - JOURNAL_TAIL;
        for (int i = 0; i < history.size(); i++) {
            if (skipped > 0 && i == JOURNAL_HEAD) {
                lines.add("… пропущено " + skipped + " …");
                i += skipped - 1;
                continue;
            }
            lines.add(line(algorithm, history.get(i)));
        }
        return lines;
    }

    private static String line(Algorithm algorithm, Snapshot s) {
        String head = "Итерация " + s.iteration() + " · ";
        if (s.iteration() == 0) {
            return head + (algorithm == Algorithm.VALUE
                    ? "начало: U = 0, в выходах их награда" : "начальная стратегия: все стрелки вверх");
        }
        String eval = algorithm == Algorithm.POLICY
                ? "оценка стратегии за " + count(s.sweeps(), "проход", "прохода", "проходов") + " · " : "";
        return head + eval + "наибольшее изменение U " + delta(s.delta()) + " · стрелок поменялось " + s.changed();
    }

    private static String delta(double d) {
        return d >= 0.001 ? String.format(Locale.ROOT, "%.3f", d) : String.format(Locale.ROOT, "%.1e", d);
    }

    public static String solverStatus(Solver solver) {
        int it = solver.current().iteration();
        if (solver.converged()) {
            return "Решено за " + count(it, "итерацию", "итерации", "итераций");
        }
        if (solver.exhausted()) {
            return "Не сошлось за " + count(it, "итерацию", "итерации", "итераций")
                    + ": при γ = 1 так бывает, если из каких-то клеток не дойти до выхода";
        }
        return "Итерация " + it + ", решение ещё не сошлось";
    }

    public static String robotStatus(Robot robot) {
        int steps = robot.steps();
        String count = count(steps, "шаг", "шага", "шагов");
        if (robot.finished()) {
            return "Дошёл до выхода " + signed(robot.journal().getLast().reward()) + " в " + robot.position()
                    + " за " + count + ", сумма " + number(robot.total());
        }
        if (robot.stopped()) {
            return "Остановлен: " + count + ", а до выхода не дошёл";
        }
        if (steps == 0) {
            return "Робот на старте " + robot.position();
        }
        return "Шаг " + steps + ", робот в " + robot.position() + ", сумма " + number(robot.total());
    }
}
