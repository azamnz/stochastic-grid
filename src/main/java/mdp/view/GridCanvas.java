package mdp.view;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import mdp.controller.Texts;
import mdp.model.Cell;
import mdp.model.CellKind;
import mdp.model.GridWorld;
import mdp.model.Move;
import mdp.robot.Robot;
import mdp.robot.RobotStep;
import mdp.solver.Snapshot;

import java.util.List;
import java.util.function.Consumer;

/** Поле как на рисунках задания: в клетке U(s) и стрелка стратегии, номера столбцов снизу и строк слева */
public class GridCanvas extends Pane {

    static final Color GRID = Color.web("#c9c7bd");
    static final Color CELL = Color.web("#ffffff");
    static final Color WALL = Color.web("#2a3f8f");
    static final Color TEXT = Color.web("#2c2c2a");
    static final Color MUTED = Color.web("#888780");
    static final Color ARROW = Color.web("#2a3f8f");
    static final Color PLUS = Color.web("#0f6e56");
    static final Color MINUS = Color.web("#a32d2d");
    static final Color ROBOT = Color.web("#d85a30");

    /** Место под номера строк и столбцов */
    private static final double AXIS = 22;
    private static final double MARGIN = 6;

    private final Canvas canvas = new Canvas();
    private GridWorld world;
    private Snapshot snapshot;
    private Robot robot;

    public GridCanvas(Consumer<Cell> leftClick, Consumer<Cell> rightClick) {
        getChildren().add(canvas);
        setMinSize(200, 160);
        widthProperty().addListener((o, a, b) -> redraw());
        heightProperty().addListener((o, a, b) -> redraw());
        canvas.setOnMouseClicked(e -> {
            Cell c = cellAt(e.getX(), e.getY());
            if (c == null) {
                return;
            }
            if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY || e.isControlDown()) {
                rightClick.accept(c);
            } else if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                leftClick.accept(c);
            }
        });
    }

    public void show(GridWorld world, Snapshot snapshot, Robot robot) {
        this.world = world;
        this.snapshot = snapshot;
        this.robot = robot;
        redraw();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
    }

    private double cellSize() {
        double w = (getWidth() - AXIS - 2 * MARGIN) / world.width();
        double h = (getHeight() - AXIS - 2 * MARGIN) / world.height();
        return Math.max(4, Math.floor(Math.min(w, h)));
    }

    private double left() {
        return MARGIN + AXIS + Math.max(0, (getWidth() - AXIS - 2 * MARGIN - cellSize() * world.width()) / 2);
    }

    private double top() {
        return MARGIN + Math.max(0, (getHeight() - AXIS - 2 * MARGIN - cellSize() * world.height()) / 2);
    }

    /** Левый верхний угол клетки на экране: строка 1 внизу */
    private double x(Cell c) {
        return left() + (c.col() - 1) * cellSize();
    }

    private double y(Cell c) {
        return top() + (world.height() - c.row()) * cellSize();
    }

    private Cell cellAt(double px, double py) {
        if (world == null) {
            return null;
        }
        double s = cellSize();
        int col = (int) Math.floor((px - left()) / s) + 1;
        int row = world.height() - (int) Math.floor((py - top()) / s);
        Cell c = new Cell(col, row);
        return px >= left() && py >= top() && world.inside(c) ? c : null;
    }

    private void redraw() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (world == null) {
            return;
        }
        double s = cellSize();
        g.setTextBaseline(VPos.CENTER);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("PT Sans", Math.min(14, s * 0.3)));
        g.setFill(MUTED);
        for (int col = 1; col <= world.width(); col++) {
            g.fillText(Integer.toString(col), left() + (col - 0.5) * s, top() + world.height() * s + AXIS / 2 + 2);
        }
        for (int row = 1; row <= world.height(); row++) {
            g.fillText(Integer.toString(row), left() - AXIS / 2 - 2, top() + (world.height() - row + 0.5) * s);
        }
        for (int row = 1; row <= world.height(); row++) {
            for (int col = 1; col <= world.width(); col++) {
                drawCell(g, new Cell(col, row), s);
            }
        }
        if (robot != null) {
            drawRobot(g, s);
        }
    }

    private void drawCell(GraphicsContext g, Cell c, double s) {
        double x = x(c);
        double y = y(c);
        CellKind kind = world.kind(c);
        g.setFill(kind == CellKind.WALL ? WALL : tint(c));
        g.fillRect(x, y, s, s);
        g.setStroke(GRID);
        g.setLineWidth(1);
        g.strokeRect(x + 0.5, y + 0.5, s - 1, s - 1);
        if (kind == CellKind.WALL) {
            return;
        }
        double cx = x + s / 2;
        if (kind == CellKind.EXIT) {
            double reward = world.exitReward(c);
            Color color = reward > 0 ? PLUS : MINUS;
            double bw = s * 0.56;
            double bh = s * 0.32;
            g.setStroke(color);
            g.setLineWidth(Math.max(1.5, s * 0.03));
            g.strokeRect(cx - bw / 2, y + s / 2 - bh / 2, bw, bh);
            g.setFill(color);
            g.setFont(Font.font("PT Sans", FontWeight.BOLD, s * 0.22));
            g.fillText(Texts.signed(reward), cx, y + s / 2);
            return;
        }
        if (snapshot != null) {
            g.setFill(TEXT);
            g.setFont(Font.font("PT Sans", FontWeight.BOLD, Math.max(8, s * 0.17)));
            g.fillText(Texts.number(snapshot.utilities().get(c)), cx, y + s * 0.2);
            Move m = snapshot.policy().get(c);
            if (m != null) {
                drawArrow(g, m, cx, y + s * 0.6, s * 0.36);
            }
        }
        if (c.equals(world.start())) {
            g.setFill(MUTED);
            g.setFont(Font.font("PT Sans", Math.max(7, s * 0.13)));
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText("старт", x + 4, y + s - s * 0.1);
            g.setTextAlign(TextAlignment.CENTER);
        }
    }

    /** Подкраска по U: зеленее к +1, краснее к −1 */
    private Color tint(Cell c) {
        if (snapshot == null) {
            return CELL;
        }
        double u = Math.clamp(snapshot.utilities().get(c), -1, 1);
        return u >= 0 ? CELL.interpolate(Color.web("#e1f5ee"), u) : CELL.interpolate(Color.web("#fcebeb"), -u);
    }

    private static void drawArrow(GraphicsContext g, Move m, double cx, double cy, double len) {
        double dx = switch (m) {
            case LEFT -> -1;
            case RIGHT -> 1;
            default -> 0;
        };
        double dy = switch (m) {
            case UP -> -1;
            case DOWN -> 1;
            default -> 0;
        };
        double x1 = cx - dx * len / 2;
        double y1 = cy - dy * len / 2;
        double x2 = cx + dx * len / 2;
        double y2 = cy + dy * len / 2;
        double head = len * 0.38;
        g.setStroke(ARROW);
        g.setFill(ARROW);
        g.setLineWidth(Math.max(2, len * 0.12));
        g.strokeLine(x1, y1, x2 - dx * head * 0.6, y2 - dy * head * 0.6);
        // наконечник: треугольник, повёрнутый по направлению
        double px = -dy;
        double py = dx;
        g.fillPolygon(new double[]{x2, x2 - dx * head + px * head * 0.55, x2 - dx * head - px * head * 0.55},
                new double[]{y2, y2 - dy * head + py * head * 0.55, y2 - dy * head - py * head * 0.55}, 3);
    }

    private void drawRobot(GraphicsContext g, double s) {
        List<RobotStep> steps = robot.journal();
        // след: линия по центрам клеток, где робот побывал
        g.setStroke(ROBOT.deriveColor(0, 1, 1, 0.45));
        g.setLineWidth(Math.max(1.5, s * 0.04));
        for (RobotStep step : steps) {
            if (!step.from().equals(step.to())) {
                g.strokeLine(x(step.from()) + s * 0.8, y(step.from()) + s * 0.8, x(step.to()) + s * 0.8, y(step.to()) + s * 0.8);
            }
        }
        Cell at = robot.position();
        double r = Math.max(5, s * 0.11);
        double cx = x(at) + s * 0.8;
        double cy = y(at) + s * 0.8;
        g.setFill(ROBOT);
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
    }
}
