package systems.courant.sd.measure;

import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;
import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.KILOMETER;
import static systems.courant.sd.measure.Units.METER;
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
}
