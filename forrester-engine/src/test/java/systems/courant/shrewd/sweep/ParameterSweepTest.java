package systems.courant.forrester.sweep;

import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.units.time.Times;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Variable;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.PEOPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterSweepTest {

    @Test
    void shouldRunSweepAndCollectResults() {
        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(new double[]{2.0, 4.0, 6.0})
                .modelFactory(this::buildSirModel)
                .timeStep(DAY)
                .duration(Times.weeks(4))
                .build()
                .execute();

        assertEquals(3, result.getRunCount());
        assertEquals("Contact Rate", result.getParameterName());

        assertEquals(2.0, result.getResult(0).getParameterValue(), 0.001);
        assertEquals(4.0, result.getResult(1).getParameterValue(), 0.001);
        assertEquals(6.0, result.getResult(2).getParameterValue(), 0.001);
    }

    @Test
    void shouldProduceMoreInfectiousWithHigherContactRate() {
        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(new double[]{2.0, 8.0, 14.0})
                .modelFactory(this::buildSirModel)
                .timeStep(DAY)
                .duration(Times.weeks(8))
                .build()
                .execute();

        double maxInfectiousLow = result.getResult(0).getMaxStockValue("Infectious");
        double maxInfectiousMid = result.getResult(1).getMaxStockValue("Infectious");
        double maxInfectiousHigh = result.getResult(2).getMaxStockValue("Infectious");

        assertTrue(maxInfectiousLow < maxInfectiousMid,
                "Higher contact rate should produce higher peak infectious");
        assertTrue(maxInfectiousMid < maxInfectiousHigh,
                "Higher contact rate should produce higher peak infectious");
    }

    @Test
    void shouldCaptureStockAndVariableNames() {
        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(new double[]{4.0})
                .modelFactory(this::buildSirModel)
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        assertEquals(3, result.getStockNames().size());
        assertTrue(result.getStockNames().contains("Susceptible"));
        assertTrue(result.getStockNames().contains("Infectious"));
        assertTrue(result.getStockNames().contains("Recovered"));
    }

    @Test
    void shouldWriteTimeSeriesCsv(@TempDir Path tempDir) {
        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(new double[]{2.0, 4.0})
                .modelFactory(this::buildSirModel)
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        String filePath = tempDir.resolve("timeseries.csv").toString();
        result.writeTimeSeriesCsv(filePath);

        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void shouldWriteSummaryCsv(@TempDir Path tempDir) {
        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(new double[]{2.0, 4.0})
                .modelFactory(this::buildSirModel)
                .timeStep(DAY)
                .duration(Times.weeks(1))
                .build()
                .execute();

        String filePath = tempDir.resolve("summary.csv").toString();
        result.writeSummaryCsv(filePath);

        File file = new File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void linspaceShouldProduceCorrectValues() {
        double[] values = ParameterSweep.linspace(2.0, 14.0, 2.0);
        assertEquals(7, values.length);
        assertEquals(2.0, values[0], 0.001);
        assertEquals(4.0, values[1], 0.001);
        assertEquals(6.0, values[2], 0.001);
        assertEquals(8.0, values[3], 0.001);
        assertEquals(10.0, values[4], 0.001);
        assertEquals(12.0, values[5], 0.001);
        assertEquals(14.0, values[6], 0.001);
    }

    @Test
    void linspaceShouldHandleSingleValue() {
        double[] values = ParameterSweep.linspace(5.0, 5.0, 1.0);
        assertEquals(1, values.length);
        assertEquals(5.0, values[0], 0.001);
    }

    @Test
    void shouldRejectMissingParameterName() {
        assertThrows(IllegalStateException.class, () ->
                ParameterSweep.builder()
                        .parameterValues(new double[]{1.0})
                        .modelFactory(v -> new Model("M"))
                        .timeStep(DAY)
                        .duration(Times.weeks(1))
                        .build());
    }

    @Test
    void shouldRejectMissingModelFactory() {
        assertThrows(IllegalStateException.class, () ->
                ParameterSweep.builder()
                        .parameterName("X")
                        .parameterValues(new double[]{1.0})
                        .timeStep(DAY)
                        .duration(Times.weeks(1))
                        .build());
    }

    private Model buildSirModel(double contactRate) {
        Model model = new Model("SIR");

        Stock susceptible = new Stock("Susceptible", 1000, PEOPLE);
        Stock infectious = new Stock("Infectious", 10, PEOPLE);
        Stock recovered = new Stock("Recovered", 0, PEOPLE);

        Variable contactRateVar = new Variable("Contact Rate", PEOPLE, () -> contactRate);

        Flow infectionRate = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getQuantity().getValue()
                    + infectious.getQuantity().getValue()
                    + recovered.getQuantity().getValue();

            double infectiousFraction = infectious.getQuantity().getValue() / totalPop;
            double infectivity = 0.10;
            double contactsMadeInfectious = contactRateVar.getValue() * infectiousFraction;
            double infectedCount = contactsMadeInfectious * susceptible.getQuantity().getValue() * infectivity;

            if (infectedCount > susceptible.getQuantity().getValue()) {
                infectedCount = susceptible.getQuantity().getValue();
            }
            return new Quantity(infectedCount, PEOPLE);
        });

        Flow recoveryRate = Flow.create("Recovered", DAY, () -> {
            double recoveredProportion = 0.2;
            return new Quantity(infectious.getQuantity().getValue() * recoveredProportion, PEOPLE);
        });

        susceptible.addOutflow(infectionRate);
        infectious.addInflow(infectionRate);
        infectious.addOutflow(recoveryRate);
        recovered.addInflow(recoveryRate);

        model.addStock(susceptible);
        model.addStock(infectious);
        model.addStock(recovered);

        return model;
    }
}
