package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PdfReportExporter")
class PdfReportExporterTest {

    @Nested
    @DisplayName("toXhtml()")
    class ToXhtml {

        @Test
        @DisplayName("should replace HTML5 doctype with XHTML doctype")
        void shouldReplaceDoctype() {
            String html = "<!DOCTYPE html>\n<html lang=\"en\">";
            String xhtml = PdfReportExporter.toXhtml(html);

            assertThat(xhtml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            assertThat(xhtml).contains("DTD XHTML 1.0 Strict");
            assertThat(xhtml).doesNotContain("<!DOCTYPE html>");
        }

        @Test
        @DisplayName("should add XHTML namespace to html element")
        void shouldAddXhtmlNamespace() {
            String html = "<!DOCTYPE html>\n<html lang=\"en\">";
            String xhtml = PdfReportExporter.toXhtml(html);

            assertThat(xhtml).contains("xmlns=\"http://www.w3.org/1999/xhtml\"");
        }

        @Test
        @DisplayName("should close self-closing meta tags")
        void shouldCloseMetaTags() {
            String html = "<meta charset=\"UTF-8\">";
            String xhtml = PdfReportExporter.toXhtml(html);

            assertThat(xhtml).contains("<meta charset=\"UTF-8\"/>");
        }

        @Test
        @DisplayName("should close br tags")
        void shouldCloseBrTags() {
            String html = "line1<br>line2";
            String xhtml = PdfReportExporter.toXhtml(html);

            assertThat(xhtml).contains("<br/>");
        }

        @Test
        @DisplayName("should close br tags with whitespace")
        void shouldCloseBrTagsWithWhitespace() {
            assertThat(PdfReportExporter.toXhtml("a<br >b")).contains("<br/>");
            assertThat(PdfReportExporter.toXhtml("a<br/>b")).contains("<br/>");
            assertThat(PdfReportExporter.toXhtml("a<br />b")).contains("<br/>");
        }

        @Test
        @DisplayName("should close hr tags with whitespace")
        void shouldCloseHrTagsWithWhitespace() {
            assertThat(PdfReportExporter.toXhtml("<hr >")).contains("<hr/>");
            assertThat(PdfReportExporter.toXhtml("<hr/>")).contains("<hr/>");
            assertThat(PdfReportExporter.toXhtml("<hr />")).contains("<hr/>");
        }

        @Test
        @DisplayName("should close meta tag without attributes")
        void shouldCloseMetaTagWithoutAttributes() {
            String xhtml = PdfReportExporter.toXhtml("<meta>");
            assertThat(xhtml).isEqualTo("<meta/>");
        }

        @Test
        @DisplayName("should not double-close already self-closed tags")
        void shouldNotDoubleCloseSelfClosedTags() {
            assertThat(PdfReportExporter.toXhtml("<meta charset=\"UTF-8\"/>"))
                    .isEqualTo("<meta charset=\"UTF-8\"/>");
            assertThat(PdfReportExporter.toXhtml("<br/>")).isEqualTo("<br/>");
        }

        @Test
        @DisplayName("should preserve existing content")
        void shouldPreserveContent() {
            String html = "<div class=\"container\"><p>Hello World</p></div>";
            String xhtml = PdfReportExporter.toXhtml(html);

            assertThat(xhtml).contains("<div class=\"container\"><p>Hello World</p></div>");
        }
    }
}
