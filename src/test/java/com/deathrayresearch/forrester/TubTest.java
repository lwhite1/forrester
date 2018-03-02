package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.volume.Volumes;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Flow;
import com.deathrayresearch.forrester.rate.FlowPerMinute;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import java.time.Duration;

import static com.deathrayresearch.forrester.measure.Units.*;

/**
 *
 */
public class TubTest {

    @Test
    public void testRun1() {

        Model model = new Model("Tub model");
        Simulation run = new Simulation(model, MINUTE, MINUTE, 10);

        Stock tub = new Stock("Water in Tub", 50, GALLON_US);

        // the water drains at the rate of the outflow capacity or the amount of water in the tub, whichever is less
        Flow outflow = new FlowPerMinute("Outflow") {

            Quantity volumeOut = Volumes.gallonsUS( 5.0);

            @Override
            public Quantity quantityPerMinute() {
                return new Quantity(
                        Math.min(volumeOut.getValue(), tub.getQuantity().getValue()),
                        GALLON_US);
            }
        };

        FlowPerMinute inflow = new FlowPerMinute("Inflow") {

            Quantity volumeIn =  Volumes.gallonsUS( 5);

            Quantity lowInflow = Volumes.gallonsUS(0.0);
            @Override
            protected Quantity quantityPerMinute() {
                // waits five minutes before adding any inflow
                if (durationIsLessThan(run.getElapsedTime(), Duration.ofMinutes(5))) {
                    return lowInflow;
                }
                return volumeIn;
            }
        };

        tub.addInflow(inflow);
        tub.addOutflow(outflow);

        model.addStock(tub);

        run.addEventHandler(new ChartViewer());
        run.addEventHandler(new CsvSubscriber("tub.csv"));
        run.execute();
    }

    private static boolean durationIsLessThan(Duration d1, Duration d2) {
        return d1.compareTo(d2) < 0;
    }
}