package com.deathrayresearch.dynamics;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.dimension.Volume;
import com.deathrayresearch.dynamics.measure.units.time.Times;
import com.deathrayresearch.dynamics.measure.units.volume.Liter;
import com.deathrayresearch.dynamics.measure.units.time.Minute;
import com.deathrayresearch.dynamics.measure.units.volume.Volumes;
import com.deathrayresearch.dynamics.model.Flow;
import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.rate.FixedRate;
import com.deathrayresearch.dynamics.model.Stock;
import com.deathrayresearch.dynamics.rate.Rate;
import org.junit.Test;

/**
 *
 */
public class TubTest {

    @Test
    public void testRun1() {

        Model model = new Model("Tub model");

        Stock<Volume> tub = new Stock<>("Water in tub", Volumes.liters(3));

        Quantity<Volume> litersPerMinuteOut = Volumes.liters(3.0);

        // the water drains at the rate of the outflow capacity or the amount of water in the tub, whichever is less
        Rate<Volume> outRate = timeUnit ->
                new Quantity<>(
                    Math.min(litersPerMinuteOut.getValue(), tub.getCurrentValue().getValue()),
                    Liter.getInstance());

        Flow<Volume> outflow = new Flow<>("Out", outRate);

        Quantity<Volume> litersPerMinuteIn =  Volumes.liters(2.96);
        FixedRate<Volume> inRate = new FixedRate<>(litersPerMinuteIn, Minute.getInstance());
        Flow<Volume> inflow = new Flow<>("In", inRate);

        tub.addInflow(inflow);
        tub.addOutflow(outflow);

        model.addStock(tub);

        Simulation run = new Simulation(model, Minute.getInstance(), Times.hours(2));
        run.execute();
    }
}