package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.sweep.MonteCarlo;
import com.deathrayresearch.forrester.sweep.MonteCarloResult;
import com.deathrayresearch.forrester.sweep.SamplingMethod;
import com.deathrayresearch.forrester.ui.FanChart;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Demonstrates Monte Carlo simulation on the SIR infectious disease model with two
 * uncertain parameters: contact rate (normally distributed) and infectivity (uniformly
 * distributed). Runs 200 iterations with Latin Hypercube Sampling, writes percentile
 * CSV output, and displays a fan chart of the Infectious stock.
 */
public class SirMonteCarloDemo {

    public static void main(String[] args) {
        new SirMonteCarloDemo().run();
    }

    public void run() {
        MonteCarloResult result = MonteCarlo.builder()
                .parameter("Contact Rate", new NormalDistribution(8, 2))
                .parameter("Infectivity", new UniformRealDistribution(0.05, 0.15))
                .modelFactory(params -> buildSirModel(
                        params.get("Contact Rate"), params.get("Infectivity")))
                .iterations(200)
                .sampling(SamplingMethod.LATIN_HYPERCUBE)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(8))
                .build()
                .execute();

        String tmpDir = System.getProperty("java.io.tmpdir");
        String outputPath = tmpDir + "/forrester-montecarlo-infectious.csv";
        result.writePercentileCsv(outputPath, "Infectious", 2.5, 25, 50, 75, 97.5);

        System.out.println("Monte Carlo complete: " + result.getRunCount() + " runs");
        System.out.println("Percentile CSV: " + outputPath);

        FanChart.show(result, "Infectious");
    }

    private Model buildSirModel(double contactRate, double infectivity) {
        Model model = new Model("SIR Monte Carlo");

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
