package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Flows;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Models a population declining through exponential decay with no births.
 *
 * <p>A single Population stock drains through a death outflow proportional to its current level
 * (1/80 per day). With no inflow the stock decays asymptotically toward zero, illustrating
 * the basic exponential decay pattern used throughout system dynamics.
 */
public class ExponentialDecayDemo {

    public static void main(String[] args) {
        new ExponentialDecayDemo().run();
    }

    public void run() {

        Model model = new Model("Population with exponential decay");

        Stock population = new Stock("Population", 100, PEOPLE);

        Flow deaths = Flows.exponentialGrowth("Deaths", DAY, population, 1/80.0);

        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, TimeUnits.DAY, Times.weeks( 52));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
