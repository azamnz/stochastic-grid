package mdp.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GridSizeTest {

    @Test
    void boundsAccepted() {
        new GridSize(GridWorld.MIN_SIZE, GridWorld.MAX_SIZE);
        new GridSize(GridWorld.MAX_SIZE, GridWorld.MIN_SIZE);
        assertEquals(new GridSize(4, 3), GridSize.defaults());
    }

    @Test
    void eachBoundChecked() {
        assertThrows(InvalidSettingsException.class, () -> new GridSize(1, 3));
        assertThrows(InvalidSettingsException.class, () -> new GridSize(11, 3));
        assertThrows(InvalidSettingsException.class, () -> new GridSize(4, 1));
        assertThrows(InvalidSettingsException.class, () -> new GridSize(4, 11));
    }

    @Test
    void bothProblemsAtOnce() {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class, () -> new GridSize(0, 99));
        assertEquals(List.of("width", "height"), e.problems().stream().map(InvalidSettingsException.Problem::field).toList());
    }
}
