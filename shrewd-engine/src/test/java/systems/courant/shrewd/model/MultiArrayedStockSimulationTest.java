package systems.courant.shrewd.model;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.Quantity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.PEOPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying that multi-arrayed stocks work correctly
 * when run through the simulation engine.
 */
public class MultiArrayedStockSimulationTest {

    @Test
    public void shouldIntegrateMultiArrayedStockWithConstantInflow() {
        Subscript region = new Subscript("Region", "North", "South");
        Subscript age = new Subscript("AgeGroup", "Young", "Elder");
        SubscriptRange range = new SubscriptRange(List.of(region, age));

        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 100, PEOPLE);

        MultiArrayedFlow inflow = MultiArrayedFlow.create("Growth", DAY, range,
                coords -> new Quantity(10, PEOPLE));
        pop.addInflow(inflow);

        Model model = new Model("Multi-Dim Integration Test");
        model.addMultiArrayedStock(pop);

        // 3 steps (0..2), each adding 10 → 100 + 30 = 130 per stock
        Simulation sim = new Simulation(model, DAY, DAY, 2);
        sim.execute();

        for (int i = 0; i < pop.size(); i++) {
            assertEquals(130, pop.getValue(i), 0.01);
        }
    }

    @Test
    public void shouldIntegrateMultiArrayedStockWithCoordinateAwareFlow() {
        Subscript region = new Subscript("Region", "North", "South");
        Subscript age = new Subscript("AgeGroup", "Young", "Elder");
        SubscriptRange range = new SubscriptRange(List.of(region, age));

        MultiArrayedStock pop = new MultiArrayedStock("Population", range,
                new double[]{100, 200, 300, 400}, PEOPLE);

        // Outflow drains 10% of each stock per day
        MultiArrayedFlow drain = MultiArrayedFlow.create("Drain", DAY, range,
                coords -> new Quantity(pop.getValueAt(coords) * 0.1, PEOPLE));
        pop.addOutflow(drain);

        Model model = new Model("Coordinate Flow Test");
        model.addMultiArrayedStock(pop);

        Simulation sim = new Simulation(model, DAY, DAY, 1);
        sim.execute();

        // After 2 steps (0,1) of 10% drain:
        // step 0: 100→90, 200→180, 300→270, 400→360
        // step 1: 90→81, 180→162, 270→243, 360→324
        assertEquals(81, pop.getValueAt("North", "Young"), 0.5);
        assertEquals(162, pop.getValueAt("North", "Elder"), 0.5);
        assertEquals(243, pop.getValueAt("South", "Young"), 0.5);
        assertEquals(324, pop.getValueAt("South", "Elder"), 0.5);
    }

    @Test
    public void shouldIntegrateTransferBetweenMultiArrayedStockElements() {
        Subscript region = new Subscript("Region", "North", "South");
        Subscript age = new Subscript("AgeGroup", "Young", "Elder");
        SubscriptRange range = new SubscriptRange(List.of(region, age));

        MultiArrayedStock pop = new MultiArrayedStock("Population", range,
                new double[]{1000, 0, 1000, 0}, PEOPLE);

        // Aging: Young→Elder within each region, 5 people/day
        for (int r = 0; r < region.size(); r++) {
            Stock young = pop.getStockAt(r, 0);
            Stock elder = pop.getStockAt(r, 1);
            Flow aging = Flow.create("Aging[" + region.getLabel(r) + "]", DAY,
                    () -> new Quantity(5, PEOPLE));
            young.addOutflow(aging);
            elder.addInflow(aging);
        }

        Model model = new Model("Transfer Test");
        model.addMultiArrayedStock(pop);

        Simulation sim = new Simulation(model, DAY, DAY, 9);
        sim.execute();

        // 10 steps, each transferring 5: Young loses 50, Elder gains 50
        assertEquals(950, pop.getValueAt("North", "Young"), 0.01);
        assertEquals(50, pop.getValueAt("North", "Elder"), 0.01);
        assertEquals(950, pop.getValueAt("South", "Young"), 0.01);
        assertEquals(50, pop.getValueAt("South", "Elder"), 0.01);

        // Total population conserved
        assertEquals(2000, pop.sum(), 0.01);
    }

    @Test
    public void shouldReportMultiArrayedStockNamesInModel() {
        Subscript region = new Subscript("Region", "North", "South");
        Subscript age = new Subscript("AgeGroup", "Young", "Elder");
        SubscriptRange range = new SubscriptRange(List.of(region, age));

        MultiArrayedStock pop = new MultiArrayedStock("Population", range, 100, PEOPLE);
        Model model = new Model("Names Test");
        model.addMultiArrayedStock(pop);

        List<String> names = model.getStockNames();
        assertEquals(4, names.size());
        assertTrue(names.contains("Population[North,Young]"));
        assertTrue(names.contains("Population[North,Elder]"));
        assertTrue(names.contains("Population[South,Young]"));
        assertTrue(names.contains("Population[South,Elder]"));
    }
}
