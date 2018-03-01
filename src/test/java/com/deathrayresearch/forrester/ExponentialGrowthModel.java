package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.archetypes.SimpleExponentialChange;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Flow;
import com.deathrayresearch.forrester.rate.FlowPerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;
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

        FlowPerDay birthRate = new FlowPerDay("Births") {
            @Override
            protected Quantity quantityPerDay() {
                return SimpleExponentialChange.from(population, 0.04);
            }
        };

        Flow deathRate = new FlowPerDay("Deaths") {
            @Override
            protected Quantity quantityPerDay() {
                return SimpleExponentialChange.from(population, 0.03);
            }
        };

        population.addInflow(birthRate);
        population.addOutflow(deathRate);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), WEEK, 52);
        run.addEventHandler(new ChartViewer());
        run.execute();
    }
}
