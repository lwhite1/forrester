package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportTemplate")
class ReportTemplateTest {

    @Test
    @DisplayName("defaults() should create template with all nulls")
    void defaultsShouldBeAllNulls() {
        ReportTemplate t = ReportTemplate.defaults();
        assertThat(t.customCss()).isNull();
        assertThat(t.headerHtml()).isNull();
        assertThat(t.footerHtml()).isNull();
    }

    @Test
    @DisplayName("withCss() should return new template with CSS set")
    void withCssShouldWork() {
        ReportTemplate t = ReportTemplate.defaults().withCss("body { color: red; }");
        assertThat(t.customCss()).isEqualTo("body { color: red; }");
        assertThat(t.headerHtml()).isNull();
    }

    @Test
    @DisplayName("withHeader() should return new template with header set")
    void withHeaderShouldWork() {
        ReportTemplate t = ReportTemplate.defaults().withHeader("<h1>Logo</h1>");
        assertThat(t.headerHtml()).isEqualTo("<h1>Logo</h1>");
        assertThat(t.customCss()).isNull();
    }

    @Test
    @DisplayName("withFooter() should return new template with footer set")
    void withFooterShouldWork() {
        ReportTemplate t = ReportTemplate.defaults().withFooter("Page footer");
        assertThat(t.footerHtml()).isEqualTo("Page footer");
    }

    @Test
    @DisplayName("builder methods should be chainable")
    void shouldBeChainable() {
        ReportTemplate t = ReportTemplate.defaults()
                .withCss("h1 { font-size: 2em; }")
                .withHeader("My Report")
                .withFooter("2026");
        assertThat(t.customCss()).contains("font-size");
        assertThat(t.headerHtml()).isEqualTo("My Report");
        assertThat(t.footerHtml()).isEqualTo("2026");
    }
}
