package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UnitRegistry")
class UnitRegistryTest {

    private final UnitRegistry registry = new UnitRegistry();

    @Test
    void shouldFindTimeUnits() {
        assertSame(TimeUnits.DAY, registry.find("Day"));
        assertSame(TimeUnits.WEEK, registry.find("Week"));
        assertSame(TimeUnits.MONTH, registry.find("Month"));
        assertSame(TimeUnits.YEAR, registry.find("Year"));
    }

    @Test
    void shouldResolveTimeUnit() {
        TimeUnit day = registry.resolveTimeUnit("Day");
        assertSame(TimeUnits.DAY, day);
    }

    @Test
    void shouldThrowForNonTimeUnit() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.resolveTimeUnit("Person"));
    }

    @Test
    void shouldFindItemUnits() {
        assertNotNull(registry.find("Person"));
        assertNotNull(registry.find("Thing"));
    }

    @Test
    void shouldFindDimensionlessUnit() {
        assertNotNull(registry.find("Dimensionless unit"));
    }

    @Test
    void shouldFindCaseInsensitive() {
        assertNotNull(registry.find("day"));
        assertNotNull(registry.find("DAY"));
    }

    @Test
    void shouldAutoCreateCustomUnit() {
        Unit widget = registry.resolve("Widget");
        assertNotNull(widget);
        assertEquals("Widget", widget.getName());
        assertInstanceOf(ItemUnit.class, widget);
    }

    @Test
    void shouldReturnSameCustomUnitOnReResolve() {
        Unit first = registry.resolve("Error");
        Unit second = registry.resolve("Error");
        assertSame(first, second);
    }

    @Test
    void shouldRegisterCustomUnit() {
        ItemUnit custom = new ItemUnit("Defect");
        registry.register(custom);
        assertSame(custom, registry.find("Defect"));
    }

    @Test
    void shouldReturnNullForUnknownFind() {
        assertNull(registry.find("NonExistentUnit12345"));
    }

    @Test
    void shouldNotLeaveSpuriousUnitAfterResolveTimeUnitFails() {
        String unknownName = "SomeCustomThing";
        assertThrows(IllegalArgumentException.class,
                () -> registry.resolveTimeUnit(unknownName));
        // The failed resolveTimeUnit should not have auto-created a unit
        assertNull(registry.find(unknownName));
    }
}
