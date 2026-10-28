package mdp.model;

import java.util.ArrayList;
import java.util.List;

/** Заказ на новое поле; обо всех ошибках сообщает сразу */
public record GridSize(int width, int height) {

    public static GridSize defaults() {
        return new GridSize(4, 3);
    }

    public GridSize {
        List<InvalidSettingsException.Problem> problems = new ArrayList<>();
        if (width < GridWorld.MIN_SIZE || width > GridWorld.MAX_SIZE) {
            problems.add(new InvalidSettingsException.Problem("width", "Ширина: допустимо от "
                    + GridWorld.MIN_SIZE + " до " + GridWorld.MAX_SIZE + ", указано " + width));
        }
        if (height < GridWorld.MIN_SIZE || height > GridWorld.MAX_SIZE) {
            problems.add(new InvalidSettingsException.Problem("height", "Высота: допустимо от "
                    + GridWorld.MIN_SIZE + " до " + GridWorld.MAX_SIZE + ", указано " + height));
        }
        if (!problems.isEmpty()) {
            throw new InvalidSettingsException(problems);
        }
    }
}
