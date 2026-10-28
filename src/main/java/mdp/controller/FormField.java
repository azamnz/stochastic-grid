package mdp.controller;

import mdp.util.SafeText;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Поле формы: ключ (имя параметра), подпись для человека и вид значения */
public record FormField(String key, String label, Kind kind) {

    public enum Kind {
        INTEGER,
        /** Дробное: точка или запятая, минус и «−» */
        DECIMAL
    }

    /** Только ASCII-цифры: «５» или «٣» числом не считаются */
    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?[0-9]+([.,][0-9]*)?");
    private static final int ECHO_LIMIT = 20;

    /** Прочитанное значение или текст ошибки */
    public sealed interface Read {
        record Value(Object value) implements Read {
        }

        record Error(String message) implements Read {
        }
    }

    public Read read(String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.isEmpty()) {
            return error("не заполнено");
        }
        String echo = "\"" + SafeText.preview(text, ECHO_LIMIT) + "\"";
        if (kind == Kind.DECIMAL) {
            return readDecimal(text.replace('−', '-'), echo);
        }
        if (!INTEGER.matcher(text).matches()) {
            return error("нужно целое число, а указано " + echo);
        }
        try {
            return new Read.Value(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return error("слишком большое число " + echo);
        }
    }

    private Read readDecimal(String text, String echo) {
        if (!DECIMAL.matcher(text).matches()) {
            return error("нужно число, а указано " + echo);
        }
        double v = Double.parseDouble(text.replace(',', '.'));
        if (Double.isInfinite(v)) {
            return error("слишком большое число " + echo);
        }
        return new Read.Value(v);
    }

    /** Значение как текст для поля ввода: 1.0 → «1» */
    public static String text(Object value) {
        if (value instanceof Double d) {
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private Read error(String what) {
        return new Read.Error(label + ": " + what);
    }
}
