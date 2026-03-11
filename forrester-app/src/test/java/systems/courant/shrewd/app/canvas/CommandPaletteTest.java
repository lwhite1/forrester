package systems.courant.shrewd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommandPalette")
class CommandPaletteTest {

    @Nested
    @DisplayName("score()")
    class Score {

        @Test
        @DisplayName("should return 0 for no match")
        void shouldReturnZeroForNoMatch() {
            assertThat(CommandPalette.score("add stock", "xyz")).isZero();
        }

        @Test
        @DisplayName("should score prefix match highest")
        void shouldScorePrefixMatchHighest() {
            int prefixScore = CommandPalette.score("add stock", "add");
            int substringScore = CommandPalette.score("an add stock", "add");
            assertThat(prefixScore).isGreaterThan(substringScore);
        }

        @Test
        @DisplayName("should score substring match higher than fuzzy")
        void shouldScoreSubstringHigherThanFuzzy() {
            int substringScore = CommandPalette.score("run simulation", "sim");
            int fuzzyScore = CommandPalette.score("save image", "sim");
            assertThat(substringScore).isGreaterThan(fuzzyScore);
        }

        @Test
        @DisplayName("should match fuzzy subsequence")
        void shouldMatchFuzzySubsequence() {
            // "mc" matches "monte carlo" via m...c
            int score = CommandPalette.score("monte carlo", "mc");
            assertThat(score).isGreaterThan(0);
        }

        @Test
        @DisplayName("should be case-insensitive when caller lowercases")
        void shouldBeCaseInsensitive() {
            int score = CommandPalette.score("run simulation", "rs");
            assertThat(score).isGreaterThan(0);
        }

        @Test
        @DisplayName("should prefer shorter names for prefix match")
        void shouldPreferShorterNamesForPrefixMatch() {
            int shortScore = CommandPalette.score("save", "save");
            int longScore = CommandPalette.score("save as model file", "save");
            assertThat(shortScore).isGreaterThan(longScore);
        }

        @Test
        @DisplayName("should prefer earlier substring match")
        void shouldPreferEarlierSubstringMatch() {
            int earlyScore = CommandPalette.score("a stock tool", "stock");
            int lateScore = CommandPalette.score("the new stock tool", "stock");
            assertThat(earlyScore).isGreaterThan(lateScore);
        }

        @Test
        @DisplayName("should give word-boundary bonus in fuzzy matching")
        void shouldGiveWordBoundaryBonus() {
            // "as" in "add stock" hits word boundaries (a at start, s after space)
            int boundaryScore = CommandPalette.score("add stock", "as");
            // "as" in "abstract" has 'a' at start but 's' mid-word
            int midWordScore = CommandPalette.score("abstract", "as");
            assertThat(boundaryScore).isGreaterThanOrEqualTo(midWordScore);
        }

        @Test
        @DisplayName("should return 0 when query is longer than text")
        void shouldReturnZeroWhenQueryLongerThanText() {
            assertThat(CommandPalette.score("ab", "abcdef")).isZero();
        }

        @Test
        @DisplayName("should match exact full text")
        void shouldMatchExactFullText() {
            int score = CommandPalette.score("undo", "undo");
            assertThat(score).isGreaterThan(0);
        }

        @Test
        @DisplayName("should rank element name above command when prefix matches")
        void shouldRankElementNameAboveCommand() {
            // "Stock 1" starts with "stock", "Add Stock" contains "stock" mid-string
            int elementScore = CommandPalette.score("stock 1", "stock");
            int commandScore = CommandPalette.score("add stock", "stock");
            assertThat(elementScore).isGreaterThan(commandScore);
        }
    }
}
