/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.Variable;
import systems.courant.shrewd.ui.FlowChartViewer;

import static systems.courant.shrewd.measure.Units.CELSIUS;
import static systems.courant.shrewd.measure.Units.MINUTE;

/**
 * Simulates Newton's law of cooling applied to a cup of coffee.
 *
 * <p>A Coffee Temperature stock cools toward a Room Temperature constant
 * via a negative-feedback outflow proportional to the temperature discrepancy. The
 * cooling rate decelerates as the gap narrows, producing the classic goal-seeking decay curve.
 */
public class CoffeeCoolingDemo {

    public static void main(String[] args) {
        double initialTemperature = 100;  // degrees Celsius
        double roomTemperature = 18;      // degrees Celsius
        double coolingRate = 0.10;        // fraction per minute
        double durationMinutes = 8;

        new CoffeeCoolingDemo().run(initialTemperature, roomTemperature, coolingRate,
                durationMinutes);
    }

    public void run(double initialTemperature, double roomTemperature, double coolingRate,
                    double durationMinutes) {
        Model model = new Model("Coffee Cooling");
        model.setMetadata(ModelMetadata.builder()
                .source("Newton's law of cooling")
                .license("CC-BY-SA-4.0")
                .build());

        Stock coffeeTemperature = new Stock("Coffee Temperature", initialTemperature, CELSIUS);

        Variable discrepancy = new Variable("Discrepancy", CELSIUS,
                () -> coffeeTemperature.getValue() - roomTemperature);

        Flow cooling = Flow.create("Cooling", MINUTE, () ->
                new Quantity(discrepancy.getValue() * coolingRate, CELSIUS));

        coffeeTemperature.addOutflow(cooling);

        model.addStock(coffeeTemperature);
        model.addVariable(discrepancy);

        Simulation run = new Simulation(model, MINUTE, MINUTE, durationMinutes);
        run.addEventHandler(new FlowChartViewer(cooling));
        run.execute();
    }
}
