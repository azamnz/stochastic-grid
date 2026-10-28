package mdp.view;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mdp.controller.AppController;
import mdp.controller.FormField;
import mdp.controller.FormResult;
import mdp.controller.GridForm;
import mdp.controller.SettingsForm;
import mdp.controller.SpeedScale;
import mdp.controller.Texts;
import mdp.model.GridSize;
import mdp.model.InvalidSettingsException.Problem;
import mdp.model.Settings;
import mdp.robot.Robot;
import mdp.robot.RobotStep;
import mdp.solver.Algorithm;
import mdp.solver.Solver;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Главное окно: поле с U(s) и стрелками, справа параметры и робот, внизу журналы решения и пути */
public class MdpApp extends Application {

    private final AppController controller = new AppController();
    private final GridCanvas grid = new GridCanvas(c -> {
        controller.clickCell(c);
        refresh();
    }, c -> {
        controller.moveStart(c);
        refresh();
    });
    private final Label info = new Label();
    private final Label solverStatus = new Label();
    private final Map<String, TextField> inputs = new LinkedHashMap<>();
    private final Label settingsErrors = new Label();
    private final Button robotRun = new Button();
    private final Button robotStep = new Button("Шаг робота");
    private final Label robotStatus = new Label();
    private final Slider speed = new Slider(SpeedScale.SLIDER_MIN, SpeedScale.SLIDER_MAX, 0);
    private final Label speedLabel = new Label();
    private final Label message = new Label();
    private final TextArea solverJournal = new TextArea();
    private final TextArea robotJournal = new TextArea();
    private final ToggleGroup algorithms = new ToggleGroup();
    private AnimationTimer timer;

    /** Что уже нарисовано: перерисовываем, только когда что-то поменялось */
    private Solver drawnSolver;
    private int drawnHistory = -1;
    private Robot drawnRobot;
    private int drawnSteps = -1;

    static String stylesheet() {
        return MdpApp.class.getResource("app.css").toExternalForm();
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        VBox center = new VBox(4, grid);
        VBox.setVgrow(grid, Priority.ALWAYS);
        center.setPadding(new Insets(10, 12, 6, 12));
        root.setCenter(center);
        root.setRight(buildPanel());
        root.setBottom(buildJournals());

        Scene scene = new Scene(root, 1180, 820);
        scene.getStylesheets().add(stylesheet());
        stage.setTitle("Стохастическое окружение");
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.setScene(scene);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                controller.tick(now);
                refresh();
            }
        };
        fillSettings();
        refresh();
        stage.show();
        timer.start();
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private HBox buildToolbar() {
        Label title = new Label("Стохастическое окружение");
        title.getStyleClass().add("title");
        title.setMinWidth(Region.USE_PREF_SIZE);
        info.getStyleClass().add("muted");
        Button iterate = button("Итерация", "iterate", controller::iterate);
        Button solve = button("Решить", "solve", controller::solve);
        Button restart = button("Сначала", "restart", controller::restart);
        Button newGrid = new Button("Новое поле…");
        newGrid.setId("newGrid");
        newGrid.setOnAction(e -> openNewGrid());
        Button example = button("Пример из задания", "example", () -> {
            controller.pdfExample();
            fillSettings();
        });
        for (Button b : List.of(iterate, solve, restart, newGrid, example)) {
            b.setMinWidth(Region.USE_PREF_SIZE);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(8, title, info, spacer, iterate, solve, restart, newGrid, example);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("toolbar-row");
        return bar;
    }

    private Button button(String text, String id, Runnable action) {
        Button b = new Button(text);
        b.setId(id);
        b.setOnAction(e -> {
            action.run();
            refresh();
        });
        return b;
    }

    private ScrollPane buildPanel() {
        RadioButton value = new RadioButton(Algorithm.VALUE.title());
        RadioButton policy = new RadioButton(Algorithm.POLICY.title());
        value.setId("algValue");
        policy.setId("algPolicy");
        value.setUserData(Algorithm.VALUE);
        policy.setUserData(Algorithm.POLICY);
        value.setToggleGroup(algorithms);
        policy.setToggleGroup(algorithms);
        value.setSelected(true);
        algorithms.selectedToggleProperty().addListener((o, a, b) -> {
            if (b != null) {
                controller.setAlgorithm((Algorithm) b.getUserData());
                refresh();
            }
        });
        solverStatus.getStyleClass().add("status");
        solverStatus.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(6);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(Region.USE_PREF_SIZE);
        form.getColumnConstraints().addAll(labels, new ColumnConstraints());
        int row = 0;
        for (FormField f : SettingsForm.FIELDS) {
            TextField input = new TextField();
            input.setPrefColumnCount(6);
            input.setId("field-" + f.key());
            input.setOnAction(e -> applySettings());
            inputs.put(f.key(), input);
            Label label = new Label(f.label());
            label.setWrapText(true);
            label.setMaxWidth(150);
            form.add(label, 0, row);
            form.add(input, 1, row);
            row++;
        }
        Button apply = new Button("Применить");
        apply.setId("applySettings");
        apply.setOnAction(e -> applySettings());
        settingsErrors.getStyleClass().add("error-text");
        settingsErrors.setWrapText(true);
        settingsErrors.setMinHeight(Region.USE_PREF_SIZE);

        robotRun.setId("robotRun");
        robotRun.setOnAction(e -> {
            controller.toggleRobot();
            refresh();
        });
        robotStep.setId("robotStep");
        robotStep.setOnAction(e -> {
            controller.robotStep();
            refresh();
        });
        Button newWalk = button("Новый путь", "newWalk", controller::newWalk);
        robotStatus.setWrapText(true);
        robotStatus.getStyleClass().add("value");
        speed.setValue(SpeedScale.toSlider(controller.getSpeed()));
        speed.valueProperty().addListener((o, a, b) -> {
            controller.setSpeed(SpeedScale.toSpeed(b.doubleValue()));
            refresh();
        });

        Label edit = new Label("""
                Щелчок по клетке: пусто → стена → +1 → −1 → пусто.
                Правый щелчок (или Ctrl+щелчок): перенести старт.""");
        edit.getStyleClass().add("legend");
        edit.setWrapText(true);
        message.getStyleClass().add("warning-text");
        message.setWrapText(true);

        Label legend = new Label("""
                число — полезность U(s)
                стрелка — стратегия π(s)
                зелёный оттенок — U ближе к +1, красный — к −1
                синяя клетка — стена
                кружок и след — робот""");
        legend.getStyleClass().add("legend");
        legend.setWrapText(true);

        HBox robotButtons = new HBox(6, robotRun, robotStep, newWalk);
        VBox panel = new VBox(6,
                section("Алгоритм"), value, policy, solverStatus,
                section("Параметры"), form, apply, settingsErrors,
                section("Робот"), robotButtons, robotStatus, new Label("Скорость"), speed, speedLabel,
                section("Правка поля"), edit, message,
                section("Обозначения"), legend);
        panel.setPadding(new Insets(12));
        // в низком окне панель прокручивается, а не обрезается
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("panel");
        scroll.setPrefWidth(320);
        scroll.setMinWidth(280);
        return scroll;
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section");
        return l;
    }

    private HBox buildJournals() {
        VBox left = journal("Ход решения", solverJournal, "solverJournal");
        VBox right = journal("Путь робота", robotJournal, "robotJournal");
        HBox box = new HBox(12, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        box.setPadding(new Insets(6, 12, 10, 12));
        return box;
    }

    private static VBox journal(String caption, TextArea area, String id) {
        Label label = new Label(caption);
        label.getStyleClass().add("muted");
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(7);
        area.setId(id);
        VBox box = new VBox(4, label, area);
        box.setPrefWidth(500);
        return box;
    }

    private void fillSettings() {
        SettingsForm.toTexts(controller.settings()).forEach((k, v) -> inputs.get(k).setText(v));
        inputs.values().forEach(i -> i.getStyleClass().remove("invalid"));
        settingsErrors.setText("");
    }

    private void applySettings() {
        Map<String, String> texts = new HashMap<>();
        inputs.forEach((k, i) -> texts.put(k, i.getText()));
        inputs.values().forEach(i -> i.getStyleClass().remove("invalid"));
        switch (SettingsForm.parse(texts)) {
            case FormResult.Valid<Settings>(Settings s) -> {
                controller.applySettings(s);
                fillSettings();
            }
            case FormResult.Invalid<Settings>(List<Problem> problems) -> {
                StringBuilder text = new StringBuilder();
                for (Problem p : problems) {
                    text.append("• ").append(p.message()).append('\n');
                    TextField input = inputs.get(p.field());
                    if (input != null && !input.getStyleClass().contains("invalid")) {
                        input.getStyleClass().add("invalid");
                    }
                }
                settingsErrors.setText(text.toString().strip());
            }
        }
        refresh();
    }

    private void openNewGrid() {
        controller.pause();
        new FormDialog<>("Новое поле",
                "Пустое поле: +1 в правом верхнем углу, −1 под ним, старт в (1,1).\nСтены и выходы потом расставляются щелчками.",
                "Создать", GridForm.FIELDS, GridForm.toTexts(controller.size()), GridForm.toTexts(GridSize.defaults()),
                GridForm::parse)
                .showAndWait().ifPresent(controller::newGrid);
        refresh();
    }

    private void refresh() {
        Solver solver = controller.solver();
        Robot robot = controller.robot();
        Settings s = controller.settings();
        info.setText("поле " + controller.world().width() + "×" + controller.world().height()
                + ", R = " + FormField.text(s.stepReward()) + ", γ = " + FormField.text(s.gamma())
                + ", вперёд " + FormField.text(s.forward()) + ", вбок по " + FormField.text(Math.round(s.side() * 1e9) / 1e9));
        solverStatus.setText(Texts.solverStatus(solver));
        solverStatus.getStyleClass().setAll("label", solver.exhausted() ? "message" : "status");
        robotRun.setText(controller.isRunning() ? "Пауза" : "Пустить");
        boolean over = robot.finished() || robot.stopped();
        robotRun.setDisable(over);
        robotStep.setDisable(over);
        robotStatus.setText(Texts.robotStatus(robot));
        speedLabel.setText(Texts.count(controller.getSpeed(), "шаг", "шага", "шагов") + " в секунду");
        speedLabel.getStyleClass().setAll("label", "muted");
        message.setText(controller.message() == null ? "" : controller.message());
        if (algorithms.getSelectedToggle() != null && algorithms.getSelectedToggle().getUserData() != controller.algorithm()) {
            algorithms.getToggles().stream().filter(t -> t.getUserData() == controller.algorithm()).findFirst()
                    .ifPresent(algorithms::selectToggle);
        }

        int history = solver.history().size();
        boolean solverChanged = solver != drawnSolver || history != drawnHistory;
        boolean robotChanged = robot != drawnRobot || robot.steps() != drawnSteps;
        if (solverChanged || robotChanged) {
            grid.show(controller.world(), solver.current(), robot);
        }
        if (solverChanged) {
            setJournal(solverJournal, Texts.journal(controller.algorithm(), solver.history()));
            drawnSolver = solver;
            drawnHistory = history;
        }
        if (robotChanged) {
            setJournal(robotJournal, robot.journal().stream().map(RobotStep::toString).toList());
            drawnRobot = robot;
            drawnSteps = robot.steps();
        }
    }

    private static void setJournal(TextArea area, List<String> lines) {
        area.setText(String.join("\n", lines));
        area.positionCaret(area.getLength());
        area.setScrollTop(Double.MAX_VALUE);
    }
}
