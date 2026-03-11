/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.LookupTable;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.Variable;
import systems.courant.shrewd.ui.StockLevelChartViewer;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.DIMENSIONLESS;
import static systems.courant.shrewd.measure.Units.PEOPLE;
import static systems.courant.shrewd.measure.Units.WEEK;

/**
 * Demonstrates the use of a {@link LookupTable} to model nonlinear crowding effects
 * on population growth.
 *
 * <p>A population grows via a birth flow whose rate is modulated by a crowding-effect
 * multiplier. As population / carrying capacity increases, the multiplier drops from 1.2
 * (favorable conditions) toward 0.0 (full saturation), producing the classic S-shaped
 * growth curve.
 */
public class LookupTableDemo {

    public static void main(String[] args) {
        double initialPopulation = 10;
        double carryingCapacity = 1000.0;
        double birthRate = 0.04;
        double durationWeeks = 52;

        new LookupTableDemo().run(initialPopulation, carryingCapacity, birthRate, durationWeeks);
    }

    public void run(double initialPopulation, double carryingCapacity, double birthRate,
                    double durationWeeks) {
        Model model = new Model("Population Growth with Crowding Lookup");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock population = new Stock("Population", initialPopulation, PEOPLE);

        // Crowding effect: as population/capacity ratio rises, the growth multiplier drops
        LookupTable crowdingEffect = LookupTable.builder()
                .at(0.0, 1.2)
                .at(0.5, 1.0)
                .at(1.0, 0.5)
                .at(1.5, 0.1)
                .at(2.0, 0.0)
                .buildLinear(() -> population.getValue() / carryingCapacity);

        Variable multiplier = new Variable("Crowding Effect", DIMENSIONLESS, crowdingEffect);

        Flow births = Flow.create("Births", DAY, () ->
                new Quantity(population.getValue() * birthRate * multiplier.getValue(), PEOPLE));

        population.addInflow(births);
        model.addStock(population);
        model.addVariable(multiplier);

        Simulation sim = new Simulation(model, DAY, WEEK, durationWeeks);
        sim.addEventHandler(new StockLevelChartViewer());
        sim.execute();
    }
}
