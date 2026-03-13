/**
 * Parameter sweep, Monte Carlo analysis, and optimization tools.
 *
 * <p>This package provides three ways to explore a model's parameter space:
 *
 * <ul>
 *   <li>{@link systems.courant.sd.sweep.ParameterSweep} — runs a model repeatedly,
 *       varying one parameter across evenly spaced values. Use this to understand how a single
 *       input affects outputs.</li>
 *   <li>{@link systems.courant.sd.sweep.MonteCarlo} — runs a model many times with
 *       parameters drawn from probability distributions (uniform, normal, triangular, etc.).
 *       Use this for uncertainty analysis and sensitivity testing.</li>
 *   <li>{@link systems.courant.sd.sweep.Optimizer} — finds parameter values that
 *       minimize or maximize an objective function using derivative-free algorithms
 *       (Nelder-Mead, BOBYQA, CMA-ES). Use this for calibration or policy optimization.</li>
 * </ul>
 *
 * <p>All three use a builder pattern. Each run produces a {@link
 * systems.courant.sd.sweep.RunResult} containing the final stock, variable, and
 * flow values for that parameter combination.
 *
 * @see systems.courant.sd.sweep.ParameterSweep
 * @see systems.courant.sd.sweep.MonteCarlo
 * @see systems.courant.sd.sweep.Optimizer
 */
package systems.courant.sd.sweep;
