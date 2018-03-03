package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.archetypes.SimpleExponentialChange;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public class ExponentialGrowthModel {


    private static final People PEOPLE = People.getInstance();

    @Test
    public void testRun1() {
        Model model = new Model("Population with unconstrained growth");

        Stock population = new Stock("population", 100, PEOPLE);

        FlowPerDay births = new FlowPerDay("Births") {
            @Override
            protected Quantity quantityPerDay() {
                return SimpleExponentialChange.from(population, 0.04);
            }
        };

        Flow deaths = new FlowPerDay("Deaths") {
            @Override
            protected Quantity quantityPerDay() {
                return SimpleExponentialChange.from(population, 0.03);
            }
        };

        population.addInflow(births);
        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), WEEK, 52);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
