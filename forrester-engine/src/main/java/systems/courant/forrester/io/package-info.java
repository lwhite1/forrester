/**
 * File-based output for simulation results.
 *
 * <p>{@link systems.courant.forrester.io.CsvSubscriber} is an
 * {@link systems.courant.forrester.event.EventHandler} that writes one CSV row per
 * timestep, with columns for step number, timestamp, all stock values, and all variable
 * values. Register it before running a simulation to capture time-series data:
 *
 * <pre>{@code
 * CsvSubscriber csv = new CsvSubscriber("output/results.csv");
 * simulation.addEventHandler(csv);
 * simulation.execute();
 * }</pre>
 *
 * @see systems.courant.forrester.io.CsvSubscriber
 */
package systems.courant.forrester.io;
