package mdp;

import javafx.application.Application;
import mdp.view.MdpApp;

/** Точка входа; не сам MdpApp, иначе запуск без модулей падает с «JavaFX runtime components are missing» */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(MdpApp.class, args);
    }
}
