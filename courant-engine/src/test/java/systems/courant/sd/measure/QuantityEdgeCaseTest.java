package systems.courant.sd.measure;

import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;
import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.KILOMETER;
import static systems.courant.sd.measure.Units.METER;
import static systems.courant.sd.measure.Units.CELSIUS;
import static systems.courant.sd.measure.Units.FAHRENHEIT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuantityEdgeCaseTest {

    @Test
    public void shouldRejectNullUnit() {
        assertThrows(IllegalArgumentException.class, () -> new Quantity(10, null));
    }

    @Test
    public void shouldThrowOnDivideByZero() {
        Quantity q = new Quantity(10, METER);
        assertThrows(ArithmeticException.class, () -> q.divide(0));
    }

    @Test
    public void shouldRejectCrossDimensionComparisons() {
        Quantity meters = new Quantity(10, METER);
        Quantity gallons = new Quantity(10, GALLON_US);
        assertThrows(IllegalArgumentException.class, () -> meters.isLessThan(gallons));
        assertThrows(IllegalArgumentException.class, () -> meters.isGreaterThan(gallons));
        assertThrows(IllegalArgumentException.class, () -> meters.isLessThanOrEqualTo(gallons));
        assertThrows(IllegalArgumentException.class, () -> meters.isGreaterThanOrEqualTo(gallons));
        assertThrows(IllegalArgumentException.class, () -> meters.isEqual(gallons));
    }

    @Test
    public void equalsShouldWorkAcrossUnitsInSameDimension() {
        // 1 kilometer = 1000 meters
        Quantity km = new Quantity(1, KILOMETER);
        Quantity m = new Quantity(1000, METER);
        assertEquals(km, m);
        assertEquals(km.hashCode(), m.hashCode());
    }

    @Test
    public void equalsShouldReturnFalseForDifferentDimensions() {
        Quantity meters = new Quantity(10, METER);
        Quantity gallons = new Quantity(10, GALLON_US);
        assertNotEquals(meters, gallons);
    }

    @Test
    public void equalsShouldReturnFalseForNull() {
        Quantity q = new Quantity(10, METER);
        assertNotEquals(null, q);
    }

    @Test
    public void convertUnitsShouldShortCircuitForSameUnit() {
        Quantity q = new Quantity(42, METER);
        Quantity result = q.convertUnits(METER);
        assertTrue(q == result, "Should return same instance for identity conversion");
    }

    @Test
    public void dimensionlessConvertToSameUnitShouldWork() {
        Quantity q = new Quantity(0.5, DimensionlessUnits.DIMENSIONLESS);
        Quantity result = q.convertUnits(DimensionlessUnits.DIMENSIONLESS);
        assertEquals(0.5, result.getValue(), 0.0);
    }

    @Test
    public void shouldConvertBetweenDifferentUnitsInSameDimension() {
        Quantity km = new Quantity(1, KILOMETER);
        Quantity result = km.convertUnits(METER);
        assertEquals(1000, result.getValue(), 0.01);
    }

    @Test
    public void equalsShouldNotCrashForCrossUnitTemperature() {
        Quantity celsius = new Quantity(100, CELSIUS);
        Quantity fahrenheit = new Quantity(212, FAHRENHEIT);
        // Should not throw — equals returns false for unconvertible cross-unit temperatures
        assertDoesNotThrow(() -> celsius.equals(fahrenheit));
        assertNotEquals(celsius, fahrenheit);
    }

    @Test
    public void equalsShouldWorkForSameTemperatureUnit() {
        Quantity c1 = new Quantity(100, CELSIUS);
        Quantity c2 = new Quantity(100, CELSIUS);
        assertEquals(c1, c2);
    }

    @Test
    public void comparisonShouldThrowUnsupportedForCrossUnitTemperature() {
        Quantity celsius = new Quantity(100, CELSIUS);
        Quantity fahrenheit = new Quantity(212, FAHRENHEIT);
        assertThrows(UnsupportedOperationException.class, () -> celsius.isLessThan(fahrenheit));
        assertThrows(UnsupportedOperationException.class, () -> celsius.isGreaterThan(fahrenheit));
        assertThrows(UnsupportedOperationException.class, () -> celsius.isEqual(fahrenheit));
    }

    @Test
    public void addSubtractShouldThrowUnsupportedForCrossUnitTemperature() {
        Quantity celsius = new Quantity(100, CELSIUS);
        Quantity fahrenheit = new Quantity(212, FAHRENHEIT);
        assertThrows(UnsupportedOperationException.class, () -> celsius.add(fahrenheit));
        assertThrows(UnsupportedOperationException.class, () -> celsius.subtract(fahrenheit));
    }

    @Test
    public void hashCodeShouldNotCrashForFahrenheit() {
        Quantity fahrenheit = new Quantity(212, FAHRENHEIT);
        assertDoesNotThrow(fahrenheit::hashCode);
    }

    @Test
    public void sameUnitFahrenheitAddShouldWork() {
        Quantity a = new Quantity(100, FAHRENHEIT);
        Quantity b = new Quantity(50, FAHRENHEIT);
        Quantity result = a.add(b);
        assertEquals(150, result.getValue(), 0.0);
        assertEquals(FAHRENHEIT, result.getUnit());
    }

    @Test
    public void sameUnitFahrenheitSubtractShouldWork() {
        Quantity a = new Quantity(100, FAHRENHEIT);
        Quantity b = new Quantity(30, FAHRENHEIT);
        Quantity result = a.subtract(b);
        assertEquals(70, result.getValue(), 0.0);
        assertEquals(FAHRENHEIT, result.getUnit());
    }
}
