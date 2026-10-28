package mdp.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static mdp.controller.FormField.Kind.DECIMAL;
import static mdp.controller.FormField.Kind.INTEGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormFieldTest {

    private final FormField decimal = new FormField("r", "R(s)", DECIMAL);
    private final FormField integer = new FormField("w", "Ширина", INTEGER);

    private static Object value(FormField.Read r) {
        return assertInstanceOf(FormField.Read.Value.class, r).value();
    }

    private static String error(FormField.Read r) {
        return assertInstanceOf(FormField.Read.Error.class, r).message();
    }

    @Test
    void decimalsWithDotCommaAndSigns() {
        assertEquals(-0.04, value(decimal.read("-0.04")));
        assertEquals(-0.04, value(decimal.read("-0,04")));
        assertEquals(-0.04, value(decimal.read("−0,04")));
        assertEquals(0.8, value(decimal.read(" +0.8 ")));
        assertEquals(1.0, value(decimal.read("1")));
        assertEquals(2.0, value(decimal.read("2.")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1e3", "NaN", "Infinity", "0x10", "٣", "５", "1.2.3", "1,2,3", ".5", "--1", "+", "-", "1 000", "0.8%"})
    void notANumber(String text) {
        assertTrue(error(decimal.read(text)).startsWith("R(s): нужно число"), text);
    }

    @Test
    void emptyAndMissing() {
        assertEquals("R(s): не заполнено", error(decimal.read("  ")));
        assertEquals("R(s): не заполнено", error(decimal.read(null)));
    }

    @Test
    void hugeDecimalIsTooBig() {
        assertTrue(error(decimal.read("1" + "0".repeat(400))).contains("слишком большое"));
    }

    @Test
    void integersStayStrict() {
        assertEquals(7, value(integer.read("7")));
        assertTrue(error(integer.read("7.0")).startsWith("Ширина: нужно целое число"));
        assertTrue(error(integer.read("99999999999")).contains("слишком большое"));
    }

    @Test
    void echoIsSafe() {
        assertTrue(error(decimal.read("a\u001b[2J")).contains("\"a?[2J\""));
    }

    @Test
    void textOfValues() {
        assertEquals("-0.04", FormField.text(-0.04));
        assertEquals("1", FormField.text(1.0));
        assertEquals("0.8", FormField.text(0.8));
        assertEquals("5", FormField.text(5));
    }
}
