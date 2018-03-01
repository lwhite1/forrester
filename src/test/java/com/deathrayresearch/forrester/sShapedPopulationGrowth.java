package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.archetypes.ExponentialChangeWithLimit;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.RatePerDay;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.DIMENSIONLESS;
import static com.deathrayresearch.forrester.measure.units.time.Times.DAY;
import static com.deathrayresearch.forrester.measure.units.time.Times.WEEK;

/**
 *
 */
public class sShapedPopulationGrowth {

    @Test
    public void testRun1() {
        Model model = new Model("Population with S-Shaped Growth");

        Stock population = new Stock("pop", 10, People.getInstance());

        Quantity carryingCapacity = new Quantity("Carrying capacity", 1000, People.getInstance());

        Constant fractionalNetBirthRate = new Constant(
                "Maximum Fractional Birth Rate",
                DIMENSIONLESS,
                0.04);

        Rate birthRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return ExponentialChangeWithLimit.from(
                        "Births",
                        population,
                        fractionalNetBirthRate.getCurrentValue(),
                        carryingCapacity.getValue());
            }
        };

        Flow births = new Flow(birthRate);

        population.addInflow(births);

        model.addStock(population);

        Simulation run = new Simulation(model, DAY, WEEK, 32);
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }
}
