package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Constant;
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
 * parameters (contactRate=8.0, infectivity=0.10), then uses the Optimizer to recover those
 * parameters from the synthetic data. Reports recovered vs true values and the fit error.
 */
public class SirCalibrationDemo {

    private static final double TRUE_CONTACT_RATE = 8.0;
    private static final double TRUE_INFECTIVITY = 0.10;

    public static void main(String[] args) {
        new SirCalibrationDemo().run();
    }

    public void run() {
        // Step 1: Generate synthetic observed data with known "true" parameters
        System.out.println("Generating synthetic data with Contact Rate=" + TRUE_CONTACT_RATE
                + ", Infectivity=" + TRUE_INFECTIVITY);

        RunResult syntheticRun = runSirModel(TRUE_CONTACT_RATE, TRUE_INFECTIVITY);
        double[] observedInfectious = syntheticRun.getStockSeries("Infectious");

        System.out.println("Observed data: " + observedInfectious.length + " time steps");
        System.out.println();

        // Step 2: Use Optimizer to recover the parameters
        System.out.println("Running Nelder-Mead optimization...");

        OptimizationResult result = Optimizer.builder()
                .parameter("Contact Rate", 1.0, 20.0)
                .parameter("Infectivity", 0.01, 0.50)
                .modelFactory(params -> buildSirModel(
                        params.get("Contact Rate"), params.get("Infectivity")))
                .objective(Objectives.fitToTimeSeries("Infectious", observedInfectious))
                .algorithm(OptimizationAlgorithm.NELDER_MEAD)
                .maxEvaluations(500)
                .timeStep(DAY)
                .duration(Times.weeks(8))
                .build()
                .execute();

        // Step 3: Report results
        Map<String, Double> best = result.getBestParameters();
        double recoveredContactRate = best.get("Contact Rate");
        double recoveredInfectivity = best.get("Infectivity");

        System.out.println();
        System.out.println("=== Calibration Results ===");
        System.out.printf("Contact Rate:  true=%.4f  recovered=%.4f  error=%.4f%n",
                TRUE_CONTACT_RATE, recoveredContactRate,
                Math.abs(TRUE_CONTACT_RATE - recoveredContactRate));
        System.out.printf("Infectivity:   true=%.4f  recovered=%.4f  error=%.4f%n",
                TRUE_INFECTIVITY, recoveredInfectivity,
                Math.abs(TRUE_INFECTIVITY - recoveredInfectivity));
        System.out.printf("SSE: %.6f%n", result.getBestObjectiveValue());
        System.out.printf("Evaluations: %d%n", result.getEvaluationCount());
    }

    private RunResult runSirModel(double contactRate, double infectivity) {
        Model model = buildSirModel(contactRate, infectivity);
        RunResult runResult = new RunResult(
                Map.of("Contact Rate", contactRate, "Infectivity", infectivity));

        Simulation simulation = new Simulation(model, DAY, Times.weeks(8));
        simulation.addEventHandler(runResult);
        simulation.execute();

        return runResult;
    }

    private Model buildSirModel(double contactRate, double infectivity) {
        Model model = new Model("SIR Calibration");

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
