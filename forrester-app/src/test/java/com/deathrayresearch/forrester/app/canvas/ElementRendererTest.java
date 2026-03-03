package com.deathrayresearch.forrester.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ElementRenderer")
class ElementRendererTest {

    @Nested
    @DisplayName("formatValue")
    class FormatValue {

        @Test
        void shouldFormatWholeNumberWithoutDecimal() {
            assertThat(ElementRenderer.formatValue(42.0)).isEqualTo("42");
        }

        @Test
        void shouldFormatZero() {
            assertThat(ElementRenderer.formatValue(0.0)).isEqualTo("0");
        }

        @Test
        void shouldFormatNegativeWholeNumber() {
            assertThat(ElementRenderer.formatValue(-5.0)).isEqualTo("-5");
        }

        @Test
        void shouldPreserveDecimalForFractionalValues() {
            assertThat(ElementRenderer.formatValue(3.14)).isEqualTo("3.14");
        }

        @Test
        void shouldPreserveSmallDecimal() {
            assertThat(ElementRenderer.formatValue(0.1)).isEqualTo("0.1");
        }

        @Test
        void shouldFormatLargeWholeNumber() {
            assertThat(ElementRenderer.formatValue(1000.0)).isEqualTo("1000");
        }
    }
}
