package com.deathrayresearch.forrester.model.flows;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.length.Mile;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Hour;
import com.deathrayresearch.forrester.measure.units.time.Minute;
import com.deathrayresearch.forrester.measure.units.time.Second;
import com.deathrayresearch.forrester.measure.units.time.Week;
import org.junit.Test;

import static org.junit.Assert.*;

public class RateConverterTest {

    @Test
    public void hoursToDay_scalesUp() {
        Quantity perHour = new Quantity(100, Mile.getInstance());
        Quantity perDay = RateConverter.convert(perHour, Hour.getInstance(), Day.getInstance());
        assertEquals(2400, perDay.getValue(), 0.0);
    }

    @Test
    public void hoursToDay_smallValue() {
        Quantity perHour = new Quantity(10, Mile.getInstance());
        Quantity perDay = RateConverter.convert(perHour, Hour.getInstance(), Day.getInstance());
        assertEquals(240, perDay.getValue(), 0.0);
    }

    @Test
    public void dayToHours_scalesDown() {
        Quantity perDay = new Quantity(2400, Mile.getInstance());
        Quantity perHour = RateConverter.convert(perDay, Day.getInstance(), Hour.getInstance());
        assertEquals(100, perHour.getValue(), 0.0);
    }

    @Test
    public void hoursToMinutes() {
        Quantity perHour = new Quantity(60, Mile.getInstance());
        Quantity perMinute = RateConverter.convert(perHour, Hour.getInstance(), Minute.getInstance());
        assertEquals(1, perMinute.getValue(), 0.0);
    }

    @Test
    public void weekToDay() {
        Quantity perWeek = new Quantity(700, Mile.getInstance());
        Quantity perDay = RateConverter.convert(perWeek, Week.getInstance(), Day.getInstance());
        assertEquals(100, perDay.getValue(), 0.0);
    }

    @Test
    public void dayToWeek() {
        Quantity perDay = new Quantity(100, Mile.getInstance());
        Quantity perWeek = RateConverter.convert(perDay, Day.getInstance(), Week.getInstance());
        assertEquals(700, perWeek.getValue(), 0.0);
    }

    @Test
    public void minutesToSeconds() {
        Quantity perMinute = new Quantity(120, Mile.getInstance());
        Quantity perSecond = RateConverter.convert(perMinute, Minute.getInstance(), Second.getInstance());
        assertEquals(2, perSecond.getValue(), 0.0);
    }

    @Test
    public void sameUnit_returnsOriginalValue() {
        Quantity perHour = new Quantity(42, Mile.getInstance());
        Quantity result = RateConverter.convert(perHour, Hour.getInstance(), Hour.getInstance());
        assertEquals(42, result.getValue(), 0.0);
    }

    @Test
    public void zeroQuantity_returnsZero() {
        Quantity zero = new Quantity(0, Mile.getInstance());
        Quantity result = RateConverter.convert(zero, Hour.getInstance(), Day.getInstance());
        assertEquals(0, result.getValue(), 0.0);
    }

    @Test
    public void roundTrip_isIdentity() {
        Quantity original = new Quantity(365, Mile.getInstance());
        Quantity converted = RateConverter.convert(original, Day.getInstance(), Minute.getInstance());
        Quantity backAgain = RateConverter.convert(converted, Minute.getInstance(), Day.getInstance());
        assertEquals(original.getValue(), backAgain.getValue(), 1e-9);
    }

    @Test
    public void preservesUnit() {
        Quantity perHour = new Quantity(100, Mile.getInstance());
        Quantity perDay = RateConverter.convert(perHour, Hour.getInstance(), Day.getInstance());
        assertEquals(Mile.getInstance(), perDay.getUnit());
    }
}
