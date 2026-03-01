package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.sweep.MultiParameterSweep;
import com.deathrayresearch.forrester.sweep.MultiSweepResult;
import com.deathrayresearch.forrester.sweep.ParameterSweep;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Demonstrates multi-parameter sweep on the SIR infectious disease model. Sweeps
 * contact rate (2, 6, 10, 14) x infectivity (0.05, 0.10, 0.15) = 12 combinations.
 * Writes time series and summary CSVs to the system temp directory.
 */
public class SirMultiSweepDemo {

    public static void main(String[] args) {
        new SirMultiSweepDemo().run();
    }

    public void run() {
        MultiSweepResult result = MultiParameterSweep.builder()
                .parameter("Contact Rate", ParameterSweep.linspace(2.0, 14.0, 4.0))
                .parameter("Infectivity", new double[]{0.05, 0.10, 0.15})
                .modelFactory(params -> buildSirModel(
                        params.get("Contact Rate"), params.get("Infectivity")))
                .timeStep(DAY)
                .duration(Times.weeks(8))
                .build()
                .execute();

        String tmpDir = System.getProperty("java.io.tmpdir");
        String timeSeriesPath = tmpDir + "/forrester-multisweep-timeseries.csv";
        String summaryPath = tmpDir + "/forrester-multisweep-summary.csv";

        result.writeTimeSeriesCsv(timeSeriesPath);
        result.writeSummaryCsv(summaryPath);

        System.out.println("Multi-parameter sweep complete: " + result.getRunCount() + " runs");
        System.out.println("Time series CSV: " + timeSeriesPath);
        System.out.println("Summary CSV:     " + summaryPath);
    }

    private Model buildSirModel(double contactRate, double infectivity) {
        Model model = new Model("SIR Multi-Sweep");

        Stock susceptible = new Stock("Susceptible", 1000, PEOPLE);
        Stock infectious = new Stock("Infectious", 10, PEOPLE);
        Stock recovered = new Stock("Recovered", 0, PEOPLE);

        Constant contactRateConstant = new Constant("Contact Rate", PEOPLE, contactRate);

        Flow infectionRate = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getQuantity().getValue()
                    + infectious.getQuantity().getValue()
                    + recovered.getQuantity().getValue();

            double infectiousFraction = infectious.getQuantity().getValue() / totalPop;
            double contactsMadeInfectious = contactRateConstant.getValue() * infectiousFraction;
            double infectedCount = contactsMadeInfectious
                    * susceptible.getQuantity().getValue() * infectivity;

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
