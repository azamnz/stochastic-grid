package mdp.robot;

import mdp.model.Cell;
import mdp.model.Move;

import java.util.Locale;

/**
 * Один шаг робота.
 *
 * @param reward R(s) клетки, куда пришёл
 * @param total  Σ γ^t·R(s_t) с начала пути
 */
public record RobotStep(int number, Cell from, Move wanted, Move actual, Cell to, double reward, double total) {

    @Override
    public String toString() {
        String where = to.equals(from) ? "упёрся в стену, остался в " + to : "пришёл в " + to;
        String wind = wanted == actual ? "иду " + actual.title() : "снесло " + actual.title();
        return String.format(Locale.ROOT, "Шаг %d · %s · хочу %s · %s · %s · R = %.3f · сумма %.3f",
                number, from, wanted.title(), wind, where, reward, total);
    }
}
