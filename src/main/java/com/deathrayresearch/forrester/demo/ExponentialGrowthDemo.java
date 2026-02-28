package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Flows;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 * Models unconstrained population growth where births exceed deaths.
 *
 * <p>A single Population stock has an exponential birth inflow (4% per day) and an exponential
 * death outflow (3% per day). Because the net rate is positive, the population grows
 * exponentially — the fundamental "positive feedback loop" archetype in system dynamics.
 */
public class ExponentialGrowthDemo {

    private static final ItemUnits PEOPLE = ItemUnits.PEOPLE;

    public static void main(String[] args) {
        new ExponentialGrowthDemo().run();
    }

    public void run() {
        Model model = new Model("Population with unconstrained growth");

        Stock population = new Stock("population", 100, PEOPLE);

        Flow births = Flows.exponentialGrowth("Births", DAY, population, 0.04);
        Flow deaths = Flows.exponentialGrowth("Deaths", DAY, population, 0.03);

        population.addInflow(births);
        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, TimeUnits.DAY, WEEK, 52);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
