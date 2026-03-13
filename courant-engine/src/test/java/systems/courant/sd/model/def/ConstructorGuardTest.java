package systems.courant.sd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for validation guards in record constructors (T2 from code audit).
 */
@DisplayName("Constructor validation guards")
class ConstructorGuardTest {

    @Nested
    @DisplayName("StockDef")
    class StockDefGuards {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        void shouldRejectBlankOrNullName(String name) {
            assertThatThrownBy(() -> new StockDef(name, 100, "Person"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void shouldRejectNonFiniteInitialValue(double value) {
            assertThatThrownBy(() -> new StockDef("S", value, "Person"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("finite");
        }

        @Test
        void shouldAcceptNonFiniteWhenInitialExpressionProvided() {
            assertThatNoException().isThrownBy(() ->
                    new StockDef("S", null, Double.NaN, "10 + 5", "Person", null, null));
        }

        @Test
        void shouldAcceptValidStock() {
            assertThatNoException().isThrownBy(() -> new StockDef("Population", 1000, "Person"));
        }

        @Test
        void shouldDefaultSubscriptsToEmptyList() {
            StockDef stock = new StockDef("S", null, 0, null, "Unit", null, null);
            assertThat(stock.subscripts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("SimulationSettings")
    class SimulationSettingsGuards {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void shouldRejectBlankOrNullTimeStep(String timeStep) {
            assertThatThrownBy(() -> new SimulationSettings(timeStep, 100, "Day"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Time step");
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void shouldRejectNonFiniteDuration(double duration) {
            assertThatThrownBy(() -> new SimulationSettings("Day", duration, "Day"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("finite");
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, -1, -100})
        void shouldRejectNonPositiveDuration(double duration) {
            assertThatThrownBy(() -> new SimulationSettings("Day", duration, "Day"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void shouldRejectBlankOrNullDurationUnit(String unit) {
            assertThatThrownBy(() -> new SimulationSettings("Day", 100, unit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Duration unit");
        }

        @Test
        void shouldAcceptValidSettings() {
            assertThatNoException().isThrownBy(
                    () -> new SimulationSettings("Day", 365, "Day"));
        }
    }

    @Nested
    @DisplayName("LookupTableDef")
    class LookupTableDefGuards {

        @Test
        void shouldRejectNaNInYValues() {
            assertThatThrownBy(() -> new LookupTableDef("tbl",
                    new double[]{0, 1, 2},
                    new double[]{1.0, Double.NaN, 3.0},
                    "LINEAR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("finite");
        }

        @Test
        void shouldRejectAllNaNYValues() {
            assertThatThrownBy(() -> new LookupTableDef("tbl",
                    new double[]{0, 1},
                    new double[]{Double.NaN, Double.NaN},
                    "LINEAR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("finite");
        }

        @Test
        void shouldRejectInfiniteInYValues() {
            assertThatThrownBy(() -> new LookupTableDef("tbl",
                    new double[]{0, 1},
                    new double[]{1.0, Double.POSITIVE_INFINITY},
                    "LINEAR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("finite");
        }

        @Test
        void shouldRejectNaNInXValues() {
            assertThatThrownBy(() -> new LookupTableDef("tbl",
                    new double[]{0, Double.NaN},
                    new double[]{1.0, 2.0},
                    "LINEAR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("finite");
        }

        @Test
        void shouldAcceptValidLookupTable() {
            assertThatNoException().isThrownBy(() -> new LookupTableDef("tbl",
                    new double[]{0, 1, 2},
                    new double[]{0, 0.5, 1.0},
                    "LINEAR"));
        }
    }
}
