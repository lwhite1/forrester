package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.FlowPerYear;
import com.deathrayresearch.forrester.ui.ChartViewer;

import static com.deathrayresearch.forrester.measure.Units.YEAR;

public class PredatorPreyModel {

    private static Model model = new Model("Predator-Prey model");

    private static final Thing RABBIT = Thing.getInstance();
    private static final Thing COYOTE = Thing.getInstance();
    public static void main(String[] args) {

        Stock predator = new Stock("Coyotes", 10, COYOTE);
        Stock prey = new Stock("Rabbits", 100, RABBIT);

        new FlowPerYear("Births") {
            @Override
            protected Quantity quantityPerYear() {
                double rate = prey.getCurrentValue().getValue();
                return new Quantity("Births", 0, RABBIT);
            }
        };
        model.addStock(predator);

        model.addStock(prey);

        Simulation run = new Simulation(model, Day.getInstance(), YEAR, 1);
        run.addEventHandler(new ChartViewer());
        run.execute();
    }
}
