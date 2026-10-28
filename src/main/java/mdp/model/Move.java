package mdp.model;

/** Направление хода; порядок как в уравнении из задания: вверх, налево, вниз, направо */
public enum Move {
    UP(0, 1, "вверх", "↑"),
    LEFT(-1, 0, "налево", "←"),
    DOWN(0, -1, "вниз", "↓"),
    RIGHT(1, 0, "направо", "→");

    private final int dCol;
    private final int dRow;
    private final String title;
    private final String arrow;

    Move(int dCol, int dRow, String title, String arrow) {
        this.dCol = dCol;
        this.dRow = dRow;
        this.title = title;
        this.arrow = arrow;
    }

    int dCol() {
        return dCol;
    }

    int dRow() {
        return dRow;
    }

    /** Куда снесёт ветер слева от направления */
    public Move left() {
        return values()[(ordinal() + 1) % 4];
    }

    /** Куда снесёт ветер справа от направления */
    public Move right() {
        return values()[(ordinal() + 3) % 4];
    }

    public String title() {
        return title;
    }

    public String arrow() {
        return arrow;
    }
}
