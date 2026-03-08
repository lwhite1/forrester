package systems.courant.forrester.demo;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.units.time.TimeUnits;
import systems.courant.forrester.measure.units.time.Times;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Flows;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.ui.StockLevelChartViewer;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.PEOPLE;

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
