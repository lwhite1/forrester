package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.volume.Volumes;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import java.time.Duration;

import static com.deathrayresearch.forrester.measure.Units.GALLON_US;
import static com.deathrayresearch.forrester.measure.Units.MINUTE;

/**
 * The classic bathtub model — the simplest stock-and-flow demonstration.
 *
 * <p>A Water-in-Tub stock (50 gallons) drains at 5 gal/min from the start while the inflow is
 * delayed for 5 minutes and then adds 5 gal/min. The tub drains down, then stabilizes once
 * inflow begins, illustrating how a stock is the accumulation of the difference between its
 * inflow and outflow over time.
 */
public class TubDemo {

    public static void main(String[] args) {
        new TubDemo().run();
    }

    public void run() {

        Model model = new Model("Tub model");
        Simulation run = new Simulation(model, MINUTE, MINUTE, 10);

        Stock tub = new Stock("Water in Tub", 50, GALLON_US);

        Quantity volumeOut = Volumes.gallonsUS(5.0);

        // the water drains at the rate of the outflow capacity or the amount of water in the tub, whichever is less
        Flow outflow = Flow.create("Outflow", MINUTE, () ->
                new Quantity(
                        Math.min(volumeOut.getValue(), tub.getQuantity().getValue()),
                        GALLON_US));

        Quantity volumeIn = Volumes.gallonsUS(5);
        Quantity lowInflow = Volumes.gallonsUS(0.0);

        Flow inflow = Flow.create("Inflow", MINUTE, () -> {
            // waits five minutes before adding any inflow
            if (durationIsLessThan(run.getElapsedTime(), Duration.ofMinutes(5))) {
                return lowInflow;
            }
            return volumeIn;
        });

        tub.addInflow(inflow);
        tub.addOutflow(outflow);

        model.addStock(tub);

        run.addEventHandler(new StockLevelChartViewer());
        run.addEventHandler(new CsvSubscriber("tub.csv"));
        run.execute();
    }

    private static boolean durationIsLessThan(Duration d1, Duration d2) {
        return d1.compareTo(d2) < 0;
    }
}
