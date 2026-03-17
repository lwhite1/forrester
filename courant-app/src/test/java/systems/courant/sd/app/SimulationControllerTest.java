package systems.courant.sd.app;

import systems.courant.sd.app.canvas.dialogs.MonteCarloDialog;
import systems.courant.sd.app.canvas.dialogs.MonteCarloDialog.DistributionType;
import systems.courant.sd.app.canvas.dialogs.MonteCarloDialog.ParameterConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulationController")
class SimulationControllerTest {

    @Nested
    @DisplayName("validateDistributionParameters")
    class ValidateDistributionParameters {

        // ---- Normal distribution ----

        @Test
        void shouldAcceptNormalDistributionWithPositiveSigma() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("alpha", DistributionType.NORMAL, 10.0, 2.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldRejectNormalDistributionWithZeroSigma() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("alpha", DistributionType.NORMAL, 10.0, 0.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result)
                    .contains("alpha")
                    .contains("positive standard deviation")
                    .contains("0.0");
        }

        @Test
        void shouldRejectNormalDistributionWithNegativeSigma() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("beta", DistributionType.NORMAL, 5.0, -1.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result)
                    .contains("beta")
                    .contains("positive standard deviation")
                    .contains("-1.0");
        }

        @Test
        void shouldAcceptNormalDistributionWithNegativeMean() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("gamma", DistributionType.NORMAL, -5.0, 1.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldAcceptNormalDistributionWithZeroMean() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("delta", DistributionType.NORMAL, 0.0, 3.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldAcceptNormalDistributionWithVerySmallSigma() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("epsilon", DistributionType.NORMAL, 0.0, 0.001));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result).isEmpty();
        }

        // ---- Uniform distribution ----

        @Test
        void shouldAcceptUniformDistributionWithMinLessThanMax() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("alpha", DistributionType.UNIFORM, 1.0, 10.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldRejectUniformDistributionWithMinEqualToMax() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("alpha", DistributionType.UNIFORM, 5.0, 5.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result)
                    .contains("alpha")
                    .contains("min < max")
                    .contains("min=5.0")
                    .contains("max=5.0");
        }

        @Test
        void shouldRejectUniformDistributionWithMinGreaterThanMax() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("beta", DistributionType.UNIFORM, 10.0, 1.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result)
                    .contains("beta")
                    .contains("min < max")
                    .contains("min=10.0")
                    .contains("max=1.0");
        }

        @Test
        void shouldAcceptUniformDistributionWithNegativeRange() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("gamma", DistributionType.UNIFORM, -10.0, -1.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result).isEmpty();
        }

        // ---- Multiple parameters ----

        @Test
        void shouldAcceptMultipleValidParameters() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("alpha", DistributionType.NORMAL, 0.0, 1.0),
                    new ParameterConfig("beta", DistributionType.UNIFORM, 0.0, 10.0),
                    new ParameterConfig("gamma", DistributionType.NORMAL, 5.0, 0.5));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReportFirstInvalidParameterWhenMultipleAreInvalid() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("alpha", DistributionType.NORMAL, 0.0, 1.0),
                    new ParameterConfig("beta", DistributionType.NORMAL, 5.0, 0.0),
                    new ParameterConfig("gamma", DistributionType.UNIFORM, 10.0, 1.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result).contains("beta");
        }

        @Test
        void shouldReportInvalidUniformAfterValidNormal() {
            List<ParameterConfig> params = List.of(
                    new ParameterConfig("alpha", DistributionType.NORMAL, 0.0, 1.0),
                    new ParameterConfig("beta", DistributionType.UNIFORM, 5.0, 5.0));

            String result = SimulationController.validateDistributionParameters(params);

            assertThat(result)
                    .contains("beta")
                    .contains("min < max");
        }

        // ---- Empty list ----

        @Test
        void shouldAcceptEmptyParameterList() {
            String result = SimulationController.validateDistributionParameters(List.of());

            assertThat(result).isEmpty();
        }
    }
}
