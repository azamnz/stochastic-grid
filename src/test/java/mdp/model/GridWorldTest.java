package mdp.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridWorldTest {

    private final GridWorld pdf = GridWorld.pdfExample();

    @Test
    void pdfExampleIsFourByThree() {
        assertEquals(4, pdf.width());
        assertEquals(3, pdf.height());
        assertEquals(new Cell(1, 1), pdf.start());
        assertEquals(CellKind.WALL, pdf.kind(new Cell(2, 2)));
        assertEquals(CellKind.EXIT, pdf.kind(new Cell(4, 3)));
        assertEquals(CellKind.EXIT, pdf.kind(new Cell(4, 2)));
        assertEquals(1.0, pdf.exitReward(new Cell(4, 3)));
        assertEquals(-1.0, pdf.exitReward(new Cell(4, 2)));
        assertEquals(CellKind.EMPTY, pdf.kind(new Cell(1, 1)));
        assertEquals(CellKind.EMPTY, pdf.kind(new Cell(4, 1)));
    }

    @Test
    void cellsOutsideAreNotInside() {
        assertTrue(pdf.inside(new Cell(1, 1)));
        assertTrue(pdf.inside(new Cell(4, 3)));
        assertFalse(pdf.inside(new Cell(0, 1)));
        assertFalse(pdf.inside(new Cell(1, 0)));
        assertFalse(pdf.inside(new Cell(5, 1)));
        assertFalse(pdf.inside(new Cell(1, 4)));
    }

    @Test
    void statesSkipWallsAndGoFromTopRowLikeTables() {
        assertEquals(11, pdf.states().size());
        assertFalse(pdf.states().contains(new Cell(2, 2)));
        assertEquals(new Cell(1, 3), pdf.states().getFirst());
        assertEquals(new Cell(4, 1), pdf.states().getLast());
    }

    @Test
    void blankHasExitsInRightColumn() {
        GridWorld g = GridWorld.blank(6, 5);
        assertEquals(6, g.width());
        assertEquals(5, g.height());
        assertEquals(new Cell(1, 1), g.start());
        assertEquals(1.0, g.exitReward(new Cell(6, 5)));
        assertEquals(-1.0, g.exitReward(new Cell(6, 4)));
        assertEquals(30, g.states().size());
        assertEquals(2, g.states().stream().filter(c -> g.kind(c) == CellKind.EXIT).count());
    }

    @Test
    void cycleGoesEmptyWallPlusMinusEmpty() {
        Cell c = new Cell(2, 1);
        GridWorld w1 = pdf.cycle(c);
        assertEquals(CellKind.WALL, w1.kind(c));
        GridWorld w2 = w1.cycle(c);
        assertEquals(CellKind.EXIT, w2.kind(c));
        assertEquals(1.0, w2.exitReward(c));
        GridWorld w3 = w2.cycle(c);
        assertEquals(-1.0, w3.exitReward(c));
        GridWorld w4 = w3.cycle(c);
        assertEquals(CellKind.EMPTY, w4.kind(c));
        assertEquals(CellKind.EMPTY, pdf.kind(c));
    }

    @Test
    void startCannotBeCycledOrMovedOntoWallOrExit() {
        assertThrows(IllegalArgumentException.class, () -> pdf.cycle(pdf.start()));
        assertThrows(IllegalArgumentException.class, () -> pdf.withStart(new Cell(2, 2)));
        assertThrows(IllegalArgumentException.class, () -> pdf.withStart(new Cell(4, 3)));
        assertThrows(IllegalArgumentException.class, () -> pdf.withStart(new Cell(9, 9)));
        assertEquals(new Cell(3, 1), pdf.withStart(new Cell(3, 1)).start());
    }

    @Test
    void constructorRejectsBadLayouts() {
        Cell s = new Cell(1, 1);
        assertThrows(IllegalArgumentException.class, () -> new GridWorld(1, 3, Set.of(), Map.of(), s));
        assertThrows(IllegalArgumentException.class, () -> new GridWorld(3, 11, Set.of(), Map.of(), s));
        assertThrows(IllegalArgumentException.class, () -> new GridWorld(3, 3, Set.of(new Cell(4, 1)), Map.of(), s));
        assertThrows(IllegalArgumentException.class, () -> new GridWorld(3, 3, Set.of(), Map.of(new Cell(1, 4), 1.0), s));
        assertThrows(IllegalArgumentException.class,
                () -> new GridWorld(3, 3, Set.of(new Cell(2, 2)), Map.of(new Cell(2, 2), 1.0), s));
        assertThrows(IllegalArgumentException.class, () -> new GridWorld(3, 3, Set.of(s), Map.of(), s));
        assertThrows(IllegalArgumentException.class, () -> new GridWorld(3, 3, Set.of(), Map.of(s, 1.0), s));
        assertThrows(IllegalArgumentException.class, () -> new GridWorld(3, 3, Set.of(), Map.of(), new Cell(4, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> new GridWorld(3, 3, Set.of(), Map.of(new Cell(3, 3), Double.NaN), s));
        new GridWorld(2, 2, Set.of(), Map.of(), s);
        new GridWorld(10, 10, Set.of(), Map.of(), s);
    }

    @Test
    void layoutIsCopiedNotShared() {
        java.util.HashSet<Cell> walls = new java.util.HashSet<>(Set.of(new Cell(2, 2)));
        GridWorld g = new GridWorld(3, 3, walls, Map.of(), new Cell(1, 1));
        walls.add(new Cell(3, 3));
        assertEquals(CellKind.EMPTY, g.kind(new Cell(3, 3)));
    }
}
