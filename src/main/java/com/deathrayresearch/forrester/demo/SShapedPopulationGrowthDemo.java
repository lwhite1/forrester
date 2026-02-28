package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.archetypes.ExponentialChangeWithLimit;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DIMENSIONLESS;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 * Models logistic (S-shaped) population growth constrained by a carrying capacity.
 *
 * <p>A Population stock grows via a birth inflow whose rate diminishes as the population
 * approaches the carrying capacity (1,000). Growth starts exponentially, inflects at the
 * midpoint, and levels off — the classic S-curve produced by a balancing feedback loop that
 * limits a reinforcing loop.
 */
public class SShapedPopulationGrowthDemo {

    public static void main(String[] args) {
        new SShapedPopulationGrowthDemo().run();
    }

    public void run() {
        Model model = new Model("Population with S-Shaped Growth");

        Stock population = new Stock("population", 10, PEOPLE);

        Constant carryingCapacity = new Constant("Carrying capacity", PEOPLE,1000);

        Constant fractionalNetBirthRate = new Constant(
                "Maximum Fractional Birth Flow",
                DIMENSIONLESS,
                0.04);

        Flow births = new FlowPerDay("Births") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return ExponentialChangeWithLimit.from(
                        population,
                        fractionalNetBirthRate.getValue(),
                        carryingCapacity.getValue());
            }
        };

        population.addInflow(births);

        model.addStock(population);

        Simulation run = new Simulation(model, DAY, WEEK, 32);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
