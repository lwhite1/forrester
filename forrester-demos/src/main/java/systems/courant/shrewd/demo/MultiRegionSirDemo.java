/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.io.CsvSubscriber;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.units.time.Times;
import systems.courant.shrewd.model.ArrayedFlow;
import systems.courant.shrewd.model.ArrayedStock;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Subscript;
import systems.courant.shrewd.ui.StockLevelChartViewer;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.PEOPLE;

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
        double[] initialSusceptible = {990, 1000, 1000};
        double[] initialInfectious = {10, 0, 0};
        double[] initialRecovered = {0, 0, 0};
        double contactRate = 8.0;
        double infectivity = 0.10;
        double recoveryProportion = 0.20;
        double migrationRate = 0.01;     // fraction of infectious per day
        double durationWeeks = 12;

        new MultiRegionSirDemo().run(initialSusceptible, initialInfectious, initialRecovered,
                contactRate, infectivity, recoveryProportion, migrationRate, durationWeeks);
    }

    public void run(double[] initialSusceptible, double[] initialInfectious,
                    double[] initialRecovered, double contactRate, double infectivity,
                    double recoveryProportion, double migrationRate, double durationWeeks) {
        Model model = new Model("Multi-Region SIR Model");
        model.setMetadata(ModelMetadata.builder()
                .source("Kermack & McKendrick SIR model (1927)")
                .license("CC-BY-SA-4.0")
                .build());

        Subscript region = new Subscript("Region", "North", "South", "East");

        ArrayedStock susceptible = new ArrayedStock("Susceptible", region,
                initialSusceptible, PEOPLE);
        ArrayedStock infectious = new ArrayedStock("Infectious", region,
                initialInfectious, PEOPLE);
        ArrayedStock recovered = new ArrayedStock("Recovered", region,
                initialRecovered, PEOPLE);

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

        // Cross-region migration of infectious people (circular: N→S→E→N)
        for (int i = 0; i < region.size(); i++) {
            int from = i;
            int to = (i + 1) % region.size();
            Flow migration = Flow.create(
                    "Migration[" + region.getLabel(from) + "->" + region.getLabel(to) + "]",
                    DAY,
                    () -> new Quantity(infectious.getValue(from) * migrationRate, PEOPLE));
            infectious.getStock(from).addOutflow(migration);
            infectious.getStock(to).addInflow(migration);
        }

        model.addArrayedStock(susceptible);
        model.addArrayedStock(infectious);
        model.addArrayedStock(recovered);

        Simulation run = new Simulation(model, DAY, Times.weeks(durationWeeks));
        run.addEventHandler(new CsvSubscriber(
                System.getProperty("java.io.tmpdir") + "/forrester-multi-region-sir.csv"));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
