/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Flows;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;
import systems.courant.sd.ui.StockLevelChartViewer;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.PEOPLE;
import static systems.courant.sd.measure.Units.WEEK;

/**
 * Models logistic (S-shaped) population growth constrained by a carrying capacity.
 *
 * <p>A Population stock grows via a birth inflow whose rate diminishes as the population
 * approaches the carrying capacity. Growth starts exponentially, inflects at the
 * midpoint, and levels off — the classic S-curve produced by a balancing feedback loop that
 * limits a reinforcing loop.
 */
public class SShapedPopulationGrowthDemo {

    public static void main(String[] args) {
        double initialPopulation = 10;
        double carryingCapacity = 1000;
        double maxFractionalBirthRate = 0.04;
        double durationWeeks = 32;

        new SShapedPopulationGrowthDemo().run(
                initialPopulation, carryingCapacity, maxFractionalBirthRate, durationWeeks);
    }

    public void run(double initialPopulation, double carryingCapacity,
                    double maxFractionalBirthRate, double durationWeeks) {
        Model model = getModel(initialPopulation, carryingCapacity, maxFractionalBirthRate);

        Simulation run = new Simulation(model, DAY, WEEK, durationWeeks);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }

    public Model getModel() {
        return getModel(10, 1000, 0.04);
    }

    public Model getModel(double initialPopulation, double carryingCapacity,
                           double maxFractionalBirthRate) {
        Model model = new Model("Population with S-Shaped Growth");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock population = new Stock("population", initialPopulation, PEOPLE);

        Flow births = Flows.exponentialGrowthWithLimit("Births", DAY, population,
                maxFractionalBirthRate, carryingCapacity);

        population.addInflow(births);

        model.addStock(population);

        return model;
    }
}
