package mdp.solver;

import mdp.model.Cell;
import mdp.model.Mdp;

import java.util.HashMap;
import java.util.Map;

/** Полезности U(s) всех клеток кроме стен */
public final class Utilities {

    private final Map<Cell, Double> values;

    Utilities(Map<Cell, Double> values) {
        this.values = Map.copyOf(values);
    }

    /** Начало: 0 везде, в конечных клетках их награда */
    static Utilities initial(Mdp mdp) {
        Map<Cell, Double> u = new HashMap<>();
        for (Cell s : mdp.states()) {
            u.put(s, mdp.terminal(s) ? mdp.reward(s) : 0.0);
        }
        return new Utilities(u);
    }

    public double get(Cell s) {
        Double v = values.get(s);
        if (v == null) {
            throw new IllegalArgumentException("нет полезности для " + s);
        }
        return v;
    }

    /** Наибольшее изменение по клеткам */
    double maxDiff(Utilities other) {
        double max = 0;
        for (Map.Entry<Cell, Double> e : values.entrySet()) {
            max = Math.max(max, Math.abs(e.getValue() - other.get(e.getKey())));
        }
        return max;
    }
}
