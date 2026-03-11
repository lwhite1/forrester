package systems.courant.forrester.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SubscriptTest {

    @Test
    public void shouldCreateSubscriptWithLabels() {
        Subscript region = new Subscript("Region", "North", "South", "East");
        assertEquals("Region", region.getName());
        assertEquals(3, region.size());
        assertEquals(List.of("North", "South", "East"), region.getLabels());
    }

    @Test
    public void shouldReturnLabelByIndex() {
        Subscript region = new Subscript("Region", "North", "South", "East");
        assertEquals("North", region.getLabel(0));
        assertEquals("South", region.getLabel(1));
        assertEquals("East", region.getLabel(2));
    }

    @Test
    public void shouldReturnIndexByLabel() {
        Subscript region = new Subscript("Region", "North", "South", "East");
        assertEquals(0, region.indexOf("North"));
        assertEquals(1, region.indexOf("South"));
        assertEquals(2, region.indexOf("East"));
    }

    @Test
    public void shouldThrowForUnknownLabel() {
        Subscript region = new Subscript("Region", "North", "South");
        assertThrows(IllegalArgumentException.class, () -> region.indexOf("West"));
    }

    @Test
    public void shouldThrowForBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Subscript("", "A"));
        assertThrows(IllegalArgumentException.class, () -> new Subscript("  ", "A"));
        assertThrows(IllegalArgumentException.class, () -> new Subscript(null, "A"));
    }

    @Test
    public void shouldThrowForNoLabels() {
        assertThrows(IllegalArgumentException.class, () -> new Subscript("Region"));
    }

    @Test
    public void shouldThrowForDuplicateLabels() {
        assertThrows(IllegalArgumentException.class,
                () -> new Subscript("Region", "North", "South", "North"));
    }

    @Test
    public void shouldThrowForBlankLabel() {
        assertThrows(IllegalArgumentException.class,
                () -> new Subscript("Region", "North", ""));
    }

    @Test
    public void shouldReturnUnmodifiableLabels() {
        Subscript region = new Subscript("Region", "North", "South");
        assertThrows(UnsupportedOperationException.class, () -> region.getLabels().add("East"));
    }

    @Test
    public void shouldSupportSingleLabel() {
        Subscript single = new Subscript("Dim", "Only");
        assertEquals(1, single.size());
        assertEquals("Only", single.getLabel(0));
    }

    @Test
    public void shouldIncludeNameAndLabelsInToString() {
        Subscript region = new Subscript("Region", "North", "South");
        String str = region.toString();
        assertEquals("Region[North, South]", str);
    }

    @Test
    public void shouldBeEqualWhenSameNameAndLabels() {
        Subscript a = new Subscript("Region", "North", "South", "East");
        Subscript b = new Subscript("Region", "North", "South", "East");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void shouldNotBeEqualWhenDifferentName() {
        Subscript a = new Subscript("Region", "North", "South");
        Subscript b = new Subscript("Area", "North", "South");
        assertNotEquals(a, b);
    }

    @Test
    public void shouldNotBeEqualWhenDifferentLabels() {
        Subscript a = new Subscript("Region", "North", "South");
        Subscript b = new Subscript("Region", "East", "West");
        assertNotEquals(a, b);
    }

    @Test
    public void shouldNotBeEqualWhenDifferentLabelOrder() {
        Subscript a = new Subscript("Region", "North", "South");
        Subscript b = new Subscript("Region", "South", "North");
        assertNotEquals(a, b);
    }
}
