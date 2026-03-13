package systems.courant.sd.model;

import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.Unit;
import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.MINUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StockTest {

    @Test
    public void shouldStoreInitialValue() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        assertEquals(100, stock.getValue(), 0.0);
        assertEquals(GALLON_US, stock.getUnit());
    }

    @Test
    public void shouldUpdateValue() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        stock.setValue(50);
        assertEquals(50, stock.getValue(), 0.0);
    }

    @Test
    public void shouldTrackInflows() {
        Stock stock = new Stock("Water", 0, GALLON_US);
        Flow inflow = createConstantFlow("Fill", 5, GALLON_US);
        stock.addInflow(inflow);

        assertEquals(1, stock.getInflows().size());
        assertTrue(stock.getInflows().contains(inflow));
        assertEquals(stock, inflow.getSink());
    }

    @Test
    public void shouldTrackOutflows() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        Flow outflow = createConstantFlow("Drain", 5, GALLON_US);
        stock.addOutflow(outflow);

        assertEquals(1, stock.getOutflows().size());
        assertTrue(stock.getOutflows().contains(outflow));
        assertEquals(stock, outflow.getSource());
    }

    @Test
    public void shouldReturnUnmodifiableInflows() {
        Stock stock = new Stock("Water", 0, GALLON_US);
        assertThrows(UnsupportedOperationException.class, () -> {
            stock.getInflows().add(createConstantFlow("X", 1, GALLON_US));
        });
    }

    @Test
    public void shouldReturnUnmodifiableOutflows() {
        Stock stock = new Stock("Water", 0, GALLON_US);
        assertThrows(UnsupportedOperationException.class, () -> {
            stock.getOutflows().add(createConstantFlow("X", 1, GALLON_US));
        });
    }

    @Test
    public void shouldReturnNameFromToString() {
        Stock stock = new Stock("Tank", 10, GALLON_US);
        assertTrue(stock.toString().contains("Tank"));
    }

    @Test
    public void shouldClampNegativeValueToZeroByDefault() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        stock.setValue(-50);
        assertEquals(0, stock.getValue(), 0.0);
    }

    @Test
    public void shouldClampNegativeInitialValueToZero() {
        Stock stock = new Stock("Water", -10, GALLON_US);
        assertEquals(0, stock.getValue(), 0.0);
    }

    @Test
    public void shouldAllowNegativeValueWhenPolicyIsAllow() {
        Stock stock = new Stock("Balance", -500, GALLON_US, NegativeValuePolicy.ALLOW);
        assertEquals(-500, stock.getValue(), 0.0);
        stock.setValue(-100);
        assertEquals(-100, stock.getValue(), 0.0);
    }

    @Test
    public void shouldThrowWhenValueIsNegativeAndPolicyIsThrow() {
        Stock stock = new Stock("Water", 10, GALLON_US, NegativeValuePolicy.THROW);
        assertThrows(IllegalArgumentException.class, () -> stock.setValue(-1));
    }

    @Test
    public void shouldAcceptPolicyInFourArgConstructor() {
        Stock stock = new Stock("Water", 100, GALLON_US, NegativeValuePolicy.ALLOW);
        assertEquals(NegativeValuePolicy.ALLOW, stock.getNegativeValuePolicy());
        assertEquals(100, stock.getValue(), 0.0);
    }

    @Test
    public void shouldKeepPreviousValueWhenSetToNaN() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        stock.setValue(Double.NaN);
        assertEquals(100, stock.getValue(), 0.0);
    }

    @Test
    public void shouldKeepPreviousValueWhenSetToInfinity() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        stock.setValue(Double.POSITIVE_INFINITY);
        assertEquals(100, stock.getValue(), 0.0);
    }

    @Test
    public void shouldKeepPreviousValueWhenSetToNegativeInfinity() {
        Stock stock = new Stock("Water", 100, GALLON_US);
        stock.setValue(Double.NEGATIVE_INFINITY);
        assertEquals(100, stock.getValue(), 0.0);
    }

    @Test
    public void shouldRejectNonFiniteInitialValue() {
        assertThrows(IllegalArgumentException.class,
                () -> new Stock("Water", Double.NaN, GALLON_US));
        assertThrows(IllegalArgumentException.class,
                () -> new Stock("Water", Double.POSITIVE_INFINITY, GALLON_US));
    }

    private static Flow createConstantFlow(String name, double value, Unit unit) {
        return Flow.create(name, MINUTE, () -> new Quantity(value, unit));
    }
}
