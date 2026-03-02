/**
 * Simulation lifecycle events and the listener interface for observing them.
 *
 * <p>The simulation engine fires three event types via a Guava {@code EventBus}:
 * <ol>
 *   <li>{@link com.deathrayresearch.forrester.event.SimulationStartEvent} — once, before any
 *       time steps are computed. Use this to initialize output writers, open files, or record
 *       initial stock values.</li>
 *   <li>{@link com.deathrayresearch.forrester.event.TimeStepEvent} — after each time step.
 *       Stock values in the event reflect the state <em>before</em> the step's update, so
 *       handlers observe lagged values (standard SD convention).</li>
 *   <li>{@link com.deathrayresearch.forrester.event.SimulationEndEvent} — once, after all
 *       time steps have completed. Use this to flush output, close files, or compute summary
 *       statistics.</li>
 * </ol>
 *
 * <p>Implement {@link com.deathrayresearch.forrester.event.EventHandler} and register it via
 * {@link com.deathrayresearch.forrester.Simulation#addEventHandler(EventHandler)} to receive
 * events. Only override the methods you need — default implementations are no-ops.
 *
 * @see com.deathrayresearch.forrester.event.EventHandler
 * @see com.deathrayresearch.forrester.Simulation#addEventHandler(EventHandler)
 */
package com.deathrayresearch.forrester.event;
