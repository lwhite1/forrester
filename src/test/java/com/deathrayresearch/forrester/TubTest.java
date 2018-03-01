package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.volume.Volumes;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.rate.RatePerMinute;
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
        Simulation run = new Simulation(model, MINUTE, HOUR, 1);

        Stock tub = new Stock("Water in Tub", 30, LITER);

        // the water drains at the rate of the outflow capacity or the amount of water in the tub, whichever is less
        Rate outRate = new RatePerMinute() {
            Quantity litersPerMinuteOut = Volumes.liters("Liters out", 3.0);

            @Override
            public Quantity quantityPerMinute() {
                return new Quantity("Outflow",
                        Math.min(litersPerMinuteOut.getValue(), tub.getCurrentValue().getValue()),
                        LITER);
            }
        };

        Flow outflow = new Flow(outRate);

        RatePerMinute inRate = new RatePerMinute() {
            Quantity litersPerMinuteIn =  Volumes.liters("Inflow", 2.96);
            Quantity lowInflow = Volumes.liters("Inflow", 1.0);
            @Override
            protected Quantity quantityPerMinute() {
                // waits five minutes before adding any inflow
                if (durationIsLessThan(run.getElapsedTime(), Duration.ofMinutes(5))) {
                    return lowInflow;
                }
                return litersPerMinuteIn;
            }
        };
        Flow inflow = new Flow(inRate);

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