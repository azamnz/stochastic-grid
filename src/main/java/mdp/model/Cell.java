package mdp.model;

/** Клетка (столбец, строка), (1,1) — левый нижний угол, как в задании */
public record Cell(int col, int row) {

    public Cell move(Move m) {
        return new Cell(col + m.dCol(), row + m.dRow());
    }

    @Override
    public String toString() {
        return "(" + col + "," + row + ")";
    }
}
