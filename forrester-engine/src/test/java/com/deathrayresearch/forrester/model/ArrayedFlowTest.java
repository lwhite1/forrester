package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.PEOPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArrayedFlowTest {

    private final Subscript region = new Subscript("Region", "North", "South", "East");

    @Test
    public void shouldExpandToNFlows() {
        ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
                i -> new Quantity(10 * (i + 1), PEOPLE));
        assertEquals(3, births.size());
    }

    @Test
    public void shouldNameFlowsWithBracketConvention() {
        ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
                i -> new Quantity(0, PEOPLE));
        List<Flow> flows = births.getFlows();
        assertEquals("Births[North]", flows.get(0).getName());
        assertEquals("Births[South]", flows.get(1).getName());
        assertEquals("Births[East]", flows.get(2).getName());
    }

    @Test
    public void shouldEvaluatePerIndexFormula() {
        ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
                i -> new Quantity(10 * (i + 1), PEOPLE));

        assertEquals(10, births.getFlow(0).flowPerTimeUnit(DAY).getValue(), 0.001);
        assertEquals(20, births.getFlow(1).flowPerTimeUnit(DAY).getValue(), 0.001);
        assertEquals(30, births.getFlow(2).flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void shouldAccessFlowByLabel() {
        ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
                i -> new Quantity(10 * (i + 1), PEOPLE));
        Flow south = births.getFlow("South");
        assertEquals("Births[South]", south.getName());
        assertEquals(20, south.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void shouldReturnUnmodifiableFlowList() {
        ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
                i -> new Quantity(0, PEOPLE));
        assertThrows(UnsupportedOperationException.class,
                () -> births.getFlows().add(Flow.create("X", DAY, () -> new Quantity(0, PEOPLE))));
    }

    @Test
    public void shouldConnectToArrayedStock() {
        ArrayedStock pop = new ArrayedStock("Population", region, 100, PEOPLE);
        ArrayedFlow births = ArrayedFlow.create("Births", DAY, pop, region,
                i -> new Quantity(pop.getValue(i) * 0.01, PEOPLE));
        pop.addInflow(births);

        for (int i = 0; i < region.size(); i++) {
            Stock stock = pop.getStock(i);
            assertEquals(1, stock.getInflows().size());
            Flow flow = stock.getInflows().iterator().next();
            assertEquals(stock, flow.getSink());
        }
    }

    @Test
    public void shouldReturnMetadata() {
        ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
                i -> new Quantity(0, PEOPLE));
        assertEquals("Births", births.getBaseName());
        assertEquals(region, births.getSubscript());
    }
}
