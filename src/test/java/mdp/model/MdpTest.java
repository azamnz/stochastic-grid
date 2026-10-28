package mdp.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MdpTest {

    private final Mdp pdf = new Mdp(GridWorld.pdfExample(), Settings.defaults());

    @Test
    void upFromStartMatchesPdfEquation() {
        // 0.8·U(1,2) + 0.1·U(2,1) + 0.1·U(1,1)
        assertEquals(List.of(
                        new Outcome(Move.UP, new Cell(1, 2), 0.8),
                        new Outcome(Move.LEFT, new Cell(1, 1), 0.1),
                        new Outcome(Move.RIGHT, new Cell(2, 1), 0.1)),
                rounded(pdf.outcomes(new Cell(1, 1), Move.UP)));
    }

    @Test
    void leftFromStartBumpsWall() {
        assertEquals(List.of(
                        new Outcome(Move.LEFT, new Cell(1, 1), 0.8),
                        new Outcome(Move.DOWN, new Cell(1, 1), 0.1),
                        new Outcome(Move.UP, new Cell(1, 2), 0.1)),
                rounded(pdf.outcomes(new Cell(1, 1), Move.LEFT)));
    }

    @Test
    void innerWallBlocksLikeBorder() {
        assertEquals(new Cell(1, 2), pdf.outcomes(new Cell(1, 2), Move.RIGHT).getFirst().to());
        assertEquals(new Cell(3, 2), pdf.outcomes(new Cell(3, 2), Move.LEFT).getFirst().to());
    }

    @Test
    void zeroProbabilitiesAreDropped() {
        Mdp sure = new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 1, 1));
        assertEquals(List.of(new Outcome(Move.UP, new Cell(1, 2), 1.0)), sure.outcomes(new Cell(1, 1), Move.UP));
        Mdp sideOnly = new Mdp(GridWorld.pdfExample(), new Settings(-0.04, 1, 0));
        assertEquals(2, sideOnly.outcomes(new Cell(1, 1), Move.UP).size());
    }

    @Test
    void probabilitiesSumToOneEverywhere() {
        for (Cell s : pdf.states()) {
            for (Move m : Move.values()) {
                assertEquals(1.0, pdf.outcomes(s, m).stream().mapToDouble(Outcome::probability).sum(), 1e-12);
            }
        }
    }

    @Test
    void rewardsAndTerminals() {
        assertEquals(-0.04, pdf.reward(new Cell(1, 1)));
        assertEquals(1.0, pdf.reward(new Cell(4, 3)));
        assertEquals(-1.0, pdf.reward(new Cell(4, 2)));
        assertTrue(pdf.terminal(new Cell(4, 3)));
        assertFalse(pdf.terminal(new Cell(3, 3)));
        assertEquals(11, pdf.states().size());
    }

    private static List<Outcome> rounded(List<Outcome> list) {
        return list.stream().map(o -> new Outcome(o.move(), o.to(), Math.round(o.probability() * 1e9) / 1e9)).toList();
    }
}
