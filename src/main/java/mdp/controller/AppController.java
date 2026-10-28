package mdp.controller;

import mdp.model.Cell;
import mdp.model.CellKind;
import mdp.model.GridSize;
import mdp.model.GridWorld;
import mdp.model.Mdp;
import mdp.model.Settings;
import mdp.robot.Robot;
import mdp.solver.Algorithm;
import mdp.solver.Solver;

import java.util.Objects;

/**
 * Логика окна без JavaFX: решение по итерациям, правка поля, робот по таймеру.
 * Любая правка начинает решение заново и возвращает робота на старт
 */
public class AppController {

    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 50;
    public static final int DEFAULT_SPEED = 4;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private GridSize size = GridSize.defaults();
    private GridWorld world = GridWorld.pdfExample();
    private Settings settings = Settings.defaults();
    private Algorithm algorithm = Algorithm.VALUE;
    private Solver solver;
    private Robot robot;
    private long seed = 1;
    private String message;
    private boolean running;
    private int speed = DEFAULT_SPEED;
    /** Время прошлого кадра; первый кадр после старта только запоминает время */
    private boolean hasLastTick;
    private long lastTick;
    /** Накопленное время × скорость: целая часть от деления на секунду — сколько шагов пора сделать */
    private long stepDebt;

    public AppController() {
        restart();
    }

    /** Кнопка «Сначала»: решение с итерации 0 */
    public void restart() {
        message = null;
        solver = algorithm.create(new Mdp(world, settings));
        resetRobot();
    }

    /** Робот на старт с текущими стрелками */
    private void resetRobot() {
        pause();
        robot = new Robot(new Mdp(world, settings), solver.current().policy(), seed);
    }

    public void iterate() {
        message = null;
        solver.step();
        resetRobot();
    }

    public void solve() {
        message = null;
        solver.solve();
        resetRobot();
    }

    public void setAlgorithm(Algorithm a) {
        algorithm = Objects.requireNonNull(a, "algorithm");
        restart();
    }

    public void applySettings(Settings s) {
        settings = Objects.requireNonNull(s, "settings");
        restart();
    }

    public void newGrid(GridSize s) {
        size = Objects.requireNonNull(s, "size");
        world = GridWorld.blank(s.width(), s.height());
        restart();
    }

    public void pdfExample() {
        size = GridSize.defaults();
        world = GridWorld.pdfExample();
        settings = Settings.defaults();
        restart();
    }

    /** Левый щелчок: пусто → стена → +1 → −1 → пусто */
    public boolean clickCell(Cell c) {
        if (!world.inside(c)) {
            return false;
        }
        if (c.equals(world.start())) {
            message = "Клетку старта менять нельзя: сначала перенесите старт правой кнопкой";
            return false;
        }
        world = world.cycle(c);
        restart();
        return true;
    }

    /** Правый щелчок: перенести старт */
    public boolean moveStart(Cell c) {
        if (!world.inside(c)) {
            return false;
        }
        if (world.kind(c) != CellKind.EMPTY) {
            message = "Старт можно поставить только в пустую клетку";
            return false;
        }
        world = world.withStart(c);
        restart();
        return true;
    }

    public void robotStep() {
        pause();
        robot.step();
    }

    public void newWalk() {
        seed++;
        resetRobot();
    }

    public void startRobot() {
        if (running || robot.finished() || robot.stopped()) {
            return;
        }
        running = true;
        hasLastTick = false;
        stepDebt = 0;
    }

    public void pause() {
        running = false;
    }

    public void toggleRobot() {
        if (running) {
            pause();
        } else {
            startRobot();
        }
    }

    public void tick(long nowNanos) {
        if (!running) {
            return;
        }
        if (!hasLastTick || nowNanos < lastTick) {
            hasLastTick = true;
            lastTick = nowNanos;
            return;
        }
        // больше секунды за кадр не учитываем: после «замерзания» не набежит гигантский долг
        long elapsed = Math.min(nowNanos - lastTick, NANOS_PER_SECOND);
        lastTick = nowNanos;
        stepDebt += elapsed * speed;
        long due = stepDebt / NANOS_PER_SECOND;
        stepDebt %= NANOS_PER_SECOND;
        for (long i = 0; i < due && running; i++) {
            robot.step();
            if (robot.finished() || robot.stopped()) {
                pause();
            }
        }
    }

    public void setSpeed(int stepsPerSecond) {
        speed = Math.clamp(stepsPerSecond, MIN_SPEED, MAX_SPEED);
    }

    public int getSpeed() {
        return speed;
    }

    public boolean isRunning() {
        return running;
    }

    public GridSize size() {
        return size;
    }

    public GridWorld world() {
        return world;
    }

    public Settings settings() {
        return settings;
    }

    public Algorithm algorithm() {
        return algorithm;
    }

    public Solver solver() {
        return solver;
    }

    public Robot robot() {
        return robot;
    }

    public long seed() {
        return seed;
    }

    /** Почему правка не прошла; null, если всё хорошо */
    public String message() {
        return message;
    }
}
