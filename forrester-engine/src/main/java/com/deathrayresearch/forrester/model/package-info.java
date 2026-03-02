/**
 * Core building blocks of a system dynamics model.
 *
 * <p>A model is assembled from these elements:
 * <ul>
 *   <li>{@link com.deathrayresearch.forrester.model.Stock} — an accumulation (level) that
 *       changes over time through inflows and outflows</li>
 *   <li>{@link com.deathrayresearch.forrester.model.Flow} — a rate that adds to or drains
 *       from a stock each timestep</li>
 *   <li>{@link com.deathrayresearch.forrester.model.Variable} — a computed value defined by
 *       a {@link com.deathrayresearch.forrester.model.Formula}, re-evaluated each timestep</li>
 *   <li>{@link com.deathrayresearch.forrester.model.Constant} — an immutable parameter</li>
 *   <li>{@link com.deathrayresearch.forrester.model.Module} — a named grouping of stocks,
 *       flows, and variables for organizing large models hierarchically</li>
 *   <li>{@link com.deathrayresearch.forrester.model.Model} — the top-level container that
 *       holds all modules and is passed to a
 *       {@link com.deathrayresearch.forrester.Simulation}</li>
 * </ul>
 *
 * <p>Standard SD input functions are also provided:
 * {@link com.deathrayresearch.forrester.model.Step},
 * {@link com.deathrayresearch.forrester.model.Ramp},
 * {@link com.deathrayresearch.forrester.model.Smooth}, and
 * {@link com.deathrayresearch.forrester.model.Delay3}.
 * Each implements {@link com.deathrayresearch.forrester.model.Formula} and can be passed
 * directly to a {@link com.deathrayresearch.forrester.model.Variable} constructor.
 *
 * @see com.deathrayresearch.forrester.model.Model
 * @see com.deathrayresearch.forrester.Simulation
 */
package com.deathrayresearch.forrester.model;
