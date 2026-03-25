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
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = Math.pow(1.1, i);
            }
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("Exponential Growth");
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
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("Exponential Decay");
        }
    }

    @Nested
    @DisplayName("Goal-seeking (increasing)")
    class GoalSeekingUp {
        @Test
        void shouldDetectGoalSeekingIncrease() {
            double[] values = new double[50];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100 * (1 - Math.exp(-0.1 * i));
            }
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("Goal-Seeking");
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
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("Goal-Seeking");
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
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("Linear Growth");
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
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("Linear Decline");
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
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("Oscillation");
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
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("Equilibrium");
        }
    }

    @Nested
    @DisplayName("S-shaped growth")
    class SShapedGrowth {
        @Test
        void shouldDetectSShapedGrowth() {
            double[] values = new double[100];
            for (int i = 0; i < values.length; i++) {
                values[i] = 100.0 / (1.0 + Math.exp(-0.1 * (i - 50)));
            }
            assertThat(BehaviorModeClassifier.classify(values))
                    .isEqualTo("S-Shaped Growth");
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
        void shouldReturnEmptyForNullInput() {
            assertThat(BehaviorModeClassifier.classify(null)).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenDataContainsNaN() {
            double[] values = {1, 2, Double.NaN, 4, 5, 6, 7, 8};
            assertThat(BehaviorModeClassifier.classify(values)).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenDataContainsPositiveInfinity() {
            double[] values = {1, 2, 3, Double.POSITIVE_INFINITY, 5, 6, 7, 8};
            assertThat(BehaviorModeClassifier.classify(values)).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenDataContainsNegativeInfinity() {
            double[] values = {1, 2, 3, 4, Double.NEGATIVE_INFINITY, 6, 7, 8};
            assertThat(BehaviorModeClassifier.classify(values)).isEmpty();
        }
    }
}
