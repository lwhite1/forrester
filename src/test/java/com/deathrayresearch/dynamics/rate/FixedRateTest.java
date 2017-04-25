package com.deathrayresearch.dynamics.rate;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.dimension.Length;
import com.deathrayresearch.dynamics.measure.units.time.Hour;
import com.deathrayresearch.dynamics.measure.units.length.Meter;
import com.deathrayresearch.dynamics.measure.units.time.Minute;
import org.junit.Test;

/**
 *
 */
public class FixedRateTest {

    @Test
    public void testFlowPerTimeUnit() {

        Quantity<Length> distance = new Quantity<>(100, Meter.getInstance());

        Rate<Length> speed = new FixedRate<>(distance, Minute.getInstance()); // 100 meters per minute
        System.out.println(speed);

        Quantity<Length> distancePerHour = speed.flowPerTimeUnit(Hour.getInstance());

        System.out.println(distancePerHour);
    }

}