package systems.courant.shrewd.sweep;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Variable;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.Stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.PEOPLE;
import static systems.courant.shrewd.measure.units.time.Times.weeks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitivitySummaryTest {

    @Test
    void shouldRankSweepSensitivity() {
        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(new double[]{2.0, 8.0, 14.0})
                .modelFactory(this::buildSirModel)
                .timeStep(DAY)
                .duration(weeks(8))
                .build()
                .execute();

        List<SensitivitySummary.ParameterImpact> impacts =
                SensitivitySummary.fromSweep(result, "Infectious");

        assertEquals(1, impacts.size());
        assertEquals("Contact Rate", impacts.getFirst().parameterName());
        assertEquals("Infectious", impacts.getFirst().targetVariable());
        assertEquals(1.0, impacts.getFirst().impactFraction(),
                "Single-parameter sweep should always be 100%");
    }

    @Test
    void shouldRankMultiSweepByMagnitude() {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Contact Rate", new double[]{2.0, 8.0, 14.0})
                .parameter("Recovery Rate", new double[]{0.1, 0.2, 0.3})
                .modelFactory(params -> buildSirModelTwoParams(
                        params.get("Contact Rate"), params.get("Recovery Rate")))
                .timeStep(DAY)
                .duration(weeks(8))
                .build()
                .execute();

        List<SensitivitySummary.ParameterImpact> impacts =
                SensitivitySummary.fromMultiSweep(result, "Infectious");

        assertEquals(2, impacts.size());

        // Fractions should be sorted descending
        assertTrue(impacts.get(0).impactFraction() >= impacts.get(1).impactFraction(),
                "Impacts should be sorted by magnitude descending");

        // Each fraction should be between 0 and 1
        for (var impact : impacts) {
            assertTrue(impact.impactFraction() >= 0 && impact.impactFraction() <= 1.0,
                    impact.parameterName() + " fraction should be 0-1 but was "
                            + impact.impactFraction());
        }

        // Fractions should sum to approximately 1.0
        double sum = impacts.stream().mapToDouble(SensitivitySummary.ParameterImpact::impactFraction).sum();
        assertEquals(1.0, sum, 0.001, "Fractions should sum to 1.0");
    }

    @Test
    void shouldProducePlainLanguageSummary() {
        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(new double[]{2.0, 8.0, 14.0})
                .modelFactory(this::buildSirModel)
                .timeStep(DAY)
                .duration(weeks(8))
                .build()
                .execute();

        List<SensitivitySummary.ParameterImpact> impacts =
                SensitivitySummary.fromSweep(result, "Infectious");
        String summary = SensitivitySummary.toPlainLanguage(impacts);

        assertFalse(summary.isEmpty());
        assertTrue(summary.contains("Contact Rate"), "Should mention parameter name");
        assertTrue(summary.contains("Infectious"), "Should mention target variable");
    }

    @Test
    void shouldProduceMultiSweepPlainLanguage() {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Contact Rate", new double[]{2.0, 8.0, 14.0})
                .parameter("Recovery Rate", new double[]{0.1, 0.2, 0.3})
                .modelFactory(params -> buildSirModelTwoParams(
                        params.get("Contact Rate"), params.get("Recovery Rate")))
                .timeStep(DAY)
                .duration(weeks(8))
                .build()
                .execute();

        List<SensitivitySummary.ParameterImpact> impacts =
                SensitivitySummary.fromMultiSweep(result, "Infectious");
        String summary = SensitivitySummary.toPlainLanguage(impacts);

        assertTrue(summary.contains("of variance"),
                "Multi-param summary should mention 'of variance': " + summary);
        assertTrue(summary.contains("%"),
                "Should contain percentage: " + summary);
    }

    @Test
    void multiSweepFractionsShouldBeBoundedWhenOutputNearZero() {
        // Regression: coffee cooling model where Discrepancy final value is near zero
        // at the midpoint. The old range/baseline formula produced 3644% or infinity.
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Contact Rate", new double[]{2.0, 8.0, 14.0})
                .parameter("Recovery Rate", new double[]{0.05, 0.2, 0.5})
                .modelFactory(params -> buildSirModelTwoParams(
                        params.get("Contact Rate"), params.get("Recovery Rate")))
                .timeStep(DAY)
                .duration(weeks(12))
                .build()
                .execute();

        // Target a variable whose final value is near zero for some param combinations
        // (Infectious tends toward zero as epidemic burns out)
        List<SensitivitySummary.ParameterImpact> impacts =
                SensitivitySummary.fromMultiSweep(result, "Infectious");

        for (var impact : impacts) {
            assertTrue(impact.impactFraction() >= 0.0 && impact.impactFraction() <= 1.0,
                    "Fraction must be 0-1 even when outputs are near zero, got "
                            + impact.impactFraction() + " for " + impact.parameterName());
        }

        double sum = impacts.stream()
                .mapToDouble(SensitivitySummary.ParameterImpact::impactFraction).sum();
        assertEquals(1.0, sum, 0.001, "Fractions must sum to 1.0");
    }

    @Test
    void shouldReturnEmptyListForEmptyResult() {
        SweepResult empty = new SweepResult("X", List.of());
        List<SensitivitySummary.ParameterImpact> impacts =
                SensitivitySummary.fromSweep(empty, "Anything");
        assertTrue(impacts.isEmpty());
    }

    @Test
    void shouldHandleEmptyImpactListInPlainLanguage() {
        String summary = SensitivitySummary.toPlainLanguage(List.of());
        assertEquals("No sensitivity data available.", summary);
    }

    @Nested
    @DisplayName("ParameterImpact bounds validation (#313)")
    class ImpactFractionBounds {

        @Test
        @DisplayName("should clamp negative impactFraction to 0")
        void shouldClampNegativeFractionToZero() {
            var impact = new SensitivitySummary.ParameterImpact(
                    "p", "v", 0, 100, 50, -0.5);
            assertThat(impact.impactFraction()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should clamp impactFraction > 1 to 1")
        void shouldClampFractionAboveOneToOne() {
            var impact = new SensitivitySummary.ParameterImpact(
                    "p", "v", 0, 100, 50, 1.5);
            assertThat(impact.impactFraction()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should preserve valid impactFraction unchanged")
        void shouldPreserveValidFraction() {
            var impact = new SensitivitySummary.ParameterImpact(
                    "p", "v", 0, 100, 50, 0.42);
            assertThat(impact.impactFraction()).isEqualTo(0.42);
        }

        @Test
        @DisplayName("should clamp NaN to 0")
        void shouldClampNaNToZero() {
            var impact = new SensitivitySummary.ParameterImpact(
                    "p", "v", 0, 100, 50, Double.NaN);
            assertThat(impact.impactFraction()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("boundary value 0.0 is preserved")
        void shouldPreserveZero() {
            var impact = new SensitivitySummary.ParameterImpact(
                    "p", "v", 0, 100, 50, 0.0);
            assertThat(impact.impactFraction()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("boundary value 1.0 is preserved")
        void shouldPreserveOne() {
            var impact = new SensitivitySummary.ParameterImpact(
                    "p", "v", 0, 100, 50, 1.0);
            assertThat(impact.impactFraction()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("formatPercent via toPlainLanguage (#313)")
    class FormatPercentBounds {

        @Test
        @DisplayName("negative fraction produces 0% in summary")
        void negativeFractionShowsZeroPercent() {
            var first = new SensitivitySummary.ParameterImpact(
                    "Alpha", "Output", 10, 20, 15, 0.7);
            var second = new SensitivitySummary.ParameterImpact(
                    "Beta", "Output", 10, 20, 15, -0.3);
            String summary = SensitivitySummary.toPlainLanguage(List.of(first, second));
            assertThat(summary).contains("0%");
            assertThat(summary).doesNotContain("-%");
        }

        @Test
        @DisplayName("fraction > 1 produces 100% in summary")
        void overOneFractionShowsHundredPercent() {
            var first = new SensitivitySummary.ParameterImpact(
                    "Alpha", "Output", 10, 20, 15, 1.5);
            var second = new SensitivitySummary.ParameterImpact(
                    "Beta", "Output", 10, 20, 15, 0.3);
            String summary = SensitivitySummary.toPlainLanguage(List.of(first, second));
            assertThat(summary).contains("100%");
            assertThat(summary).doesNotContain("150%");
        }
    }

    private Model buildSirModel(double contactRate) {
        return buildSirModelTwoParams(contactRate, 0.2);
    }

    private Model buildSirModelTwoParams(double contactRate, double recoveryRate) {
        Model model = new Model("SIR");

        Stock susceptible = new Stock("Susceptible", 1000, PEOPLE);
        Stock infectious = new Stock("Infectious", 10, PEOPLE);
        Stock recovered = new Stock("Recovered", 0, PEOPLE);

        Variable contactRateVar = new Variable("Contact Rate", PEOPLE, () -> contactRate);

        Flow infectionFlow = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getQuantity().getValue()
                    + infectious.getQuantity().getValue()
                    + recovered.getQuantity().getValue();
            double infectiousFraction = infectious.getQuantity().getValue() / totalPop;
            double infectivity = 0.10;
            double infectedCount = contactRateVar.getValue() * infectiousFraction
                    * susceptible.getQuantity().getValue() * infectivity;
            if (infectedCount > susceptible.getQuantity().getValue()) {
                infectedCount = susceptible.getQuantity().getValue();
            }
            return new Quantity(infectedCount, PEOPLE);
        });

        Flow recoveryFlow = Flow.create("Recovered", DAY, () ->
                new Quantity(infectious.getQuantity().getValue() * recoveryRate, PEOPLE));

        susceptible.addOutflow(infectionFlow);
        infectious.addInflow(infectionFlow);
        infectious.addOutflow(recoveryFlow);
        recovered.addInflow(recoveryFlow);

        model.addStock(susceptible);
        model.addStock(infectious);
        model.addStock(recovered);

        return model;
    }
}
