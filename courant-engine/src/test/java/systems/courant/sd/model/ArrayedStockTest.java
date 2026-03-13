package systems.courant.sd.model;

import systems.courant.sd.measure.Quantity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.PEOPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArrayedStockTest {

    private final Subscript region = new Subscript("Region", "North", "South", "East");

    @Test
    public void shouldExpandToNStocksWithUniformInitialValue() {
        ArrayedStock pop = new ArrayedStock("Population", region, 1000, PEOPLE);
        assertEquals(3, pop.size());
        assertEquals(1000, pop.getValue(0), 0.0);
        assertEquals(1000, pop.getValue(1), 0.0);
        assertEquals(1000, pop.getValue(2), 0.0);
    }

    @Test
    public void shouldExpandToNStocksWithPerElementInitialValues() {
        ArrayedStock pop = new ArrayedStock("Population", region, new double[]{100, 200, 300}, PEOPLE);
        assertEquals(100, pop.getValue(0), 0.0);
        assertEquals(200, pop.getValue(1), 0.0);
        assertEquals(300, pop.getValue(2), 0.0);
    }

    @Test
    public void shouldThrowWhenInitialValuesLengthMismatches() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArrayedStock("Pop", region, new double[]{100, 200}, PEOPLE));
    }

    @Test
    public void shouldNameStocksWithBracketConvention() {
        ArrayedStock pop = new ArrayedStock("Population", region, 0, PEOPLE);
        List<Stock> stocks = pop.getStocks();
        assertEquals("Population[North]", stocks.get(0).getName());
        assertEquals("Population[South]", stocks.get(1).getName());
        assertEquals("Population[East]", stocks.get(2).getName());
    }

    @Test
    public void shouldAccessValueByLabel() {
        ArrayedStock pop = new ArrayedStock("Population", region, new double[]{100, 200, 300}, PEOPLE);
        assertEquals(100, pop.getValue("North"), 0.0);
        assertEquals(200, pop.getValue("South"), 0.0);
        assertEquals(300, pop.getValue("East"), 0.0);
    }

    @Test
    public void shouldAccessStockByLabel() {
        ArrayedStock pop = new ArrayedStock("Population", region, 500, PEOPLE);
        Stock south = pop.getStock("South");
        assertEquals("Population[South]", south.getName());
        assertEquals(500, south.getValue(), 0.0);
    }

    @Test
    public void shouldComputeSum() {
        ArrayedStock pop = new ArrayedStock("Population", region, new double[]{100, 200, 300}, PEOPLE);
        assertEquals(600, pop.sum(), 0.0);
    }

    @Test
    public void shouldReturnUnmodifiableStockList() {
        ArrayedStock pop = new ArrayedStock("Population", region, 0, PEOPLE);
        assertThrows(UnsupportedOperationException.class,
                () -> pop.getStocks().add(new Stock("X", 0, PEOPLE)));
    }

    @Test
    public void shouldWireArrayedFlowAsInflow() {
        ArrayedStock pop = new ArrayedStock("Population", region, 100, PEOPLE);
        ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
                i -> new Quantity(10, PEOPLE));
        pop.addInflow(births);

        for (int i = 0; i < region.size(); i++) {
            assertEquals(1, pop.getStock(i).getInflows().size());
        }
    }

    @Test
    public void shouldWireArrayedFlowAsOutflow() {
        ArrayedStock pop = new ArrayedStock("Population", region, 100, PEOPLE);
        ArrayedFlow deaths = ArrayedFlow.create("Deaths", DAY, region,
                i -> new Quantity(5, PEOPLE));
        pop.addOutflow(deaths);

        for (int i = 0; i < region.size(); i++) {
            assertEquals(1, pop.getStock(i).getOutflows().size());
        }
    }

    @Test
    public void shouldRejectScalarFlowAsInflow() {
        ArrayedStock pop = new ArrayedStock("Population", region, 100, PEOPLE);
        Flow immigration = Flow.create("Immigration", DAY, () -> new Quantity(10, PEOPLE));
        assertThrows(UnsupportedOperationException.class, () -> pop.addInflow(immigration));
    }

    @Test
    public void shouldRejectScalarFlowAsOutflow() {
        ArrayedStock pop = new ArrayedStock("Population", region, 100, PEOPLE);
        Flow emigration = Flow.create("Emigration", DAY, () -> new Quantity(5, PEOPLE));
        assertThrows(UnsupportedOperationException.class, () -> pop.addOutflow(emigration));
    }

    @Test
    public void shouldReturnMetadata() {
        ArrayedStock pop = new ArrayedStock("Population", region, 0, PEOPLE);
        assertEquals("Population", pop.getBaseName());
        assertEquals(region, pop.getSubscript());
        assertEquals(PEOPLE, pop.getUnit());
    }
}
