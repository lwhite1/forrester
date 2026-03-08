package systems.courant.forrester.demo;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.io.CsvSubscriber;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.units.time.Times;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.MultiArrayedStock;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.Subscript;
import systems.courant.forrester.model.SubscriptRange;
import systems.courant.forrester.ui.StockLevelChartViewer;

import java.util.List;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.PEOPLE;

/**
 * A population model with Region (North, South, East) x AgeGroup (Young, Adult, Elder) = 9 stocks.
 *
 * <p>Demonstrates multi-dimensional subscript capabilities:
 * <ul>
 *   <li>Two {@link Subscript} dimensions composed into a {@link SubscriptRange}</li>
 *   <li>{@link MultiArrayedStock} with per-element initial values</li>
 *   <li>Aging flows: Young->Adult, Adult->Elder within each region</li>
 *   <li>Birth flows: new Young proportional to Adult population per region</li>
 *   <li>Death flows: Elder outflow per region</li>
 *   <li>Migration flows: cross-region movement per age group</li>
 * </ul>
 */
public class PopulationRegionAgeDemo {

    public static void main(String[] args) {
        // Row-major: [North,Young], [North,Adult], [North,Elder],
        //            [South,Young], [South,Adult], [South,Elder],
        //            [East,Young],  [East,Adult],  [East,Elder]
        double[] initialPopulations = {
                500, 800, 200,   // North
                400, 600, 150,   // South
                300, 500, 100    // East
        };
        double agingRate = 0.005;      // fraction per day
        double birthRate = 0.003;      // fraction of adults per day
        double deathRate = 0.008;      // fraction of elders per day
        double migrationRate = 0.001;  // fraction per day
        double durationYears = 2;

        new PopulationRegionAgeDemo().run(initialPopulations, agingRate, birthRate,
                deathRate, migrationRate, durationYears);
    }

    public void run(double[] initialPopulations, double agingRate, double birthRate,
                    double deathRate, double migrationRate, double durationYears) {
        Model model = new Model("Population Region-Age Model");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Subscript region = new Subscript("Region", "North", "South", "East");
        Subscript ageGroup = new Subscript("AgeGroup", "Young", "Adult", "Elder");
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));

        MultiArrayedStock pop = new MultiArrayedStock("Population", range,
                initialPopulations, PEOPLE);

        // Aging flows: Young->Adult, Adult->Elder within each region
        for (int r = 0; r < region.size(); r++) {
            String regionLabel = region.getLabel(r);

            Stock young = pop.getStockAt(regionLabel, "Young");
            Stock adult = pop.getStockAt(regionLabel, "Adult");
            Flow youngToAdult = Flow.create(
                    "Aging[" + regionLabel + ",Young->Adult]", DAY,
                    () -> new Quantity(young.getValue() * agingRate, PEOPLE));
            young.addOutflow(youngToAdult);
            adult.addInflow(youngToAdult);

            Stock elder = pop.getStockAt(regionLabel, "Elder");
            Flow adultToElder = Flow.create(
                    "Aging[" + regionLabel + ",Adult->Elder]", DAY,
                    () -> new Quantity(adult.getValue() * agingRate, PEOPLE));
            adult.addOutflow(adultToElder);
            elder.addInflow(adultToElder);
        }

        // Birth flows: new Young proportional to Adult population per region
        for (int r = 0; r < region.size(); r++) {
            String regionLabel = region.getLabel(r);
            Stock adult = pop.getStockAt(regionLabel, "Adult");
            Stock young = pop.getStockAt(regionLabel, "Young");
            Flow births = Flow.create(
                    "Births[" + regionLabel + "]", DAY,
                    () -> new Quantity(adult.getValue() * birthRate, PEOPLE));
            young.addInflow(births);
        }

        // Death flows: Elder outflow per region
        for (int r = 0; r < region.size(); r++) {
            String regionLabel = region.getLabel(r);
            Stock elder = pop.getStockAt(regionLabel, "Elder");
            Flow deaths = Flow.create(
                    "Deaths[" + regionLabel + "]", DAY,
                    () -> new Quantity(elder.getValue() * deathRate, PEOPLE));
            elder.addOutflow(deaths);
        }

        // Migration flows: cross-region movement per age group (circular: N->S->E->N)
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

        model.addMultiArrayedStock(pop);

        Simulation run = new Simulation(model, DAY, Times.years(durationYears));
        run.addEventHandler(new CsvSubscriber(
                System.getProperty("java.io.tmpdir") + "/forrester-population-region-age.csv"));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
