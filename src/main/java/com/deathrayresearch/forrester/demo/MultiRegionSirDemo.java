package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.ArrayedFlow;
import com.deathrayresearch.forrester.model.ArrayedStock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Subscript;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * A three-region SIR (Susceptible-Infectious-Recovered) model with migration between regions.
 *
 * <p>Demonstrates the subscript / arrayed-element capability:
 * <ul>
 *   <li>Defining a {@link Subscript} for regions (North, South, East)</li>
 *   <li>Creating {@link ArrayedStock} and {@link ArrayedFlow} for per-region S/I/R stocks</li>
 *   <li>Cross-element scalar flows for migration between regions</li>
 *   <li>Output showing separate time series per region (e.g., "Infectious[North]")</li>
 * </ul>
 */
public class MultiRegionSirDemo {

    public static void main(String[] args) {
        new MultiRegionSirDemo().run();
    }

    public void run() {
        Model model = new Model("Multi-Region SIR Model");

        Subscript region = new Subscript("Region", "North", "South", "East");

        // Initial populations: North has the outbreak seed
        ArrayedStock susceptible = new ArrayedStock("Susceptible", region,
                new double[]{990, 1000, 1000}, PEOPLE);
        ArrayedStock infectious = new ArrayedStock("Infectious", region,
                new double[]{10, 0, 0}, PEOPLE);
        ArrayedStock recovered = new ArrayedStock("Recovered", region,
                new double[]{0, 0, 0}, PEOPLE);

        // --- Per-region infection and recovery flows ---

        double contactRate = 8.0;
        double infectivity = 0.10;
        double recoveryProportion = 0.20;

        ArrayedFlow infectionFlow = ArrayedFlow.create("Infection", DAY, region, i -> {
            double s = susceptible.getValue(i);
            double inf = infectious.getValue(i);
            double r = recovered.getValue(i);
            double totalPop = s + inf + r;
            if (totalPop == 0) {
                return new Quantity(0, PEOPLE);
            }
            double infectiousFraction = inf / totalPop;
            double newInfections = contactRate * infectiousFraction * infectivity * s;
            if (newInfections > s) {
                newInfections = s;
            }
            return new Quantity(newInfections, PEOPLE);
        });

        ArrayedFlow recoveryFlow = ArrayedFlow.create("Recovery", DAY, region,
                i -> new Quantity(infectious.getValue(i) * recoveryProportion, PEOPLE));

        susceptible.addOutflow(infectionFlow);
        infectious.addInflow(infectionFlow);
        infectious.addOutflow(recoveryFlow);
        recovered.addInflow(recoveryFlow);

        // --- Cross-region migration of infectious people ---
        // Small daily migration: 1% of infectious in each region move to the next region
        double migrationRate = 0.01;

        for (int i = 0; i < region.size(); i++) {
            int from = i;
            int to = (i + 1) % region.size();
            Flow migration = Flow.create(
                    "Migration[" + region.getLabel(from) + "->" + region.getLabel(to) + "]",
                    DAY,
                    () -> new Quantity(infectious.getValue(from) * migrationRate, PEOPLE)
            );
            infectious.getStock(from).addOutflow(migration);
            infectious.getStock(to).addInflow(migration);
        }

        // --- Add to model ---
        model.addArrayedStock(susceptible);
        model.addArrayedStock(infectious);
        model.addArrayedStock(recovered);

        // --- Run simulation ---
        Simulation run = new Simulation(model, DAY, Times.weeks(12));
        run.addEventHandler(new CsvSubscriber(
                System.getProperty("java.io.tmpdir") + "/forrester-multi-region-sir.csv"));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
