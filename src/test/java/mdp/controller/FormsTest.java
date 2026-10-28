package mdp.controller;

import mdp.model.GridSize;
import mdp.model.InvalidSettingsException.Problem;
import mdp.model.Settings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FormsTest {

    private static List<String> fields(FormResult<?> r) {
        return assertInstanceOf(FormResult.Invalid.class, r).problems().stream().map(p -> ((Problem) p).field()).toList();
    }

    @Test
    void settingsRoundTrip() {
        Settings s = new Settings(-0.4, 0.9, 0.7);
        assertEquals(new FormResult.Valid<>(s), SettingsForm.parse(SettingsForm.toTexts(s)));
        assertEquals(List.of("stepReward", "gamma", "forward"), SettingsForm.FIELDS.stream().map(FormField::key).toList());
    }

    @Test
    void settingsReadingAndRangeErrors() {
        Map<String, String> t = new HashMap<>(SettingsForm.toTexts(Settings.defaults()));
        t.put("gamma", "abc");
        assertEquals(List.of("gamma"), fields(SettingsForm.parse(t)));
        t.put("gamma", "0");
        t.put("forward", "1,5");
        assertEquals(List.of("gamma", "forward"), fields(SettingsForm.parse(t)));
    }

    @Test
    void gridRoundTripAndErrors() {
        assertEquals(new FormResult.Valid<>(new GridSize(7, 5)), GridForm.parse(GridForm.toTexts(new GridSize(7, 5))));
        assertEquals(List.of("width", "height"), GridForm.FIELDS.stream().map(FormField::key).toList());
        assertEquals(List.of("width", "height"), fields(GridForm.parse(Map.of("width", "1", "height", "99"))));
        assertEquals(List.of("height"), fields(GridForm.parse(Map.of("width", "4", "height", "x"))));
        assertEquals(List.of("width"), fields(GridForm.parse(Map.of("width", "11", "height", "3"))));
    }
}
