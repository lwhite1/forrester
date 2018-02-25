package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.units.length.Mile;
import com.deathrayresearch.forrester.measure.units.time.Times;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class AbstractRateTest {

    @Test
    public void flowPerHour() throws Exception {


        Rate dailyRate = new AbstractRate("A daily rate", Times.DAY) {

            @Override
            public Quantity flowPerTimeUnit(TimeUnit timeUnit) {

                // return a rate of 240 miles per day
                Quantity quantityPerDay = new Quantity(240, Mile.getInstance());
                return RateConverter.convert(quantityPerDay, getTimeUnit(), timeUnit);
            }
        };

        Quantity hoursWorth = dailyRate.flowPerTimeUnit(Times.HOUR);

        assertEquals(new Quantity(10, Mile.getInstance()).getValue(), hoursWorth.getValue(), 0.01);
    }

    @Test
    public void flowPerWeek() throws Exception {

        Rate dailyRate = new AbstractRate("A daily rate", Times.DAY) {

            @Override
            public Quantity flowPerTimeUnit(TimeUnit timeUnit) {

                // return a rate of 240 miles per day
                Quantity quantityPerDay = new Quantity(240, Mile.getInstance());
                return RateConverter.convert(quantityPerDay, getTimeUnit(), timeUnit);
            }
        };

        Quantity weeksWorth =  dailyRate.flowPerTimeUnit(Times.WEEK);

        assertEquals(new Quantity(7 * 240, Mile.getInstance()).getValue(), weeksWorth.getValue(), 0.01);

    }

}