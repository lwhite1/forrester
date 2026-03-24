package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
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
            assertThat(ChartUtils.formatNumber(0.000001)).isEqualTo("1.00000e-06");
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
    @DisplayName("filterSimulationSettings() (#880)")
    class FilterSimulationSettings {

        @Test
        @DisplayName("filters out simulation settings variable names")
        void shouldFilterSettingsNames() {
            List<String> names = List.of("Population", "TIME_STEP", "INITIAL_TIME",
                    "FINAL_TIME", "SAVEPER", "Birth Rate");
            assertThat(ChartUtils.filterSimulationSettings(names))
                    .containsExactly("Population", "Birth Rate");
        }

        @Test
        @DisplayName("filters space-separated variants")
        void shouldFilterSpaceSeparatedNames() {
            List<String> names = List.of("Tank", "TIME STEP", "INITIAL TIME", "FINAL TIME");
            assertThat(ChartUtils.filterSimulationSettings(names))
                    .containsExactly("Tank");
        }

        @Test
        @DisplayName("returns all names when no settings present")
        void shouldReturnAllWhenNoSettings() {
            List<String> names = List.of("Stock A", "Flow B", "Var C");
            assertThat(ChartUtils.filterSimulationSettings(names))
                    .containsExactly("Stock A", "Flow B", "Var C");
        }

        @Test
        @DisplayName("isSimulationSetting returns false for normal names")
        void shouldNotFlagNormalNames() {
            assertThat(ChartUtils.isSimulationSetting("Population")).isFalse();
            assertThat(ChartUtils.isSimulationSetting("Time")).isFalse();
        }

        @Test
        @DisplayName("isSimulationSetting returns true for all known settings")
        void shouldFlagAllKnownSettings() {
            assertThat(ChartUtils.isSimulationSetting("TIME_STEP")).isTrue();
            assertThat(ChartUtils.isSimulationSetting("INITIAL_TIME")).isTrue();
            assertThat(ChartUtils.isSimulationSetting("FINAL_TIME")).isTrue();
            assertThat(ChartUtils.isSimulationSetting("SAVEPER")).isTrue();
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

    @Nested
    @DisplayName("subscript column helpers")
    class SubscriptColumnHelpers {

        @Test
        void shouldExtractBaseNameFromBracketedColumn() {
            assertThat(ChartUtils.baseElementName("Population[North]")).isEqualTo("Population");
            assertThat(ChartUtils.baseElementName("Population[North,Young]")).isEqualTo("Population");
        }

        @Test
        void shouldReturnNameUnchangedForScalarColumn() {
            assertThat(ChartUtils.baseElementName("Population")).isEqualTo("Population");
        }

        @Test
        void shouldDetectSubscriptedColumn() {
            assertThat(ChartUtils.isSubscriptedColumn("Population[North]")).isTrue();
            assertThat(ChartUtils.isSubscriptedColumn("Population")).isFalse();
        }
    }
}
