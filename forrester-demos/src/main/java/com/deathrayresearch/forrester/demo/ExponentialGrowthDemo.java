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
 * <p>A single Population stock has an exponential birth inflow and an exponential
 * death outflow. Because the net rate is positive, the population grows
 * exponentially — the fundamental "positive feedback loop" archetype in system dynamics.
 */
public class ExponentialGrowthDemo {

    private static final ItemUnits PEOPLE = ItemUnits.PEOPLE;

    public static void main(String[] args) {
        double initialPopulation = 100;
        double birthRate = 0.04;       // fraction per day
        double deathRate = 0.03;       // fraction per day
        double durationWeeks = 52;

        new ExponentialGrowthDemo().run(initialPopulation, birthRate, deathRate, durationWeeks);
    }

    public void run(double initialPopulation, double birthRate, double deathRate,
                    double durationWeeks) {
        Model model = new Model("Population with unconstrained growth");

        Stock population = new Stock("population", initialPopulation, PEOPLE);

        Flow births = Flows.exponentialGrowth("Births", DAY, population, birthRate);
        Flow deaths = Flows.exponentialGrowth("Deaths", DAY, population, deathRate);

        population.addInflow(births);
        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, TimeUnits.DAY, WEEK, durationWeeks);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
