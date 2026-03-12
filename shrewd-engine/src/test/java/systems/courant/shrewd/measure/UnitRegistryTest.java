package systems.courant.shrewd.measure;

import systems.courant.shrewd.measure.units.item.ItemUnit;
import systems.courant.shrewd.measure.units.time.TimeUnits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UnitRegistry")
class UnitRegistryTest {

    private final UnitRegistry registry = new UnitRegistry();

    @Test
    void shouldFindTimeUnits() {
        assertThat(registry.find("Day")).isSameAs(TimeUnits.DAY);
        assertThat(registry.find("Week")).isSameAs(TimeUnits.WEEK);
        assertThat(registry.find("Month")).isSameAs(TimeUnits.MONTH);
        assertThat(registry.find("Year")).isSameAs(TimeUnits.YEAR);
    }

    @Test
    void shouldResolveTimeUnit() {
        TimeUnit day = registry.resolveTimeUnit("Day");
        assertThat(day).isSameAs(TimeUnits.DAY);
    }

    @Test
    void shouldThrowForNonTimeUnit() {
        assertThatThrownBy(() -> registry.resolveTimeUnit("Person"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldResolvePluralTimeUnits() {
        assertThat(registry.resolveTimeUnit("Days")).isSameAs(TimeUnits.DAY);
        assertThat(registry.resolveTimeUnit("Weeks")).isSameAs(TimeUnits.WEEK);
        assertThat(registry.resolveTimeUnit("Months")).isSameAs(TimeUnits.MONTH);
        assertThat(registry.resolveTimeUnit("Years")).isSameAs(TimeUnits.YEAR);
    }

    @Test
    void shouldFindPluralTimeUnitsCaseInsensitive() {
        assertThat(registry.find("days")).isSameAs(TimeUnits.DAY);
        assertThat(registry.find("DAYS")).isSameAs(TimeUnits.DAY);
    }

    @Test
    void shouldFindItemUnits() {
        assertThat(registry.find("Person")).isNotNull();
        assertThat(registry.find("Thing")).isNotNull();
    }

    @Test
    void shouldFindDimensionlessUnit() {
        assertThat(registry.find("Dimensionless unit")).isNotNull();
    }

    @Test
    void shouldFindCaseInsensitive() {
        assertThat(registry.find("day")).isNotNull();
        assertThat(registry.find("DAY")).isNotNull();
    }

    @Test
    void shouldAutoCreateCustomUnit() {
        Unit widget = registry.resolve("Widget");
        assertThat(widget).isNotNull();
        assertThat(widget.getName()).isEqualTo("Widget");
        assertThat(widget).isInstanceOf(ItemUnit.class);
    }

    @Test
    void shouldReturnSameCustomUnitOnReResolve() {
        Unit first = registry.resolve("Error");
        Unit second = registry.resolve("Error");
        assertThat(second).isSameAs(first);
    }

    @Test
    void shouldRegisterCustomUnit() {
        ItemUnit custom = new ItemUnit("Defect");
        registry.register(custom);
        assertThat(registry.find("Defect")).isSameAs(custom);
    }

    @Test
    void shouldReturnNullForUnknownFind() {
        assertThat(registry.find("NonExistentUnit12345")).isNull();
    }

    @Test
    void shouldNotLeaveSpuriousUnitAfterResolveTimeUnitFails() {
        String unknownName = "SomeCustomThing";
        assertThatThrownBy(() -> registry.resolveTimeUnit(unknownName))
                .isInstanceOf(IllegalArgumentException.class);
        // The failed resolveTimeUnit should not have auto-created a unit
        assertThat(registry.find(unknownName)).isNull();
    }

    @Test
    @DisplayName("concurrent resolve() calls for the same name should return the same instance")
    void shouldReturnSameInstanceUnderConcurrentResolve() throws Exception {
        int threadCount = 16;
        UnitRegistry shared = new UnitRegistry();
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(threadCount)) {
            List<Future<Unit>> futures = IntStream.range(0, threadCount)
                    .mapToObj(i -> pool.submit(() -> {
                        ready.countDown();
                        go.await();
                        return shared.resolve("ConcurrentWidget");
                    }))
                    .toList();

            ready.await();
            go.countDown();

            Set<Unit> distinct = ConcurrentHashMap.newKeySet();
            for (Future<Unit> f : futures) {
                distinct.add(f.get());
            }
            assertThat(distinct).as("All threads should get the same Unit instance").hasSize(1);
            assertThat(distinct.iterator().next().getName()).isEqualTo("ConcurrentWidget");
        }
    }

    @Test
    @DisplayName("concurrent resolve() calls for distinct names should not corrupt the registry")
    void shouldNotCorruptRegistryUnderConcurrentDistinctResolves() throws Exception {
        int threadCount = 64;
        UnitRegistry shared = new UnitRegistry();
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(threadCount)) {
            List<Future<Unit>> futures = IntStream.range(0, threadCount)
                    .mapToObj(i -> pool.submit(() -> {
                        ready.countDown();
                        go.await();
                        return shared.resolve("Unit_" + i);
                    }))
                    .toList();

            ready.await();
            go.countDown();

            for (Future<Unit> f : futures) {
                f.get(); // ensure no exceptions
            }

            // Every unit should be findable after all threads complete
            for (int i = 0; i < threadCount; i++) {
                assertThat(shared.find("Unit_" + i))
                        .as("Unit_%d should be registered", i)
                        .isNotNull();
            }
        }
    }
}
