package com.deathrayresearch.forrester.app.canvas;

import javafx.scene.paint.Color;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    }
}
