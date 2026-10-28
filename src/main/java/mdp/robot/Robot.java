package mdp.robot;

import mdp.model.Cell;
import mdp.model.Mdp;
import mdp.model.Move;
import mdp.model.Outcome;
import mdp.solver.Policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Робот идёт по стрелкам стратегии, ветер сносит его случайно (по seed) */
public final class Robot {

    public static final int MAX_STEPS = 1000;

    private final Mdp mdp;
    private final Policy policy;
    private final Random random;
    private final List<RobotStep> journal = new ArrayList<>();
    private Cell position;
    private double total;
    private double discount = 1;

    public Robot(Mdp mdp, Policy policy, long seed) {
        this.mdp = mdp;
        this.policy = policy;
        this.random = new Random(seed);
        this.position = mdp.world().start();
        this.total = mdp.reward(position);
    }

    /** Исход по случайному r из [0, 1): первый, у которого накопленная вероятность больше r */
    static Outcome pick(List<Outcome> outcomes, double r) {
        double sum = 0;
        for (Outcome o : outcomes) {
            sum += o.probability();
            if (r < sum) {
                return o;
            }
        }
        // сумма вероятностей могла выйти чуть меньше 1 из-за округления
        return outcomes.getLast();
    }

    public void step() {
        if (finished() || stopped()) {
            return;
        }
        Move wanted = policy.get(position);
        Outcome o = pick(mdp.outcomes(position, wanted), random.nextDouble());
        discount *= mdp.settings().gamma();
        double reward = mdp.reward(o.to());
        total += discount * reward;
        journal.add(new RobotStep(steps() + 1, position, wanted, o.move(), o.to(), reward, total));
        position = o.to();
    }

    public Cell position() {
        return position;
    }

    public double total() {
        return total;
    }

    public int steps() {
        return journal.size();
    }

    public boolean finished() {
        return mdp.terminal(position);
    }

    /** Остановлен по лимиту шагов, до выхода не дошёл */
    public boolean stopped() {
        return !finished() && steps() >= MAX_STEPS;
    }

    public List<RobotStep> journal() {
        return List.copyOf(journal);
    }
}
