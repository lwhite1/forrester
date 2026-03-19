package systems.courant.sd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TimeSeriesDef")
class TimeSeriesDefTest {

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void shouldRejectEmptyArrays() {
            assertThatThrownBy(() -> new TimeSeriesDef("ts", new double[0], new double[0], "unit"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2");
        }

        @Test
        @DisplayName("should reject single data point (#1050)")
        void shouldRejectSingleDataPoint() {
            assertThatThrownBy(() -> new TimeSeriesDef("ts", new double[]{1.0}, new double[]{2.0}, "unit"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2");
        }

        @Test
        void shouldAcceptTwoDataPoints() {
            TimeSeriesDef def = new TimeSeriesDef("ts", new double[]{0, 1}, new double[]{5, 10}, "unit");
            assertThat(def.name()).isEqualTo("ts");
            assertThat(def.timeValues()).containsExactly(0, 1);
            assertThat(def.dataValues()).containsExactly(5, 10);
        }

        @Test
        void shouldRejectMismatchedLengths() {
            assertThatThrownBy(() -> new TimeSeriesDef("ts", new double[]{0, 1}, new double[]{5}, "unit"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same length");
        }

        @Test
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new TimeSeriesDef("", new double[]{0, 1}, new double[]{5, 10}, "unit"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }
    }
}
