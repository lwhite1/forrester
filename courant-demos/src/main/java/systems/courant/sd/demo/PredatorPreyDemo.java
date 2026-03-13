/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;
import systems.courant.sd.ui.StockLevelChartViewer;

import static systems.courant.sd.measure.Units.THING;
import static systems.courant.sd.measure.Units.YEAR;

/**
 * Classic Lotka-Volterra predator-prey model.
 *
 * Two populations (rabbits and coyotes) interact through four flows:
 * - Prey births: proportional to prey population
 * - Prey deaths: proportional to prey-predator encounters
 * - Predator births: proportional to prey-predator encounters (conversion efficiency)
 * - Predator deaths: proportional to predator population
 */
public class PredatorPreyDemo {

    public static void main(String[] args) {
        double initialPrey = 100;
        double initialPredators = 10;
        double preyBirthRate = 1.0;
        double predationRate = 0.01;
        double predatorEfficiency = 0.5;
        double predatorDeathRate = 0.8;
        double durationYears = 20;

        new PredatorPreyDemo().run(initialPrey, initialPredators, preyBirthRate,
                predationRate, predatorEfficiency, predatorDeathRate, durationYears);
    }

    public void run(double initialPrey, double initialPredators, double preyBirthRate,
                    double predationRate, double predatorEfficiency, double predatorDeathRate,
                    double durationYears) {
        Model model = new Model("Predator-Prey model");
        model.setMetadata(ModelMetadata.builder()
                .source("Lotka-Volterra predator-prey equations")
                .license("CC-BY-SA-4.0")
                .build());

        Stock prey = new Stock("Rabbits", initialPrey, THING);
        Stock predator = new Stock("Coyotes", initialPredators, THING);

        Flow preyBirths = Flow.create("Prey Births", YEAR, () -> {
            double value = preyBirthRate * prey.getValue();
            return new Quantity(value, THING);
        });

        Flow preyDeaths = Flow.create("Prey Deaths", YEAR, () -> {
            double value = predationRate * prey.getValue() * predator.getValue();
            return new Quantity(value, THING);
        });

        Flow predatorBirths = Flow.create("Predator Births", YEAR, () -> {
            double value = predatorEfficiency * predationRate
                    * prey.getValue() * predator.getValue();
            return new Quantity(value, THING);
        });

        Flow predatorDeaths = Flow.create("Predator Deaths", YEAR, () -> {
            double value = predatorDeathRate * predator.getValue();
            return new Quantity(value, THING);
        });

        prey.addInflow(preyBirths);
        prey.addOutflow(preyDeaths);
        predator.addInflow(predatorBirths);
        predator.addOutflow(predatorDeaths);

        model.addStock(prey);
        model.addStock(predator);

        Simulation run = new Simulation(model, TimeUnits.DAY, Times.years(durationYears));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
