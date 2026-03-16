package systems.courant.sd.measure;

import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;
import systems.courant.sd.measure.units.item.ItemUnits;
import systems.courant.sd.measure.units.length.LengthUnits;
import systems.courant.sd.measure.units.mass.MassUnits;
import systems.courant.sd.measure.units.money.MoneyUnits;
import systems.courant.sd.measure.units.temperature.TemperatureUnits;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.measure.units.volume.VolumeUnits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps unit name strings to {@link Unit} objects. Auto-registers all built-in unit enums.
 * Custom units (e.g. {@link systems.courant.sd.measure.units.item.ItemUnit})
 * can be registered dynamically.
 *
 * <p>Lookup is case-sensitive with a case-insensitive fallback.
 */
public class UnitRegistry {

    private static final Logger log = LoggerFactory.getLogger(UnitRegistry.class);
    private static final int MAX_CUSTOM_UNITS = 10_000;

    /**
     * Unit names that should be silently accepted as dimensionless/item units
     * without a "possible typo" warning. Lowercase for case-insensitive matching.
     * Includes Vensim conventions, common physical-sounding labels, and generic quantity words.
     */
    private static final Set<String> KNOWN_ACCEPTABLE_NAMES = Set.of(
            "units", "unit", "dmnl", "dimensionless",
            "fraction", "percent", "percentage", "ratio", "index",
            "gallon", "gallons", "gallon per minute", "gallons per minute",
            "liter", "liters", "litre", "litres",
            "person", "persons", "people",
            "patient", "patients",
            "vehicle", "vehicles",
            "ship", "ships",
            "fish",
            "dollar", "dollars", "euro", "euros", "eur", "usd",
            "widget", "widgets",
            "item", "items",
            "job", "jobs",
            "order", "orders",
            "call", "calls",
            "trip", "trips",
            "ton", "tons", "tonne", "tonnes"
    );

    private final Map<String, Unit> byName = new ConcurrentHashMap<>();
    private final Map<String, Unit> byNameLower = new ConcurrentHashMap<>();
    private int customUnitCount;

    /**
     * Creates a new registry pre-loaded with all built-in unit enums.
     */
    public UnitRegistry() {
        registerAll(TimeUnits.values());
        registerAll(ItemUnits.values());
        registerAll(LengthUnits.values());
        registerAll(MassUnits.values());
        registerAll(MoneyUnits.values());
        registerAll(VolumeUnits.values());
        registerAll(TemperatureUnits.values());
        registerAll(DimensionlessUnits.values());
        registerTimeUnitAliases();
    }

    /**
     * Registers common plural and abbreviated aliases for time units.
     * Vensim models use both singular ("Day") and plural ("Days") forms.
     */
    private void registerTimeUnitAliases() {
        for (TimeUnits tu : TimeUnits.values()) {
            String plural = tu.getName() + "s";
            byName.put(plural, tu);
            byNameLower.put(plural.toLowerCase(), tu);
        }
    }

    private void registerAll(Unit[] units) {
        for (Unit unit : units) {
            registerBuiltIn(unit);
        }
    }

    /**
     * Registers a built-in unit without counting it toward the custom unit limit.
     */
    private void registerBuiltIn(Unit unit) {
        byName.put(unit.getName(), unit);
        byNameLower.put(unit.getName().toLowerCase(), unit);
    }

    /**
     * Registers a custom unit. If a unit with the same name already exists, it is replaced.
     *
     * @throws IllegalStateException if the registry has exceeded the maximum number of custom units
     */
    public void register(Unit unit) {
        synchronized (this) {
            if (byName.containsKey(unit.getName())) {
                // Replacing existing unit — no count change
                byName.put(unit.getName(), unit);
                byNameLower.put(unit.getName().toLowerCase(), unit);
                return;
            }
            if (customUnitCount >= MAX_CUSTOM_UNITS) {
                throw new IllegalStateException(
                        "Unit registry exceeded " + MAX_CUSTOM_UNITS
                                + " custom units — possible unbounded auto-creation");
            }
            byName.put(unit.getName(), unit);
            byNameLower.put(unit.getName().toLowerCase(), unit);
            customUnitCount++;
        }
    }

    /**
     * Resolves a unit by name. Case-sensitive first, then case-insensitive fallback.
     * If the name is not found, creates and registers a new
     * {@link systems.courant.sd.measure.units.item.ItemUnit} with that name.
     *
     * @param name the unit name
     * @return the resolved unit
     */
    public Unit resolve(String name) {
        Unit unit = find(name);
        if (unit != null) {
            return unit;
        }
        synchronized (this) {
            // Double-check after acquiring lock
            unit = find(name);
            if (unit != null) {
                return unit;
            }
            // Auto-create custom ItemUnit for unknown names
            // Treat "Dmnl", "units", "dimensionless" as equivalent to DIMENSIONLESS
            String lower = name.toLowerCase();
            if ("dmnl".equals(lower) || "units".equals(lower) || "unit".equals(lower)
                    || "dimensionless".equals(lower)) {
                Unit dmnl = systems.courant.sd.measure.units.dimensionless.DimensionlessUnits.DIMENSIONLESS;
                byName.put(name, dmnl);
                byNameLower.put(lower, dmnl);
                return dmnl;
            }
            if (!KNOWN_ACCEPTABLE_NAMES.contains(lower)) {
                log.warn("Auto-creating unit for unknown name '{}' — possible typo", name);
            }
            systems.courant.sd.measure.units.item.ItemUnit custom =
                    new systems.courant.sd.measure.units.item.ItemUnit(name);
            register(custom);
            return custom;
        }
    }

    /**
     * Resolves a time unit by name. Throws if the resolved unit is not a {@link TimeUnit}.
     *
     * @param name the time unit name
     * @return the resolved time unit
     * @throws IllegalArgumentException if the name does not resolve to a TimeUnit
     */
    public TimeUnit resolveTimeUnit(String name) {
        Unit unit = find(name);
        if (unit == null) {
            throw new IllegalArgumentException(
                    "'" + name + "' is not a known time unit");
        }
        if (unit instanceof TimeUnit timeUnit) {
            return timeUnit;
        }
        throw new IllegalArgumentException(
                "'" + name + "' is not a time unit");
    }

    /**
     * Finds a unit by name without auto-creating. Returns null if not found.
     */
    public Unit find(String name) {
        if (name == null) {
            return null;
        }
        Unit unit = byName.get(name);
        if (unit != null) {
            return unit;
        }
        return byNameLower.get(name.toLowerCase());
    }
}
