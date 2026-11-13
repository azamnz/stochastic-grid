package mdp.solver;

/** Итог оценки стратегии: U^π, сколько проходов, сошлась ли */
public record Evaluation(Utilities utilities, int sweeps, boolean converged) {
}
