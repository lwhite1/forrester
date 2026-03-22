package systems.courant.sd.measure;

import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;
import systems.courant.sd.measure.units.item.ItemUnit;
import systems.courant.sd.measure.units.item.ItemUnits;
import systems.courant.sd.measure.units.length.LengthUnits;
import systems.courant.sd.measure.units.mass.MassUnits;
import systems.courant.sd.measure.units.money.MoneyUnits;
import systems.courant.sd.measure.units.money.NamedCurrency;
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
 * Custom units (e.g. {@link ItemUnit}) can be registered dynamically.
 *
 * <p>Lookup is case-sensitive with a case-insensitive fallback.
 */
public class UnitRegistry {

    private static final Logger log = LoggerFactory.getLogger(UnitRegistry.class);
    private static final int MAX_CUSTOM_UNITS = 10_000;

    /** Dimensionless-equivalent names (lowercase). Resolved to DIMENSIONLESS. */
    private static final Set<String> DIMENSIONLESS_NAMES = Set.of(
            "1", "dmnl", "units", "unit", "dimensionless",
            "fraction", "percent", "percentage", "ratio", "index");

    /**
     * Currency names (lowercase) that should be created as {@link NamedCurrency}
     * in the MONEY dimension rather than as ITEM units.
     */
    private static final Set<String> CURRENCY_NAMES = Set.of(
            // Major currencies
            "eur", "euro", "euros", "gbp", "jpy", "chf", "cad", "aud", "nzd",
            "cny", "yuan", "inr", "rupee", "rupees", "krw", "won",
            "sek", "nok", "dkk", "sgd", "hkd", "brl", "real", "reais",
            "mxn", "peso", "pesos", "zar", "rand",
            // USD aliases
            "dollar", "dollars", "$", "us$",
            // Scale-prefixed
            "m$", "thou $", "k$",
            // Generic money labels used in SD models
            "currency", "money", "cost", "revenue", "price");

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
        registerCurrencyAliases();
        registerAreaUnits();
    }

    /**
     * Registers common plural and abbreviated aliases for time units.
     * Vensim models use both singular ("Day") and plural ("Days") forms.
     */
    private void registerTimeUnitAliases() {
        synchronized (this) {
            for (TimeUnits tu : TimeUnits.values()) {
                String plural = tu.getName() + "s";
                byName.put(plural, tu);
                byNameLower.put(plural.toLowerCase(), tu);
            }
        }
    }

    /**
     * Pre-registers common currency aliases as MONEY-dimension units.
     */
    private void registerCurrencyAliases() {
        registerAlias("dollar", MoneyUnits.USD);
        registerAlias("dollars", MoneyUnits.USD);
        registerAlias("$", MoneyUnits.USD);
        registerAlias("US$", MoneyUnits.USD);
    }

    /**
     * Registers area units as composite Length^2 constants with conversion factors to m^2.
     */
    private void registerAreaUnits() {
        // hectare = 10,000 m^2
        registerBuiltIn(new AreaUnit("hectare", 10_000.0));
        registerBuiltIn(new AreaUnit("hectares", 10_000.0));
        registerBuiltIn(new AreaUnit("ha", 10_000.0));
        // km^2 = 1,000,000 m^2
        registerBuiltIn(new AreaUnit("km2", 1_000_000.0));
        registerBuiltIn(new AreaUnit("km²", 1_000_000.0));
        // acre = 4,046.8564224 m^2
        registerBuiltIn(new AreaUnit("acre", 4_046.8564224));
        registerBuiltIn(new AreaUnit("acres", 4_046.8564224));
    }

    /**
     * Registers an alias that maps to an existing unit.
     *
     * @param alias  the alias name
     * @param target the unit the alias resolves to
     */
    public void registerAlias(String alias, Unit target) {
        synchronized (this) {
            byName.put(alias, target);
            byNameLower.put(alias.toLowerCase(), target);
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
        synchronized (this) {
            byName.put(unit.getName(), unit);
            byNameLower.put(unit.getName().toLowerCase(), unit);
        }
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
     *
     * <p>If the name is not found:
     * <ul>
     *   <li>Dimensionless-equivalent names resolve to {@link DimensionlessUnits#DIMENSIONLESS}</li>
     *   <li>Currency names create a {@link NamedCurrency} in the MONEY dimension</li>
     *   <li>All other names create an {@link ItemUnit} in the ITEM dimension</li>
     * </ul>
     *
     * <p>Imported models are expected to carry arbitrary domain-specific names (e.g.,
     * "Deer", "Widget", "Ship"), so auto-creation is logged at DEBUG level.
     *
     * @param name the unit name
     * @return the resolved unit
     */
    public Unit resolve(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Unit name must not be null");
        }
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
            String lower = name.toLowerCase();

            // Dimensionless-equivalent names
            if (DIMENSIONLESS_NAMES.contains(lower)) {
                Unit dmnl = DimensionlessUnits.DIMENSIONLESS;
                byName.put(name, dmnl);
                byNameLower.put(lower, dmnl);
                return dmnl;
            }

            // Currency names → MONEY dimension
            if (CURRENCY_NAMES.contains(lower)) {
                log.debug("Auto-creating currency unit for '{}'", name);
                NamedCurrency currency = new NamedCurrency(name);
                register(currency);
                return currency;
            }

            // All other names → ITEM dimension
            log.debug("Auto-creating item unit for '{}'", name);
            ItemUnit custom = new ItemUnit(name);
            register(custom);
            return custom;
        }
    }

    /**
     * Resolves a compound unit string such as {@code "Deer/year"} or {@code "Deer/(year*lion)"}
     * into a {@link CompositeUnit}. Supports division ({@code /}), multiplication ({@code *}),
     * and parentheses for grouping. A leading {@code "1"} denotes dimensionless
     * (e.g. {@code "1/year"} → inverse-time).
     *
     * <p>Simple names without operators are resolved via {@link #resolve(String)} and
     * wrapped with {@link CompositeUnit#of(Unit)}.
     *
     * @param unitString the unit string to parse
     * @return the composite unit, or dimensionless for null/blank/dimensionless inputs
     */
    public CompositeUnit resolveComposite(String unitString) {
        if (unitString == null || unitString.isBlank()) {
            return CompositeUnit.dimensionless();
        }
        String trimmed = unitString.trim();

        if ("1".equals(trimmed) || DIMENSIONLESS_NAMES.contains(trimmed.toLowerCase())) {
            return CompositeUnit.dimensionless();
        }

        if (!hasUnitOperators(trimmed)) {
            return CompositeUnit.of(resolve(trimmed));
        }

        return new UnitStringParser(trimmed, this).parse();
    }

    private static boolean hasUnitOperators(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/' || c == '*' || c == '(' || c == ')') {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursive-descent parser for compound unit strings.
     * <pre>
     * expression → product ('/' product)?
     * product    → atom ('*' atom)*
     * atom       → '(' expression ')' | '1' | name
     * </pre>
     */
    private static final class UnitStringParser {
        private final String input;
        private final UnitRegistry registry;
        private int pos;

        UnitStringParser(String input, UnitRegistry registry) {
            this.input = input;
            this.registry = registry;
        }

        CompositeUnit parse() {
            pos = 0;
            CompositeUnit result = parseExpression();
            // If we didn't consume everything, fall back to treating the whole string as a name
            if (pos < input.length()) {
                return CompositeUnit.of(registry.resolve(input));
            }
            return result;
        }

        private CompositeUnit parseExpression() {
            CompositeUnit left = parseProduct();
            skipSpaces();
            if (pos < input.length() && input.charAt(pos) == '/') {
                pos++;
                CompositeUnit right = parseProduct();
                return left.divide(right);
            }
            return left;
        }

        private CompositeUnit parseProduct() {
            CompositeUnit result = parseAtom();
            while (pos < input.length()) {
                skipSpaces();
                if (pos < input.length() && input.charAt(pos) == '*') {
                    pos++;
                    result = result.multiply(parseAtom());
                } else {
                    break;
                }
            }
            return result;
        }

        private CompositeUnit parseAtom() {
            skipSpaces();
            if (pos >= input.length()) {
                return CompositeUnit.dimensionless();
            }
            if (input.charAt(pos) == '(') {
                pos++;
                CompositeUnit inner = parseExpression();
                skipSpaces();
                if (pos < input.length() && input.charAt(pos) == ')') {
                    pos++;
                }
                return inner;
            }
            String name = readName();
            if (name.isEmpty() || "1".equals(name)) {
                return CompositeUnit.dimensionless();
            }
            return CompositeUnit.of(registry.resolve(name));
        }

        private String readName() {
            skipSpaces();
            int start = pos;
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '/' || c == '*' || c == '(' || c == ')') {
                    break;
                }
                pos++;
            }
            return input.substring(start, pos).trim();
        }

        private void skipSpaces() {
            while (pos < input.length() && input.charAt(pos) == ' ') {
                pos++;
            }
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

    /**
     * A simple area unit with a conversion factor to square meters.
     */
    private static final class AreaUnit implements Unit {

        private final String name;
        private final double sqMeters;

        AreaUnit(String name, double sqMeters) {
            this.name = name;
            this.sqMeters = sqMeters;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Dimension getDimension() {
            return Dimension.AREA;
        }

        @Override
        public double ratioToBaseUnit() {
            return sqMeters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AreaUnit areaUnit = (AreaUnit) o;
            return name.equals(areaUnit.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
