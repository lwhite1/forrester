package systems.courant.forrester.measure;

import systems.courant.forrester.measure.units.length.LengthUnits;
import org.junit.jupiter.api.Test;

import static systems.courant.forrester.measure.Units.GALLON_US;
import static systems.courant.forrester.measure.Units.LITER;
import static systems.courant.forrester.measure.Units.METER;
import static systems.courant.forrester.measure.Units.MILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuantityTest {

    @Test
    public void shouldStoreValueAndUnit() {
        Quantity q = new Quantity(10, METER);
        assertEquals(10, q.getValue(), 0.0);
        assertEquals(METER, q.getUnit());
    }

    @Test
    public void shouldMultiply() {
        Quantity q = new Quantity(10, METER);
        Quantity result = q.multiply(3);
        assertEquals(30, result.getValue(), 0.0);
        assertEquals(METER, result.getUnit());
    }

    @Test
    public void shouldDivide() {
        Quantity q = new Quantity(10, METER);
        Quantity result = q.divide(2);
        assertEquals(5, result.getValue(), 0.0);
    }

    @Test
    public void shouldAddCompatibleQuantities() {
        Quantity a = new Quantity(10, METER);
        Quantity b = new Quantity(20, METER);
        Quantity result = a.add(b);
        assertEquals(30, result.getValue(), 0.0);
    }

    @Test
    public void shouldSubtractCompatibleQuantities() {
        Quantity a = new Quantity(30, METER);
        Quantity b = new Quantity(10, METER);
        Quantity result = a.subtract(b);
        assertEquals(20, result.getValue(), 0.0);
    }

    @Test
    public void shouldRejectAddingIncompatibleQuantities() {
        Quantity meters = new Quantity(10, METER);
        Quantity gallons = new Quantity(5, GALLON_US);
        assertThrows(IllegalArgumentException.class, () -> meters.add(gallons));
    }

    @Test
    public void shouldRejectSubtractingIncompatibleQuantities() {
        Quantity meters = new Quantity(10, METER);
        Quantity gallons = new Quantity(5, GALLON_US);
        assertThrows(IllegalArgumentException.class, () -> meters.subtract(gallons));
    }

    @Test
    public void shouldAddDifferentUnitsInSameDimension() {
        Quantity miles = new Quantity(1, MILE);
        Quantity meters = new Quantity(1609.34, METER);
        Quantity result = miles.add(meters);
        assertEquals(2, result.getValue(), 0.01);
    }

    @Test
    public void shouldCompareLessThan() {
        Quantity a = new Quantity(5, METER);
        Quantity b = new Quantity(10, METER);
        assertTrue(a.isLessThan(b));
        assertFalse(b.isLessThan(a));
    }

    @Test
    public void shouldCompareGreaterThan() {
        Quantity a = new Quantity(10, METER);
        Quantity b = new Quantity(5, METER);
        assertTrue(a.isGreaterThan(b));
        assertFalse(b.isGreaterThan(a));
    }

    @Test
    public void shouldCompareLessThanOrEqual() {
        Quantity a = new Quantity(5, METER);
        Quantity b = new Quantity(5, METER);
        assertTrue(a.isLessThanOrEqualTo(b));
        assertTrue(b.isLessThanOrEqualTo(a));
    }

    @Test
    public void shouldCompareGreaterThanOrEqual() {
        Quantity a = new Quantity(10, METER);
        Quantity b = new Quantity(10, METER);
        assertTrue(a.isGreaterThanOrEqualTo(b));
    }

    @Test
    public void shouldBeEqualForSameValueAndUnit() {
        Quantity a = new Quantity(10, METER);
        Quantity b = new Quantity(10, METER);
        assertTrue(a.isEqual(b));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void shouldNotBeEqualForDifferentValues() {
        Quantity a = new Quantity(10, METER);
        Quantity b = new Quantity(20, METER);
        assertFalse(a.isEqual(b));
        assertNotEquals(a, b);
    }

    @Test
    public void shouldConvertToBaseUnits() {
        Quantity miles = new Quantity(1, MILE);
        Quantity base = miles.inBaseUnits();
        assertEquals(1609.34, base.getValue(), 0.01);
        assertEquals(LengthUnits.METER, base.getUnit());
    }

    @Test
    public void shouldDetectCompatibility() {
        Quantity a = new Quantity(1, METER);
        Quantity b = new Quantity(1, MILE);
        Quantity c = new Quantity(1, GALLON_US);
        assertTrue(a.isCompatibleWith(b));
        assertFalse(a.isCompatibleWith(c));
    }

    @Test
    public void shouldIncludeUnitInToString() {
        Quantity q = new Quantity(5, METER);
        assertTrue(q.toString().contains("Meter"));
    }
}
