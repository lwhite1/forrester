package systems.courant.sd.app.canvas;

import com.lowagie.text.DocumentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converts self-contained HTML reports to PDF using Flying Saucer (XHTML renderer)
 * backed by OpenPDF. The HTML must be well-formed XHTML for the renderer to process it.
 *
 * <p>The generated PDF preserves:
 * <ul>
 *   <li>All CSS styling (inline and embedded)</li>
 *   <li>SVG charts as vector graphics</li>
 *   <li>Table layouts and page breaks</li>
 *   <li>Print media query styles</li>
 * </ul>
 */
public final class PdfReportExporter {

    private static final Logger log = LoggerFactory.getLogger(PdfReportExporter.class);

    private PdfReportExporter() {
    }

    /**
     * Converts an HTML report string to PDF and writes it to the given path.
     *
     * @param html       the self-contained HTML report content
     * @param outputPath the file path to write the PDF to
     * @throws IOException if writing fails
     */
    public static void exportToPdf(String html, Path outputPath) throws IOException {
        String xhtml = toXhtml(html);
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            log.info("PDF report exported to {}", outputPath);
        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Converts the HTML report to a byte array containing the PDF.
     *
     * @param html the self-contained HTML report content
     * @return PDF bytes
     * @throws IOException if conversion fails
     */
    public static byte[] toPdfBytes(String html) throws IOException {
        String xhtml = toXhtml(html);
        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Converts our HTML5 report to well-formed XHTML that Flying Saucer can parse.
     * This handles common HTML5 patterns that are not valid XHTML.
     */
    static String toXhtml(String html) {
        String xhtml = html;

        // Replace HTML5 doctype with XHTML
        xhtml = xhtml.replace("<!DOCTYPE html>",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                        + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");

        // Add XHTML namespace if not present
        xhtml = xhtml.replace("<html lang=\"en\">",
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">");

        // Close self-closing tags that are not closed in HTML5
        xhtml = xhtml.replaceAll("<meta ([^>]*[^/])>", "<meta $1/>");
        xhtml = xhtml.replaceAll("<link ([^>]*[^/])>", "<link $1/>");
        xhtml = xhtml.replaceAll("<input ([^>]*[^/])>", "<input $1/>");
        xhtml = xhtml.replaceAll("<br>", "<br/>");
        xhtml = xhtml.replaceAll("<hr>", "<hr/>");
        xhtml = xhtml.replaceAll("<img ([^>]*[^/])>", "<img $1/>");

        return xhtml;
    }
}
