/**
 * Dimensional analysis and unit-of-measure framework.
 *
 * <p>Every numeric value in a simulation carries a {@link com.deathrayresearch.forrester.measure.Unit}
 * belonging to a {@link com.deathrayresearch.forrester.measure.Dimension} (TIME, ITEM, MASS, etc.).
 * The {@link com.deathrayresearch.forrester.measure.Quantity} class pairs a {@code double} value
 * with its unit, enabling unit-aware arithmetic (add, subtract, multiply, divide, convert).
 *
 * <p>Built-in unit families live in sub-packages:
 * <ul>
 *   <li>{@code units.time} — DAY, WEEK, MONTH, YEAR, etc.</li>
 *   <li>{@code units.item} — PEOPLE, THING, and custom countable units via
 *       {@link com.deathrayresearch.forrester.measure.units.item.ItemUnit}</li>
 *   <li>{@code units.dimensionless} — ratios, fractions, and other unitless quantities</li>
 * </ul>
 *
 * <p>To create a domain-specific unit (e.g., "Widget", "Patient"), instantiate
 * {@link com.deathrayresearch.forrester.measure.units.item.ItemUnit} with the desired name.
 *
 * @see com.deathrayresearch.forrester.measure.Quantity
 * @see com.deathrayresearch.forrester.measure.Unit
 * @see com.deathrayresearch.forrester.measure.units.item.ItemUnit
 */
package com.deathrayresearch.forrester.measure;
