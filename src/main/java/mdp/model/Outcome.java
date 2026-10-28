package mdp.model;

/** Один исход действия: куда на самом деле пошёл агент, где оказался и с какой вероятностью */
public record Outcome(Move move, Cell to, double probability) {
}
