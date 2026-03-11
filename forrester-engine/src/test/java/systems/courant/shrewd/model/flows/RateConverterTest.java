package systems.courant.forrester.model.flows;

import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.units.length.LengthUnits;
import systems.courant.forrester.measure.units.time.TimeUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RateConverterTest {

    @Test
    public void hoursToDay_scalesUp() {
        Quantity perHour = new Quantity(100, LengthUnits.MILE);
        Quantity perDay = RateConverter.convert(perHour, TimeUnits.HOUR, TimeUnits.DAY);
        assertEquals(2400, perDay.getValue(), 0.0);
    }

    @Test
    public void hoursToDay_smallValue() {
        Quantity perHour = new Quantity(10, LengthUnits.MILE);
        Quantity perDay = RateConverter.convert(perHour, TimeUnits.HOUR, TimeUnits.DAY);
        assertEquals(240, perDay.getValue(), 0.0);
    }

    @Test
    public void dayToHours_scalesDown() {
        Quantity perDay = new Quantity(2400, LengthUnits.MILE);
        Quantity perHour = RateConverter.convert(perDay, TimeUnits.DAY, TimeUnits.HOUR);
        assertEquals(100, perHour.getValue(), 0.0);
    }

    @Test
    public void hoursToMinutes() {
        Quantity perHour = new Quantity(60, LengthUnits.MILE);
        Quantity perMinute = RateConverter.convert(perHour, TimeUnits.HOUR, TimeUnits.MINUTE);
        assertEquals(1, perMinute.getValue(), 0.0);
    }

    @Test
    public void weekToDay() {
        Quantity perWeek = new Quantity(700, LengthUnits.MILE);
        Quantity perDay = RateConverter.convert(perWeek, TimeUnits.WEEK, TimeUnits.DAY);
        assertEquals(100, perDay.getValue(), 0.0);
    }

    @Test
    public void dayToWeek() {
        Quantity perDay = new Quantity(100, LengthUnits.MILE);
        Quantity perWeek = RateConverter.convert(perDay, TimeUnits.DAY, TimeUnits.WEEK);
        assertEquals(700, perWeek.getValue(), 0.0);
    }

    @Test
    public void minutesToSeconds() {
        Quantity perMinute = new Quantity(120, LengthUnits.MILE);
        Quantity perSecond = RateConverter.convert(perMinute, TimeUnits.MINUTE, TimeUnits.SECOND);
        assertEquals(2, perSecond.getValue(), 0.0);
    }

    @Test
    public void sameUnit_returnsOriginalValue() {
        Quantity perHour = new Quantity(42, LengthUnits.MILE);
        Quantity result = RateConverter.convert(perHour, TimeUnits.HOUR, TimeUnits.HOUR);
        assertEquals(42, result.getValue(), 0.0);
    }

    @Test
    public void zeroQuantity_returnsZero() {
        Quantity zero = new Quantity(0, LengthUnits.MILE);
        Quantity result = RateConverter.convert(zero, TimeUnits.HOUR, TimeUnits.DAY);
        assertEquals(0, result.getValue(), 0.0);
    }

    @Test
    public void roundTrip_isIdentity() {
        Quantity original = new Quantity(365, LengthUnits.MILE);
        Quantity converted = RateConverter.convert(original, TimeUnits.DAY, TimeUnits.MINUTE);
        Quantity backAgain = RateConverter.convert(converted, TimeUnits.MINUTE, TimeUnits.DAY);
        assertEquals(original.getValue(), backAgain.getValue(), 1e-9);
    }

    @Test
    public void preservesUnit() {
        Quantity perHour = new Quantity(100, LengthUnits.MILE);
        Quantity perDay = RateConverter.convert(perHour, TimeUnits.HOUR, TimeUnits.DAY);
        assertEquals(LengthUnits.MILE, perDay.getUnit());
    }
}
