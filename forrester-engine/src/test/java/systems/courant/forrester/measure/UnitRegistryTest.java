package systems.courant.forrester.measure;

import systems.courant.forrester.measure.units.item.ItemUnit;
import systems.courant.forrester.measure.units.time.TimeUnits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UnitRegistry")
class UnitRegistryTest {

    private final UnitRegistry registry = new UnitRegistry();

    @Test
    void shouldFindTimeUnits() {
        assertThat(registry.find("Day")).isSameAs(TimeUnits.DAY);
        assertThat(registry.find("Week")).isSameAs(TimeUnits.WEEK);
        assertThat(registry.find("Month")).isSameAs(TimeUnits.MONTH);
        assertThat(registry.find("Year")).isSameAs(TimeUnits.YEAR);
    }

    @Test
    void shouldResolveTimeUnit() {
        TimeUnit day = registry.resolveTimeUnit("Day");
        assertThat(day).isSameAs(TimeUnits.DAY);
    }

    @Test
    void shouldThrowForNonTimeUnit() {
        assertThatThrownBy(() -> registry.resolveTimeUnit("Person"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFindItemUnits() {
        assertThat(registry.find("Person")).isNotNull();
        assertThat(registry.find("Thing")).isNotNull();
    }

    @Test
    void shouldFindDimensionlessUnit() {
        assertThat(registry.find("Dimensionless unit")).isNotNull();
    }

    @Test
    void shouldFindCaseInsensitive() {
        assertThat(registry.find("day")).isNotNull();
        assertThat(registry.find("DAY")).isNotNull();
    }

    @Test
    void shouldAutoCreateCustomUnit() {
        Unit widget = registry.resolve("Widget");
        assertThat(widget).isNotNull();
        assertThat(widget.getName()).isEqualTo("Widget");
        assertThat(widget).isInstanceOf(ItemUnit.class);
    }

    @Test
    void shouldReturnSameCustomUnitOnReResolve() {
        Unit first = registry.resolve("Error");
        Unit second = registry.resolve("Error");
        assertThat(second).isSameAs(first);
    }

    @Test
    void shouldRegisterCustomUnit() {
        ItemUnit custom = new ItemUnit("Defect");
        registry.register(custom);
        assertThat(registry.find("Defect")).isSameAs(custom);
    }

    @Test
    void shouldReturnNullForUnknownFind() {
        assertThat(registry.find("NonExistentUnit12345")).isNull();
    }

    @Test
    void shouldNotLeaveSpuriousUnitAfterResolveTimeUnitFails() {
        String unknownName = "SomeCustomThing";
        assertThatThrownBy(() -> registry.resolveTimeUnit(unknownName))
                .isInstanceOf(IllegalArgumentException.class);
        // The failed resolveTimeUnit should not have auto-created a unit
        assertThat(registry.find(unknownName)).isNull();
    }
}
