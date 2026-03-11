package systems.courant.shrewd.model;

/**
 * Controls how a {@link Stock} handles attempts to set a negative value.
 *
 * <p>Most physical quantities (population, inventory, water volume) cannot be negative.
 * System dynamics tools like Vensim and Stella prevent this by default. This enum lets
 * modelers choose the appropriate behavior for each stock.
 */
public enum NegativeValuePolicy {

    /** Silently clamp negative values to zero (default). */
    CLAMP_TO_ZERO,

    /** Permit negative values (e.g., bank balances, temperature deltas). */
    ALLOW,

    /** Throw an {@link IllegalArgumentException} when a negative value is set. */
    THROW
}
