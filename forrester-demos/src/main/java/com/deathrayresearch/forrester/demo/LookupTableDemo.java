package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.LookupTable;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.DIMENSIONLESS;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

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
