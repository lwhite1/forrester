package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.ModelMetadata;
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
 * distributed). Runs Latin Hypercube Sampling, writes percentile CSV output, and
 * displays a fan chart of the Infectious stock.
 */
public class SirMonteCarloDemo {

    public static void main(String[] args) {
        double initialSusceptible = 1000;
        double initialInfectious = 10;
        double initialRecovered = 0;
        double recoveryProportion = 0.2;
        double contactRateMean = 8;
        double contactRateStdDev = 2;
        double infectivityMin = 0.05;
        double infectivityMax = 0.15;
        int iterations = 200;
        long seed = 42L;
        double durationWeeks = 8;

        new SirMonteCarloDemo().run(initialSusceptible, initialInfectious, initialRecovered,
                recoveryProportion, contactRateMean, contactRateStdDev,
                infectivityMin, infectivityMax, iterations, seed, durationWeeks);
    }

    public void run(double initialSusceptible, double initialInfectious,
                    double initialRecovered, double recoveryProportion,
                    double contactRateMean, double contactRateStdDev,
                    double infectivityMin, double infectivityMax,
                    int iterations, long seed, double durationWeeks) {

        MonteCarloResult result = MonteCarlo.builder()
                .parameter("Contact Rate",
                        new NormalDistribution(contactRateMean, contactRateStdDev))
                .parameter("Infectivity",
                        new UniformRealDistribution(infectivityMin, infectivityMax))
                .modelFactory(params -> buildSirModel(
                        params.get("Contact Rate"), params.get("Infectivity"),
                        initialSusceptible, initialInfectious, initialRecovered,
                        recoveryProportion))
                .iterations(iterations)
                .sampling(SamplingMethod.LATIN_HYPERCUBE)
                .seed(seed)
                .timeStep(DAY)
                .duration(Times.weeks(durationWeeks))
                .build()
                .execute();

        String tmpDir = System.getProperty("java.io.tmpdir");
        String outputPath = tmpDir + "/forrester-montecarlo-infectious.csv";
        result.writePercentileCsv(outputPath, "Infectious", 2.5, 25, 50, 75, 97.5);

        System.out.println("Monte Carlo complete: " + result.getRunCount() + " runs");
        System.out.println("Percentile CSV: " + outputPath);

        FanChart.show(result, "Infectious");
    }

    private Model buildSirModel(double contactRate, double infectivity,
                                double initialSusceptible, double initialInfectious,
                                double initialRecovered, double recoveryProportion) {
        Model model = new Model("SIR Monte Carlo");
        model.setMetadata(ModelMetadata.builder()
                .source("Kermack & McKendrick SIR model (1927)")
                .license("CC-BY-SA-4.0")
                .build());

        Stock susceptible = new Stock("Susceptible", initialSusceptible, PEOPLE);
        Stock infectious = new Stock("Infectious", initialInfectious, PEOPLE);
        Stock recovered = new Stock("Recovered", initialRecovered, PEOPLE);

        Flow infectionRate = Flow.create("Infected", DAY, () -> {
            double totalPop = susceptible.getValue() + infectious.getValue()
                    + recovered.getValue();
            double infectiousFraction = infectious.getValue() / totalPop;
            double infectedCount = contactRate * infectiousFraction * infectivity
                    * susceptible.getValue();
            if (infectedCount > susceptible.getValue()) {
                infectedCount = susceptible.getValue();
            }
            return new Quantity(infectedCount, PEOPLE);
        });

        Flow recoveryRate = Flow.create("Recovered", DAY, () ->
                new Quantity(infectious.getValue() * recoveryProportion, PEOPLE));

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
