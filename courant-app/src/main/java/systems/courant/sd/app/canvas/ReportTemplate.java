package systems.courant.sd.app.canvas;

/**
 * Customization template for generated reports. Allows user-provided CSS,
 * configurable header/footer content, and branding.
 *
 * @param customCss   additional CSS to inject after the default stylesheet (null to use defaults only)
 * @param headerHtml  HTML content for the report header area, e.g. logo and institution name (null to omit)
 * @param footerHtml  HTML content for the report footer area, e.g. date and disclaimer (null to omit)
 */
public record ReportTemplate(
        String customCss,
        String headerHtml,
        String footerHtml
) {

    /**
     * Returns a template with no customizations (default styling, no header/footer).
     */
    public static ReportTemplate defaults() {
        return new ReportTemplate(null, null, null);
    }

    /**
     * Returns a new template with the given custom CSS added.
     */
    public ReportTemplate withCss(String css) {
        return new ReportTemplate(css, headerHtml, footerHtml);
    }

    /**
     * Returns a new template with the given header HTML.
     */
    public ReportTemplate withHeader(String header) {
        return new ReportTemplate(customCss, header, footerHtml);
    }

    /**
     * Returns a new template with the given footer HTML.
     */
    public ReportTemplate withFooter(String footer) {
        return new ReportTemplate(customCss, headerHtml, footer);
    }
}
