package systems.courant.sd.app.canvas;

import javafx.scene.paint.Color;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import systems.courant.sd.app.canvas.SvgExporter;

@DisplayName("SvgExporter")
class SvgExporterTest {

    @Nested
    @DisplayName("svgColor")
    class SvgColor {

        @Test
        void shouldConvertBlackToHex() {
            assertThat(SvgExporter.svgColor(Color.BLACK)).isEqualTo("#000000");
        }

        @Test
        void shouldConvertWhiteToHex() {
            assertThat(SvgExporter.svgColor(Color.WHITE)).isEqualTo("#FFFFFF");
        }

        @Test
        void shouldConvertRedToHex() {
            assertThat(SvgExporter.svgColor(Color.RED)).isEqualTo("#FF0000");
        }

        @Test
        void shouldConvertCustomColorToHex() {
            Color custom = Color.rgb(64, 128, 255);
            assertThat(SvgExporter.svgColor(custom)).isEqualTo("#4080FF");
        }

        @Test
        void shouldIgnoreOpacityInHexOutput() {
            Color semiTransparent = Color.rgb(255, 0, 0, 0.5);
            assertThat(SvgExporter.svgColor(semiTransparent)).isEqualTo("#FF0000");
        }

        @Test
        void shouldConvertGreenToHex() {
            assertThat(SvgExporter.svgColor(Color.GREEN)).isEqualTo("#008000");
        }

        @Test
        void shouldConvertBlueToHex() {
            assertThat(SvgExporter.svgColor(Color.BLUE)).isEqualTo("#0000FF");
        }

        @Test
        void shouldTruncateFractionalRgbValues() {
            // Color.color(double r, double g, double b) accepts 0.0–1.0
            // 0.5 * 255 = 127.5, cast to int = 127 = 0x7F
            Color half = Color.color(0.5, 0.5, 0.5);
            assertThat(SvgExporter.svgColor(half)).isEqualTo("#7F7F7F");
        }

        @Test
        void shouldHandleFullyTransparentColor() {
            assertThat(SvgExporter.svgColor(Color.TRANSPARENT)).isEqualTo("#000000");
        }
    }

    @Nested
    @DisplayName("svgOpacity")
    class SvgOpacity {

        @Test
        void shouldReturnFullOpacityForOpaqueColor() {
            assertThat(SvgExporter.svgOpacity(Color.RED)).isEqualTo(1.0);
        }

        @Test
        void shouldReturnHalfOpacity() {
            Color semiTransparent = Color.rgb(0, 0, 0, 0.5);
            assertThat(SvgExporter.svgOpacity(semiTransparent)).isEqualTo(0.5);
        }

        @Test
        void shouldReturnZeroForTransparent() {
            assertThat(SvgExporter.svgOpacity(Color.TRANSPARENT)).isEqualTo(0.0);
        }

        @Test
        void shouldReturnSmallNonZeroOpacity() {
            Color almostTransparent = Color.rgb(0, 0, 0, 0.01);
            assertThat(SvgExporter.svgOpacity(almostTransparent)).isCloseTo(0.01, offset(1e-6));
        }

        @Test
        void shouldReturnNearFullOpacity() {
            Color almostOpaque = Color.rgb(0, 0, 0, 0.99);
            assertThat(SvgExporter.svgOpacity(almostOpaque)).isCloseTo(0.99, offset(1e-6));
        }

        @Test
        void shouldReturnQuarterOpacity() {
            Color quarterOpaque = Color.rgb(100, 200, 50, 0.25);
            assertThat(SvgExporter.svgOpacity(quarterOpaque)).isEqualTo(0.25);
        }
    }

    @Nested
    @DisplayName("escapeXml")
    class EscapeXml {

        @Test
        void shouldReturnEmptyForNull() {
            assertThat(SvgExporter.escapeXml(null)).isEmpty();
        }

        @Test
        void shouldPassThroughPlainText() {
            assertThat(SvgExporter.escapeXml("hello world")).isEqualTo("hello world");
        }

        @Test
        void shouldEscapeAmpersand() {
            assertThat(SvgExporter.escapeXml("A & B")).isEqualTo("A &amp; B");
        }

        @Test
        void shouldEscapeAngleBrackets() {
            assertThat(SvgExporter.escapeXml("x < y > z")).isEqualTo("x &lt; y &gt; z");
        }

        @Test
        void shouldEscapeQuotes() {
            assertThat(SvgExporter.escapeXml("say \"hi\" & 'bye'"))
                    .isEqualTo("say &quot;hi&quot; &amp; &apos;bye&apos;");
        }

        @Test
        void shouldHandleAllSpecialCharsTogether() {
            assertThat(SvgExporter.escapeXml("<a b=\"c\" d='e'>&"))
                    .isEqualTo("&lt;a b=&quot;c&quot; d=&apos;e&apos;&gt;&amp;");
        }

        @Test
        void shouldReturnEmptyForEmptyString() {
            assertThat(SvgExporter.escapeXml("")).isEmpty();
        }

        @Test
        void shouldPreserveUnicodeCharacters() {
            assertThat(SvgExporter.escapeXml("\u00B5 \u2264 \u03B1")).isEqualTo("\u00B5 \u2264 \u03B1");
        }

        @Test
        void shouldPreserveNewlinesAndWhitespace() {
            assertThat(SvgExporter.escapeXml("line1\nline2\ttab"))
                    .isEqualTo("line1\nline2\ttab");
        }

        @Test
        void shouldEscapeConsecutiveAmpersands() {
            assertThat(SvgExporter.escapeXml("&&&&")).isEqualTo("&amp;&amp;&amp;&amp;");
        }

        @Test
        void shouldEscapeStringContainingOnlySpecialChars() {
            assertThat(SvgExporter.escapeXml("<>&\"'"))
                    .isEqualTo("&lt;&gt;&amp;&quot;&apos;");
        }

        @Test
        void shouldHandleRepeatedEscapeSequenceLikeStrings() {
            // Ensure no double-escaping: input contains literal "&amp;"
            assertThat(SvgExporter.escapeXml("&amp;")).isEqualTo("&amp;amp;");
        }
    }
}
