package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.archetypes.ExponentialChangeWithLimit;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Flow;
import com.deathrayresearch.forrester.rate.FlowPerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.DIMENSIONLESS;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public class sShapedPopulationGrowth {

    @Test
    public void testRun1() {
        Model model = new Model("Population with S-Shaped Growth");

        Stock population = new Stock("population", 10, PEOPLE);

        Constant carryingCapacity = new Constant("Carrying capacity", PEOPLE,1000);

        Constant fractionalNetBirthRate = new Constant(
                "Maximum Fractional Birth Flow",
                DIMENSIONLESS,
                0.04);

        Flow births = new FlowPerDay("Births") {
            @Override
            protected Quantity quantityPerDay() {
                return ExponentialChangeWithLimit.from(
                        population,
                        fractionalNetBirthRate.getValue(),
                        carryingCapacity.getValue());
            }
        };

        population.addInflow(births);

        model.addStock(population);

        Simulation run = new Simulation(model, DAY, WEEK, 32);
        run.addEventHandler(new ChartViewer());
        run.execute();
    }
}
