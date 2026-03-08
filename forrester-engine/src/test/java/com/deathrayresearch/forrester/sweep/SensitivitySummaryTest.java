package com.deathrayresearch.forrester.sweep;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static com.deathrayresearch.forrester.measure.units.time.Times.weeks;
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
        assertTrue(impacts.getFirst().impactFraction() > 0,
                "Contact rate should have positive impact on Infectious");
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
        // First entry should have larger impact
        assertTrue(Math.abs(impacts.get(0).impactFraction())
                        >= Math.abs(impacts.get(1).impactFraction()),
                "Impacts should be sorted by magnitude descending");
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
        assertTrue(summary.contains("Contact Rate"));
        assertTrue(summary.contains("Infectious"));
        assertTrue(summary.contains("\u00B1")); // ± character
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

    private Model buildSirModel(double contactRate) {
        return buildSirModelTwoParams(contactRate, 0.2);
    }

    private Model buildSirModelTwoParams(double contactRate, double recoveryRate) {
        Model model = new Model("SIR");

        Stock susceptible = new Stock("Susceptible", 1000, PEOPLE);
        Stock infectious = new Stock("Infectious", 10, PEOPLE);
        Stock recovered = new Stock("Recovered", 0, PEOPLE);

        Constant contactRateConstant = new Constant("Contact Rate", PEOPLE, contactRate);

        Flow infectionFlow = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getQuantity().getValue()
                    + infectious.getQuantity().getValue()
                    + recovered.getQuantity().getValue();
            double infectiousFraction = infectious.getQuantity().getValue() / totalPop;
            double infectivity = 0.10;
            double infectedCount = contactRateConstant.getValue() * infectiousFraction
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
