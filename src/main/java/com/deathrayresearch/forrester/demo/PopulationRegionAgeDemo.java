package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.MultiArrayedFlow;
import com.deathrayresearch.forrester.model.MultiArrayedStock;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Subscript;
import com.deathrayresearch.forrester.model.SubscriptRange;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import java.util.List;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;

/**
 * A population model with Region (North, South, East) × AgeGroup (Young, Adult, Elder) = 9 stocks.
 *
 * <p>Demonstrates multi-dimensional subscript capabilities:
 * <ul>
 *   <li>Two {@link Subscript} dimensions composed into a {@link SubscriptRange}</li>
 *   <li>{@link MultiArrayedStock} with per-element initial values</li>
 *   <li>Aging flows: Young→Adult, Adult→Elder within each region (scalar flows via getStockAt)</li>
 *   <li>Birth flows: new Young proportional to Adult population per region</li>
 *   <li>Death flows: Elder outflow per region</li>
 *   <li>Migration flows: cross-region movement per age group</li>
 *   <li>Output showing "Population[North,Young]", "Population[South,Elder]", etc.</li>
 * </ul>
 */
public class PopulationRegionAgeDemo {

    public static void main(String[] args) {
        new PopulationRegionAgeDemo().run();
    }

    public void run() {
        Model model = new Model("Population Region-Age Model");

        Subscript region = new Subscript("Region", "North", "South", "East");
        Subscript ageGroup = new Subscript("AgeGroup", "Young", "Adult", "Elder");
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));

        // Initial populations (row-major: [North,Young], [North,Adult], [North,Elder],
        //                                 [South,Young], [South,Adult], [South,Elder],
        //                                 [East,Young],  [East,Adult],  [East,Elder])
        MultiArrayedStock pop = new MultiArrayedStock("Population", range,
                new double[]{
                        500, 800, 200,   // North
                        400, 600, 150,   // South
                        300, 500, 100    // East
                }, PEOPLE);

        // --- Aging flows: Young→Adult, Adult→Elder within each region ---
        double agingRate = 0.005;  // fraction per day

        for (int r = 0; r < region.size(); r++) {
            String regionLabel = region.getLabel(r);

            // Young → Adult
            Stock young = pop.getStockAt(regionLabel, "Young");
            Stock adult = pop.getStockAt(regionLabel, "Adult");
            Flow youngToAdult = Flow.create(
                    "Aging[" + regionLabel + ",Young->Adult]", DAY,
                    () -> new Quantity(young.getValue() * agingRate, PEOPLE));
            young.addOutflow(youngToAdult);
            adult.addInflow(youngToAdult);

            // Adult → Elder
            Stock elder = pop.getStockAt(regionLabel, "Elder");
            Flow adultToElder = Flow.create(
                    "Aging[" + regionLabel + ",Adult->Elder]", DAY,
                    () -> new Quantity(adult.getValue() * agingRate, PEOPLE));
            adult.addOutflow(adultToElder);
            elder.addInflow(adultToElder);
        }

        // --- Birth flows: new Young proportional to Adult population per region ---
        double birthRate = 0.003;  // fraction of adults per day

        for (int r = 0; r < region.size(); r++) {
            String regionLabel = region.getLabel(r);
            Stock adult = pop.getStockAt(regionLabel, "Adult");
            Stock young = pop.getStockAt(regionLabel, "Young");
            Flow births = Flow.create(
                    "Births[" + regionLabel + "]", DAY,
                    () -> new Quantity(adult.getValue() * birthRate, PEOPLE));
            young.addInflow(births);
        }

        // --- Death flows: Elder outflow per region ---
        double deathRate = 0.008;  // fraction of elders per day

        for (int r = 0; r < region.size(); r++) {
            String regionLabel = region.getLabel(r);
            Stock elder = pop.getStockAt(regionLabel, "Elder");
            Flow deaths = Flow.create(
                    "Deaths[" + regionLabel + "]", DAY,
                    () -> new Quantity(elder.getValue() * deathRate, PEOPLE));
            elder.addOutflow(deaths);
        }

        // --- Migration flows: cross-region movement per age group ---
        // Small daily migration: 0.1% of each age group moves to the next region (circular)
        double migrationRate = 0.001;

        for (int a = 0; a < ageGroup.size(); a++) {
            String ageLabel = ageGroup.getLabel(a);
            for (int r = 0; r < region.size(); r++) {
                int from = r;
                int to = (r + 1) % region.size();
                Stock fromStock = pop.getStockAt(region.getLabel(from), ageLabel);
                Stock toStock = pop.getStockAt(region.getLabel(to), ageLabel);
                Flow migration = Flow.create(
                        "Migration[" + region.getLabel(from) + "->" + region.getLabel(to)
                                + "," + ageLabel + "]",
                        DAY,
                        () -> new Quantity(fromStock.getValue() * migrationRate, PEOPLE));
                fromStock.addOutflow(migration);
                toStock.addInflow(migration);
            }
        }

        // --- Add to model ---
        model.addMultiArrayedStock(pop);

        // --- Run simulation ---
        Simulation run = new Simulation(model, DAY, Times.years(2));
        run.addEventHandler(new CsvSubscriber(
                System.getProperty("java.io.tmpdir") + "/forrester-population-region-age.csv"));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
