package systems.courant.shrewd.measure.units;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.units.length.LengthUnits;
import systems.courant.shrewd.measure.units.length.Lengths;
import systems.courant.shrewd.measure.units.mass.MassUnits;
import systems.courant.shrewd.measure.units.mass.Masses;
import systems.courant.shrewd.measure.units.time.TimeUnits;
import systems.courant.shrewd.measure.units.time.Times;
import systems.courant.shrewd.measure.units.volume.VolumeUnits;
import systems.courant.shrewd.measure.units.volume.Volumes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FactoryMethodTest {

    @Nested
    class LengthsTest {

        @Test
        public void shouldCreateFeet() {
            Quantity q = Lengths.feet(3.5);
            assertEquals(3.5, q.getValue(), 0.001);
            assertEquals(LengthUnits.FOOT, q.getUnit());
        }

        @Test
        public void shouldCreateInches() {
            Quantity q = Lengths.inches(12.0);
            assertEquals(12.0, q.getValue(), 0.001);
            assertEquals(LengthUnits.INCH, q.getUnit());
        }

        @Test
        public void shouldCreateMeters() {
            Quantity q = Lengths.meters(100.0);
            assertEquals(100.0, q.getValue(), 0.001);
            assertEquals(LengthUnits.METER, q.getUnit());
        }

        @Test
        public void shouldCreateMiles() {
            Quantity q = Lengths.miles(2.0);
            assertEquals(2.0, q.getValue(), 0.001);
            assertEquals(LengthUnits.MILE, q.getUnit());
        }

        @Test
        public void shouldCreateNauticalMiles() {
            Quantity q = Lengths.nauticalMiles(5.0);
            assertEquals(5.0, q.getValue(), 0.001);
            assertEquals(LengthUnits.NAUTICAL_MILE, q.getUnit());
        }
    }

    @Nested
    class MassesTest {

        @Test
        public void shouldCreateKilograms() {
            Quantity q = Masses.kilograms(75.0);
            assertEquals(75.0, q.getValue(), 0.001);
            assertEquals(MassUnits.KILOGRAM, q.getUnit());
        }

        @Test
        public void shouldCreatePounds() {
            Quantity q = Masses.pounds(150.0);
            assertEquals(150.0, q.getValue(), 0.001);
            assertEquals(MassUnits.POUND, q.getUnit());
        }

        @Test
        public void shouldCreateOunces() {
            Quantity q = Masses.ounces(8.0);
            assertEquals(8.0, q.getValue(), 0.001);
            assertEquals(MassUnits.OUNCE, q.getUnit());
        }
    }

    @Nested
    class VolumesTest {

        @Test
        public void shouldCreateLiters() {
            Quantity q = Volumes.liters(2.0);
            assertEquals(2.0, q.getValue(), 0.001);
            assertEquals(VolumeUnits.LITER, q.getUnit());
        }

        @Test
        public void shouldCreateGallonsUS() {
            Quantity q = Volumes.gallonsUS(1.0);
            assertEquals(1.0, q.getValue(), 0.001);
            assertEquals(VolumeUnits.GALLON_US, q.getUnit());
        }

        @Test
        public void shouldCreateCubicMeters() {
            Quantity q = Volumes.cubicMeters(10.0);
            assertEquals(10.0, q.getValue(), 0.001);
            assertEquals(VolumeUnits.CUBIC_METER, q.getUnit());
        }

        @Test
        public void shouldCreateFluidOuncesUS() {
            Quantity q = Volumes.fluidOuncesUS(16.0);
            assertEquals(16.0, q.getValue(), 0.001);
            assertEquals(VolumeUnits.FLUID_OUNCE_US, q.getUnit());
        }

        @Test
        public void shouldCreateQuartsUS() {
            Quantity q = Volumes.quartsUS(4.0);
            assertEquals(4.0, q.getValue(), 0.001);
            assertEquals(VolumeUnits.QUART_US, q.getUnit());
        }
    }

    @Nested
    class TimesTest {

        @Test
        public void shouldCreateSeconds() {
            Quantity q = Times.seconds(30.0);
            assertEquals(30.0, q.getValue(), 0.001);
            assertEquals(TimeUnits.SECOND, q.getUnit());
        }

        @Test
        public void shouldCreateMinutes() {
            Quantity q = Times.minutes(5.0);
            assertEquals(5.0, q.getValue(), 0.001);
            assertEquals(TimeUnits.MINUTE, q.getUnit());
        }

        @Test
        public void shouldCreateHours() {
            Quantity q = Times.hours(2.0);
            assertEquals(2.0, q.getValue(), 0.001);
            assertEquals(TimeUnits.HOUR, q.getUnit());
        }

        @Test
        public void shouldCreateDays() {
            Quantity q = Times.days(7.0);
            assertEquals(7.0, q.getValue(), 0.001);
            assertEquals(TimeUnits.DAY, q.getUnit());
        }

        @Test
        public void shouldCreateWeeks() {
            Quantity q = Times.weeks(4.0);
            assertEquals(4.0, q.getValue(), 0.001);
            assertEquals(TimeUnits.WEEK, q.getUnit());
        }

        @Test
        public void shouldCreateYears() {
            Quantity q = Times.years(1.0);
            assertEquals(1.0, q.getValue(), 0.001);
            assertEquals(TimeUnits.YEAR, q.getUnit());
        }
    }
}
