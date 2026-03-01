/**
 * Parameter sweep, Monte Carlo analysis, and optimization tools.
 *
 * <p>This package provides three ways to explore a model's parameter space:
 *
 * <ul>
 *   <li>{@link com.deathrayresearch.forrester.sweep.ParameterSweep} — runs a model repeatedly,
 *       varying one parameter across evenly spaced values. Use this to understand how a single
 *       input affects outputs.</li>
 *   <li>{@link com.deathrayresearch.forrester.sweep.MonteCarlo} — runs a model many times with
 *       parameters drawn from probability distributions (uniform, normal, triangular, etc.).
 *       Use this for uncertainty analysis and sensitivity testing.</li>
 *   <li>{@link com.deathrayresearch.forrester.sweep.Optimizer} — finds parameter values that
 *       minimize or maximize an objective function using derivative-free algorithms
 *       (Nelder-Mead, BOBYQA, CMA-ES). Use this for calibration or policy optimization.</li>
 * </ul>
 *
 * <p>All three use a builder pattern. Each run produces a {@link
 * com.deathrayresearch.forrester.sweep.RunResult} containing the final stock, variable, and
 * flow values for that parameter combination.
 *
 * @see com.deathrayresearch.forrester.sweep.ParameterSweep
 * @see com.deathrayresearch.forrester.sweep.MonteCarlo
 * @see com.deathrayresearch.forrester.sweep.Optimizer
 */
package com.deathrayresearch.forrester.sweep;
