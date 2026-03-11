/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.units.item.ItemUnits;
import systems.courant.shrewd.measure.units.time.TimeUnits;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Flows;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.ui.StockLevelChartViewer;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.WEEK;

/**
 * Models unconstrained population growth where births exceed deaths.
 *
 * <p>A single Population stock has an exponential birth inflow and an exponential
 * death outflow. Because the net rate is positive, the population grows
 * exponentially — the fundamental "positive feedback loop" archetype in system dynamics.
 */
public class ExponentialGrowthDemo {

    public static void main(String[] args) {
        double initialPopulation = 100;
        double birthRate = 0.04;       // fraction per day
        double deathRate = 0.03;       // fraction per day
        double durationWeeks = 52;

        new ExponentialGrowthDemo().run(initialPopulation, birthRate, deathRate, durationWeeks);
    }

    public void run(double initialPopulation, double birthRate, double deathRate,
                    double durationWeeks) {
        Model model = new Model("Population with unconstrained growth");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock population = new Stock("population", initialPopulation, ItemUnits.PEOPLE);

        Flow births = Flows.exponentialGrowth("Births", DAY, population, birthRate);
        Flow deaths = Flows.exponentialGrowth("Deaths", DAY, population, deathRate);

        population.addInflow(births);
        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, TimeUnits.DAY, WEEK, durationWeeks);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
