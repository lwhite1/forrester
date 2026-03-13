/**
 * Dimensional analysis and unit-of-measure framework.
 *
 * <p>Every numeric value in a simulation carries a {@link systems.courant.sd.measure.Unit}
 * belonging to a {@link systems.courant.sd.measure.Dimension} (TIME, ITEM, MASS, etc.).
 * The {@link systems.courant.sd.measure.Quantity} class pairs a {@code double} value
 * with its unit, enabling unit-aware arithmetic (add, subtract, multiply, divide, convert).
 *
 * <p>Built-in unit families live in sub-packages:
 * <ul>
 *   <li>{@code units.time} — DAY, WEEK, MONTH, YEAR, etc.</li>
 *   <li>{@code units.item} — PEOPLE, THING, and custom countable units via
 *       {@link systems.courant.sd.measure.units.item.ItemUnit}</li>
 *   <li>{@code units.dimensionless} — ratios, fractions, and other unitless quantities</li>
 * </ul>
 *
 * <p>To create a domain-specific unit (e.g., "Widget", "Patient"), instantiate
 * {@link systems.courant.sd.measure.units.item.ItemUnit} with the desired name.
 *
 * @see systems.courant.sd.measure.Quantity
 * @see systems.courant.sd.measure.Unit
 * @see systems.courant.sd.measure.units.item.ItemUnit
 */
package systems.courant.sd.measure;
