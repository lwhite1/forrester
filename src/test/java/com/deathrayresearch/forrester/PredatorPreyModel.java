package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.RatePerYear;
import com.deathrayresearch.forrester.ui.ChartViewer;

public class PredatorPreyModel {

    private static Model model = new Model("Predator-Prey model");

    private static final Thing RABBIT = Thing.getInstance();
    private static final Thing COYOTE = Thing.getInstance();
    public static void main(String[] args) {

        Stock predator = new Stock("Coyotes", 10, COYOTE);
        Stock prey = new Stock("Rabbits", 100, RABBIT);

        Flow rabbitBirths = new Flow(
                new RatePerYear() {
                    @Override
                    protected Quantity quantityPerYear() {
                        double rate = prey.getCurrentValue().getValue();
                        return new Quantity("Births", 0, RABBIT);
                    }
                }
        );
        model.addStock(predator);

        model.addStock(prey);

        Simulation run = new Simulation(model, Day.getInstance(), Times.YEAR, 1);
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();

    }

}
