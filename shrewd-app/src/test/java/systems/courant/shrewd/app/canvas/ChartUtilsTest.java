package systems.courant.shrewd.app.canvas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChartUtils")
class ChartUtilsTest {

    @Nested
    @DisplayName("formatNumber()")
    class FormatNumber {

        @Test
        @DisplayName("Integer values are formatted without decimals")
        void integerValues() {
            assertThat(ChartUtils.formatNumber(42.0)).isEqualTo("42");
            assertThat(ChartUtils.formatNumber(0.0)).isEqualTo("0");
            assertThat(ChartUtils.formatNumber(-7.0)).isEqualTo("-7");
        }

        @Test
        @DisplayName("Fractional values are formatted with 4 decimal places")
        void fractionalValues() {
            assertThat(ChartUtils.formatNumber(3.14159)).isEqualTo("3.1416");
            assertThat(ChartUtils.formatNumber(0.1)).isEqualTo("0.1000");
        }

        @Test
        @DisplayName("Very small fractional values show decimals")
        void smallFractionalValues() {
            assertThat(ChartUtils.formatNumber(0.0001)).isEqualTo("0.0001");
        }

        @Test
        @DisplayName("Large integer values are formatted without decimals")
        void largeIntegers() {
            assertThat(ChartUtils.formatNumber(1000000.0)).isEqualTo("1000000");
        }

        @Test
        @DisplayName("Negative fractional values are formatted correctly")
        void negativeFractional() {
            assertThat(ChartUtils.formatNumber(-2.5)).isEqualTo("-2.5000");
        }

        @Test
        @DisplayName("NaN is formatted with decimals (not integer path)")
        void nanValue() {
            String result = ChartUtils.formatNumber(Double.NaN);
            assertThat(result).isEqualTo("NaN");
        }

        @Test
        @DisplayName("Infinity is formatted with decimals (not integer path)")
        void infinityValue() {
            String result = ChartUtils.formatNumber(Double.POSITIVE_INFINITY);
            // Double.isFinite returns false, so it goes through the format path
            assertThat(result).contains("Infinity");
        }
    }

    @Nested
    @DisplayName("formatNumber() locale independence (#188)")
    class FormatNumberLocale {

        private Locale originalLocale;

        @BeforeEach
        void saveLocale() {
            originalLocale = Locale.getDefault();
        }

        @AfterEach
        void restoreLocale() {
            Locale.setDefault(originalLocale);
        }

        @Test
        @DisplayName("Fractional values use dot separator under German locale")
        void shouldUseDotSeparatorUnderGermanLocale() {
            Locale.setDefault(Locale.GERMANY);
            assertThat(ChartUtils.formatNumber(3.14159)).isEqualTo("3.1416");
            assertThat(ChartUtils.formatNumber(0.1)).isEqualTo("0.1000");
        }

        @Test
        @DisplayName("Fractional values use dot separator under French locale")
        void shouldUseDotSeparatorUnderFrenchLocale() {
            Locale.setDefault(Locale.FRANCE);
            assertThat(ChartUtils.formatNumber(1234.5678)).isEqualTo("1234.5678");
            assertThat(ChartUtils.formatNumber(-2.5)).isEqualTo("-2.5000");
        }

        @Test
        @DisplayName("Integer values unaffected by locale")
        void shouldFormatIntegersUnderNonUsLocale() {
            Locale.setDefault(Locale.GERMANY);
            assertThat(ChartUtils.formatNumber(42.0)).isEqualTo("42");
            assertThat(ChartUtils.formatNumber(0.0)).isEqualTo("0");
        }
    }

    @Nested
    @DisplayName("constants")
    class Constants {

        @Test
        @DisplayName("SERIES_COLORS has 10 entries")
        void seriesColorsSize() {
            assertThat(ChartUtils.SERIES_COLORS).hasSize(10);
        }

        @Test
        @DisplayName("GHOST_COLORS has 5 entries")
        void ghostColorsSize() {
            assertThat(ChartUtils.GHOST_COLORS).hasSize(5);
        }

        @Test
        @DisplayName("GHOST_OPACITY is between 0 and 1")
        void ghostOpacityRange() {
            assertThat(ChartUtils.GHOST_OPACITY).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Series colors are valid hex color strings")
        void seriesColorsAreHex() {
            for (String color : ChartUtils.SERIES_COLORS) {
                assertThat(color).matches("#[0-9a-fA-F]{6}");
            }
        }
    }
}
