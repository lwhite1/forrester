package systems.courant.forrester.demo;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.units.time.TimeUnits;
import systems.courant.forrester.measure.units.time.Times;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.ui.StockLevelChartViewer;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.PEOPLE;

/**
 * Demonstrates a first-order material delay (exponential smoothing).
 *
 * <p>A Potential Customers stock drains through a sales outflow equal to the stock level divided
 * by an average delay. This assumes the stock is fully mixed (no FIFO ordering),
 * producing exponential decay — the simplest material delay in system dynamics.
 */
public class FirstOrderMaterialDelayDemo {

    public static void main(String[] args) {
        double initialCustomers = 1000;
        double averageDelayDays = 120;
        double durationWeeks = 52;

        new FirstOrderMaterialDelayDemo().run(initialCustomers, averageDelayDays, durationWeeks);
    }

    public void run(double initialCustomers, double averageDelayDays, double durationWeeks) {
        Model model = new Model("First order material delay");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock potentialCustomers = new Stock("Potential Customers", initialCustomers, PEOPLE);

        Flow sales = Flow.create("Sales", DAY, () ->
                potentialCustomers.getQuantity().divide(averageDelayDays));

        potentialCustomers.addOutflow(sales);

        model.addStock(potentialCustomers);

        Simulation run = new Simulation(model, TimeUnits.DAY, Times.weeks(durationWeeks));
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
