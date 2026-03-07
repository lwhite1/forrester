package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Flows;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.ModelMetadata;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * Models a population declining through exponential decay with no births.
 *
 * <p>A single Population stock drains through a death outflow proportional to its current level.
 * With no inflow the stock decays asymptotically toward zero, illustrating
 * the basic exponential decay pattern used throughout system dynamics.
 */
public class ExponentialDecayDemo {

    public static void main(String[] args) {
        double initialPopulation = 100;
        double deathRate = 1 / 80.0;  // fraction per day
        double durationWeeks = 52;

        new ExponentialDecayDemo().run(initialPopulation, deathRate, durationWeeks);
    }

    public void run(double initialPopulation, double deathRate, double durationWeeks) {
        Model model = new Model("Population with exponential decay");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock population = new Stock("Population", initialPopulation, PEOPLE);

        Flow deaths = Flows.exponentialGrowth("Deaths", DAY, population, deathRate);

        population.addOutflow(deaths);

        model.addStock(population);

        Simulation run = new Simulation(model, TimeUnits.DAY, Times.weeks(durationWeeks));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
