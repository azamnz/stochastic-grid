package mdp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Поле: размер, стены, конечные клетки с наградой, старт. Неизменяемое: правка даёт новое поле */
public final class GridWorld {

    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 10;

    private final int width;
    private final int height;
    private final Set<Cell> walls;
    private final Map<Cell, Double> exits;
    private final Cell start;

    public GridWorld(int width, int height, Set<Cell> walls, Map<Cell, Double> exits, Cell start) {
        if (width < MIN_SIZE || width > MAX_SIZE || height < MIN_SIZE || height > MAX_SIZE) {
            throw new IllegalArgumentException("размер поля от " + MIN_SIZE + " до " + MAX_SIZE + ": " + width + "×" + height);
        }
        this.width = width;
        this.height = height;
        this.walls = Set.copyOf(walls);
        this.exits = Map.copyOf(exits);
        this.start = Objects.requireNonNull(start, "start");
        for (Cell c : this.walls) {
            requireInside(c);
        }
        for (Map.Entry<Cell, Double> e : this.exits.entrySet()) {
            requireInside(e.getKey());
            if (this.walls.contains(e.getKey())) {
                throw new IllegalArgumentException("клетка и стена, и выход: " + e.getKey());
            }
            if (!Double.isFinite(e.getValue())) {
                throw new IllegalArgumentException("награда выхода не число: " + e.getKey());
            }
        }
        requireInside(start);
        if (kind(start) != CellKind.EMPTY) {
            throw new IllegalArgumentException("старт должен быть в пустой клетке: " + start);
        }
    }

    /** Поле из задания: 4×3, стена (2,2), +1 в (4,3), −1 в (4,2), старт (1,1) */
    public static GridWorld pdfExample() {
        return new GridWorld(4, 3, Set.of(new Cell(2, 2)),
                Map.of(new Cell(4, 3), 1.0, new Cell(4, 2), -1.0), new Cell(1, 1));
    }

    /** Пустое поле: +1 в правом верхнем углу, −1 под ним, старт (1,1) */
    public static GridWorld blank(int width, int height) {
        return new GridWorld(width, height, Set.of(),
                Map.of(new Cell(width, height), 1.0, new Cell(width, height - 1), -1.0), new Cell(1, 1));
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Cell start() {
        return start;
    }

    public boolean inside(Cell c) {
        return c.col() >= 1 && c.col() <= width && c.row() >= 1 && c.row() <= height;
    }

    public CellKind kind(Cell c) {
        if (walls.contains(c)) {
            return CellKind.WALL;
        }
        return exits.containsKey(c) ? CellKind.EXIT : CellKind.EMPTY;
    }

    public double exitReward(Cell c) {
        Double r = exits.get(c);
        if (r == null) {
            throw new IllegalArgumentException("не выход: " + c);
        }
        return r;
    }

    /** Все клетки кроме стен: сверху вниз, слева направо, как в таблицах задания */
    public List<Cell> states() {
        List<Cell> states = new ArrayList<>();
        for (int row = height; row >= 1; row--) {
            for (int col = 1; col <= width; col++) {
                Cell c = new Cell(col, row);
                if (!walls.contains(c)) {
                    states.add(c);
                }
            }
        }
        return states;
    }

    /** Правка клетки по кругу: пусто → стена → +1 → −1 → пусто */
    public GridWorld cycle(Cell c) {
        requireInside(c);
        if (c.equals(start)) {
            throw new IllegalArgumentException("старт нельзя менять: " + c);
        }
        Set<Cell> w = new HashSet<>(walls);
        Map<Cell, Double> e = new HashMap<>(exits);
        switch (kind(c)) {
            case EMPTY -> w.add(c);
            case WALL -> {
                w.remove(c);
                e.put(c, 1.0);
            }
            case EXIT -> {
                if (exits.get(c) > 0) {
                    e.put(c, -1.0);
                } else {
                    e.remove(c);
                }
            }
        }
        return new GridWorld(width, height, w, e, start);
    }

    public GridWorld withStart(Cell c) {
        return new GridWorld(width, height, walls, exits, c);
    }

    private void requireInside(Cell c) {
        if (!inside(c)) {
            throw new IllegalArgumentException("клетка вне поля: " + c);
        }
    }
}
