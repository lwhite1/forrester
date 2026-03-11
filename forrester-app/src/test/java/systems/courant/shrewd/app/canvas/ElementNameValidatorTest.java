package systems.courant.shrewd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ElementNameValidator")
class ElementNameValidatorTest {

    @Nested
    @DisplayName("isValidName")
    class IsValidName {

        @Test
        void shouldAcceptSimpleName() {
            assertThat(ElementNameValidator.isValidName("Population")).isTrue();
        }

        @Test
        void shouldAcceptNameWithSpaces() {
            assertThat(ElementNameValidator.isValidName("Birth Rate")).isTrue();
        }

        @Test
        void shouldAcceptNameWithUnderscores() {
            assertThat(ElementNameValidator.isValidName("Contact_Rate")).isTrue();
        }

        @Test
        void shouldRejectNull() {
            assertThat(ElementNameValidator.isValidName(null)).isFalse();
        }

        @Test
        void shouldRejectBlank() {
            assertThat(ElementNameValidator.isValidName("")).isFalse();
            assertThat(ElementNameValidator.isValidName("   ")).isFalse();
        }

        @Test
        void shouldRejectSpecialCharacters() {
            assertThat(ElementNameValidator.isValidName("Rate*2")).isFalse();
            assertThat(ElementNameValidator.isValidName("a+b")).isFalse();
        }

        @Test
        void shouldRejectReservedWords() {
            assertThat(ElementNameValidator.isValidName("TIME")).isFalse();
            assertThat(ElementNameValidator.isValidName("time")).isFalse();
            assertThat(ElementNameValidator.isValidName("DT")).isFalse();
            assertThat(ElementNameValidator.isValidName("IF")).isFalse();
            assertThat(ElementNameValidator.isValidName("AND")).isFalse();
            assertThat(ElementNameValidator.isValidName("or")).isFalse();
        }

        @Test
        void shouldAcceptReservedWordAsSubstring() {
            assertThat(ElementNameValidator.isValidName("Time Delay")).isTrue();
            assertThat(ElementNameValidator.isValidName("IF Rate")).isTrue();
        }

        @Test
        void shouldRejectNameExceedingMaxLength() {
            String longName = "A".repeat(ElementNameValidator.MAX_NAME_LENGTH + 1);
            assertThat(ElementNameValidator.isValidName(longName)).isFalse();
        }

        @Test
        void shouldAcceptNameAtMaxLength() {
            String maxName = "A".repeat(ElementNameValidator.MAX_NAME_LENGTH);
            assertThat(ElementNameValidator.isValidName(maxName)).isTrue();
        }
    }

    @Nested
    @DisplayName("resolveUniqueName")
    class ResolveUniqueName {

        @Test
        void shouldReturnOriginalNameWhenNotTaken() {
            Set<String> names = Set.of("Stock 1", "Flow 1");
            assertThat(ElementNameValidator.resolveUniqueName("Population", "Stock ", 2, names))
                    .isEqualTo("Population");
        }

        @Test
        void shouldAutoGenerateWhenOriginalIsTaken() {
            Set<String> names = Set.of("Population", "Stock 1");
            assertThat(ElementNameValidator.resolveUniqueName("Population", "Stock ", 2, names))
                    .isEqualTo("Stock 2");
        }

        @Test
        void shouldSkipTakenIds() {
            Set<String> names = Set.of("Population", "Stock 2", "Stock 3");
            assertThat(ElementNameValidator.resolveUniqueName("Population", "Stock ", 2, names))
                    .isEqualTo("Stock 4");
        }

        @Test
        void shouldAutoGenerateForNullOriginal() {
            Set<String> names = Set.of("Stock 1");
            assertThat(ElementNameValidator.resolveUniqueName(null, "Stock ", 2, names))
                    .isEqualTo("Stock 2");
        }

        @Test
        void shouldAutoGenerateForBlankOriginal() {
            Set<String> names = Set.of();
            assertThat(ElementNameValidator.resolveUniqueName("  ", "Stock ", 1, names))
                    .isEqualTo("Stock 1");
        }
    }

    @Nested
    @DisplayName("parseIdSuffix")
    class ParseIdSuffix {

        @Test
        void shouldParseNumericSuffix() {
            assertThat(ElementNameValidator.parseIdSuffix("Stock 3", "Stock ")).isEqualTo(3);
        }

        @Test
        void shouldReturnZeroForNonNumericSuffix() {
            assertThat(ElementNameValidator.parseIdSuffix("Stock abc", "Stock ")).isEqualTo(0);
        }

        @Test
        void shouldReturnZeroForMismatchedPrefix() {
            assertThat(ElementNameValidator.parseIdSuffix("Flow 5", "Stock ")).isEqualTo(0);
        }
    }
}
