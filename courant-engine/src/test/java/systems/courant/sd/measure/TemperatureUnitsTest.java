package systems.courant.sd.measure;

import systems.courant.sd.measure.units.temperature.TemperatureUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemperatureUnitsTest {

    @Test
    public void celsiusShouldBeBaseUnit() {
        assertEquals(1.0, TemperatureUnits.CELSIUS.ratioToBaseUnit(), 0.0);
        assertEquals(Dimension.TEMPERATURE, TemperatureUnits.CELSIUS.getDimension());
    }

    @Test
    public void fahrenheitShouldHaveFiveNinthsRatio() {
        assertEquals(5.0 / 9.0, TemperatureUnits.FAHRENHEIT.ratioToBaseUnit(), 1e-10);
    }

    @Test
    public void celsiusToBaseUnitsShouldReturnSameValue() {
        Quantity celsius = new Quantity(100, TemperatureUnits.CELSIUS);
        Quantity base = celsius.inBaseUnits();
        assertEquals(100, base.getValue(), 0.0);
    }

    @Test
    public void fahrenheitToBaseUnitsQuantityShouldThrow() {
        Quantity fahrenheit = new Quantity(100, TemperatureUnits.FAHRENHEIT);
        assertThrows(UnsupportedOperationException.class, fahrenheit::inBaseUnits);
    }

    @Test
    public void fahrenheitToBaseUnitsDoubleShouldThrow() {
        assertThrows(UnsupportedOperationException.class,
                () -> TemperatureUnits.FAHRENHEIT.toBaseUnits(100.0));
    }

    @Test
    public void fahrenheitFromBaseUnitsQuantityShouldThrow() {
        Quantity celsius = new Quantity(37.78, TemperatureUnits.CELSIUS);
        assertThrows(UnsupportedOperationException.class,
                () -> TemperatureUnits.FAHRENHEIT.fromBaseUnits(celsius));
    }

    @Test
    public void fahrenheitFromBaseUnitsDoubleShouldThrow() {
        assertThrows(UnsupportedOperationException.class,
                () -> TemperatureUnits.FAHRENHEIT.fromBaseUnits(37.78));
    }

    @Test
    public void fahrenheitEqualsShouldWorkForSameUnit() {
        Quantity a = new Quantity(100, TemperatureUnits.FAHRENHEIT);
        Quantity b = new Quantity(100, TemperatureUnits.FAHRENHEIT);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void fahrenheitNotEqualForDifferentValues() {
        Quantity a = new Quantity(100, TemperatureUnits.FAHRENHEIT);
        Quantity b = new Quantity(212, TemperatureUnits.FAHRENHEIT);
        assertNotEquals(a, b);
    }

    @Test
    public void fahrenheitComparisonsShouldWorkForSameUnit() {
        Quantity low = new Quantity(32, TemperatureUnits.FAHRENHEIT);
        Quantity high = new Quantity(212, TemperatureUnits.FAHRENHEIT);
        assertTrue(low.isLessThan(high));
        assertTrue(high.isGreaterThan(low));
        assertTrue(low.isLessThanOrEqualTo(low));
        assertTrue(high.isGreaterThanOrEqualTo(high));
        assertTrue(low.isEqual(new Quantity(32, TemperatureUnits.FAHRENHEIT)));
    }

    @Test
    public void fahrenheitConvertToSameUnitShouldReturnSelf() {
        Quantity f = new Quantity(100, TemperatureUnits.FAHRENHEIT);
        Quantity result = f.convertUnits(TemperatureUnits.FAHRENHEIT);
        assertEquals(100, result.getValue(), 0.0);
        assertEquals(TemperatureUnits.FAHRENHEIT, result.getUnit());
    }

    @Test
    public void celsiusConvertToSameUnitShouldReturnSelf() {
        Quantity c = new Quantity(100, TemperatureUnits.CELSIUS);
        Quantity result = c.convertUnits(TemperatureUnits.CELSIUS);
        assertEquals(100, result.getValue(), 0.0);
    }

    @Test
    public void fahrenheitCrossUnitConvertShouldThrow() {
        Quantity f = new Quantity(100, TemperatureUnits.FAHRENHEIT);
        assertThrows(UnsupportedOperationException.class,
                () -> f.convertUnits(TemperatureUnits.CELSIUS));
    }

    @Test
    public void celsiusToFahrenheitConvertShouldThrow() {
        Quantity c = new Quantity(100, TemperatureUnits.CELSIUS);
        assertThrows(UnsupportedOperationException.class,
                () -> c.convertUnits(TemperatureUnits.FAHRENHEIT));
    }

    @Test
    public void fahrenheitShouldBeCompatibleWithCelsius() {
        Quantity f = new Quantity(100, TemperatureUnits.FAHRENHEIT);
        Quantity c = new Quantity(100, TemperatureUnits.CELSIUS);
        assertTrue(f.isCompatibleWith(c));
    }

    @Test
    public void fahrenheitShouldNotBeCompatibleWithLength() {
        Quantity f = new Quantity(100, TemperatureUnits.FAHRENHEIT);
        Quantity m = new Quantity(100, Units.METER);
        assertFalse(f.isCompatibleWith(m));
    }

    @Test
    public void celsiusShouldSupportBaseConversion() {
        assertTrue(TemperatureUnits.CELSIUS.supportsBaseConversion());
    }

    @Test
    public void fahrenheitShouldNotSupportBaseConversion() {
        assertFalse(TemperatureUnits.FAHRENHEIT.supportsBaseConversion());
    }
}
