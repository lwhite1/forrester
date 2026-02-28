package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.sweep.ParameterSweep;
import com.deathrayresearch.forrester.sweep.SweepResult;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Demonstrates a parameter sweep on the SIR infectious disease model.
 * Sweeps the contact rate from 2 to 14 (step 2) and writes both a time series
 * and summary CSV to the system temp directory.
 */
public class SirSweepDemo {

    public static void main(String[] args) {
        new SirSweepDemo().run();
    }

    public void run() {
        SweepResult result = ParameterSweep.builder()
                .parameterName("Contact Rate")
                .parameterValues(ParameterSweep.linspace(2.0, 14.0, 2.0))
                .modelFactory(this::buildSirModel)
                .timeStep(DAY)
                .duration(Times.weeks(8))
                .build()
                .execute();

        String tmpDir = System.getProperty("java.io.tmpdir");
        result.writeTimeSeriesCsv(tmpDir + "/forrester-sweep-timeseries.csv");
        result.writeSummaryCsv(tmpDir + "/forrester-sweep-summary.csv");

        System.out.println("Sweep complete: " + result.getRunCount() + " runs");
        System.out.println("Time series CSV: " + tmpDir + "/forrester-sweep-timeseries.csv");
        System.out.println("Summary CSV:     " + tmpDir + "/forrester-sweep-summary.csv");
    }

    private Model buildSirModel(double contactRate) {
        Model model = new Model("SIR Sweep (contact rate=" + contactRate + ")");

        Stock susceptible = new Stock("Susceptible", 1000, PEOPLE);
        Stock infectious = new Stock("Infectious", 10, PEOPLE);
        Stock recovered = new Stock("Recovered", 0, PEOPLE);

        Constant contactRateConstant = new Constant("Contact Rate", PEOPLE, contactRate);

        Flow infectionRate = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getQuantity().getValue()
                    + infectious.getQuantity().getValue()
                    + recovered.getQuantity().getValue();

            double infectiousFraction = infectious.getQuantity().getValue() / totalPop;
            double infectivity = 0.10;
            double contactsMadeInfectious = contactRateConstant.getValue() * infectiousFraction;
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
