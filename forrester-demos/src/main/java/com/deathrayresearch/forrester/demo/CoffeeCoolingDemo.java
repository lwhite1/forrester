package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.ModelMetadata;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.ui.FlowChartViewer;

import static com.deathrayresearch.forrester.measure.Units.CELSIUS;
import static com.deathrayresearch.forrester.measure.Units.MINUTE;

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
