package mdp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Задача целиком: поле + параметры. Отвечает, куда ведёт действие и какая награда в клетке */
public final class Mdp {

    private final GridWorld world;
    private final Settings settings;

    public Mdp(GridWorld world, Settings settings) {
        this.world = Objects.requireNonNull(world, "world");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public GridWorld world() {
        return world;
    }

    public Settings settings() {
        return settings;
    }

    public List<Cell> states() {
        return world.states();
    }

    public boolean terminal(Cell s) {
        return world.kind(s) == CellKind.EXIT;
    }

    /** R(s): награда выхода или плата за шаг */
    public double reward(Cell s) {
        return terminal(s) ? world.exitReward(s) : settings.stepReward();
    }

    /** Исходы действия a в клетке s: вперёд, снос влево, снос вправо; в стену — остаётся на месте */
    public List<Outcome> outcomes(Cell s, Move a) {
        List<Outcome> list = new ArrayList<>(3);
        add(list, s, a, settings.forward());
        add(list, s, a.left(), settings.side());
        add(list, s, a.right(), settings.side());
        return list;
    }

    private void add(List<Outcome> list, Cell s, Move m, double p) {
        if (p == 0) {
            return;
        }
        Cell to = s.move(m);
        list.add(new Outcome(m, world.inside(to) && world.kind(to) != CellKind.WALL ? to : s, p));
    }
}
