package systems.courant.forrester.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReferenceDataset")
class ReferenceDatasetTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create dataset with valid inputs")
        void shouldCreateWithValidInputs() {
            ReferenceDataset ds = new ReferenceDataset(
                    "Test Data",
                    new double[]{0, 1, 2},
                    Map.of("Pop", new double[]{100, 110, 120})
            );
            assertThat(ds.name()).isEqualTo("Test Data");
            assertThat(ds.timeValues()).containsExactly(0, 1, 2);
            assertThat(ds.columns().get("Pop")).containsExactly(100, 110, 120);
            assertThat(ds.size()).isEqualTo(3);
            assertThat(ds.variableNames()).containsExactly("Pop");
        }

        @Test
        @DisplayName("should reject blank name")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new ReferenceDataset(
                    "", new double[]{0}, Map.of("X", new double[]{1})))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should reject null time values")
        void shouldRejectNullTimeValues() {
            assertThatThrownBy(() -> new ReferenceDataset(
                    "Test", null, Map.of("X", new double[]{1})))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("time value");
        }

        @Test
        @DisplayName("should reject empty time values")
        void shouldRejectEmptyTimeValues() {
            assertThatThrownBy(() -> new ReferenceDataset(
                    "Test", new double[]{}, Map.of("X", new double[]{})))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("time value");
        }

        @Test
        @DisplayName("should reject empty columns")
        void shouldRejectEmptyColumns() {
            assertThatThrownBy(() -> new ReferenceDataset(
                    "Test", new double[]{0}, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("data column");
        }

        @Test
        @DisplayName("should reject mismatched column length")
        void shouldRejectMismatchedColumnLength() {
            assertThatThrownBy(() -> new ReferenceDataset(
                    "Test", new double[]{0, 1}, Map.of("X", new double[]{1})))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Column 'X'")
                    .hasMessageContaining("1 values but expected 2");
        }

        @Test
        @DisplayName("should defensively copy arrays")
        void shouldDefensivelyCopy() {
            double[] times = {0, 1, 2};
            double[] values = {10, 20, 30};
            ReferenceDataset ds = new ReferenceDataset("Test", times, Map.of("X", values));

            times[0] = 999;
            values[0] = 999;

            assertThat(ds.timeValues()[0]).isEqualTo(0);
            assertThat(ds.columns().get("X")[0]).isEqualTo(10);
        }

        @Test
        @DisplayName("should support multiple columns")
        void shouldSupportMultipleColumns() {
            ReferenceDataset ds = new ReferenceDataset(
                    "Multi", new double[]{0, 1},
                    Map.of("A", new double[]{1, 2}, "B", new double[]{3, 4})
            );
            assertThat(ds.variableNames()).hasSize(2);
            assertThat(ds.columns()).containsKeys("A", "B");
        }
    }
}
