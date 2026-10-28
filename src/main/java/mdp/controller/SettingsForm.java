package mdp.controller;

import mdp.model.Settings;

import java.util.List;
import java.util.Map;

import static mdp.controller.FormField.Kind.DECIMAL;

/** Поля параметров без JavaFX: тексты → {@link Settings} или список проблем */
public final class SettingsForm {

    /** Поля в том же порядке, что и части {@link Settings} */
    public static final List<FormField> FIELDS = List.of(
            new FormField("stepReward", "R(s) за шаг", DECIMAL),
            new FormField("gamma", "γ (дисконт)", DECIMAL),
            new FormField("forward", "Вероятность идти куда хотел", DECIMAL));

    private SettingsForm() {
    }

    public static Map<String, String> toTexts(Settings s) {
        return Forms.texts(FIELDS, s.stepReward(), s.gamma(), s.forward());
    }

    public static FormResult<Settings> parse(Map<String, String> texts) {
        return Forms.parse(FIELDS, texts, v -> new Settings((double) v[0], (double) v[1], (double) v[2]));
    }
}
