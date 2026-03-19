package systems.courant.sd.model.graph;

import systems.courant.sd.model.graph.BehaviorClassification.Mode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BehaviorClassification")
class BehaviorClassificationTest {

    @Nested
    @DisplayName("classify()")
    class Classify {

        @Test
        @DisplayName("should classify exponential growth")
        void shouldClassifyExponentialGrowth() {
            // Values that increase with accelerating rate
            double[] values = new double[20];
            for (int i = 0; i < values.length; i++) {
                values[i] = Math.exp(0.2 * i);
            }
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.EXPONENTIAL_GROWTH);
        }

        @Test
        @DisplayName("should classify exponential decay")
        void shouldClassifyExponentialDecay() {
            double[] values = new double[20];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100 * Math.exp(-0.2 * i);
            }
            assertThat(BehaviorClassification.classify(values))
                    .isIn(Mode.EXPONENTIAL_DECAY, Mode.GOAL_SEEKING);
        }

        @Test
        @DisplayName("should classify goal-seeking behavior")
        void shouldClassifyGoalSeeking() {
            // Asymptotic approach: 100 * (1 - e^(-t))
            double[] values = new double[20];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100 * (1 - Math.exp(-0.3 * i));
            }
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.GOAL_SEEKING);
        }

        @Test
        @DisplayName("should classify oscillation")
        void shouldClassifyOscillation() {
            double[] values = new double[40];
            for (int i = 0; i < values.length; i++) {
                values[i] = 50 + 30 * Math.sin(0.5 * i);
            }
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.OSCILLATION);
        }

        @Test
        @DisplayName("should classify S-shaped growth")
        void shouldClassifySShapedGrowth() {
            // Logistic function: K / (1 + e^(-r*(t-t0)))
            double[] values = new double[40];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100.0 / (1.0 + Math.exp(-0.3 * (i - 20)));
            }
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.S_SHAPED_GROWTH);
        }

        @Test
        @DisplayName("should classify overshoot and collapse")
        void shouldClassifyOvershootAndCollapse() {
            // Rise then fall: e.g. epidemic curve
            double[] values = new double[20];
            for (int i = 0; i < 10; i++) {
                values[i] = i * 10.0; // rising
            }
            for (int i = 10; i < 20; i++) {
                values[i] = (20 - i) * 10.0; // falling
            }
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.OVERSHOOT_AND_COLLAPSE);
        }

        @Test
        @DisplayName("should classify equilibrium for flat series")
        void shouldClassifyEquilibrium() {
            double[] values = {50.0, 50.0, 50.0, 50.0, 50.0};
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.EQUILIBRIUM);
        }

        @Test
        @DisplayName("should classify linear growth")
        void shouldClassifyLinearGrowth() {
            double[] values = new double[20];
            for (int i = 0; i < values.length; i++) {
                values[i] = 10 + 5.0 * i;
            }
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.LINEAR_GROWTH);
        }

        @Test
        @DisplayName("should classify linear decline")
        void shouldClassifyLinearDecline() {
            double[] values = new double[20];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100 - 5.0 * i;
            }
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.LINEAR_DECLINE);
        }

        @Test
        @DisplayName("should return equilibrium for null or short series")
        void shouldReturnEquilibriumForInvalidInput() {
            assertThat(BehaviorClassification.classify(null))
                    .isEqualTo(Mode.EQUILIBRIUM);
            assertThat(BehaviorClassification.classify(new double[]{1, 2}))
                    .isEqualTo(Mode.EQUILIBRIUM);
            assertThat(BehaviorClassification.classify(new double[0]))
                    .isEqualTo(Mode.EQUILIBRIUM);
        }

        @Test
        @DisplayName("should handle NaN values in series")
        void shouldHandleNaNValues() {
            double[] values = {1, Double.NaN, 3, 4, 5, 6, 7, 8};
            Mode mode = BehaviorClassification.classify(values);
            assertThat(mode).isNotNull();
        }

        @Test
        @DisplayName("should handle series with all NaN")
        void shouldHandleAllNaN() {
            double[] values = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
            assertThat(BehaviorClassification.classify(values))
                    .isEqualTo(Mode.EQUILIBRIUM);
        }
    }

    @Nested
    @DisplayName("classifyAll()")
    class ClassifyAll {

        @Test
        @DisplayName("should classify multiple series")
        void shouldClassifyMultipleSeries() {
            double[] growing = new double[20];
            double[] flat = new double[20];
            for (int i = 0; i < 20; i++) {
                growing[i] = Math.exp(0.2 * i);
                flat[i] = 50.0;
            }

            Map<String, BehaviorClassification.Result> results =
                    BehaviorClassification.classifyAll(
                            List.of("Population", "Constant"),
                            List.of(growing, flat));

            assertThat(results).hasSize(2);
            assertThat(results.get("Population").mode())
                    .isEqualTo(Mode.EXPONENTIAL_GROWTH);
            assertThat(results.get("Constant").mode())
                    .isEqualTo(Mode.EQUILIBRIUM);
        }

        @Test
        @DisplayName("should throw on mismatched list sizes")
        void shouldThrowOnMismatchedSizes() {
            assertThatThrownBy(() ->
                    BehaviorClassification.classifyAll(
                            List.of("A", "B"),
                            List.of(new double[]{1, 2, 3, 4})))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("classifyToList()")
    class ClassifyToList {

        @Test
        @DisplayName("should return ordered list of results")
        void shouldReturnOrderedList() {
            double[] series = new double[20];
            for (int i = 0; i < 20; i++) {
                series[i] = 10 + 5.0 * i;
            }

            List<BehaviorClassification.Result> results =
                    BehaviorClassification.classifyToList(
                            List.of("Linear"),
                            List.of(series));

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().variableName()).isEqualTo("Linear");
            assertThat(results.getFirst().mode()).isEqualTo(Mode.LINEAR_GROWTH);
        }
    }

    @Nested
    @DisplayName("Mode enum")
    class ModeEnum {

        @Test
        @DisplayName("should have display names for all modes")
        void shouldHaveDisplayNames() {
            for (Mode mode : Mode.values()) {
                assertThat(mode.displayName()).isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("internal helpers")
    class InternalHelpers {

        @Test
        @DisplayName("derivative should compute differences")
        void derivativeShouldComputeDifferences() {
            double[] d = BehaviorClassification.derivative(new double[]{1, 3, 6, 10});
            assertThat(d).containsExactly(2.0, 3.0, 4.0);
        }

        @Test
        @DisplayName("countSignChanges should detect direction reversals")
        void countSignChangesShouldWork() {
            assertThat(BehaviorClassification.countSignChanges(new double[]{1, 2, -1, -2, 3}))
                    .isEqualTo(2);
            assertThat(BehaviorClassification.countSignChanges(new double[]{1, 2, 3}))
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("filterFinite should remove non-finite values")
        void filterFiniteShouldWork() {
            double[] filtered = BehaviorClassification.filterFinite(
                    new double[]{1, Double.NaN, 3, Double.POSITIVE_INFINITY, 5});
            assertThat(filtered).containsExactly(1.0, 3.0, 5.0);
        }

        @Test
        @DisplayName("filterFinite should return same array when all finite")
        void filterFiniteShouldReturnSameArray() {
            double[] input = {1, 2, 3};
            assertThat(BehaviorClassification.filterFinite(input)).isSameAs(input);
        }
    }
}
