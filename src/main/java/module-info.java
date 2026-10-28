module mdp {
    requires javafx.controls;
    exports mdp;
    exports mdp.model;
    exports mdp.solver;
    exports mdp.robot;
    exports mdp.controller;
    exports mdp.util;
    exports mdp.view to javafx.graphics;
    opens mdp.view to javafx.graphics;
}
