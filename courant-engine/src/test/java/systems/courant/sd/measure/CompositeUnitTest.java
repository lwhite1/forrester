package systems.courant.sd.measure;

import systems.courant.sd.measure.units.item.ItemUnits;
import systems.courant.sd.measure.units.mass.MassUnits;
import systems.courant.sd.measure.units.time.TimeUnits;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompositeUnit")
class CompositeUnitTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        void shouldCreateDimensionless() {
            CompositeUnit unit = CompositeUnit.dimensionless();
            assertThat(unit.isDimensionless()).isTrue();
            assertThat(unit.exponents()).isEmpty();
        }

        @Test
        void shouldCreateFromSingleUnit() {
            CompositeUnit unit = CompositeUnit.of(ItemUnits.PEOPLE);
            assertThat(unit.isDimensionless()).isFalse();
            assertThat(unit.exponents()).containsEntry(Dimension.ITEM, 1);
            assertThat(unit.exponents()).hasSize(1);
        }

        @Test
        void shouldCreateRateUnit() {
            CompositeUnit rate = CompositeUnit.ofRate(ItemUnits.PEOPLE, TimeUnits.DAY);
            assertThat(rate.exponents()).containsEntry(Dimension.ITEM, 1);
            assertThat(rate.exponents()).containsEntry(Dimension.TIME, -1);
            assertThat(rate.exponents()).hasSize(2);
        }

        @Test
        void shouldCreateRateWithNullMaterial() {
            CompositeUnit rate = CompositeUnit.ofRate(null, TimeUnits.DAY);
            assertThat(rate.exponents()).containsEntry(Dimension.TIME, -1);
            assertThat(rate.exponents()).hasSize(1);
        }

        @Test
        void shouldReturnDimensionlessForDimensionlessUnit() {
            CompositeUnit unit = CompositeUnit.of(
                    systems.courant.sd.measure.units.dimensionless.DimensionlessUnits.DIMENSIONLESS);
            assertThat(unit.isDimensionless()).isTrue();
        }
    }

    @Nested
    @DisplayName("Arithmetic")
    class Arithmetic {

        @Test
        void shouldMultiplyDimensions() {
            CompositeUnit items = CompositeUnit.of(ItemUnits.PEOPLE);
            CompositeUnit time = new CompositeUnit(Map.of(Dimension.TIME, 1));
            CompositeUnit result = items.multiply(time);
            assertThat(result.exponents()).containsEntry(Dimension.ITEM, 1);
            assertThat(result.exponents()).containsEntry(Dimension.TIME, 1);
        }

        @Test
        void shouldDivideDimensions() {
            CompositeUnit items = CompositeUnit.of(ItemUnits.PEOPLE);
            CompositeUnit time = new CompositeUnit(Map.of(Dimension.TIME, 1));
            CompositeUnit rate = items.divide(time);
            assertThat(rate.exponents()).containsEntry(Dimension.ITEM, 1);
            assertThat(rate.exponents()).containsEntry(Dimension.TIME, -1);
        }

        @Test
        void shouldCancelDimensionsOnDivide() {
            CompositeUnit items = CompositeUnit.of(ItemUnits.PEOPLE);
            CompositeUnit result = items.divide(items);
            assertThat(result.isDimensionless()).isTrue();
        }

        @Test
        void shouldPowerDimensions() {
            CompositeUnit length = new CompositeUnit(Map.of(Dimension.LENGTH, 1));
            CompositeUnit area = length.power(2);
            assertThat(area.exponents()).containsEntry(Dimension.LENGTH, 2);
        }

        @Test
        void shouldReturnDimensionlessForPowerZero() {
            CompositeUnit length = new CompositeUnit(Map.of(Dimension.LENGTH, 1));
            CompositeUnit result = length.power(0);
            assertThat(result.isDimensionless()).isTrue();
        }
    }

    @Nested
    @DisplayName("Compatibility")
    class Compatibility {

        @Test
        void shouldBeCompatibleWithSameDimensions() {
            CompositeUnit a = CompositeUnit.of(ItemUnits.PEOPLE);
            CompositeUnit b = CompositeUnit.of(ItemUnits.THING);
            // Both are ITEM dimension
            assertThat(a.isCompatibleWith(b)).isTrue();
        }

        @Test
        void shouldBeIncompatibleWithDifferentDimensions() {
            CompositeUnit items = CompositeUnit.of(ItemUnits.PEOPLE);
            CompositeUnit mass = CompositeUnit.of(MassUnits.KILOGRAM);
            assertThat(items.isCompatibleWith(mass)).isFalse();
        }

        @Test
        void shouldBeCompatibleBothDimensionless() {
            assertThat(CompositeUnit.dimensionless()
                    .isCompatibleWith(CompositeUnit.dimensionless())).isTrue();
        }
    }

    @Nested
    @DisplayName("Display")
    class Display {

        @Test
        void shouldDisplayDimensionless() {
            assertThat(CompositeUnit.dimensionless().displayString()).isEqualTo("Dimensionless");
        }

        @Test
        void shouldDisplaySingleDimension() {
            CompositeUnit items = CompositeUnit.of(ItemUnits.PEOPLE);
            // Base unit for ITEM dimension is "Thing"
            assertThat(items.displayString()).isEqualTo("Thing");
        }

        @Test
        void shouldDisplayRate() {
            CompositeUnit rate = CompositeUnit.ofRate(ItemUnits.PEOPLE, TimeUnits.DAY);
            String display = rate.displayString();
            assertThat(display).contains("/");
            assertThat(display).contains("Thing");
        }

        @Test
        void shouldDisplaySquaredDimension() {
            CompositeUnit area = new CompositeUnit(Map.of(Dimension.LENGTH, 2));
            assertThat(area.displayString()).contains("^2");
        }
    }
}
