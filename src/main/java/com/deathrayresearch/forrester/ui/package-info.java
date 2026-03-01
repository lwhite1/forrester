/**
 * JavaFX chart viewers for visualizing simulation output.
 *
 * <p>Two {@link com.deathrayresearch.forrester.event.EventHandler} implementations render
 * live time-series charts during simulation execution:
 * <ul>
 *   <li>{@link com.deathrayresearch.forrester.ui.StockLevelChartViewer} — plots all stock
 *       values over time</li>
 *   <li>{@link com.deathrayresearch.forrester.ui.FlowChartViewer} — plots all flow rates
 *       over time</li>
 * </ul>
 *
 * <p>Register a viewer as an event handler before executing a simulation:
 *
 * <pre>{@code
 * simulation.addEventHandler(new StockLevelChartViewer());
 * simulation.execute();  // chart window opens when the simulation finishes
 * }</pre>
 *
 * <p>These viewers require a JavaFX runtime on the classpath. For headless environments
 * (CI, servers), use {@link com.deathrayresearch.forrester.io.CsvSubscriber} instead.
 */
package com.deathrayresearch.forrester.ui;
