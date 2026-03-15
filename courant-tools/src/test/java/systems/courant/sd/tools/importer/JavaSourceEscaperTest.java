package systems.courant.sd.tools.importer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceEscaperTest {

    @Nested
    class EscapeString {

        @Test
        void shouldReturnNullTokenForNullInput() {
            assertThat(JavaSourceEscaper.escapeString(null)).isEqualTo("null");
        }

        @Test
        void shouldWrapSimpleStringInQuotes() {
            assertThat(JavaSourceEscaper.escapeString("hello")).isEqualTo("\"hello\"");
        }

        @Test
        void shouldEscapeQuotes() {
            assertThat(JavaSourceEscaper.escapeString("say \"hi\""))
                    .isEqualTo("\"say \\\"hi\\\"\"");
        }

        @Test
        void shouldEscapeBackslash() {
            assertThat(JavaSourceEscaper.escapeString("a\\b")).isEqualTo("\"a\\\\b\"");
        }

        @Test
        void shouldEscapeNewlineAndTab() {
            assertThat(JavaSourceEscaper.escapeString("a\nb\tc"))
                    .isEqualTo("\"a\\nb\\tc\"");
        }
    }

    @Nested
    class DoubleArrayLiteral {

        @Test
        void shouldReturnNullForNullArray() {
            assertThat(JavaSourceEscaper.doubleArrayLiteral(null)).isEqualTo("null");
        }

        @Test
        void shouldFormatEmptyArray() {
            assertThat(JavaSourceEscaper.doubleArrayLiteral(new double[]{}))
                    .isEqualTo("new double[]{}");
        }

        @Test
        void shouldFormatArrayWithValues() {
            assertThat(JavaSourceEscaper.doubleArrayLiteral(new double[]{1.0, 2.5, 3.7}))
                    .isEqualTo("new double[]{1.0, 2.5, 3.7}");
        }
    }

    @Nested
    class ToPascalCase {

        @Test
        void shouldConvertSpaceSeparated() {
            assertThat(JavaSourceEscaper.toPascalCase("my model name")).isEqualTo("MyModelName");
        }

        @Test
        void shouldConvertHyphenSeparated() {
            assertThat(JavaSourceEscaper.toPascalCase("SIR-model")).isEqualTo("SIRModel");
        }

        @Test
        void shouldPreserveAllUppercaseAcronyms() {
            assertThat(JavaSourceEscaper.toPascalCase("HIV AIDS model")).isEqualTo("HIVAIDSModel");
            assertThat(JavaSourceEscaper.toPascalCase("GDP growth")).isEqualTo("GDPGrowth");
            assertThat(JavaSourceEscaper.toPascalCase("SIR")).isEqualTo("SIR");
        }

        @Test
        void shouldNotPreserveSingleCharUppercase() {
            // Single uppercase chars are just capitalized, not "acronyms"
            assertThat(JavaSourceEscaper.toPascalCase("A-B-C")).isEqualTo("ABC");
        }

        @Test
        void shouldConvertUnderscoreSeparated() {
            assertThat(JavaSourceEscaper.toPascalCase("predator_prey")).isEqualTo("PredatorPrey");
        }

        @Test
        void shouldReturnEmptyForBlank() {
            assertThat(JavaSourceEscaper.toPascalCase("")).isEqualTo("");
            assertThat(JavaSourceEscaper.toPascalCase(null)).isEqualTo("");
        }
    }

    @Nested
    class ToPackageSegment {

        @Test
        void shouldLowercaseAndStripNonAlphanumeric() {
            assertThat(JavaSourceEscaper.toPackageSegment("Supply Chain"))
                    .isEqualTo("supplychain");
        }

        @Test
        void shouldHandleSimpleWord() {
            assertThat(JavaSourceEscaper.toPackageSegment("Epidemiology"))
                    .isEqualTo("epidemiology");
        }

        @Test
        void shouldReturnEmptyForBlank() {
            assertThat(JavaSourceEscaper.toPackageSegment("")).isEqualTo("");
            assertThat(JavaSourceEscaper.toPackageSegment(null)).isEqualTo("");
        }
    }

    @Nested
    class ToValidIdentifier {

        @Test
        void shouldPrefixDigitStartingName() {
            assertThat(JavaSourceEscaper.toValidIdentifier("3rd Stage"))
                    .isEqualTo("_3rdstage");
        }

        @Test
        void shouldNotPrefixLetterStartingName() {
            assertThat(JavaSourceEscaper.toValidIdentifier("my module"))
                    .isEqualTo("mymodule");
        }

        @Test
        void shouldHandleAllDigitName() {
            assertThat(JavaSourceEscaper.toValidIdentifier("123"))
                    .isEqualTo("_123");
        }

        @Test
        void shouldReturnEmptyForBlank() {
            assertThat(JavaSourceEscaper.toValidIdentifier("")).isEqualTo("");
            assertThat(JavaSourceEscaper.toValidIdentifier(null)).isEqualTo("");
        }
    }
}
