package mdp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoveTest {

    @Test
    void sidesAreNinetyDegreesAway() {
        assertEquals(Move.LEFT, Move.UP.left());
        assertEquals(Move.RIGHT, Move.UP.right());
        assertEquals(Move.DOWN, Move.LEFT.left());
        assertEquals(Move.UP, Move.LEFT.right());
        assertEquals(Move.RIGHT, Move.DOWN.left());
        assertEquals(Move.LEFT, Move.DOWN.right());
        assertEquals(Move.UP, Move.RIGHT.left());
        assertEquals(Move.DOWN, Move.RIGHT.right());
    }

    @Test
    void movesChangeCellAsInPdf() {
        Cell c = new Cell(2, 2);
        assertEquals(new Cell(2, 3), c.move(Move.UP));
        assertEquals(new Cell(2, 1), c.move(Move.DOWN));
        assertEquals(new Cell(1, 2), c.move(Move.LEFT));
        assertEquals(new Cell(3, 2), c.move(Move.RIGHT));
    }

    @Test
    void orderAndTextsMatchPdf() {
        assertEquals("[UP, LEFT, DOWN, RIGHT]", java.util.Arrays.toString(Move.values()));
        assertEquals("вверх", Move.UP.title());
        assertEquals("налево", Move.LEFT.title());
        assertEquals("вниз", Move.DOWN.title());
        assertEquals("направо", Move.RIGHT.title());
        assertEquals("↑", Move.UP.arrow());
        assertEquals("←", Move.LEFT.arrow());
        assertEquals("↓", Move.DOWN.arrow());
        assertEquals("→", Move.RIGHT.arrow());
    }

    @Test
    void cellPrintsLikePdf() {
        assertEquals("(3,4)", new Cell(3, 4).toString());
    }
}
