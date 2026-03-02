package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.measure.units.length.LengthUnits;
import com.deathrayresearch.forrester.measure.units.mass.MassUnits;
import com.deathrayresearch.forrester.measure.units.money.MoneyUnits;
import com.deathrayresearch.forrester.measure.units.temperature.TemperatureUnits;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.measure.units.volume.VolumeUnits;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps unit name strings to {@link Unit} objects. Auto-registers all built-in unit enums.
 * Custom units (e.g. {@link com.deathrayresearch.forrester.measure.units.item.ItemUnit})
 * can be registered dynamically.
 *
 * <p>Lookup is case-sensitive with a case-insensitive fallback.
 */
public class UnitRegistry {

    private final Map<String, Unit> byName = new LinkedHashMap<>();
    private final Map<String, Unit> byNameLower = new LinkedHashMap<>();

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
    }

    private void registerAll(Unit[] units) {
        for (Unit unit : units) {
            register(unit);
        }
    }

    /**
     * Registers a unit. If a unit with the same name already exists, it is replaced.
     */
    public void register(Unit unit) {
        byName.put(unit.getName(), unit);
        byNameLower.put(unit.getName().toLowerCase(), unit);
    }

    /**
     * Resolves a unit by name. Case-sensitive first, then case-insensitive fallback.
     * If the name is not found, creates and registers a new
     * {@link com.deathrayresearch.forrester.measure.units.item.ItemUnit} with that name.
     *
     * @param name the unit name
     * @return the resolved unit
     */
    public Unit resolve(String name) {
        Unit unit = find(name);
        if (unit != null) {
            return unit;
        }
        // Auto-create custom ItemUnit for unknown names
        com.deathrayresearch.forrester.measure.units.item.ItemUnit custom =
                new com.deathrayresearch.forrester.measure.units.item.ItemUnit(name);
        register(custom);
        return custom;
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
        Unit unit = byName.get(name);
        if (unit != null) {
            return unit;
        }
        return byNameLower.get(name.toLowerCase());
    }
}
