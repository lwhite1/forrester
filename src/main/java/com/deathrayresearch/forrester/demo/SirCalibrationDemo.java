package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.sweep.Objectives;
import com.deathrayresearch.forrester.sweep.OptimizationAlgorithm;
import com.deathrayresearch.forrester.sweep.OptimizationResult;
import com.deathrayresearch.forrester.sweep.Optimizer;
import com.deathrayresearch.forrester.sweep.RunResult;

import java.util.Map;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Twin experiment: generates synthetic observed data by running an SIR model with known
 * parameters, then uses the Optimizer to recover those parameters from the synthetic data.
 * Reports recovered vs true values and the fit error.
 *
 * <p><strong>Note on identifiability:</strong> The infection equation uses the product
 * {@code contactRate * infectivity}, so the optimizer can only identify this product (beta),
 * not the individual factors. Different factor combinations yielding the same beta produce
 * identical dynamics. The twin experiment succeeds because the search landscape has a
 * valley of equivalent solutions, and Nelder-Mead typically finds one near the true values.
 */
public class SirCalibrationDemo {

    public static void main(String[] args) {
        double trueContactRate = 8.0;
        double trueInfectivity = 0.10;
        double initialSusceptible = 1000;
        double initialInfectious = 10;
        double initialRecovered = 0;
        double recoveryProportion = 0.2;
        double contactRateSearchMin = 1.0;
        double contactRateSearchMax = 20.0;
        double infectivitySearchMin = 0.01;
        double infectivitySearchMax = 0.50;
        int maxEvaluations = 500;
        double durationWeeks = 8;

        new SirCalibrationDemo().run(trueContactRate, trueInfectivity,
                initialSusceptible, initialInfectious, initialRecovered, recoveryProportion,
                contactRateSearchMin, contactRateSearchMax,
                infectivitySearchMin, infectivitySearchMax,
                maxEvaluations, durationWeeks);
    }

    public void run(double trueContactRate, double trueInfectivity,
                    double initialSusceptible, double initialInfectious,
                    double initialRecovered, double recoveryProportion,
                    double contactRateSearchMin, double contactRateSearchMax,
                    double infectivitySearchMin, double infectivitySearchMax,
                    int maxEvaluations, double durationWeeks) {

        // Step 1: Generate synthetic observed data with known "true" parameters
        System.out.println("Generating synthetic data with Contact Rate=" + trueContactRate
                + ", Infectivity=" + trueInfectivity);

        RunResult syntheticRun = runSirModel(trueContactRate, trueInfectivity,
                initialSusceptible, initialInfectious, initialRecovered,
                recoveryProportion, durationWeeks);
        double[] observedInfectious = syntheticRun.getStockSeries("Infectious");

        System.out.println("Observed data: " + observedInfectious.length + " time steps");
        System.out.println();

        // Step 2: Use Optimizer to recover the parameters
        System.out.println("Running Nelder-Mead optimization...");

        OptimizationResult result = Optimizer.builder()
                .parameter("Contact Rate", contactRateSearchMin, contactRateSearchMax)
                .parameter("Infectivity", infectivitySearchMin, infectivitySearchMax)
                .modelFactory(params -> buildSirModel(
                        params.get("Contact Rate"), params.get("Infectivity"),
                        initialSusceptible, initialInfectious, initialRecovered,
                        recoveryProportion))
                .objective(Objectives.fitToTimeSeries("Infectious", observedInfectious))
                .algorithm(OptimizationAlgorithm.NELDER_MEAD)
                .maxEvaluations(maxEvaluations)
                .timeStep(DAY)
                .duration(Times.weeks(durationWeeks))
                .build()
                .execute();

        // Step 3: Report results
        Map<String, Double> best = result.getBestParameters();
        double recoveredContactRate = best.get("Contact Rate");
        double recoveredInfectivity = best.get("Infectivity");

        System.out.println();
        System.out.println("=== Calibration Results ===");
        System.out.printf("Contact Rate:  true=%.4f  recovered=%.4f  error=%.4f%n",
                trueContactRate, recoveredContactRate,
                Math.abs(trueContactRate - recoveredContactRate));
        System.out.printf("Infectivity:   true=%.4f  recovered=%.4f  error=%.4f%n",
                trueInfectivity, recoveredInfectivity,
                Math.abs(trueInfectivity - recoveredInfectivity));
        System.out.printf("SSE: %.6f%n", result.getBestObjectiveValue());
        System.out.printf("Evaluations: %d%n", result.getEvaluationCount());
    }

    private RunResult runSirModel(double contactRate, double infectivity,
                                  double initialSusceptible, double initialInfectious,
                                  double initialRecovered, double recoveryProportion,
                                  double durationWeeks) {
        Model model = buildSirModel(contactRate, infectivity,
                initialSusceptible, initialInfectious, initialRecovered, recoveryProportion);
        RunResult runResult = new RunResult(
                Map.of("Contact Rate", contactRate, "Infectivity", infectivity));

        Simulation simulation = new Simulation(model, DAY, Times.weeks(durationWeeks));
        simulation.addEventHandler(runResult);
        simulation.execute();

        return runResult;
    }

    private Model buildSirModel(double contactRate, double infectivity,
                                double initialSusceptible, double initialInfectious,
                                double initialRecovered, double recoveryProportion) {
        Model model = new Model("SIR Calibration");

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
