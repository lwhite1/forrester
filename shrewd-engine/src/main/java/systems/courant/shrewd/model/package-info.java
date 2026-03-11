/**
 * Core building blocks of a system dynamics model.
 *
 * <p>A model is assembled from these elements:
 * <ul>
 *   <li>{@link systems.courant.shrewd.model.Stock} — an accumulation (level) that
 *       changes over time through inflows and outflows</li>
 *   <li>{@link systems.courant.shrewd.model.Flow} — a rate that adds to or drains
 *       from a stock each timestep</li>
 *   <li>{@link systems.courant.shrewd.model.Variable} — a computed value defined by
 *       a {@link systems.courant.shrewd.model.Formula}, re-evaluated each timestep</li>
 *   <li>{@link systems.courant.shrewd.model.Constant} — an immutable parameter</li>
 *   <li>{@link systems.courant.shrewd.model.Module} — a named grouping of stocks,
 *       flows, and variables for organizing large models hierarchically</li>
 *   <li>{@link systems.courant.shrewd.model.Model} — the top-level container that
 *       holds all modules and is passed to a
 *       {@link systems.courant.shrewd.Simulation}</li>
 * </ul>
 *
 * <p>Standard SD input functions are also provided:
 * {@link systems.courant.shrewd.model.Step},
 * {@link systems.courant.shrewd.model.Ramp},
 * {@link systems.courant.shrewd.model.Smooth}, and
 * {@link systems.courant.shrewd.model.Delay3}.
 * Each implements {@link systems.courant.shrewd.model.Formula} and can be passed
 * directly to a {@link systems.courant.shrewd.model.Variable} constructor.
 *
 * @see systems.courant.shrewd.model.Model
 * @see systems.courant.shrewd.Simulation
 */
package systems.courant.shrewd.model;
