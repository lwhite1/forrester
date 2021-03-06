package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.archetypes.SimpleExponentialChange;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 *
 */
public class ExponentialDecayModel {

    @Test
    public void testRun1() {

        Model model = new Model("Population with exponential decay");

        Stock population = new Stock("Population", 100, PEOPLE);

        Flow deaths = new FlowPerDay("Deaths") {
            @Override
            protected Quantity quantityPerDay() {
                return SimpleExponentialChange.from(population, 1/80.0);
            }
        };

        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks( 52));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
