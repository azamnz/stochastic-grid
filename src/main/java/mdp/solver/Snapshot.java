package mdp.solver;

/**
 * Состояние решения после итерации.
 *
 * @param delta   наибольшее изменение U за итерацию
 * @param changed сколько стрелок поменялось
 * @param sweeps  проходов оценки стратегии (для итерации по значениям 1)
 */
public record Snapshot(int iteration, Utilities utilities, Policy policy, double delta, int changed, int sweeps,
                       Phase phase) {
}
