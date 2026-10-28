package mdp.controller;

import mdp.model.GridSize;

import java.util.List;
import java.util.Map;

import static mdp.controller.FormField.Kind.INTEGER;

/** Форма «Новое поле» без JavaFX */
public final class GridForm {

    public static final List<FormField> FIELDS = List.of(
            new FormField("width", "Ширина (столбцов)", INTEGER),
            new FormField("height", "Высота (строк)", INTEGER));

    private GridForm() {
    }

    public static Map<String, String> toTexts(GridSize s) {
        return Forms.texts(FIELDS, s.width(), s.height());
    }

    public static FormResult<GridSize> parse(Map<String, String> texts) {
        return Forms.parse(FIELDS, texts, v -> new GridSize((int) v[0], (int) v[1]));
    }
}
