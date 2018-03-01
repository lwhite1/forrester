package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.archetypes.SimpleExponentialChange;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.RatePerDay;
import com.deathrayresearch.forrester.rate.Rate;
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

        RatePerDay birthRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return SimpleExponentialChange.from("Births", population, 0.04);
            }
        };

        Flow newBirths = new Flow(birthRate);

        Rate deathRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return SimpleExponentialChange.from("Deaths", population, 0.03);
            }
        };

        Flow deaths = new Flow(deathRate);

        population.addInflow(newBirths);
        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, Day.getInstance(), WEEK, 52);
        run.addEventHandler(new ChartViewer());
        run.execute();
    }
}
