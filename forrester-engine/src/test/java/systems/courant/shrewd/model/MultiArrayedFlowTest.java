package systems.courant.forrester.model;

import systems.courant.forrester.measure.Quantity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.PEOPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MultiArrayedFlowTest {

    private final Subscript region = new Subscript("Region", "North", "South", "East");
    private final Subscript ageGroup = new Subscript("AgeGroup", "Young", "Adult", "Elder");
    private final SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));

    @Test
    public void shouldExpandToCorrectNumberOfFlows() {
        MultiArrayedFlow flow = MultiArrayedFlow.create("Births", DAY, range,
                coords -> new Quantity(10, PEOPLE));
        assertEquals(9, flow.size());
        assertEquals(9, flow.getFlows().size());
    }

    @Test
    public void shouldNameFlowsWithCommaSeparatedLabels() {
        MultiArrayedFlow flow = MultiArrayedFlow.create("Births", DAY, range,
                coords -> new Quantity(0, PEOPLE));
        assertEquals("Births[North,Young]", flow.getFlow(0).getName());
        assertEquals("Births[North,Adult]", flow.getFlow(1).getName());
        assertEquals("Births[South,Elder]", flow.getFlow(5).getName());
        assertEquals("Births[East,Elder]", flow.getFlow(8).getName());
    }

    @Test
    public void shouldEvaluateCoordinateAwareFormula() {
        MultiArrayedFlow flow = MultiArrayedFlow.create("Births", DAY, range,
                coords -> new Quantity(coords[0] * 10 + coords[1], PEOPLE));

        // North,Young = [0,0] → 0
        assertEquals(0, flow.getFlow(0).flowPerTimeUnit(DAY).getValue(), 0.001);
        // South,Elder = [1,2] → 12
        assertEquals(12, flow.getFlow(5).flowPerTimeUnit(DAY).getValue(), 0.001);
        // East,Adult = [2,1] → 21
        assertEquals(21, flow.getFlow(7).flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void shouldEvaluateFlatIndexFormula() {
        MultiArrayedFlow flow = MultiArrayedFlow.createByIndex("Deaths", DAY, range,
                i -> new Quantity(i * 100, PEOPLE));

        assertEquals(0, flow.getFlow(0).flowPerTimeUnit(DAY).getValue(), 0.001);
        assertEquals(500, flow.getFlow(5).flowPerTimeUnit(DAY).getValue(), 0.001);
        assertEquals(800, flow.getFlow(8).flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void shouldAccessFlowByCoordinates() {
        MultiArrayedFlow flow = MultiArrayedFlow.create("F", DAY, range,
                coords -> new Quantity(0, PEOPLE));
        assertEquals("F[South,Adult]", flow.getFlowAt(1, 1).getName());
        assertEquals("F[East,Elder]", flow.getFlowAt(2, 2).getName());
    }

    @Test
    public void shouldAccessFlowByLabels() {
        MultiArrayedFlow flow = MultiArrayedFlow.create("F", DAY, range,
                coords -> new Quantity(0, PEOPLE));
        assertEquals("F[North,Young]", flow.getFlowAt("North", "Young").getName());
        assertEquals("F[South,Elder]", flow.getFlowAt("South", "Elder").getName());
    }

    @Test
    public void shouldReturnUnmodifiableFlowList() {
        MultiArrayedFlow flow = MultiArrayedFlow.create("F", DAY, range,
                coords -> new Quantity(0, PEOPLE));
        assertThrows(UnsupportedOperationException.class,
                () -> flow.getFlows().add(Flow.create("X", DAY, () -> new Quantity(0, PEOPLE))));
    }

    @Test
    public void shouldReturnMetadata() {
        MultiArrayedFlow flow = MultiArrayedFlow.create("Births", DAY, range,
                coords -> new Quantity(0, PEOPLE));
        assertEquals("Births", flow.getBaseName());
        assertEquals(range, flow.getRange());
    }

    @Test
    public void shouldConnectToMultiArrayedStock() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 100, PEOPLE);
        MultiArrayedFlow births = MultiArrayedFlow.create("Births", DAY, range,
                coords -> new Quantity(pop.getValueAt(coords) * 0.01, PEOPLE));
        pop.addInflow(births);

        for (int i = 0; i < pop.size(); i++) {
            assertEquals(1, pop.getStock(i).getInflows().size());
        }
    }

    @Test
    public void shouldWorkWithStockConvenienceFactory() {
        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 100, PEOPLE);
        MultiArrayedFlow births = MultiArrayedFlow.create("Births", DAY, pop, range,
                coords -> new Quantity(pop.getValueAt(coords) * 0.01, PEOPLE));
        assertEquals(9, births.size());
    }
}
