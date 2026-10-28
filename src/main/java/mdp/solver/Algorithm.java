package mdp.solver;

import mdp.model.Mdp;

import java.util.function.Function;

public enum Algorithm {
    VALUE("итерация по значениям", ValueIteration::new),
    POLICY("итерация по стратегиям", PolicyIteration::new);

    private final String title;
    private final Function<Mdp, Solver> factory;

    Algorithm(String title, Function<Mdp, Solver> factory) {
        this.title = title;
        this.factory = factory;
    }

    public String title() {
        return title;
    }

    public Solver create(Mdp mdp) {
        return factory.apply(mdp);
    }
}
