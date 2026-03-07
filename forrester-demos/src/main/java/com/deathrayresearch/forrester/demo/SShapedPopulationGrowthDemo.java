package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Flows;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.ModelMetadata;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 * Models logistic (S-shaped) population growth constrained by a carrying capacity.
 *
 * <p>A Population stock grows via a birth inflow whose rate diminishes as the population
 * approaches the carrying capacity. Growth starts exponentially, inflects at the
 * midpoint, and levels off — the classic S-curve produced by a balancing feedback loop that
 * limits a reinforcing loop.
 */
public class SShapedPopulationGrowthDemo {

    public static void main(String[] args) {
        double initialPopulation = 10;
        double carryingCapacity = 1000;
        double maxFractionalBirthRate = 0.04;
        double durationWeeks = 32;

        new SShapedPopulationGrowthDemo().run(
                initialPopulation, carryingCapacity, maxFractionalBirthRate, durationWeeks);
    }

    public void run(double initialPopulation, double carryingCapacity,
                    double maxFractionalBirthRate, double durationWeeks) {
        Model model = new Model("Population with S-Shaped Growth");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock population = new Stock("population", initialPopulation, PEOPLE);

        Flow births = Flows.exponentialGrowthWithLimit("Births", DAY, population,
                maxFractionalBirthRate, carryingCapacity);

        population.addInflow(births);

        model.addStock(population);

        Simulation run = new Simulation(model, DAY, WEEK, durationWeeks);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
