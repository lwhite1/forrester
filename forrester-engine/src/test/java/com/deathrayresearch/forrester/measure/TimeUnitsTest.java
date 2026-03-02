package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeUnitsTest {

    @Test
    public void millisecondShouldHaveCorrectRatio() {
        assertEquals(0.001, TimeUnits.MILLISECOND.ratioToBaseUnit(), 1e-10);
    }

    @Test
    public void secondShouldBeBaseUnit() {
        assertEquals(1.0, TimeUnits.SECOND.ratioToBaseUnit(), 0.0);
    }

    @Test
    public void minuteShouldBe60Seconds() {
        assertEquals(60.0, TimeUnits.MINUTE.ratioToBaseUnit(), 0.0);
    }

    @Test
    public void hourShouldBe3600Seconds() {
        assertEquals(3600.0, TimeUnits.HOUR.ratioToBaseUnit(), 0.0);
    }

    @Test
    public void dayShouldBe86400Seconds() {
        assertEquals(86400.0, TimeUnits.DAY.ratioToBaseUnit(), 0.0);
    }

    @Test
    public void weekShouldBe7Days() {
        assertEquals(7 * 86400.0, TimeUnits.WEEK.ratioToBaseUnit(), 0.0);
    }

    @Test
    public void monthShouldBe30Days() {
        assertEquals(30L * 24 * 60 * 60, TimeUnits.MONTH.ratioToBaseUnit(), 0.0);
    }

    @Test
    public void yearShouldBe365Days() {
        assertEquals(365L * 24 * 60 * 60, TimeUnits.YEAR.ratioToBaseUnit(), 0.0);
    }

    @Test
    public void allTimeDimensionsShouldBeTIME() {
        for (TimeUnits unit : TimeUnits.values()) {
            assertEquals(Dimension.TIME, unit.getDimension(), unit.getName() + " should be TIME");
        }
    }

    @Test
    public void millisecondConversionShouldWork() {
        Quantity oneSecond = new Quantity(1, TimeUnits.SECOND);
        Quantity inMillis = oneSecond.convertUnits(TimeUnits.MILLISECOND);
        assertEquals(1000, inMillis.getValue(), 0.01);
    }

    @Test
    public void monthConversionShouldWork() {
        Quantity oneYear = new Quantity(1, TimeUnits.YEAR);
        Quantity inMonths = oneYear.convertUnits(TimeUnits.MONTH);
        // 365 days / 30 days = 12.1667
        assertTrue(inMonths.getValue() > 12 && inMonths.getValue() < 13);
    }

    @Test
    public void millisecondToBaseUnitsShouldWork() {
        double baseValue = TimeUnits.MILLISECOND.toBaseUnits(500.0);
        assertEquals(0.5, baseValue, 1e-10);
    }

    @Test
    public void monthToBaseUnitsShouldWork() {
        double baseValue = TimeUnits.MONTH.toBaseUnits(1.0);
        assertEquals(30.0 * 24 * 60 * 60, baseValue, 0.0);
    }
}
