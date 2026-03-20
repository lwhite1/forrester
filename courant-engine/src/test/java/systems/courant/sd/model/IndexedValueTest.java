package systems.courant.sd.model;

import systems.courant.sd.measure.units.item.ItemUnits;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexedValueTest {

    private final Subscript region = new Subscript("Region", "North", "South", "East");
    private final Subscript ageGroup = new Subscript("AgeGroup", "Young", "Adult", "Elder");
    private final Subscript scenario = new Subscript("Scenario", "Low", "High");

    @Nested
    class FactoryMethods {

        @Test
        void shouldCreateScalar() {
            IndexedValue v = IndexedValue.scalar(42.0);
            assertTrue(v.isScalar());
            assertNull(v.getRange());
            assertEquals(1, v.size());
            assertEquals(42.0, v.scalarValue());
            assertEquals(42.0, v.get(0));
        }

        @Test
        void shouldCreateFromSingleSubscript() {
            IndexedValue v = IndexedValue.of(region, 10, 20, 30);
            assertFalse(v.isScalar());
            assertEquals(3, v.size());
            assertEquals(10, v.get(0));
            assertEquals(20, v.get(1));
            assertEquals(30, v.get(2));
        }

        @Test
        void shouldCreateFromSubscriptRange() {
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            double[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9};
            IndexedValue v = IndexedValue.of(range, values);
            assertEquals(9, v.size());
            assertEquals(1, v.getAt("North", "Young"));
            assertEquals(5, v.getAt("South", "Adult"));
            assertEquals(9, v.getAt("East", "Elder"));
        }

        @Test
        void shouldFillWithConstantValue() {
            IndexedValue v = IndexedValue.fill(region, 5.0);
            assertEquals(3, v.size());
            assertEquals(5.0, v.get(0));
            assertEquals(5.0, v.get(1));
            assertEquals(5.0, v.get(2));
        }

        @Test
        void shouldFillWithSubscriptRange() {
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            IndexedValue v = IndexedValue.fill(range, 7.0);
            assertEquals(9, v.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(7.0, v.get(i));
            }
        }

        @Test
        void shouldRejectWrongArrayLength() {
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.of(region, 1, 2));
        }

        @Test
        void shouldRejectWrongArrayLengthForRange() {
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.of(range, new double[]{1, 2, 3}));
        }
    }

    @Nested
    class Accessors {

        @Test
        void shouldAccessByCoordinates() {
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            // Row-major: [N,Y]=0, [N,A]=1, [N,E]=2, [S,Y]=3, [S,A]=4, [S,E]=5, [E,Y]=6, [E,A]=7, [E,E]=8
            double[] values = {11, 12, 13, 21, 22, 23, 31, 32, 33};
            IndexedValue v = IndexedValue.of(range, values);

            assertEquals(11, v.getAt(0, 0));
            assertEquals(22, v.getAt(1, 1));
            assertEquals(33, v.getAt(2, 2));
        }

        @Test
        void shouldAccessByLabels() {
            IndexedValue v = IndexedValue.of(region, 100, 200, 300);
            assertEquals(100, v.getAt("North"));
            assertEquals(200, v.getAt("South"));
            assertEquals(300, v.getAt("East"));
        }

        @Test
        void shouldRejectCoordinateAccessOnScalar() {
            IndexedValue v = IndexedValue.scalar(1.0);
            assertThrows(IllegalStateException.class, () -> v.getAt(0));
        }

        @Test
        void shouldRejectLabelAccessOnScalar() {
            IndexedValue v = IndexedValue.scalar(1.0);
            assertThrows(IllegalStateException.class, () -> v.getAt("North"));
        }

        @Test
        void shouldRejectScalarValueOnIndexed() {
            IndexedValue v = IndexedValue.of(region, 1, 2, 3);
            assertThrows(IllegalStateException.class, v::scalarValue);
        }

        @Test
        void shouldReturnCopyFromToArray() {
            IndexedValue v = IndexedValue.of(region, 1, 2, 3);
            double[] arr = v.toArray();
            arr[0] = 999;
            assertEquals(1, v.get(0)); // original unchanged
        }
    }

    @Nested
    class ScalarArithmetic {

        @Test
        void shouldAddScalars() {
            IndexedValue a = IndexedValue.scalar(3);
            IndexedValue b = IndexedValue.scalar(4);
            IndexedValue result = a.add(b);
            assertTrue(result.isScalar());
            assertEquals(7, result.scalarValue());
        }

        @Test
        void shouldSubtractScalars() {
            assertEquals(1, IndexedValue.scalar(5).subtract(IndexedValue.scalar(4)).scalarValue());
        }

        @Test
        void shouldMultiplyScalars() {
            assertEquals(12, IndexedValue.scalar(3).multiply(IndexedValue.scalar(4)).scalarValue());
        }

        @Test
        void shouldDivideScalars() {
            assertEquals(2.5, IndexedValue.scalar(5).divide(IndexedValue.scalar(2)).scalarValue());
        }

        @Test
        void shouldReturnInfinityForDivisionByZero() {
            IndexedValue result = IndexedValue.scalar(1).divide(IndexedValue.scalar(0));
            assertTrue(Double.isInfinite(result.scalarValue()));
        }

        @Test
        void shouldReturnNaNForZeroDividedByZero() {
            IndexedValue result = IndexedValue.scalar(0).divide(IndexedValue.scalar(0));
            assertTrue(Double.isNaN(result.scalarValue()));
        }
    }

    @Nested
    class SameRangeArithmetic {

        @Test
        void shouldAddElementwise() {
            IndexedValue a = IndexedValue.of(region, 10, 20, 30);
            IndexedValue b = IndexedValue.of(region, 1, 2, 3);
            IndexedValue result = a.add(b);
            assertArrayEquals(new double[]{11, 22, 33}, result.toArray());
        }

        @Test
        void shouldSubtractElementwise() {
            IndexedValue a = IndexedValue.of(region, 10, 20, 30);
            IndexedValue b = IndexedValue.of(region, 1, 2, 3);
            IndexedValue result = a.subtract(b);
            assertArrayEquals(new double[]{9, 18, 27}, result.toArray());
        }

        @Test
        void shouldMultiplyElementwise() {
            IndexedValue a = IndexedValue.of(region, 2, 3, 4);
            IndexedValue b = IndexedValue.of(region, 5, 6, 7);
            IndexedValue result = a.multiply(b);
            assertArrayEquals(new double[]{10, 18, 28}, result.toArray());
        }

        @Test
        void shouldDivideElementwise() {
            IndexedValue a = IndexedValue.of(region, 10, 20, 30);
            IndexedValue b = IndexedValue.of(region, 2, 4, 5);
            IndexedValue result = a.divide(b);
            assertArrayEquals(new double[]{5, 5, 6}, result.toArray());
        }

        @Test
        void shouldHandleMultiDimensionalSameRange() {
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            IndexedValue a = IndexedValue.of(range, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
            IndexedValue b = IndexedValue.fill(range, 10);
            IndexedValue result = a.add(b);
            assertArrayEquals(new double[]{11, 12, 13, 14, 15, 16, 17, 18, 19}, result.toArray());
        }
    }

    @Nested
    class ScalarBroadcast {

        @Test
        void shouldBroadcastScalarToIndexed_add() {
            IndexedValue indexed = IndexedValue.of(region, 10, 20, 30);
            IndexedValue result = indexed.add(5);
            assertArrayEquals(new double[]{15, 25, 35}, result.toArray());
        }

        @Test
        void shouldBroadcastScalarToIndexed_multiply() {
            IndexedValue indexed = IndexedValue.of(region, 10, 20, 30);
            IndexedValue result = indexed.multiply(2);
            assertArrayEquals(new double[]{20, 40, 60}, result.toArray());
        }

        @Test
        void shouldBroadcastLeftScalarToRight() {
            IndexedValue scalar = IndexedValue.scalar(100);
            IndexedValue indexed = IndexedValue.of(region, 1, 2, 3);
            IndexedValue result = scalar.add(indexed);
            assertArrayEquals(new double[]{101, 102, 103}, result.toArray());
        }

        @Test
        void shouldPreserveRangeWhenBroadcastingScalar() {
            IndexedValue indexed = IndexedValue.of(region, 10, 20, 30);
            IndexedValue result = indexed.multiply(2);
            assertEquals(3, result.size());
            assertEquals(20, result.getAt("North"));
            assertEquals(40, result.getAt("South"));
            assertEquals(60, result.getAt("East"));
        }
    }

    @Nested
    class CrossDimensionBroadcast {

        @Test
        void shouldBroadcast_region_times_ageGroup() {
            // [Region] * [AgeGroup] → [Region × AgeGroup]
            IndexedValue byRegion = IndexedValue.of(region, 1, 2, 3);
            IndexedValue byAge = IndexedValue.of(ageGroup, 10, 100, 1000);
            IndexedValue result = byRegion.multiply(byAge);

            assertEquals(9, result.size());
            assertEquals(2, result.getRange().dimensionCount());

            // North(1) * Young(10) = 10
            assertEquals(10, result.getAt("North", "Young"));
            // North(1) * Adult(100) = 100
            assertEquals(100, result.getAt("North", "Adult"));
            // South(2) * Young(10) = 20
            assertEquals(20, result.getAt("South", "Young"));
            // East(3) * Elder(1000) = 3000
            assertEquals(3000, result.getAt("East", "Elder"));
        }

        @Test
        void shouldBroadcast_regionAge_plus_region() {
            // [Region × AgeGroup] + [Region] → [Region × AgeGroup]
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            IndexedValue byRegionAge = IndexedValue.fill(range, 100);
            IndexedValue byRegion = IndexedValue.of(region, 1, 2, 3);
            IndexedValue result = byRegionAge.add(byRegion);

            assertEquals(9, result.size());
            // North elements all get +1
            assertEquals(101, result.getAt("North", "Young"));
            assertEquals(101, result.getAt("North", "Adult"));
            assertEquals(101, result.getAt("North", "Elder"));
            // South elements all get +2
            assertEquals(102, result.getAt("South", "Young"));
            // East elements all get +3
            assertEquals(103, result.getAt("East", "Elder"));
        }

        @Test
        void shouldBroadcast_region_plus_regionAge() {
            // [Region] + [Region × AgeGroup] → [Region × AgeGroup]
            // Same as above but operands reversed
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            IndexedValue byRegion = IndexedValue.of(region, 1, 2, 3);
            IndexedValue byRegionAge = IndexedValue.fill(range, 100);
            IndexedValue result = byRegion.add(byRegionAge);

            assertEquals(9, result.size());
            assertEquals(101, result.getAt("North", "Young"));
            assertEquals(102, result.getAt("South", "Adult"));
            assertEquals(103, result.getAt("East", "Elder"));
        }

        @Test
        void shouldBroadcast_regionAge_times_ageGroupScenario() {
            // [Region × AgeGroup] * [AgeGroup × Scenario] → [Region × AgeGroup × Scenario]
            SubscriptRange rangeRA = new SubscriptRange(List.of(region, ageGroup));
            SubscriptRange rangeAS = new SubscriptRange(List.of(ageGroup, scenario));

            // Population: each Region × AgeGroup = flat value (region_idx+1)*10 + age_idx+1
            // N,Y=11  N,A=12  N,E=13  S,Y=21  S,A=22  S,E=23  E,Y=31  E,A=32  E,E=33
            IndexedValue pop = IndexedValue.of(rangeRA,
                    new double[]{11, 12, 13, 21, 22, 23, 31, 32, 33});

            // Multiplier per AgeGroup × Scenario
            // Young,Low=1  Young,High=2  Adult,Low=3  Adult,High=4  Elder,Low=5  Elder,High=6
            IndexedValue mult = IndexedValue.of(rangeAS,
                    new double[]{1, 2, 3, 4, 5, 6});

            IndexedValue result = pop.multiply(mult);

            // Result should be [Region × AgeGroup × Scenario] = 3*3*2 = 18 elements
            assertEquals(18, result.size());
            assertEquals(3, result.getRange().dimensionCount());

            // North,Young,Low = 11 * 1 = 11
            assertEquals(11, result.getAt("North", "Young", "Low"));
            // North,Young,High = 11 * 2 = 22
            assertEquals(22, result.getAt("North", "Young", "High"));
            // South,Adult,Low = 22 * 3 = 66
            assertEquals(66, result.getAt("South", "Adult", "Low"));
            // East,Elder,High = 33 * 6 = 198
            assertEquals(198, result.getAt("East", "Elder", "High"));
        }

        @Test
        void shouldBroadcast_ageGroup_plus_regionAge() {
            // [AgeGroup] + [Region × AgeGroup] → [AgeGroup × Region]
            // Note: result dimension order is left-first, then right-only
            IndexedValue byAge = IndexedValue.of(ageGroup, 10, 20, 30);
            SubscriptRange rangeRA = new SubscriptRange(List.of(region, ageGroup));
            IndexedValue byRegionAge = IndexedValue.of(rangeRA,
                    new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});

            IndexedValue result = byAge.add(byRegionAge);
            assertEquals(9, result.size());

            // Result dims: [AgeGroup, Region] (left first, then right-only=Region)
            // AgeGroup(Young=10) + [North,Young]=1 → at [Young,North] = 11
            assertEquals(11, result.getAt("Young", "North"));
            // AgeGroup(Adult=20) + [South,Adult]=5 → at [Adult,South] = 25
            assertEquals(25, result.getAt("Adult", "South"));
            // AgeGroup(Elder=30) + [East,Elder]=9 → at [Elder,East] = 39
            assertEquals(39, result.getAt("Elder", "East"));
        }
    }

    @Nested
    class BroadcastErrorCases {

        @Test
        void shouldRejectMismatchedLabels() {
            Subscript region2 = new Subscript("Region", "North", "West");
            IndexedValue a = IndexedValue.of(region, 1, 2, 3);
            IndexedValue b = IndexedValue.of(region2, 10, 20);
            assertThrows(IllegalArgumentException.class, () -> a.add(b));
        }

        @Test
        void shouldReturnInfinityForDivisionByZeroInBroadcast() {
            IndexedValue a = IndexedValue.of(region, 10, 20, 30);
            IndexedValue b = IndexedValue.of(region, 1, 0, 3);
            IndexedValue result = a.divide(b);
            assertEquals(10.0, result.get(0));
            assertTrue(Double.isInfinite(result.get(1)));
            assertEquals(10.0, result.get(2));
        }
    }

    @Nested
    class Negate {

        @Test
        void shouldNegateScalar() {
            assertEquals(-5, IndexedValue.scalar(5).negate().scalarValue());
        }

        @Test
        void shouldNegateIndexed() {
            IndexedValue v = IndexedValue.of(region, 1, -2, 3);
            assertArrayEquals(new double[]{-1, 2, -3}, v.negate().toArray());
        }
    }

    @Nested
    class Aggregation {

        @Test
        void shouldSum() {
            assertEquals(60, IndexedValue.of(region, 10, 20, 30).sum());
        }

        @Test
        void shouldSumScalar() {
            assertEquals(5, IndexedValue.scalar(5).sum());
        }

        @Test
        void shouldMean() {
            assertEquals(20, IndexedValue.of(region, 10, 20, 30).mean());
        }

        @Test
        void shouldMax() {
            assertEquals(30, IndexedValue.of(region, 10, 30, 20).max());
        }

        @Test
        void shouldMin() {
            assertEquals(10, IndexedValue.of(region, 30, 10, 20).min());
        }
    }

    @Nested
    class SumOver {

        @Test
        void shouldSumOverOnlyDimension_returnsScalar() {
            IndexedValue v = IndexedValue.of(region, 10, 20, 30);
            IndexedValue result = v.sumOver(region);
            assertTrue(result.isScalar());
            assertEquals(60, result.scalarValue());
        }

        @Test
        void shouldSumOverSecondDimension() {
            // [Region × AgeGroup], sum over AgeGroup → [Region]
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            // N,Y=1  N,A=2  N,E=3  S,Y=4  S,A=5  S,E=6  E,Y=7  E,A=8  E,E=9
            IndexedValue v = IndexedValue.of(range, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
            IndexedValue result = v.sumOver(ageGroup);

            assertEquals(3, result.size());
            // North: 1+2+3 = 6
            assertEquals(6, result.getAt("North"));
            // South: 4+5+6 = 15
            assertEquals(15, result.getAt("South"));
            // East: 7+8+9 = 24
            assertEquals(24, result.getAt("East"));
        }

        @Test
        void shouldSumOverFirstDimension() {
            // [Region × AgeGroup], sum over Region → [AgeGroup]
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            IndexedValue v = IndexedValue.of(range, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
            IndexedValue result = v.sumOver(region);

            assertEquals(3, result.size());
            // Young: 1+4+7 = 12
            assertEquals(12, result.getAt("Young"));
            // Adult: 2+5+8 = 15
            assertEquals(15, result.getAt("Adult"));
            // Elder: 3+6+9 = 18
            assertEquals(18, result.getAt("Elder"));
        }

        @Test
        void shouldSumOverMiddleDimension() {
            // [Region × AgeGroup × Scenario], sum over AgeGroup → [Region × Scenario]
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup, scenario));
            double[] values = new double[18];
            for (int i = 0; i < 18; i++) {
                values[i] = i + 1;
            }
            IndexedValue v = IndexedValue.of(range, values);
            IndexedValue result = v.sumOver(ageGroup);

            // Result: [Region × Scenario] = 3 × 2 = 6 elements
            assertEquals(6, result.size());
            // North,Low: sum of [N,Y,L]=1, [N,A,L]=3, [N,E,L]=5 → 9
            assertEquals(9, result.getAt("North", "Low"));
            // North,High: sum of [N,Y,H]=2, [N,A,H]=4, [N,E,H]=6 → 12
            assertEquals(12, result.getAt("North", "High"));
        }

        @Test
        void shouldRejectSumOverOnScalar() {
            assertThrows(IllegalStateException.class, () ->
                    IndexedValue.scalar(1).sumOver(region));
        }

        @Test
        void shouldRejectSumOverMissingDimension() {
            IndexedValue v = IndexedValue.of(region, 1, 2, 3);
            assertThrows(IllegalArgumentException.class, () -> v.sumOver(ageGroup));
        }
    }

    @Nested
    class GetIndexedValueConvenience {

        @Test
        void shouldGetFromArrayedStock() {
            ArrayedStock stock = new ArrayedStock("Pop", region, new double[]{100, 200, 300}, ItemUnits.PEOPLE);
            IndexedValue v = stock.getIndexedValue();
            assertEquals(3, v.size());
            assertEquals(100, v.getAt("North"));
            assertEquals(200, v.getAt("South"));
            assertEquals(300, v.getAt("East"));
        }

        @Test
        void shouldGetFromArrayedVariable() {
            ArrayedVariable var = ArrayedVariable.create("Rate", ItemUnits.PEOPLE, region,
                    i -> (i + 1) * 10.0);
            IndexedValue v = var.getIndexedValue();
            assertEquals(3, v.size());
            assertEquals(10, v.get(0));
            assertEquals(20, v.get(1));
            assertEquals(30, v.get(2));
        }

        @Test
        void shouldGetFromMultiArrayedStock() {
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            MultiArrayedStock stock = new MultiArrayedStock("Pop", range,
                    new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, ItemUnits.PEOPLE);
            IndexedValue v = stock.getIndexedValue();
            assertEquals(9, v.size());
            assertEquals(1, v.getAt("North", "Young"));
            assertEquals(5, v.getAt("South", "Adult"));
            assertEquals(9, v.getAt("East", "Elder"));
        }

        @Test
        void shouldGetFromMultiArrayedVariable() {
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            MultiArrayedVariable var = MultiArrayedVariable.create("Density", ItemUnits.PEOPLE, range,
                    coords -> (coords[0] + 1) * 10.0 + coords[1] + 1);
            IndexedValue v = var.getIndexedValue();
            assertEquals(9, v.size());
            assertEquals(11, v.getAt("North", "Young"));
            assertEquals(22, v.getAt("South", "Adult"));
            assertEquals(33, v.getAt("East", "Elder"));
        }
    }

    @Nested
    class BroadcastWithArithmetic {

        @Test
        void shouldSupportChainedOperations() {
            // (population * birthRate) - (population * deathRate)
            IndexedValue population = IndexedValue.of(region, 1000, 2000, 500);
            IndexedValue birthRate = IndexedValue.scalar(0.03);
            IndexedValue deathRate = IndexedValue.scalar(0.01);

            IndexedValue netGrowth = population.multiply(birthRate).subtract(population.multiply(deathRate));
            assertArrayEquals(new double[]{20, 40, 10}, netGrowth.toArray());
        }

        @Test
        void shouldSupportBroadcastThenAggregate() {
            // Multiply population[Region] by rate[AgeGroup], then sum over Region
            IndexedValue population = IndexedValue.of(region, 100, 200, 300);
            IndexedValue rate = IndexedValue.of(ageGroup, 0.1, 0.2, 0.3);

            IndexedValue product = population.multiply(rate); // [Region × AgeGroup]
            assertEquals(9, product.size());

            IndexedValue byAge = product.sumOver(region); // sum over Region → [AgeGroup]
            assertEquals(3, byAge.size());
            // Young: 100*0.1 + 200*0.1 + 300*0.1 = 60
            assertEquals(60, byAge.getAt("Young"), 1e-10);
            // Adult: 100*0.2 + 200*0.2 + 300*0.2 = 120
            assertEquals(120, byAge.getAt("Adult"), 1e-10);
            // Elder: 100*0.3 + 200*0.3 + 300*0.3 = 180
            assertEquals(180, byAge.getAt("Elder"), 1e-10);
        }
    }

    @Nested
    class NullValidation {

        @Test
        void shouldRejectNullSubscriptInOf() {
            assertThrows(NullPointerException.class, () ->
                    IndexedValue.of((Subscript) null, 1, 2, 3));
        }

        @Test
        void shouldRejectNullValuesInOfSubscript() {
            assertThrows(NullPointerException.class, () ->
                    IndexedValue.of(region, (double[]) null));
        }

        @Test
        void shouldRejectNullRangeInOf() {
            assertThrows(NullPointerException.class, () ->
                    IndexedValue.of((SubscriptRange) null, new double[]{1, 2}));
        }

        @Test
        void shouldRejectNullValuesInOfRange() {
            SubscriptRange range = new SubscriptRange(List.of(region));
            assertThrows(NullPointerException.class, () ->
                    IndexedValue.of(range, null));
        }

        @Test
        void shouldRejectNullRangeInFill() {
            assertThrows(NullPointerException.class, () ->
                    IndexedValue.fill((SubscriptRange) null, 1.0));
        }

        @Test
        void shouldRejectNullSubscriptInFill() {
            assertThrows(NullPointerException.class, () ->
                    IndexedValue.fill((Subscript) null, 1.0));
        }
    }

    @Nested
    class BoundsChecking {

        @Test
        void shouldRejectNegativeFlatIndex() {
            IndexedValue v = IndexedValue.of(region, 1, 2, 3);
            assertThrows(IndexOutOfBoundsException.class, () -> v.get(-1));
        }

        @Test
        void shouldRejectFlatIndexEqualToSize() {
            IndexedValue v = IndexedValue.of(region, 1, 2, 3);
            assertThrows(IndexOutOfBoundsException.class, () -> v.get(3));
        }

        @Test
        void shouldRejectFlatIndexBeyondSize() {
            IndexedValue v = IndexedValue.of(region, 1, 2, 3);
            assertThrows(IndexOutOfBoundsException.class, () -> v.get(100));
        }

        @Test
        void shouldRejectWrongCoordinateCount() {
            SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));
            IndexedValue v = IndexedValue.of(range, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
            // Pass 1 coordinate for a 2-D value
            assertThrows(IllegalArgumentException.class, () -> v.getAt(0));
        }
    }

    @Nested
    class SingleElementSubscript {

        @Test
        void shouldWorkWithSingleLabel() {
            Subscript single = new Subscript("Only", "One");
            IndexedValue v = IndexedValue.of(single, 42);
            assertEquals(1, v.size());
            assertEquals(42, v.getAt("One"));
        }

        @Test
        void shouldBroadcastSingleElementWithMultiElement() {
            Subscript single = new Subscript("Scenario", "Base");
            IndexedValue base = IndexedValue.of(single, 1.5);
            IndexedValue byRegion = IndexedValue.of(region, 100, 200, 300);
            IndexedValue result = byRegion.multiply(base);

            assertEquals(3, result.size());
            assertEquals(150, result.getAt("North", "Base"));
            assertEquals(300, result.getAt("South", "Base"));
            assertEquals(450, result.getAt("East", "Base"));
        }

        @Test
        void shouldSumOverSingleElement() {
            Subscript single = new Subscript("Only", "One");
            IndexedValue v = IndexedValue.of(single, 42);
            IndexedValue result = v.sumOver(single);
            assertTrue(result.isScalar());
            assertEquals(42, result.scalarValue());
        }
    }

    @Nested
    class ScalarConvenienceOverloads {

        @Test
        void shouldSubtractDouble() {
            IndexedValue v = IndexedValue.of(region, 10, 20, 30);
            assertArrayEquals(new double[]{7, 17, 27}, v.subtract(3).toArray());
        }

        @Test
        void shouldDivideByDouble() {
            IndexedValue v = IndexedValue.of(region, 10, 20, 30);
            assertArrayEquals(new double[]{5, 10, 15}, v.divide(2).toArray());
        }

        @Test
        void shouldReturnInfinityForDivideByZeroDouble() {
            IndexedValue v = IndexedValue.of(region, 10, 20, 30);
            IndexedValue result = v.divide(0);
            for (int i = 0; i < result.size(); i++) {
                assertTrue(Double.isInfinite(result.get(i)));
            }
        }
    }

    @Nested
    class NaNInfinityValidation {

        @Test
        void shouldRejectNaNInScalar() {
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.scalar(Double.NaN));
        }

        @Test
        void shouldRejectPositiveInfinityInScalar() {
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.scalar(Double.POSITIVE_INFINITY));
        }

        @Test
        void shouldRejectNegativeInfinityInScalar() {
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.scalar(Double.NEGATIVE_INFINITY));
        }

        @Test
        void shouldRejectNaNInOfSubscript() {
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.of(region, 1, Double.NaN, 3));
        }

        @Test
        void shouldRejectInfinityInOfSubscript() {
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.of(region, 1, Double.POSITIVE_INFINITY, 3));
        }

        @Test
        void shouldRejectNaNInOfRange() {
            SubscriptRange range = new SubscriptRange(List.of(region));
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.of(range, new double[]{1, Double.NaN, 3}));
        }

        @Test
        void shouldRejectInfinityInOfRange() {
            SubscriptRange range = new SubscriptRange(List.of(region));
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.of(range, new double[]{1, Double.NEGATIVE_INFINITY, 3}));
        }

        @Test
        void shouldRejectNaNInFillRange() {
            SubscriptRange range = new SubscriptRange(List.of(region));
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.fill(range, Double.NaN));
        }

        @Test
        void shouldRejectInfinityInFillSubscript() {
            assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.fill(region, Double.POSITIVE_INFINITY));
        }

        @Test
        void shouldReportIndexOfBadValue() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    IndexedValue.of(region, 1, 2, Double.NaN));
            assertTrue(ex.getMessage().contains("index 2"));
        }
    }

    @Nested
    class ReversedDimensionOrder {

        @Test
        void shouldAlignSharedDimensionsRegardlessOfOrder() {
            // [Region × AgeGroup] + [AgeGroup × Region]
            // Both have Region and AgeGroup but in different order.
            // Result should be [Region × AgeGroup] (left dimension order preserved).
            SubscriptRange rangeRA = new SubscriptRange(List.of(region, ageGroup));
            SubscriptRange rangeAR = new SubscriptRange(List.of(ageGroup, region));

            // [Region × AgeGroup]: N,Y=1  N,A=2  N,E=3  S,Y=4  S,A=5  S,E=6  E,Y=7  E,A=8  E,E=9
            IndexedValue ra = IndexedValue.of(rangeRA, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
            // [AgeGroup × Region]: Y,N=10  Y,S=20  Y,E=30  A,N=40  A,S=50  A,E=60  E,N=70  E,S=80  E,E=90
            IndexedValue ar = IndexedValue.of(rangeAR,
                    new double[]{10, 20, 30, 40, 50, 60, 70, 80, 90});

            IndexedValue result = ra.add(ar);

            // Result should be [Region × AgeGroup] since left dims come first
            // and right has no new dims. All dims are shared.
            assertEquals(9, result.size());

            // North,Young: ra[N,Y]=1 + ar[Y,N]=10 → 11
            assertEquals(11, result.getAt("North", "Young"));
            // North,Adult: ra[N,A]=2 + ar[A,N]=40 → 42
            assertEquals(42, result.getAt("North", "Adult"));
            // South,Young: ra[S,Y]=4 + ar[Y,S]=20 → 24
            assertEquals(24, result.getAt("South", "Young"));
            // East,Elder: ra[E,E]=9 + ar[E,E]=90 → 99
            assertEquals(99, result.getAt("East", "Elder"));
            // South,Elder: ra[S,E]=6 + ar[E,S]=80 → 86
            assertEquals(86, result.getAt("South", "Elder"));
        }
    }
}
