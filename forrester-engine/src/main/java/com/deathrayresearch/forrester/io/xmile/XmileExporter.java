package com.deathrayresearch.forrester.io.xmile;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.StockDef;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Exports a {@link ModelDefinition} to XMILE XML format.
 *
 * <p>Generates valid XMILE 1.0 XML that can be imported by Stella/iThink and
 * other XMILE-compatible system dynamics tools.
 *
 * <p>Usage:
 * <pre>{@code
 * String xml = XmileExporter.toXmile(modelDefinition);
 * XmileExporter.toFile(modelDefinition, Path.of("model.xmile"));
 * }</pre>
 */
public final class XmileExporter {

    private XmileExporter() {
    }

    /**
     * Exports a model definition to an XMILE XML string.
     *
     * @param def the model definition to export
     * @return the XMILE XML string
     */
    public static String toXmile(ModelDefinition def) {
        try {
            Document doc = buildDocument(def);
            return serialize(doc);
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IllegalStateException("Failed to generate XMILE XML: " + e.getMessage(), e);
        }
    }

    /**
     * Exports a model definition to an XMILE file.
     *
     * @param def the model definition to export
     * @param path the output file path
     * @throws IOException if the file cannot be written
     */
    public static void toFile(ModelDefinition def, Path path) throws IOException {
        String xml = toXmile(def);
        Files.writeString(path, xml, StandardCharsets.UTF_8);
    }

    private static Document buildDocument(ModelDefinition def)
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Root <xmile> element
        Element root = doc.createElementNS(XmileConstants.NAMESPACE_URI, XmileConstants.XMILE);
        root.setAttribute(XmileConstants.ATTR_VERSION, XmileConstants.VERSION_1_0);
        doc.appendChild(root);

        // <header>
        writeHeader(doc, root, def);

        // <sim_specs>
        writeSimSpecs(doc, root, def);

        // <model>
        Element modelElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.MODEL);
        root.appendChild(modelElem);

        // <variables>
        Element variablesElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.VARIABLES);
        modelElem.appendChild(variablesElem);

        // Collect lookup table names that are referenced by aux/flow (embedded gf)
        Set<String> embeddedLookupNames = collectEmbeddedLookupNames(def);

        // Write stocks
        for (StockDef stock : def.stocks()) {
            writeStock(doc, variablesElem, stock, def);
        }

        // Write flows
        for (FlowDef flow : def.flows()) {
            writeFlow(doc, variablesElem, flow, def, embeddedLookupNames);
        }

        // Write auxiliaries
        for (AuxDef aux : def.auxiliaries()) {
            writeAux(doc, variablesElem, aux, def, embeddedLookupNames);
        }

        // Write constants as <aux> with numeric eqn
        for (ConstantDef constant : def.constants()) {
            writeConstant(doc, variablesElem, constant);
        }

        // Write standalone lookup tables as <aux> with <gf>
        for (LookupTableDef lookup : def.lookupTables()) {
            if (!embeddedLookupNames.contains(lookup.name())) {
                writeLookupAsAux(doc, variablesElem, lookup);
            }
        }

        // <views>
        if (!def.views().isEmpty()) {
            XmileViewWriter.write(doc, modelElem, def.views());
        }

        return doc;
    }

    private static void writeHeader(Document doc, Element root, ModelDefinition def) {
        Element header = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.HEADER);
        root.appendChild(header);

        Element name = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.NAME);
        name.setTextContent(def.name());
        header.appendChild(name);

        Element vendor = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.VENDOR);
        vendor.setTextContent("Forrester");
        header.appendChild(vendor);

        Element product = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.PRODUCT);
        product.setTextContent("Forrester Library");
        header.appendChild(product);
    }

    private static void writeSimSpecs(Document doc, Element root, ModelDefinition def) {
        SimulationSettings sim = def.defaultSimulation();
        if (sim == null) {
            return;
        }

        Element simSpecs = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.SIM_SPECS);
        simSpecs.setAttribute(XmileConstants.ATTR_TIME_UNITS,
                sim.durationUnit().toLowerCase(Locale.ROOT));
        root.appendChild(simSpecs);

        Element start = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.START);
        start.setTextContent("0");
        simSpecs.appendChild(start);

        Element stop = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.STOP);
        stop.setTextContent(formatDouble(sim.duration()));
        simSpecs.appendChild(stop);

        Element dt = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.DT);
        dt.setTextContent("1");
        simSpecs.appendChild(dt);
    }

    private static void writeStock(Document doc, Element variablesElem,
                                    StockDef stock, ModelDefinition def) {
        Element elem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.STOCK);
        elem.setAttribute(XmileConstants.ATTR_NAME, stock.name());

        // <eqn> — initial value
        Element eqn = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.EQN);
        eqn.setTextContent(formatDouble(stock.initialValue()));
        elem.appendChild(eqn);

        // <inflow> and <outflow> from flows that reference this stock
        for (FlowDef flow : def.flows()) {
            if (stock.name().equals(flow.sink())) {
                Element inflow = doc.createElementNS(
                        XmileConstants.NAMESPACE_URI, XmileConstants.INFLOW);
                inflow.setTextContent(flow.name());
                elem.appendChild(inflow);
            }
            if (stock.name().equals(flow.source())) {
                Element outflow = doc.createElementNS(
                        XmileConstants.NAMESPACE_URI, XmileConstants.OUTFLOW);
                outflow.setTextContent(flow.name());
                elem.appendChild(outflow);
            }
        }

        // <units>
        if (stock.unit() != null && !stock.unit().isBlank()) {
            Element units = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.UNITS);
            units.setTextContent(stock.unit());
            elem.appendChild(units);
        }

        // <non_negative>
        if ("CLAMP_TO_ZERO".equals(stock.negativeValuePolicy())) {
            Element nonNeg = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.NON_NEGATIVE);
            elem.appendChild(nonNeg);
        }

        variablesElem.appendChild(elem);
    }

    private static void writeFlow(Document doc, Element variablesElem,
                                   FlowDef flow, ModelDefinition def,
                                   Set<String> embeddedLookupNames) {
        Element elem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.FLOW);
        elem.setAttribute(XmileConstants.ATTR_NAME, flow.name());

        // <eqn>
        Element eqn = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.EQN);
        eqn.setTextContent(XmileExprTranslator.toXmile(flow.equation()));
        elem.appendChild(eqn);

        // <units>
        if (flow.timeUnit() != null && !flow.timeUnit().isBlank()) {
            Element units = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.UNITS);
            units.setTextContent(flow.timeUnit());
            elem.appendChild(units);
        }

        variablesElem.appendChild(elem);
    }

    private static void writeAux(Document doc, Element variablesElem,
                                  AuxDef aux, ModelDefinition def,
                                  Set<String> embeddedLookupNames) {
        Element elem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.AUX);
        elem.setAttribute(XmileConstants.ATTR_NAME, aux.name());

        // Check if this aux references a lookup — if so, embed the gf
        String lookupName = extractLookupReference(aux.equation());
        if (lookupName != null) {
            LookupTableDef lookup = findLookup(def, lookupName);
            if (lookup != null) {
                // Extract the input expression from LOOKUP(name, input)
                String inputExpr = extractLookupInput(aux.equation());
                if (inputExpr != null) {
                    Element eqn = doc.createElementNS(
                            XmileConstants.NAMESPACE_URI, XmileConstants.EQN);
                    eqn.setTextContent(XmileExprTranslator.toXmile(inputExpr));
                    elem.appendChild(eqn);
                }
                writeGf(doc, elem, lookup);
                variablesElem.appendChild(elem);
                return;
            }
        }

        // Regular auxiliary
        Element eqn = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.EQN);
        eqn.setTextContent(XmileExprTranslator.toXmile(aux.equation()));
        elem.appendChild(eqn);

        // <units>
        if (aux.unit() != null && !aux.unit().isBlank()) {
            Element units = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.UNITS);
            units.setTextContent(aux.unit());
            elem.appendChild(units);
        }

        variablesElem.appendChild(elem);
    }

    private static void writeConstant(Document doc, Element variablesElem,
                                       ConstantDef constant) {
        Element elem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.AUX);
        elem.setAttribute(XmileConstants.ATTR_NAME, constant.name());

        Element eqn = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.EQN);
        eqn.setTextContent(formatDouble(constant.value()));
        elem.appendChild(eqn);

        if (constant.unit() != null && !constant.unit().isBlank()) {
            Element units = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.UNITS);
            units.setTextContent(constant.unit());
            elem.appendChild(units);
        }

        variablesElem.appendChild(elem);
    }

    private static void writeLookupAsAux(Document doc, Element variablesElem,
                                          LookupTableDef lookup) {
        Element elem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.AUX);
        elem.setAttribute(XmileConstants.ATTR_NAME, lookup.name());

        writeGf(doc, elem, lookup);

        variablesElem.appendChild(elem);
    }

    private static void writeGf(Document doc, Element parent, LookupTableDef lookup) {
        Element gf = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.GF);

        double[] xVals = lookup.xValues();
        double[] yVals = lookup.yValues();

        // <xscale>
        Element xscale = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.XSCALE);
        xscale.setAttribute(XmileConstants.ATTR_MIN, formatDouble(xVals[0]));
        xscale.setAttribute(XmileConstants.ATTR_MAX, formatDouble(xVals[xVals.length - 1]));
        gf.appendChild(xscale);

        // <yscale>
        double ymin = Double.MAX_VALUE;
        double ymax = -Double.MAX_VALUE;
        for (double y : yVals) {
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }
        Element yscale = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.YSCALE);
        yscale.setAttribute(XmileConstants.ATTR_MIN, formatDouble(ymin));
        yscale.setAttribute(XmileConstants.ATTR_MAX, formatDouble(ymax));
        gf.appendChild(yscale);

        // <xpts>
        Element xpts = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.XPTS);
        xpts.setTextContent(joinDoubles(xVals));
        gf.appendChild(xpts);

        // <ypts>
        Element ypts = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.YPTS);
        ypts.setTextContent(joinDoubles(yVals));
        gf.appendChild(ypts);

        parent.appendChild(gf);
    }

    private static Set<String> collectEmbeddedLookupNames(ModelDefinition def) {
        Set<String> names = new HashSet<>();
        for (AuxDef aux : def.auxiliaries()) {
            String lookupName = extractLookupReference(aux.equation());
            if (lookupName != null) {
                names.add(lookupName);
            }
        }
        return names;
    }

    /**
     * Extracts the lookup table name from a LOOKUP(name, input) expression.
     */
    static String extractLookupReference(String equation) {
        if (equation == null) {
            return null;
        }
        String trimmed = equation.strip();
        if (!trimmed.toUpperCase().startsWith("LOOKUP(")) {
            return null;
        }
        int openParen = trimmed.indexOf('(');
        int comma = findTopLevelComma(trimmed, openParen + 1);
        if (comma < 0) {
            return null;
        }
        return trimmed.substring(openParen + 1, comma).strip();
    }

    /**
     * Extracts the input expression from a LOOKUP(name, input) expression.
     */
    static String extractLookupInput(String equation) {
        if (equation == null) {
            return null;
        }
        String trimmed = equation.strip();
        if (!trimmed.toUpperCase().startsWith("LOOKUP(")) {
            return null;
        }
        int openParen = trimmed.indexOf('(');
        int comma = findTopLevelComma(trimmed, openParen + 1);
        if (comma < 0) {
            return null;
        }
        int closeParen = trimmed.lastIndexOf(')');
        if (closeParen <= comma) {
            return null;
        }
        return trimmed.substring(comma + 1, closeParen).strip();
    }

    private static int findTopLevelComma(String content, int startPos) {
        int depth = 0;
        for (int i = startPos; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    return -1;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static LookupTableDef findLookup(ModelDefinition def, String name) {
        for (LookupTableDef lt : def.lookupTables()) {
            if (lt.name().equals(name)) {
                return lt;
            }
        }
        return null;
    }

    private static String joinDoubles(double[] values) {
        StringJoiner joiner = new StringJoiner(",");
        for (double v : values) {
            joiner.add(formatDouble(v));
        }
        return joiner.toString();
    }

    private static String formatDouble(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)
                && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String serialize(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
