package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.flows.FlowPerYear;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

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

    private static final ItemUnits RABBIT = ItemUnits.THING;
    private static final ItemUnits COYOTE = ItemUnits.THING;

    private static final double PREY_BIRTH_RATE = 1.0;
    private static final double PREDATION_RATE = 0.01;
    private static final double PREDATOR_EFFICIENCY = 0.5;
    private static final double PREDATOR_DEATH_RATE = 0.8;

    public static void main(String[] args) {
        new PredatorPreyDemo().run();
    }

    public void run() {
        Model model = new Model("Predator-Prey model");

        Stock prey = new Stock("Rabbits", 100, RABBIT);
        Stock predator = new Stock("Coyotes", 10, COYOTE);

        FlowPerYear preyBirths = new FlowPerYear("Prey Births") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double value = PREY_BIRTH_RATE * prey.getValue();
                return new Quantity(value, RABBIT);
            }
        };

        FlowPerYear preyDeaths = new FlowPerYear("Prey Deaths") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double value = PREDATION_RATE * prey.getValue() * predator.getValue();
                return new Quantity(value, RABBIT);
            }
        };

        FlowPerYear predatorBirths = new FlowPerYear("Predator Births") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double value = PREDATOR_EFFICIENCY * PREDATION_RATE
                        * prey.getValue() * predator.getValue();
                return new Quantity(value, COYOTE);
            }
        };

        FlowPerYear predatorDeaths = new FlowPerYear("Predator Deaths") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double value = PREDATOR_DEATH_RATE * predator.getValue();
                return new Quantity(value, COYOTE);
            }
        };

        prey.addInflow(preyBirths);
        prey.addOutflow(preyDeaths);
        predator.addInflow(predatorBirths);
        predator.addOutflow(predatorDeaths);

        model.addStock(prey);
        model.addStock(predator);

        Simulation run = new Simulation(model, TimeUnits.DAY, Times.years(20));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
