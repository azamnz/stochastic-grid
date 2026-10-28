package mdp.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettingsTest {

    @Test
    void defaultsAreFromPdf() {
        Settings s = Settings.defaults();
        assertEquals(-0.04, s.stepReward());
        assertEquals(1.0, s.gamma());
        assertEquals(0.8, s.forward());
        assertEquals(0.1, s.side(), 1e-12);
    }

    @Test
    void boundsAreAccepted() {
        new Settings(-10, 1, 0);
        new Settings(10, 0.01, 1);
    }

    @Test
    void allProblemsReportedAtOnce() {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class, () -> new Settings(11, 0, 1.5));
        assertEquals(List.of("stepReward", "gamma", "forward"),
                e.problems().stream().map(InvalidSettingsException.Problem::field).toList());
    }

    @Test
    void eachBoundCheckedSeparately() {
        assertThrows(InvalidSettingsException.class, () -> new Settings(-10.001, 1, 0.8));
        assertThrows(InvalidSettingsException.class, () -> new Settings(10.001, 1, 0.8));
        assertThrows(InvalidSettingsException.class, () -> new Settings(0, 1.001, 0.8));
        assertThrows(InvalidSettingsException.class, () -> new Settings(0, 0, 0.8));
        assertThrows(InvalidSettingsException.class, () -> new Settings(0, 1, -0.001));
        assertThrows(InvalidSettingsException.class, () -> new Settings(0, 1, 1.001));
    }

    @Test
    void nanAndInfinityRejected() {
        assertThrows(InvalidSettingsException.class, () -> new Settings(Double.NaN, 1, 0.8));
        assertThrows(InvalidSettingsException.class, () -> new Settings(0, Double.NaN, 0.8));
        assertThrows(InvalidSettingsException.class, () -> new Settings(0, 1, Double.NaN));
        assertThrows(InvalidSettingsException.class, () -> new Settings(Double.NEGATIVE_INFINITY, 1, 0.8));
    }
}
