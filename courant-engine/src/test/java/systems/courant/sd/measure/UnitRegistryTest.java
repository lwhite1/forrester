package systems.courant.sd.measure;

import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;
import systems.courant.sd.measure.units.item.ItemUnit;
import systems.courant.sd.measure.units.money.MoneyUnits;
import systems.courant.sd.measure.units.money.NamedCurrency;
import systems.courant.sd.measure.units.time.TimeUnits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    @DisplayName("resolve() for unknown name should not throw but should auto-create (#557)")
    void shouldAutoCreateForUnknownNameWithoutThrowing() {
        // Simulates a typo — "Perosn" instead of "Person"
        Unit typo = registry.resolve("Perosn");
        assertThat(typo).isNotNull();
        assertThat(typo.getName()).isEqualTo("Perosn");
        // The auto-created unit is not compatible with the real "Person" unit
        Unit person = registry.find("Person");
        assertThat(typo).isNotSameAs(person);
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
    @DisplayName("resolve(null) should throw IllegalArgumentException")
    void shouldThrowForNullResolve() {
        assertThatThrownBy(() -> registry.resolve(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("resolve('') should auto-create unit for blank name")
    void shouldAutoCreateForBlankResolve() {
        // Some Vensim models have blank unit names — resolve should handle them
        Unit unit = registry.resolve("");
        assertThat(unit).isNotNull();
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

    @Nested
    @DisplayName("Dimensionless name recognition (#673)")
    class DimensionlessNames {

        @Test
        @DisplayName("should resolve 'Dmnl' to DIMENSIONLESS unit")
        void shouldResolveDmnlToDimensionless() {
            Unit unit = registry.resolve("Dmnl");
            assertThat(unit).isSameAs(DimensionlessUnits.DIMENSIONLESS);
        }

        @Test
        @DisplayName("should resolve 'units' to DIMENSIONLESS unit")
        void shouldResolveUnitsToDimensionless() {
            Unit unit = registry.resolve("units");
            assertThat(unit).isSameAs(DimensionlessUnits.DIMENSIONLESS);
        }

        @Test
        @DisplayName("should resolve 'dimensionless' to DIMENSIONLESS unit")
        void shouldResolveDimensionlessToDimensionless() {
            Unit unit = registry.resolve("dimensionless");
            assertThat(unit).isSameAs(DimensionlessUnits.DIMENSIONLESS);
        }

        @Test
        @DisplayName("should silently create unit for domain-specific names like 'Deer'")
        void shouldSilentlyCreateDomainSpecificName() {
            Unit unit = registry.resolve("Deer");
            assertThat(unit).isNotNull();
            assertThat(unit.getName()).isEqualTo("Deer");
            assertThat(unit).isInstanceOf(ItemUnit.class);
        }

        @Test
        @DisplayName("should resolve 'fraction' to DIMENSIONLESS unit")
        void shouldResolveFractionToDimensionless() {
            Unit unit = registry.resolve("fraction");
            assertThat(unit).isSameAs(DimensionlessUnits.DIMENSIONLESS);
        }

        @Test
        @DisplayName("should resolve 'percent' to DIMENSIONLESS unit")
        void shouldResolvePercentToDimensionless() {
            Unit unit = registry.resolve("percent");
            assertThat(unit).isSameAs(DimensionlessUnits.DIMENSIONLESS);
        }
    }

    @Nested
    @DisplayName("Currency recognition (#749)")
    class CurrencyRecognition {

        @Test
        @DisplayName("should resolve 'EURO' to a NamedCurrency in MONEY dimension")
        void shouldResolveEuroToMoney() {
            Unit unit = registry.resolve("EURO");
            assertThat(unit).isInstanceOf(NamedCurrency.class);
            assertThat(unit.getDimension()).isSameAs(Dimension.MONEY);
            assertThat(unit.getName()).isEqualTo("EURO");
        }

        @Test
        @DisplayName("should resolve 'GBP' to a NamedCurrency in MONEY dimension")
        void shouldResolveGbpToMoney() {
            Unit unit = registry.resolve("GBP");
            assertThat(unit).isInstanceOf(NamedCurrency.class);
            assertThat(unit.getDimension()).isSameAs(Dimension.MONEY);
        }

        @Test
        @DisplayName("should resolve 'dollar' to USD via alias")
        void shouldResolveDollarAlias() {
            Unit unit = registry.find("dollar");
            assertThat(unit).isSameAs(MoneyUnits.USD);
        }

        @Test
        @DisplayName("should resolve '$' to USD via alias")
        void shouldResolveDollarSign() {
            Unit unit = registry.find("$");
            assertThat(unit).isSameAs(MoneyUnits.USD);
        }

        @Test
        @DisplayName("should resolve 'M$' as currency")
        void shouldResolveMDollar() {
            Unit unit = registry.resolve("M$");
            assertThat(unit.getDimension()).isSameAs(Dimension.MONEY);
        }

        @Test
        @DisplayName("should resolve currency names case-insensitively")
        void shouldResolveCurrencyCaseInsensitive() {
            Unit euro = registry.resolve("euro");
            assertThat(euro.getDimension()).isSameAs(Dimension.MONEY);

            Unit yen = registry.resolve("JPY");
            assertThat(yen.getDimension()).isSameAs(Dimension.MONEY);
        }
    }

    @Nested
    @DisplayName("Area unit recognition (#749)")
    class AreaUnits {

        @Test
        @DisplayName("should find 'hectare' as a built-in unit")
        void shouldFindHectare() {
            Unit unit = registry.find("hectare");
            assertThat(unit).isNotNull();
            assertThat(unit.getDimension()).isSameAs(Dimension.LENGTH);
        }

        @Test
        @DisplayName("should find 'km2' as a built-in unit")
        void shouldFindKm2() {
            Unit unit = registry.find("km2");
            assertThat(unit).isNotNull();
            assertThat(unit.getDimension()).isSameAs(Dimension.LENGTH);
        }

        @Test
        @DisplayName("should find 'acre' as a built-in unit")
        void shouldFindAcre() {
            Unit unit = registry.find("acre");
            assertThat(unit).isNotNull();
            assertThat(unit.getDimension()).isSameAs(Dimension.LENGTH);
        }

        @Test
        @DisplayName("should find 'hectares' (plural) as a built-in unit")
        void shouldFindHectaresPlural() {
            Unit unit = registry.find("hectares");
            assertThat(unit).isNotNull();
        }
    }

    @Nested
    @DisplayName("Alias registration (#749)")
    class AliasRegistration {

        @Test
        @DisplayName("registerAlias should make name resolve to target unit")
        void shouldRegisterAlias() {
            UnitRegistry reg = new UnitRegistry();
            reg.registerAlias("personen", reg.find("Person"));
            assertThat(reg.find("personen")).isNotNull();
            assertThat(reg.find("personen").getName()).isEqualTo("Person");
        }

        @Test
        @DisplayName("alias should be case-insensitive")
        void shouldRegisterAliasCaseInsensitive() {
            UnitRegistry reg = new UnitRegistry();
            reg.registerAlias("Persoon", reg.find("Person"));
            assertThat(reg.find("persoon")).isNotNull();
        }
    }

    @Nested
    @DisplayName("MAX_CUSTOM_UNITS guard on register() (#632)")
    class RegisterGuard {

        @Test
        @DisplayName("register() should enforce MAX_CUSTOM_UNITS limit")
        void shouldEnforceMaxCustomUnitsOnRegister() {
            UnitRegistry reg = new UnitRegistry();
            // Register many custom units up to the limit
            for (int i = 0; i < 10_000; i++) {
                reg.register(new ItemUnit("Custom_" + i));
            }
            // Next registration should throw
            assertThatThrownBy(() -> reg.register(new ItemUnit("OneMore")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("10000");
        }

        @Test
        @DisplayName("re-registering same name should not increment count")
        void shouldNotIncrementCountForReRegistration() {
            UnitRegistry reg = new UnitRegistry();
            ItemUnit unit = new ItemUnit("Reusable");
            reg.register(unit);
            // Re-register same name many times — should not increase count
            for (int i = 0; i < 100; i++) {
                reg.register(new ItemUnit("Reusable"));
            }
            assertThat(reg.find("Reusable")).isNotNull();
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
