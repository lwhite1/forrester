package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.length.Meter;
import com.deathrayresearch.forrester.measure.units.time.Hour;
import com.deathrayresearch.forrester.measure.units.time.Minute;
import org.junit.Test;

/**
 *
 */
public class FixedRateTest {

    @Test
    public void testFlowPerTimeUnit() {

        Quantity distance = new Quantity(100, Meter.getInstance());

        Rate speed = new FixedRate(distance, Minute.getInstance()); // 100 meters per minute
        System.out.println(speed);

        Quantity distancePerHour = speed.flowPerTimeUnit(Hour.getInstance());

        System.out.println(distancePerHour);
    }

}