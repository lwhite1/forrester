package systems.courant.sd.io;

import systems.courant.sd.model.def.ReferenceDataset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReferenceDataCsvReader")
class ReferenceDataCsvReaderTest {

    @Nested
    @DisplayName("Valid CSV parsing")
    class ValidCsv {

        @Test
        @DisplayName("should parse two-column CSV")
        void shouldParseTwoColumnCsv() throws IOException {
            String csv = """
                    Time,Population
                    0,100
                    1,110
                    2,120
                    """;
            ReferenceDataset ds = ReferenceDataCsvReader.read(new StringReader(csv), "Test");
            assertThat(ds.name()).isEqualTo("Test");
            assertThat(ds.timeValues()).containsExactly(0, 1, 2);
            assertThat(ds.columns().get("Population")).containsExactly(100, 110, 120);
        }

        @Test
        @DisplayName("should parse multi-column CSV")
        void shouldParseMultiColumnCsv() throws IOException {
            String csv = """
                    Time,Prey,Predator
                    0,100,10
                    1,120,12
                    2,110,18
                    """;
            ReferenceDataset ds = ReferenceDataCsvReader.read(new StringReader(csv), "Ecology");
            assertThat(ds.variableNames()).containsExactly("Prey", "Predator");
            assertThat(ds.timeValues()).containsExactly(0, 1, 2);
            assertThat(ds.columns().get("Prey")).containsExactly(100, 120, 110);
            assertThat(ds.columns().get("Predator")).containsExactly(10, 12, 18);
        }

        @Test
        @DisplayName("should handle decimal values")
        void shouldHandleDecimalValues() throws IOException {
            String csv = """
                    Time,Rate
                    0.5,3.14
                    1.5,2.72
                    """;
            ReferenceDataset ds = ReferenceDataCsvReader.read(new StringReader(csv), "Decimals");
            assertThat(ds.timeValues()).containsExactly(0.5, 1.5);
            assertThat(ds.columns().get("Rate")).containsExactly(3.14, 2.72);
        }

        @Test
        @DisplayName("should trim whitespace in headers and values")
        void shouldTrimWhitespace() throws IOException {
            String csv = """
                    Time , Population
                    0 , 100
                    1 , 110
                    """;
            ReferenceDataset ds = ReferenceDataCsvReader.read(new StringReader(csv), "Trimmed");
            assertThat(ds.variableNames()).containsExactly("Population");
            assertThat(ds.timeValues()).containsExactly(0, 1);
        }

        @Test
        @DisplayName("should skip empty lines")
        void shouldSkipEmptyLines() throws IOException {
            String csv = "Time,X\n0,1\n\n1,2\n";
            ReferenceDataset ds = ReferenceDataCsvReader.read(new StringReader(csv), "Gaps");
            assertThat(ds.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should treat blank cells as NaN")
        void shouldTreatBlankCellsAsNaN() throws IOException {
            String csv = """
                    Time,X
                    0,1
                    1,
                    2,3
                    """;
            ReferenceDataset ds = ReferenceDataCsvReader.read(new StringReader(csv), "NaN");
            assertThat(ds.columns().get("X")[1]).isNaN();
        }
    }

    @Nested
    @DisplayName("Invalid CSV handling")
    class InvalidCsv {

        @Test
        @DisplayName("should reject single-column CSV")
        void shouldRejectSingleColumn() {
            String csv = "Time\n0\n1\n";
            assertThatThrownBy(() ->
                    ReferenceDataCsvReader.read(new StringReader(csv), "Bad"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("at least two columns");
        }

        @Test
        @DisplayName("should reject empty CSV")
        void shouldRejectEmptyCsv() {
            assertThatThrownBy(() ->
                    ReferenceDataCsvReader.read(new StringReader(""), "Bad"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("at least two columns");
        }

        @Test
        @DisplayName("should reject header-only CSV")
        void shouldRejectHeaderOnly() {
            String csv = "Time,X\n";
            assertThatThrownBy(() ->
                    ReferenceDataCsvReader.read(new StringReader(csv), "Bad"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("no data rows");
        }

        @Test
        @DisplayName("should reject non-numeric values")
        void shouldRejectNonNumeric() {
            String csv = "Time,X\n0,abc\n";
            assertThatThrownBy(() ->
                    ReferenceDataCsvReader.read(new StringReader(csv), "Bad"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Invalid number")
                    .hasMessageContaining("abc");
        }
    }
}
