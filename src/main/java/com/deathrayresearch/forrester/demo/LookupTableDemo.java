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
 *
 * <p>The lookup table replaces the algebraic approximation
 * {@code rate * (1 - population / capacity)} with a more flexible, user-defined curve.
 */
public class LookupTableDemo {

    private static final double CARRYING_CAPACITY = 1000.0;
    private static final double BIRTH_RATE = 0.04;

    public static void main(String[] args) {
        new LookupTableDemo().run();
    }

    public void run() {
        Model model = new Model("Population Growth with Crowding Lookup");

        Stock population = new Stock("Population", 10, PEOPLE);

        // Crowding effect: as population/capacity ratio rises, the growth multiplier drops
        LookupTable crowdingEffect = LookupTable.builder()
                .at(0.0, 1.2)
                .at(0.5, 1.0)
                .at(1.0, 0.5)
                .at(1.5, 0.1)
                .at(2.0, 0.0)
                .buildLinear(() -> population.getValue() / CARRYING_CAPACITY);

        Variable multiplier = new Variable("Crowding Effect", DIMENSIONLESS, crowdingEffect);

        Flow births = Flow.create("Births", DAY, () ->
                new Quantity(population.getValue() * BIRTH_RATE * multiplier.getValue(), PEOPLE));

        population.addInflow(births);
        model.addStock(population);
        model.addVariable(multiplier);

        Simulation sim = new Simulation(model, DAY, WEEK, 52);
        sim.addEventHandler(new StockLevelChartViewer());
        sim.execute();
    }
}
