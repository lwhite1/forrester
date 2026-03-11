package systems.courant.shrewd.measure;

import systems.courant.shrewd.measure.units.item.ItemUnit;
import systems.courant.shrewd.measure.units.item.ItemUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ItemUnitTest {

    @Test
    public void shouldHaveItemDimension() {
        ItemUnit widget = new ItemUnit("Widget");
        assertEquals(Dimension.ITEM, widget.getDimension());
    }

    @Test
    public void shouldHaveRatioOfOne() {
        ItemUnit widget = new ItemUnit("Widget");
        assertEquals(1.0, widget.ratioToBaseUnit(), 0.0);
    }

    @Test
    public void equalsShouldMatchByName() {
        ItemUnit a = new ItemUnit("Widget");
        ItemUnit b = new ItemUnit("Widget");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalsShouldDistinguishDifferentNames() {
        ItemUnit widget = new ItemUnit("Widget");
        ItemUnit gadget = new ItemUnit("Gadget");
        assertNotEquals(widget, gadget);
    }

    @Test
    public void toStringShouldReturnName() {
        ItemUnit widget = new ItemUnit("Widget");
        assertEquals("Widget", widget.toString());
    }

    @Test
    public void shouldNotEqualItemUnitsEnum() {
        // ItemUnit and ItemUnits are different classes; they don't interoperate for equality
        ItemUnit thing = new ItemUnit("Thing");
        assertNotEquals(thing, ItemUnits.THING);
    }

    @Test
    public void shouldWorkAsQuantityUnit() {
        ItemUnit widget = new ItemUnit("Widget");
        Quantity q = new Quantity(5, widget);
        assertEquals(5, q.getValue(), 0.0);
        assertEquals(widget, q.getUnit());
    }
}
