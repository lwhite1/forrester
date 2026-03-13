package systems.courant.sd.model;

import systems.courant.sd.measure.Quantity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.PEOPLE;
import static systems.courant.sd.measure.Units.US_DOLLAR;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MultiArrayedStockTest {

    private final Subscript region = new Subscript("Region", "North", "South", "East");
    private final Subscript ageGroup = new Subscript("AgeGroup", "Young", "Adult", "Elder");
    private final SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));

    @Test
    public void shouldExpandToCorrectNumberOfStocks() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 1000, PEOPLE);
        assertEquals(9, pop.size());
        assertEquals(9, pop.getStocks().size());
    }

    @Test
    public void shouldNameStocksWithCommaSeparatedLabels() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 0, PEOPLE);
        List<Stock> stocks = pop.getStocks();
        assertEquals("Population[North,Young]", stocks.get(0).getName());
        assertEquals("Population[North,Adult]", stocks.get(1).getName());
        assertEquals("Population[North,Elder]", stocks.get(2).getName());
        assertEquals("Population[South,Young]", stocks.get(3).getName());
        assertEquals("Population[East,Elder]", stocks.get(8).getName());
    }

    @Test
    public void shouldSetUniformInitialValue() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 500, PEOPLE);
        for (int i = 0; i < pop.size(); i++) {
            assertEquals(500, pop.getValue(i), 0.0);
        }
    }

    @Test
    public void shouldSetPerElementInitialValues() {
        double[] values = {100, 200, 300, 400, 500, 600, 700, 800, 900};
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, values, PEOPLE);
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], pop.getValue(i), 0.0);
        }
    }

    @Test
    public void shouldThrowWhenInitialValuesLengthMismatches() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiArrayedStock("Pop", range, new double[]{1, 2, 3}, PEOPLE));
    }

    @Test
    public void shouldAccessValueByCoordinates() {
        double[] values = {100, 200, 300, 400, 500, 600, 700, 800, 900};
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, values, PEOPLE);
        assertEquals(100, pop.getValueAt(0, 0), 0.0);  // North, Young
        assertEquals(500, pop.getValueAt(1, 1), 0.0);  // South, Adult
        assertEquals(900, pop.getValueAt(2, 2), 0.0);  // East, Elder
    }

    @Test
    public void shouldAccessValueByLabels() {
        double[] values = {100, 200, 300, 400, 500, 600, 700, 800, 900};
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, values, PEOPLE);
        assertEquals(100, pop.getValueAt("North", "Young"), 0.0);
        assertEquals(500, pop.getValueAt("South", "Adult"), 0.0);
        assertEquals(900, pop.getValueAt("East", "Elder"), 0.0);
    }

    @Test
    public void shouldAccessStockByCoordinates() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 0, PEOPLE);
        Stock stock = pop.getStockAt(1, 2);
        assertEquals("Population[South,Elder]", stock.getName());
    }

    @Test
    public void shouldAccessStockByLabels() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 0, PEOPLE);
        Stock stock = pop.getStockAt("East", "Young");
        assertEquals("Population[East,Young]", stock.getName());
    }

    @Test
    public void shouldComputeSum() {
        double[] values = {100, 200, 300, 400, 500, 600, 700, 800, 900};
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, values, PEOPLE);
        assertEquals(4500, pop.sum(), 0.0);
    }

    @Test
    public void shouldSumOverAgeGroupDimension() {
        // Values: North=[100,200,300], South=[400,500,600], East=[700,800,900]
        double[] values = {100, 200, 300, 400, 500, 600, 700, 800, 900};
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, values, PEOPLE);
        double[] perRegion = pop.sumOver(1);  // collapse AgeGroup
        assertEquals(3, perRegion.length);
        assertEquals(600, perRegion[0], 0.0);   // North: 100+200+300
        assertEquals(1500, perRegion[1], 0.0);  // South: 400+500+600
        assertEquals(2400, perRegion[2], 0.0);  // East:  700+800+900
    }

    @Test
    public void shouldSumOverRegionDimension() {
        // Values: North=[100,200,300], South=[400,500,600], East=[700,800,900]
        double[] values = {100, 200, 300, 400, 500, 600, 700, 800, 900};
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, values, PEOPLE);
        double[] perAge = pop.sumOver(0);  // collapse Region
        assertEquals(3, perAge.length);
        assertEquals(1200, perAge[0], 0.0);  // Young: 100+400+700
        assertEquals(1500, perAge[1], 0.0);  // Adult: 200+500+800
        assertEquals(1800, perAge[2], 0.0);  // Elder: 300+600+900
    }

    @Test
    public void shouldSliceByLabel() {
        double[] values = {100, 200, 300, 400, 500, 600, 700, 800, 900};
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, values, PEOPLE);
        Stock[] northSlice = pop.slice(0, "North");
        assertEquals(3, northSlice.length);
        assertEquals("Population[North,Young]", northSlice[0].getName());
        assertEquals("Population[North,Adult]", northSlice[1].getName());
        assertEquals("Population[North,Elder]", northSlice[2].getName());
        assertEquals(100, northSlice[0].getValue(), 0.0);
        assertEquals(200, northSlice[1].getValue(), 0.0);
        assertEquals(300, northSlice[2].getValue(), 0.0);
    }

    @Test
    public void shouldSliceByIndex() {
        double[] values = {100, 200, 300, 400, 500, 600, 700, 800, 900};
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, values, PEOPLE);
        Stock[] youngSlice = pop.slice(1, 0);  // Fix AgeGroup to index 0 (Young)
        assertEquals(3, youngSlice.length);
        assertEquals("Population[North,Young]", youngSlice[0].getName());
        assertEquals("Population[South,Young]", youngSlice[1].getName());
        assertEquals("Population[East,Young]", youngSlice[2].getName());
    }

    @Test
    public void shouldWireMultiArrayedFlowAsInflow() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 100, PEOPLE);
        MultiArrayedFlow births = MultiArrayedFlow.create("Births", DAY, range,
                coords -> new Quantity(10, PEOPLE));
        pop.addInflow(births);

        for (int i = 0; i < pop.size(); i++) {
            assertEquals(1, pop.getStock(i).getInflows().size());
        }
    }

    @Test
    public void shouldWireMultiArrayedFlowAsOutflow() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 100, PEOPLE);
        MultiArrayedFlow deaths = MultiArrayedFlow.create("Deaths", DAY, range,
                coords -> new Quantity(5, PEOPLE));
        pop.addOutflow(deaths);

        for (int i = 0; i < pop.size(); i++) {
            assertEquals(1, pop.getStock(i).getOutflows().size());
        }
    }

    @Test
    public void shouldRejectScalarFlowAsInflow() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 100, PEOPLE);
        Flow immigration = Flow.create("Immigration", DAY, () -> new Quantity(10, PEOPLE));
        assertThrows(UnsupportedOperationException.class, () -> pop.addInflow(immigration));
    }

    @Test
    public void shouldRejectScalarFlowAsOutflow() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 100, PEOPLE);
        Flow emigration = Flow.create("Emigration", DAY, () -> new Quantity(5, PEOPLE));
        assertThrows(UnsupportedOperationException.class, () -> pop.addOutflow(emigration));
    }

    @Test
    public void shouldReturnUnmodifiableStockList() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 0, PEOPLE);
        assertThrows(UnsupportedOperationException.class,
                () -> pop.getStocks().add(new Stock("X", 0, PEOPLE)));
    }

    @Test
    public void shouldReturnMetadata() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 0, PEOPLE);
        assertEquals("Population", pop.getBaseName());
        assertEquals(range, pop.getRange());
        assertEquals(PEOPLE, pop.getUnit());
    }

    @Test
    public void shouldConstructFromSubscriptList() {
        MultiArrayedStock pop = new MultiArrayedStock("Population",
                List.of(region, ageGroup), 1000, PEOPLE);
        assertEquals(9, pop.size());
        assertEquals("Population[North,Young]", pop.getStock(0).getName());
    }

    @Test
    public void shouldWorkWithThreeDimensions() {
        Subscript dim1 = new Subscript("A", "a0", "a1");
        Subscript dim2 = new Subscript("B", "b0", "b1");
        Subscript dim3 = new Subscript("C", "c0", "c1");
        SubscriptRange range3d = new SubscriptRange(List.of(dim1, dim2, dim3));

        MultiArrayedStock stock = new MultiArrayedStock("X", range3d, 10, PEOPLE);
        assertEquals(8, stock.size());
        assertEquals("X[a0,b0,c0]", stock.getStock(0).getName());
        assertEquals("X[a1,b1,c1]", stock.getStock(7).getName());
        assertEquals(10, stock.getValueAt(1, 0, 1), 0.0);
        assertEquals(80, stock.sum(), 0.0);
    }

    @Test
    public void shouldSumOverMiddleDimensionIn3D() {
        Subscript dim1 = new Subscript("A", "a0", "a1");
        Subscript dim2 = new Subscript("B", "b0", "b1");
        Subscript dim3 = new Subscript("C", "c0", "c1");
        SubscriptRange range3d = new SubscriptRange(List.of(dim1, dim2, dim3));

        // Values 0..7 in row-major order
        double[] values = {0, 1, 2, 3, 4, 5, 6, 7};
        MultiArrayedStock stock = new MultiArrayedStock("X", range3d, values, PEOPLE);

        // Collapse middle dim (B, size 2): result has 2*2=4 elements for A×C
        // [a0,c0]: val[a0,b0,c0]+val[a0,b1,c0] = 0+2 = 2
        // [a0,c1]: val[a0,b0,c1]+val[a0,b1,c1] = 1+3 = 4
        // [a1,c0]: val[a1,b0,c0]+val[a1,b1,c0] = 4+6 = 10
        // [a1,c1]: val[a1,b0,c1]+val[a1,b1,c1] = 5+7 = 12
        double[] result = stock.sumOver(1);
        assertEquals(4, result.length);
        assertEquals(2, result[0], 0.0);
        assertEquals(4, result[1], 0.0);
        assertEquals(10, result[2], 0.0);
        assertEquals(12, result[3], 0.0);
    }

    @Test
    public void shouldSliceMiddleDimensionIn3D() {
        Subscript dim1 = new Subscript("A", "a0", "a1");
        Subscript dim2 = new Subscript("B", "b0", "b1");
        Subscript dim3 = new Subscript("C", "c0", "c1");
        SubscriptRange range3d = new SubscriptRange(List.of(dim1, dim2, dim3));

        double[] values = {0, 1, 2, 3, 4, 5, 6, 7};
        MultiArrayedStock stock = new MultiArrayedStock("X", range3d, values, PEOPLE);

        // Fix B to "b1" (index 1) → elements where coords[1]==1
        // [a0,b1,c0]=2, [a0,b1,c1]=3, [a1,b1,c0]=6, [a1,b1,c1]=7
        Stock[] slice = stock.slice(1, "b1");
        assertEquals(4, slice.length);
        assertEquals("X[a0,b1,c0]", slice[0].getName());
        assertEquals("X[a0,b1,c1]", slice[1].getName());
        assertEquals("X[a1,b1,c0]", slice[2].getName());
        assertEquals("X[a1,b1,c1]", slice[3].getName());
        assertEquals(2, slice[0].getValue(), 0.0);
        assertEquals(7, slice[3].getValue(), 0.0);
    }

    @Test
    public void shouldThrowWhenWiringMismatchedSizeFlow() {
        Subscript s1 = new Subscript("S1", "x", "y");
        Subscript s2 = new Subscript("S2", "a", "b");
        SubscriptRange range2x2 = new SubscriptRange(List.of(s1, s2));

        MultiArrayedStock stock = new MultiArrayedStock("Pop", range, 100, PEOPLE);  // 3x3 = 9
        MultiArrayedFlow flow = MultiArrayedFlow.create("F", DAY, range2x2,
                coords -> new Quantity(1, PEOPLE));  // 2x2 = 4

        assertThrows(IllegalArgumentException.class, () -> stock.addInflow(flow));
        assertThrows(IllegalArgumentException.class, () -> stock.addOutflow(flow));
    }

    @Test
    public void shouldSupportNegativeValuePolicyAllow() {
        MultiArrayedStock balance = new MultiArrayedStock("Balance", range,
                -100, US_DOLLAR, NegativeValuePolicy.ALLOW);
        assertEquals(-100, balance.getValue(0), 0.0);
        assertEquals(-100, balance.getValueAt("North", "Young"), 0.0);
    }

    @Test
    public void shouldSupportNegativeValuePolicyThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiArrayedStock("X", range, -1, PEOPLE, NegativeValuePolicy.THROW));
    }

    @Test
    public void shouldDefaultToClampToZero() {
        MultiArrayedStock stock = new MultiArrayedStock("X", range, -50, PEOPLE);
        assertEquals(0, stock.getValue(0), 0.0);
    }
}
