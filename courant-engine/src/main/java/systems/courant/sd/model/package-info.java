/**
 * Core building blocks of a system dynamics model.
 *
 * <p>A model is assembled from these elements:
 * <ul>
 *   <li>{@link systems.courant.sd.model.Stock} — an accumulation (level) that
 *       changes over time through inflows and outflows</li>
 *   <li>{@link systems.courant.sd.model.Flow} — a rate that adds to or drains
 *       from a stock each timestep</li>
 *   <li>{@link systems.courant.sd.model.Variable} — a computed value defined by
 *       a {@link systems.courant.sd.model.Formula}, re-evaluated each timestep</li>
 *   <li>{@link systems.courant.sd.model.Module} — a named grouping of stocks,
 *       flows, and variables for organizing large models hierarchically</li>
 *   <li>{@link systems.courant.sd.model.Model} — the top-level container that
 *       holds all modules and is passed to a
 *       {@link systems.courant.sd.Simulation}</li>
 * </ul>
 *
 * <p>Standard SD input functions are also provided:
 * {@link systems.courant.sd.model.Step},
 * {@link systems.courant.sd.model.Ramp},
 * {@link systems.courant.sd.model.Smooth}, and
 * {@link systems.courant.sd.model.Delay3}.
 * Each implements {@link systems.courant.sd.model.Formula} and can be passed
 * directly to a {@link systems.courant.sd.model.Variable} constructor.
 *
 * @see systems.courant.sd.model.Model
 * @see systems.courant.sd.Simulation
 */
package systems.courant.sd.model;
