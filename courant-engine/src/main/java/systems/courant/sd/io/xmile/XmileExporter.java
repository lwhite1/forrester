package systems.courant.sd.io.xmile;

import systems.courant.sd.io.ExportUtils;
import systems.courant.sd.io.FormatUtils;
import systems.courant.sd.io.ModelExporter;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.SimulationSettings;
import systems.courant.sd.model.def.StockDef;

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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
public final class XmileExporter implements ModelExporter {

    private static final int MAX_MODULE_DEPTH = 50;

    public XmileExporter() {
    }

    @Override
    public String export(ModelDefinition definition) {
        return toXmile(definition);
    }

    @Override
    public void exportToFile(ModelDefinition definition, Path path) throws IOException {
        toFile(definition, path);
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
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
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

        // Write named <model> elements for module definitions (before the main model)
        Set<String> writtenModels = new LinkedHashSet<>();
        writeModuleModels(doc, root, def, writtenModels, 0);

        // Main <model>
        Element modelElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.MODEL);
        root.appendChild(modelElem);

        writeVariables(doc, modelElem, def);

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
        vendor.setTextContent("Courant");
        header.appendChild(vendor);

        Element product = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.PRODUCT);
        product.setTextContent("Courant Library");
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
        start.setTextContent(formatDouble(sim.initialTime()));
        simSpecs.appendChild(start);

        Element stop = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.STOP);
        stop.setTextContent(formatDouble(sim.initialTime() + sim.duration()));
        simSpecs.appendChild(stop);

        Element dt = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.DT);
        dt.setTextContent(formatDouble(sim.dt()));
        simSpecs.appendChild(dt);
    }

    private static void writeVariables(Document doc, Element modelElem, ModelDefinition def) {
        Element variablesElem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.VARIABLES);
        modelElem.appendChild(variablesElem);

        Set<String> embeddedLookupNames = ExportUtils.collectEmbeddedLookupNames(def);

        for (StockDef stock : def.stocks()) {
            writeStock(doc, variablesElem, stock, def);
        }
        for (FlowDef flow : def.flows()) {
            writeFlow(doc, variablesElem, flow, def, embeddedLookupNames);
        }
        for (VariableDef v : def.variables()) {
            writeVariable(doc, variablesElem, v, def, embeddedLookupNames);
        }
        for (LookupTableDef lookup : def.lookupTables()) {
            if (!embeddedLookupNames.contains(lookup.name())) {
                writeLookupAsAux(doc, variablesElem, lookup);
            }
        }
        for (ModuleInstanceDef module : def.modules()) {
            writeModule(doc, variablesElem, module);
        }
    }

    /**
     * Writes named {@code <model>} elements for all module definitions,
     * recursively handling nested modules.
     */
    private static void writeModuleModels(Document doc, Element root,
                                           ModelDefinition def,
                                           Set<String> writtenModels,
                                           int depth) {
        if (depth > MAX_MODULE_DEPTH) {
            throw new IllegalStateException(
                    "Module nesting depth exceeds maximum of " + MAX_MODULE_DEPTH);
        }
        for (ModuleInstanceDef mod : def.modules()) {
            String modelName = mod.definition().name();
            if (writtenModels.contains(modelName)) {
                continue;
            }
            writtenModels.add(modelName);

            // Recurse first so nested module definitions appear before their parents
            writeModuleModels(doc, root, mod.definition(), writtenModels, depth + 1);

            Element modelElem = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.MODEL);
            modelElem.setAttribute(XmileConstants.ATTR_NAME, modelName);
            root.appendChild(modelElem);

            writeVariables(doc, modelElem, mod.definition());
        }
    }

    private static void writeModule(Document doc, Element variablesElem,
                                     ModuleInstanceDef module) {
        Element elem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.MODULE);
        elem.setAttribute(XmileConstants.ATTR_NAME, module.instanceName());

        // Input bindings: <connect to="inner_port" from="outer_var"/>
        for (Map.Entry<String, String> entry : module.inputBindings().entrySet()) {
            Element connect = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.CONNECT);
            connect.setAttribute(XmileConstants.ATTR_TO, entry.getKey());
            connect.setAttribute(XmileConstants.ATTR_FROM, entry.getValue());
            elem.appendChild(connect);
        }

        // Output bindings: <connect to=".outer_alias" from="inner_var"/>
        for (Map.Entry<String, String> entry : module.outputBindings().entrySet()) {
            Element connect = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.CONNECT);
            connect.setAttribute(XmileConstants.ATTR_TO, "." + entry.getValue());
            connect.setAttribute(XmileConstants.ATTR_FROM, entry.getKey());
            elem.appendChild(connect);
        }

        variablesElem.appendChild(elem);
    }

    private static void writeStock(Document doc, Element variablesElem,
                                    StockDef stock, ModelDefinition def) {
        Element elem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.STOCK);
        elem.setAttribute(XmileConstants.ATTR_NAME, stock.name());

        // <eqn> — initial value (prefer expression over numeric constant)
        Element eqn = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.EQN);
        if (stock.initialExpression() != null && !stock.initialExpression().isBlank()) {
            eqn.setTextContent(XmileExprTranslator.toXmile(stock.initialExpression()));
        } else {
            eqn.setTextContent(formatDouble(stock.initialValue()));
        }
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

        // <doc>
        if (stock.comment() != null && !stock.comment().isBlank()) {
            Element docElem = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, "doc");
            docElem.setTextContent(stock.comment());
            elem.appendChild(docElem);
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

        // <doc>
        if (flow.comment() != null && !flow.comment().isBlank()) {
            Element docElem = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, "doc");
            docElem.setTextContent(flow.comment());
            elem.appendChild(docElem);
        }

        variablesElem.appendChild(elem);
    }

    private static void writeVariable(Document doc, Element variablesElem,
                                  VariableDef v, ModelDefinition def,
                                  Set<String> embeddedLookupNames) {
        Element elem = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.AUX);
        elem.setAttribute(XmileConstants.ATTR_NAME, v.name());

        // Check if this variable references a lookup — if so, embed the gf
        Optional<String> lookupNameOpt = ExportUtils.extractLookupReference(v.equation());
        if (lookupNameOpt.isPresent()) {
            Optional<LookupTableDef> lookupOpt = ExportUtils.findLookup(def, lookupNameOpt.get());
            if (lookupOpt.isPresent()) {
                // Extract the input expression from LOOKUP(name, input)
                String inputExpr = ExportUtils.extractLookupInput(v.equation())
                        .orElse(v.equation());
                Element eqn = doc.createElementNS(
                        XmileConstants.NAMESPACE_URI, XmileConstants.EQN);
                eqn.setTextContent(XmileExprTranslator.toXmile(inputExpr));
                elem.appendChild(eqn);
                writeGf(doc, elem, lookupOpt.get());
                variablesElem.appendChild(elem);
                return;
            }
        }

        // Regular variable
        Element eqn = doc.createElementNS(
                XmileConstants.NAMESPACE_URI, XmileConstants.EQN);
        eqn.setTextContent(XmileExprTranslator.toXmile(v.equation()));
        elem.appendChild(eqn);

        // <units>
        if (v.unit() != null && !v.unit().isBlank()) {
            Element units = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, XmileConstants.UNITS);
            units.setTextContent(v.unit());
            elem.appendChild(units);
        }

        // <doc>
        if (v.comment() != null && !v.comment().isBlank()) {
            Element docElem = doc.createElementNS(
                    XmileConstants.NAMESPACE_URI, "doc");
            docElem.setTextContent(v.comment());
            elem.appendChild(docElem);
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

        if (xVals.length == 0) {
            parent.appendChild(gf);
            return;
        }

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
            if (!Double.isNaN(y)) {
                ymin = Math.min(ymin, y);
                ymax = Math.max(ymax, y);
            }
        }
        if (ymin == Double.MAX_VALUE) {
            ymin = 0;
            ymax = 0;
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

    private static String joinDoubles(double[] values) {
        StringJoiner joiner = new StringJoiner(",");
        for (double v : values) {
            joiner.add(formatDouble(v));
        }
        return joiner.toString();
    }

    private static String formatDouble(double value) {
        return FormatUtils.formatDouble(value);
    }

    private static String serialize(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
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
