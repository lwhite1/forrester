package systems.courant.forrester.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SubscriptRangeTest {

    private final Subscript region = new Subscript("Region", "North", "South", "East");
    private final Subscript ageGroup = new Subscript("AgeGroup", "Young", "Adult", "Elder");

    @Test
    public void shouldComputeTotalSizeAsProductOfDimensions() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertEquals(9, range.totalSize());
        assertEquals(2, range.dimensionCount());
    }

    @Test
    public void shouldSupportSingleDimension() {
        SubscriptRange range = new SubscriptRange(List.of(region));
        assertEquals(3, range.totalSize());
        assertEquals(1, range.dimensionCount());
    }

    @Test
    public void shouldConvertCoordinatesToFlatIndex() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        // Row-major: [Region(3), AgeGroup(3)], strides = [3, 1]
        assertEquals(0, range.toFlatIndex(0, 0));  // North, Young
        assertEquals(1, range.toFlatIndex(0, 1));  // North, Adult
        assertEquals(2, range.toFlatIndex(0, 2));  // North, Elder
        assertEquals(3, range.toFlatIndex(1, 0));  // South, Young
        assertEquals(5, range.toFlatIndex(1, 2));  // South, Elder
        assertEquals(8, range.toFlatIndex(2, 2));  // East, Elder
    }

    @Test
    public void shouldConvertFlatIndexToCoordinates() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertArrayEquals(new int[]{0, 0}, range.toCoordinates(0));
        assertArrayEquals(new int[]{0, 2}, range.toCoordinates(2));
        assertArrayEquals(new int[]{1, 0}, range.toCoordinates(3));
        assertArrayEquals(new int[]{1, 2}, range.toCoordinates(5));
        assertArrayEquals(new int[]{2, 2}, range.toCoordinates(8));
    }

    @Test
    public void shouldRoundTripFlatIndexAndCoordinates() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        for (int i = 0; i < range.totalSize(); i++) {
            int[] coords = range.toCoordinates(i);
            assertEquals(i, range.toFlatIndex(coords));
        }
    }

    @Test
    public void shouldConvertLabelsToFlatIndex() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertEquals(0, range.toFlatIndex("North", "Young"));
        assertEquals(5, range.toFlatIndex("South", "Elder"));
        assertEquals(8, range.toFlatIndex("East", "Elder"));
    }

    @Test
    public void shouldComposeNameWithCommaSeparatedLabels() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertEquals("Pop[North,Young]", range.composeName("Pop", 0));
        assertEquals("Pop[South,Elder]", range.composeName("Pop", 5));
        assertEquals("Pop[East,Elder]", range.composeName("Pop", 8));
    }

    @Test
    public void shouldReturnLabelsAtFlatIndex() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertEquals(List.of("North", "Young"), range.getLabelsAt(0));
        assertEquals(List.of("South", "Elder"), range.getLabelsAt(5));
    }

    @Test
    public void shouldReturnAllCombinationsInRowMajorOrder() {
        Subscript small1 = new Subscript("A", "a0", "a1");
        Subscript small2 = new Subscript("B", "b0", "b1");
        SubscriptRange range = new SubscriptRange(List.of(small1, small2));

        List<List<String>> combos = range.allCombinations();
        assertEquals(4, combos.size());
        assertEquals(List.of("a0", "b0"), combos.get(0));
        assertEquals(List.of("a0", "b1"), combos.get(1));
        assertEquals(List.of("a1", "b0"), combos.get(2));
        assertEquals(List.of("a1", "b1"), combos.get(3));
    }

    @Test
    public void shouldReturnSubscriptsAndDimensionInfo() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertEquals(region, range.getSubscript(0));
        assertEquals(ageGroup, range.getSubscript(1));
        assertEquals(2, range.getSubscripts().size());
    }

    @Test
    public void shouldRemoveDimension() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        SubscriptRange reduced = range.removeDimension(0);
        assertEquals(1, reduced.dimensionCount());
        assertEquals(ageGroup, reduced.getSubscript(0));
        assertEquals(3, reduced.totalSize());
    }

    @Test
    public void shouldRemoveLastDimension() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        SubscriptRange reduced = range.removeDimension(1);
        assertEquals(1, reduced.dimensionCount());
        assertEquals(region, reduced.getSubscript(0));
        assertEquals(3, reduced.totalSize());
    }

    @Test
    public void shouldThrowWhenRemovingOnlyDimension() {
        SubscriptRange range = new SubscriptRange(List.of(region));
        assertThrows(IllegalArgumentException.class, () -> range.removeDimension(0));
    }

    @Test
    public void shouldThrowWhenRemovingOutOfRangeDimension() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertThrows(IllegalArgumentException.class, () -> range.removeDimension(3));
        assertThrows(IllegalArgumentException.class, () -> range.removeDimension(-1));
    }

    @Test
    public void shouldThrowWhenSubscriptListIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new SubscriptRange(List.of()));
    }

    @Test
    public void shouldThrowWhenSubscriptListIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new SubscriptRange(null));
    }

    @Test
    public void shouldThrowWhenDuplicateDimensionNames() {
        Subscript dup = new Subscript("Region", "X", "Y");
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptRange(List.of(region, dup)));
    }

    @Test
    public void shouldThrowWhenCoordinateCountMismatches() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertThrows(IllegalArgumentException.class, () -> range.toFlatIndex(0));
        assertThrows(IllegalArgumentException.class, () -> range.toFlatIndex(0, 0, 0));
    }

    @Test
    public void shouldThrowWhenCoordinateOutOfRange() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertThrows(IllegalArgumentException.class, () -> range.toFlatIndex(3, 0));
        assertThrows(IllegalArgumentException.class, () -> range.toFlatIndex(0, -1));
    }

    @Test
    public void shouldThrowWhenFlatIndexOutOfRange() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertThrows(IllegalArgumentException.class, () -> range.toCoordinates(-1));
        assertThrows(IllegalArgumentException.class, () -> range.toCoordinates(9));
    }

    @Test
    public void shouldThrowWhenLabelCountMismatches() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertThrows(IllegalArgumentException.class, () -> range.toFlatIndex("North"));
    }

    @Test
    public void shouldThrowWhenLabelIsInvalid() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertThrows(IllegalArgumentException.class,
                () -> range.toFlatIndex("North", "INVALID"));
        assertThrows(IllegalArgumentException.class,
                () -> range.toFlatIndex("INVALID", "Young"));
    }

    @Test
    public void shouldRemoveMiddleDimensionFrom3D() {
        Subscript dim1 = new Subscript("A", "a0", "a1");
        Subscript dim2 = new Subscript("B", "b0", "b1", "b2");
        Subscript dim3 = new Subscript("C", "c0", "c1");
        SubscriptRange range = new SubscriptRange(List.of(dim1, dim2, dim3));
        SubscriptRange reduced = range.removeDimension(1);  // remove B
        assertEquals(2, reduced.dimensionCount());
        assertEquals("A", reduced.getSubscript(0).getName());
        assertEquals("C", reduced.getSubscript(1).getName());
        assertEquals(4, reduced.totalSize());  // 2 * 2
        assertEquals(0, reduced.toFlatIndex(0, 0));
        assertEquals(3, reduced.toFlatIndex(1, 1));
    }

    @Test
    public void shouldThrowOnOverflow() {
        // Create subscripts whose product would overflow int
        // 50000 * 50000 = 2.5 billion > Integer.MAX_VALUE
        String[] labels = new String[50000];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = "L" + i;
        }
        Subscript big1 = new Subscript("Big1", labels);
        Subscript big2 = new Subscript("Big2", labels);
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptRange(List.of(big1, big2)));
    }

    @Test
    public void shouldWorkWithThreeDimensions() {
        Subscript dim1 = new Subscript("A", "a0", "a1");
        Subscript dim2 = new Subscript("B", "b0", "b1", "b2");
        Subscript dim3 = new Subscript("C", "c0", "c1");
        SubscriptRange range = new SubscriptRange(List.of(dim1, dim2, dim3));

        assertEquals(12, range.totalSize());
        assertEquals(3, range.dimensionCount());

        // Strides: [6, 2, 1]
        assertEquals(0, range.toFlatIndex(0, 0, 0));
        assertEquals(1, range.toFlatIndex(0, 0, 1));
        assertEquals(2, range.toFlatIndex(0, 1, 0));
        assertEquals(6, range.toFlatIndex(1, 0, 0));
        assertEquals(11, range.toFlatIndex(1, 2, 1));

        assertArrayEquals(new int[]{1, 0, 0}, range.toCoordinates(6));
        assertArrayEquals(new int[]{1, 2, 1}, range.toCoordinates(11));
    }

    @Test
    public void shouldReturnUnmodifiableCombinations() {
        SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
        assertThrows(UnsupportedOperationException.class,
                () -> range.allCombinations().add(List.of("X")));
        assertThrows(UnsupportedOperationException.class,
                () -> range.getLabelsAt(0).add("X"));
    }
}
