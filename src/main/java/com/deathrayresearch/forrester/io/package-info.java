/**
 * File-based output for simulation results.
 *
 * <p>{@link com.deathrayresearch.forrester.io.CsvSubscriber} is an
 * {@link com.deathrayresearch.forrester.event.EventHandler} that writes one CSV row per
 * timestep, with columns for step number, timestamp, all stock values, and all variable
 * values. Register it before running a simulation to capture time-series data:
 *
 * <pre>{@code
 * CsvSubscriber csv = new CsvSubscriber("output/results.csv");
 * simulation.addEventHandler(csv);
 * simulation.execute();
 * }</pre>
 *
 * @see com.deathrayresearch.forrester.io.CsvSubscriber
 */
package com.deathrayresearch.forrester.io;
