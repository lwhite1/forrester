package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Minute;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.measure.units.volume.Liter;
import com.deathrayresearch.forrester.measure.units.volume.Volumes;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.rate.RatePerMinute;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class TubTest {

    @Test
    public void testRun1() {

        Model model = new Model("Tub model");

        Stock tub = new Stock(Volumes.liters("Water in Tub", 3));

        // the water drains at the rate of the outflow capacity or the amount of water in the tub, whichever is less
        Rate outRate = new RatePerMinute() {
            Quantity litersPerMinuteOut = Volumes.liters("Liters out", 3.0);

            @Override
            public Quantity quantityPerMinute() {
                return new Quantity("Outflow",
                        Math.min(litersPerMinuteOut.getValue(), tub.getCurrentValue().getValue()),
                        Liter.getInstance());
            }
        };

        Flow outflow = new Flow(outRate);


        RatePerMinute inRate = new RatePerMinute() {
            Quantity litersPerMinuteIn =  Volumes.liters("Inflow", 2.96);

            @Override
            protected Quantity quantityPerMinute() {
                return litersPerMinuteIn;
            }
        };
        Flow inflow = new Flow(inRate);

        tub.addInflow(inflow);
        tub.addOutflow(outflow);

        model.addStock(tub);

        Simulation run = new Simulation(model, Minute.getInstance(), Times.HOUR, 2);
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }
}