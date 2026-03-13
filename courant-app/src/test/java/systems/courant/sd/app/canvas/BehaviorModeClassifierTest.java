package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BehaviorModeClassifier")
class BehaviorModeClassifierTest {

    @Nested
    @DisplayName("Exponential growth")
    class ExponentialGrowth {
        @Test
        void shouldDetectExponentialGrowth() {
            // y = 2^x
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = Math.pow(1.1, i);
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("Exponential growth");
        }
    }

    @Nested
    @DisplayName("Exponential decay")
    class ExponentialDecay {
        @Test
        void shouldDetectExponentialDecay() {
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100 * Math.pow(0.9, i);
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("Exponential decay");
        }
    }

    @Nested
    @DisplayName("Goal-seeking (increasing)")
    class GoalSeekingUp {
        @Test
        void shouldDetectGoalSeekingIncrease() {
            // Asymptotic approach to 100 from below
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100 * (1 - Math.exp(-0.1 * i));
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("Goal-seeking");
        }
    }

    @Nested
    @DisplayName("Goal-seeking (decreasing)")
    class GoalSeekingDown {
        @Test
        void shouldDetectGoalSeekingDecrease() {
            // Asymptotic approach to 10 from above (starting at 100)
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = 10 + 90 * Math.exp(-0.1 * i);
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("Goal-seeking");
        }
    }

    @Nested
    @DisplayName("Linear growth")
    class LinearGrowth {
        @Test
        void shouldDetectLinearGrowth() {
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = 10 + 2.0 * i;
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("Linear growth");
        }
    }

    @Nested
    @DisplayName("Linear decline")
    class LinearDecline {
        @Test
        void shouldDetectLinearDecline() {
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100 - 1.5 * i;
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("Linear decline");
        }
    }

    @Nested
    @DisplayName("Oscillation")
    class Oscillation {
        @Test
        void shouldDetectOscillation() {
            double[] values = new double[100];
            for (int i = 0; i < values.length; i++) {
                values[i] = 50 + 20 * Math.sin(0.2 * i);
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("Oscillation");
        }
    }

    @Nested
    @DisplayName("Equilibrium")
    class Equilibrium {
        @Test
        void shouldDetectEquilibrium() {
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = 42.0;
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("Equilibrium");
        }
    }

    @Nested
    @DisplayName("S-shaped growth")
    class SShapedGrowth {
        @Test
        void shouldDetectSShapedGrowth() {
            // Logistic curve
            double[] values = new double[100];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100.0 / (1.0 + Math.exp(-0.1 * (i - 50)));
            }
            assertThat(BehaviorModeClassifier.classify(values)).isEqualTo("S-shaped growth");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {
        @Test
        void shouldReturnEmptyForTooFewPoints() {
            assertThat(BehaviorModeClassifier.classify(new double[]{1, 2, 3})).isEmpty();
        }

        @Test
        void shouldReturnEmptyForFourPointMixedSeries() {
            assertThat(BehaviorModeClassifier.classify(new double[]{1, 5, 2, 8})).isEmpty();
        }
    }
}
