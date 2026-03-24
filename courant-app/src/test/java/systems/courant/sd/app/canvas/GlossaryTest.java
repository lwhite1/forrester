package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Glossary (#1331)")
class GlossaryTest {

    @Nested
    @DisplayName("Loading")
    class Loading {

        @Test
        @DisplayName("should load entries from JSON resource")
        void shouldLoadEntries() {
            assertThat(Glossary.instance().entries()).isNotEmpty();
        }

        @Test
        @DisplayName("entries should be sorted alphabetically by term")
        void shouldBeSortedAlphabetically() {
            List<Glossary.Entry> entries = Glossary.instance().entries();
            for (int i = 1; i < entries.size(); i++) {
                assertThat(entries.get(i).term().compareToIgnoreCase(entries.get(i - 1).term()))
                        .as("'%s' should come after '%s'", entries.get(i).term(), entries.get(i - 1).term())
                        .isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("every entry should have non-blank term, definition, and relevance")
        void shouldHaveRequiredFields() {
            for (Glossary.Entry entry : Glossary.instance().entries()) {
                assertThat(entry.term()).as("term").isNotBlank();
                assertThat(entry.definition()).as("definition for %s", entry.term()).isNotBlank();
                assertThat(entry.relevance()).as("relevance for %s", entry.term()).isNotBlank();
            }
        }

        @Test
        @DisplayName("related terms should reference existing entries")
        void shouldHaveValidRelatedTerms() {
            Glossary glossary = Glossary.instance();
            for (Glossary.Entry entry : glossary.entries()) {
                for (String related : entry.related()) {
                    assertThat(glossary.lookup(related))
                            .as("related term '%s' from '%s'", related, entry.term())
                            .isPresent();
                }
            }
        }
    }

    @Nested
    @DisplayName("Lookup")
    class Lookup {

        @Test
        @DisplayName("should find entry by exact term")
        void shouldFindByExactTerm() {
            Optional<Glossary.Entry> entry = Glossary.instance().lookup("Stock");
            assertThat(entry).isPresent();
            assertThat(entry.get().term()).isEqualTo("Stock");
        }

        @Test
        @DisplayName("should find entry by alias")
        void shouldFindByAlias() {
            Optional<Glossary.Entry> entry = Glossary.instance().lookup("CLD");
            assertThat(entry).isPresent();
            assertThat(entry.get().term()).isEqualTo("Causal Loop Diagram");
        }

        @Test
        @DisplayName("should be case-insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(Glossary.instance().lookup("stock")).isPresent();
            assertThat(Glossary.instance().lookup("STOCK")).isPresent();
            assertThat(Glossary.instance().lookup("Stock")).isPresent();
        }

        @Test
        @DisplayName("should return empty for unknown term")
        void shouldReturnEmptyForUnknown() {
            assertThat(Glossary.instance().lookup("nonexistent-xyz")).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null")
        void shouldReturnEmptyForNull() {
            assertThat(Glossary.instance().lookup(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Search")
    class Search {

        @Test
        @DisplayName("should return all entries for blank query")
        void shouldReturnAllForBlank() {
            assertThat(Glossary.instance().search(""))
                    .hasSize(Glossary.instance().entries().size());
        }

        @Test
        @DisplayName("should return all entries for null query")
        void shouldReturnAllForNull() {
            assertThat(Glossary.instance().search(null))
                    .hasSize(Glossary.instance().entries().size());
        }

        @Test
        @DisplayName("should find entries by term substring")
        void shouldFindByTermSubstring() {
            List<Glossary.Entry> results = Glossary.instance().search("feedback");
            assertThat(results).extracting(Glossary.Entry::term).contains("Feedback Loop");
        }

        @Test
        @DisplayName("should find entries by alias")
        void shouldFindByAlias() {
            List<Glossary.Entry> results = Glossary.instance().search("SFD");
            assertThat(results).extracting(Glossary.Entry::term).contains("Stock-and-Flow Diagram");
        }

        @Test
        @DisplayName("should find entries by definition content")
        void shouldFindByDefinition() {
            List<Glossary.Entry> results = Glossary.instance().search("accumulates over time");
            assertThat(results).extracting(Glossary.Entry::term).contains("Stock");
        }

        @Test
        @DisplayName("should prioritize term matches over definition matches")
        void shouldPrioritizeTermMatches() {
            List<Glossary.Entry> results = Glossary.instance().search("stock");
            assertThat(results).isNotEmpty();
            assertThat(results.getFirst().term()).isEqualTo("Stock");
        }

        @Test
        @DisplayName("should be case-insensitive")
        void shouldBeCaseInsensitive() {
            List<Glossary.Entry> lower = Glossary.instance().search("euler");
            List<Glossary.Entry> upper = Glossary.instance().search("EULER");
            assertThat(lower).isEqualTo(upper);
        }
    }
}
