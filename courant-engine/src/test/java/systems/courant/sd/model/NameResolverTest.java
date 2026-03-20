package systems.courant.sd.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NameResolverTest {

    @Nested
    @DisplayName("resolve(name, lookup)")
    class ResolveTests {

        @Test
        void shouldReturnExactMatch() {
            Map<String, String> map = Map.of("birth rate", "value");
            assertThat(NameResolver.resolve("birth rate", map::get)).isEqualTo("value");
        }

        @Test
        void shouldFallbackUnderscoreToSpace() {
            Map<String, String> map = Map.of("birth rate", "value");
            assertThat(NameResolver.resolve("birth_rate", map::get)).isEqualTo("value");
        }

        @Test
        void shouldReturnNullWhenNotFound() {
            Map<String, String> map = Map.of("other", "value");
            assertThat(NameResolver.resolve("birth_rate", map::get)).isNull();
        }

        @Test
        void shouldPreferExactMatchOverFallback() {
            Map<String, String> map = new HashMap<>();
            map.put("a_b", "exact");
            map.put("a b", "fallback");
            assertThat(NameResolver.resolve("a_b", map::get)).isEqualTo("exact");
        }

        @Test
        void shouldSkipFallbackWhenNoUnderscore() {
            Map<String, String> map = Map.of("other", "value");
            assertThat(NameResolver.resolve("nounderscore", map::get)).isNull();
        }

        @Test
        void shouldFallbackSpaceToUnderscore() {
            Map<String, String> map = Map.of("My_Table", "value");
            assertThat(NameResolver.resolve("My Table", map::get)).isEqualTo("value");
        }

        @Test
        void shouldPreferUnderscoreToSpaceFallbackOverSpaceToUnderscore() {
            Map<String, String> map = new HashMap<>();
            map.put("a b", "spaced");
            map.put("a_b", "underscored");
            // Exact match "a_b" wins
            assertThat(NameResolver.resolve("a_b", map::get)).isEqualTo("underscored");
        }
    }

    @Nested
    @DisplayName("resolveInSet(name, names)")
    class ResolveInSetTests {

        @Test
        void shouldReturnExactMatch() {
            Set<String> names = Set.of("birth rate", "death rate");
            assertThat(NameResolver.resolveInSet("birth rate", names)).isEqualTo("birth rate");
        }

        @Test
        void shouldFallbackUnderscoreToSpace() {
            Set<String> names = Set.of("birth rate", "death rate");
            assertThat(NameResolver.resolveInSet("birth_rate", names)).isEqualTo("birth rate");
        }

        @Test
        void shouldFallbackSpaceToUnderscore() {
            Set<String> names = Set.of("My_Table");
            assertThat(NameResolver.resolveInSet("My Table", names)).isEqualTo("My_Table");
        }

        @Test
        void shouldReturnNullWhenNotFound() {
            Set<String> names = Set.of("birth rate");
            assertThat(NameResolver.resolveInSet("unknown", names)).isNull();
        }
    }

    @Nested
    @DisplayName("containsName(name, names)")
    class ContainsNameTests {

        @Test
        void shouldMatchExact() {
            Set<String> names = Set.of("birth_rate");
            assertThat(NameResolver.containsName("birth_rate", names)).isTrue();
        }

        @Test
        void shouldMatchSpaceToUnderscore() {
            Set<String> names = Set.of("birth_rate");
            assertThat(NameResolver.containsName("birth rate", names)).isTrue();
        }

        @Test
        void shouldMatchUnderscoreToSpace() {
            Set<String> names = Set.of("birth rate");
            assertThat(NameResolver.containsName("birth_rate", names)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNotFound() {
            Set<String> names = Set.of("birth rate");
            assertThat(NameResolver.containsName("unknown", names)).isFalse();
        }
    }
}
