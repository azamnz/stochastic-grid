package mdp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Параметры задачи; обо всех ошибках сообщает сразу.
 *
 * @param stepReward R(s) в неконечных клетках
 * @param gamma      коэффициент дисконтирования γ
 * @param forward    вероятность пойти куда хотел; остальное поровну сносит влево и вправо
 */
public record Settings(double stepReward, double gamma, double forward) {

    public static final double MAX_REWARD = 10;

    public static Settings defaults() {
        return new Settings(-0.04, 1, 0.8);
    }

    public Settings {
        List<InvalidSettingsException.Problem> problems = new ArrayList<>();
        // !(a <= x) ловит и NaN
        if (!(stepReward >= -MAX_REWARD && stepReward <= MAX_REWARD)) {
            problems.add(new InvalidSettingsException.Problem("stepReward",
                    "R(s): допустимо от −" + (int) MAX_REWARD + " до " + (int) MAX_REWARD + ", указано " + stepReward));
        }
        if (!(gamma > 0 && gamma <= 1)) {
            problems.add(new InvalidSettingsException.Problem("gamma",
                    "γ: допустимо больше 0 и не больше 1, указано " + gamma));
        }
        if (!(forward >= 0 && forward <= 1)) {
            problems.add(new InvalidSettingsException.Problem("forward",
                    "Вероятность идти куда хотел: допустимо от 0 до 1, указано " + forward));
        }
        if (!problems.isEmpty()) {
            throw new InvalidSettingsException(problems);
        }
    }

    /** Вероятность сноса в одну сторону */
    public double side() {
        return (1 - forward) / 2;
    }
}
