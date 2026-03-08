package systems.courant.forrester.io.xmile;

import systems.courant.forrester.io.ImportResult;
import systems.courant.forrester.io.ModelImporter;
import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.ConstantDef;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.LookupTableDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.def.ModuleInstanceDef;
import systems.courant.forrester.model.def.StockDef;
import systems.courant.forrester.model.def.ViewDef;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Imports XMILE format model files into Forrester
 * {@link systems.courant.forrester.model.def.ModelDefinition}.
 *
 * <p>Supports stocks, flows, auxiliaries, constants, lookup tables (standalone and
 * embedded {@code <gf>}), simulation settings ({@code <sim_specs>}), and view data.
 *
 * <p>Usage:
 * <pre>{@code
 * XmileImporter importer = new XmileImporter();
 * ImportResult result = importer.importModel(Path.of("model.xmile"));
 * if (!result.isClean()) {
 *     result.warnings().forEach(System.out::println);
 * }
 * ModelDefinition def = result.definition();
 * }</pre>
 */
public class XmileImporter implements ModelImporter {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> UNSUPPORTED_ELEMENTS = Set.of(
            XmileConstants.GROUP, XmileConstants.MACRO,
            XmileConstants.EVENT_POSTER);

    @Override
    public ImportResult importModel(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            throw new IOException("File exceeds maximum allowed size of "
                    + (MAX_FILE_SIZE / (1024 * 1024)) + " MB: " + path);
        }
        String content = Files.readString(path, StandardCharsets.UTF_8);
        Path fileName = path.getFileName();
        String modelName = fileName != null ? fileName.toString() : path.toString();
        int dotPos = modelName.lastIndexOf('.');
        if (dotPos > 0) {
            modelName = modelName.substring(0, dotPos);
        }
        return importModel(content, modelName);
    }

    @Override
    public ImportResult importModel(String content, String modelName) {
        List<String> warnings = new ArrayList<>();

        Document doc;
        try {
            doc = parseXml(content);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new IllegalArgumentException("Failed to parse XMILE XML: " + e.getMessage(), e);
        }

        Element root = doc.getDocumentElement();

        // Extract model name from header
        String headerName = getHeaderName(root);
        if (headerName != null && !headerName.isBlank()) {
            modelName = headerName;
        }

        // Extract simulation settings
        String timeUnit = "Month";
        double start = 0;
        double stop = 100;
        double dt = 1;
        Element simSpecs = getFirstChild(root, XmileConstants.SIM_SPECS);
        if (simSpecs != null) {
            String tu = simSpecs.getAttribute(XmileConstants.ATTR_TIME_UNITS);
            if (tu != null && !tu.isBlank()) {
                timeUnit = capitalizeFirst(tu);
            }
            start = getChildDouble(simSpecs, XmileConstants.START, 0);
            stop = getChildDouble(simSpecs, XmileConstants.STOP, 100);
            dt = getChildDouble(simSpecs, XmileConstants.DT, 1);
        }

        double duration = stop - start;
        if (duration <= 0) {
            warnings.add("stop (" + stop + ") <= start (" + start
                    + "), defaulting duration to 100");
            duration = 100;
        }

        if (dt != 1.0) {
            warnings.add("dt = " + dt
                    + " (Forrester uses fixed step; value preserved as metadata only)");
        }

        // Collect all <model> elements — named ones are module types
        Map<String, Element> namedModels = new HashMap<>();
        Element mainModelElem = null;
        List<Element> modelElems = getChildElements(root, XmileConstants.MODEL);
        for (Element me : modelElems) {
            String mName = me.getAttribute(XmileConstants.ATTR_NAME);
            if (mName != null && !mName.isBlank()) {
                namedModels.put(mName, me);
            } else if (mainModelElem == null) {
                mainModelElem = me;
            }
        }
        // If no unnamed model, use the first one
        if (mainModelElem == null && !modelElems.isEmpty()) {
            mainModelElem = modelElems.getFirst();
        }

        // Parse named models into definitions (for module references)
        Map<String, ModelDefinition> moduleDefinitions = new HashMap<>();
        for (Map.Entry<String, Element> entry : namedModels.entrySet()) {
            ModelDefinition moduleDef = parseModelElement(
                    entry.getValue(), entry.getKey(), timeUnit, moduleDefinitions, warnings);
            moduleDefinitions.put(entry.getKey(), moduleDef);
        }

        ModelDefinitionBuilder builder = new ModelDefinitionBuilder()
                .name(modelName)
                .defaultSimulation(timeUnit, duration, timeUnit);

        if (mainModelElem == null) {
            return new ImportResult(builder.build(), warnings);
        }

        // Parse the main model
        populateBuilder(mainModelElem, builder, timeUnit, moduleDefinitions, warnings);

        return new ImportResult(builder.build(), warnings);
    }

    private void buildStock(Element stockElem, String name,
                             ModelDefinitionBuilder builder, List<String> warnings) {
        String eqnText = getChildText(stockElem, XmileConstants.EQN);
        double initialValue = 0;
        if (eqnText != null && !eqnText.isBlank()) {
            if (isNumericLiteral(eqnText)) {
                initialValue = Double.parseDouble(eqnText.strip());
            } else {
                warnings.add("Non-literal initial value for stock '" + name + "': '"
                        + eqnText + "', defaulting to 0.0");
            }
        }

        String unit = getChildText(stockElem, XmileConstants.UNITS);
        String doc = getChildText(stockElem, XmileConstants.DOC);
        String comment = (doc != null && !doc.isBlank()) ? doc : name;

        String negPolicy = null;
        if (hasChild(stockElem, XmileConstants.NON_NEGATIVE)) {
            negPolicy = "CLAMP_TO_ZERO";
        }

        // Warn about unsupported stock types
        if ("true".equalsIgnoreCase(stockElem.getAttribute(XmileConstants.ATTR_CONVEYOR))) {
            warnings.add("Stock '" + name + "' is a conveyor stock (not supported, treated as standard stock)");
        }
        if ("true".equalsIgnoreCase(stockElem.getAttribute(XmileConstants.ATTR_QUEUE))) {
            warnings.add("Stock '" + name + "' is a queue stock (not supported, treated as standard stock)");
        }
        if ("true".equalsIgnoreCase(stockElem.getAttribute(XmileConstants.ATTR_OVEN))) {
            warnings.add("Stock '" + name + "' is an oven stock (not supported, treated as standard stock)");
        }

        // Warn about range specifications
        if (hasChild(stockElem, XmileConstants.RANGE)) {
            warnings.add("Range specification on stock '" + name + "' ignored");
        }

        builder.stock(new StockDef(name, comment, initialValue, unit, negPolicy));
    }

    private void buildFlow(Element flowElem, String name, String timeUnit,
                            Map<String, List<String>> stockInflows,
                            Map<String, List<String>> stockOutflows,
                            ModelDefinitionBuilder builder,
                            Set<String> lookupNames, List<String> warnings) {
        String eqnText = getChildText(flowElem, XmileConstants.EQN);
        if (eqnText == null || eqnText.isBlank()) {
            eqnText = "0";
            warnings.add("Flow '" + name + "' has no equation, defaulting to 0");
        }

        // Translate expression
        XmileExprTranslator.TranslationResult tr = XmileExprTranslator.toForrester(eqnText);
        warnings.addAll(tr.warnings());
        String equation = tr.expression();

        // Resolve source and sink from stock inflow/outflow declarations
        String source = null;
        String sink = null;

        for (Map.Entry<String, List<String>> entry : stockOutflows.entrySet()) {
            if (entry.getValue().contains(name)) {
                source = entry.getKey();
                break;
            }
        }
        for (Map.Entry<String, List<String>> entry : stockInflows.entrySet()) {
            if (entry.getValue().contains(name)) {
                sink = entry.getKey();
                break;
            }
        }

        // Check for embedded graphical function
        Element gfElem = getFirstChild(flowElem, XmileConstants.GF);
        if (gfElem != null) {
            buildGf(gfElem, name + "_lookup", builder, lookupNames, warnings);
        }

        // Warn about biflow (XMILE default is biflow; non_negative makes it uniflow)
        if (!hasChild(flowElem, XmileConstants.NON_NEGATIVE)) {
            warnings.add("Flow '" + name
                    + "' is a biflow (may allow negative values; Forrester treats all flows as unidirectional)");
        }

        // Warn about range specifications
        if (hasChild(flowElem, XmileConstants.RANGE)) {
            warnings.add("Range specification on flow '" + name + "' ignored");
        }

        // unit is available via getChildText(flowElem, XmileConstants.UNITS) but not yet used
        String doc = getChildText(flowElem, XmileConstants.DOC);
        String comment = (doc != null && !doc.isBlank()) ? doc : name;

        builder.flow(new FlowDef(name, comment, equation, timeUnit, source, sink));
    }

    private void buildAux(Element auxElem, String name,
                           ModelDefinitionBuilder builder,
                           Set<String> lookupNames, List<String> warnings) {
        String eqnText = getChildText(auxElem, XmileConstants.EQN);
        String unit = getChildText(auxElem, XmileConstants.UNITS);
        String doc = getChildText(auxElem, XmileConstants.DOC);
        String comment = (doc != null && !doc.isBlank()) ? doc : name;

        // Warn about range specifications
        if (hasChild(auxElem, XmileConstants.RANGE)) {
            warnings.add("Range specification on auxiliary '" + name + "' ignored");
        }

        // Check for embedded graphical function
        Element gfElem = getFirstChild(auxElem, XmileConstants.GF);
        if (gfElem != null) {
            String lookupTableName = name + "_lookup";
            buildGf(gfElem, lookupTableName, builder, lookupNames, warnings);

            // If there's also an eqn, create an aux that uses LOOKUP
            if (eqnText != null && !eqnText.isBlank()) {
                XmileExprTranslator.TranslationResult tr =
                        XmileExprTranslator.toForrester(eqnText);
                warnings.addAll(tr.warnings());
                String lookupExpr = "LOOKUP(" + lookupTableName + ", " + tr.expression() + ")";
                builder.aux(new AuxDef(name, comment, lookupExpr, unit));
            } else {
                // gf without eqn — just the lookup table is added, create aux referencing it
                builder.aux(new AuxDef(name, comment,
                        "LOOKUP(" + lookupTableName + ", TIME)", unit));
            }
            return;
        }

        if (eqnText == null || eqnText.isBlank()) {
            // Aux with no equation — treat as constant 0
            builder.constant(new ConstantDef(name, comment, 0, unit));
            warnings.add("Aux '" + name + "' has no equation, treated as constant 0");
            return;
        }

        // Numeric literal → constant
        if (isNumericLiteral(eqnText)) {
            builder.constant(new ConstantDef(name, comment,
                    Double.parseDouble(eqnText.strip()), unit));
            return;
        }

        // General auxiliary
        XmileExprTranslator.TranslationResult tr = XmileExprTranslator.toForrester(eqnText);
        warnings.addAll(tr.warnings());
        builder.aux(new AuxDef(name, comment, tr.expression(), unit));
    }

    private void buildGf(Element gfElem, String lookupName,
                          ModelDefinitionBuilder builder,
                          Set<String> lookupNames, List<String> warnings) {
        if (lookupNames.contains(lookupName)) {
            return;
        }

        // Warn about non-LINEAR interpolation modes
        String interpType = gfElem.getAttribute(XmileConstants.ATTR_TYPE);
        if (interpType != null && !interpType.isBlank()
                && !"continuous".equalsIgnoreCase(interpType)) {
            warnings.add("Graphical function '" + lookupName + "' uses interpolation type '"
                    + interpType + "' (only LINEAR/continuous is supported)");
        }

        double[] xValues = null;
        double[] yValues = null;

        // Try explicit xpts/ypts first
        String xptsText = getChildText(gfElem, XmileConstants.XPTS);
        String yptsText = getChildText(gfElem, XmileConstants.YPTS);

        if (yptsText != null && !yptsText.isBlank()) {
            yValues = parseCommaSeparated(yptsText);

            if (xptsText != null && !xptsText.isBlank()) {
                xValues = parseCommaSeparated(xptsText);
            } else {
                // Generate x values from xscale min/max
                Element xscale = getFirstChild(gfElem, XmileConstants.XSCALE);
                if (xscale != null && yValues != null) {
                    double xmin = parseDoubleAttr(xscale, XmileConstants.ATTR_MIN, 0);
                    double xmax = parseDoubleAttr(xscale, XmileConstants.ATTR_MAX, 1);
                    xValues = linspace(xmin, xmax, yValues.length);
                }
            }
        }

        if (xValues == null || yValues == null || xValues.length < 2 || yValues.length < 2
                || xValues.length != yValues.length) {
            warnings.add("Could not parse graphical function data for '" + lookupName + "'");
            return;
        }

        try {
            builder.lookupTable(new LookupTableDef(lookupName, null,
                    xValues, yValues, "LINEAR"));
            lookupNames.add(lookupName);
        } catch (IllegalArgumentException e) {
            warnings.add("Could not create lookup '" + lookupName + "': " + e.getMessage());
        }
    }

    /**
     * Parses a {@code <model>} element into a {@link ModelDefinition}.
     * Used for named models that serve as module types.
     */
    private ModelDefinition parseModelElement(Element modelElem, String name, String timeUnit,
                                               Map<String, ModelDefinition> moduleDefinitions,
                                               List<String> warnings) {
        ModelDefinitionBuilder builder = new ModelDefinitionBuilder().name(name);
        populateBuilder(modelElem, builder, timeUnit, moduleDefinitions, warnings);
        return builder.build();
    }

    /**
     * Populates a builder from a {@code <model>} element's variables and views.
     */
    private void populateBuilder(Element modelElem, ModelDefinitionBuilder builder,
                                  String timeUnit,
                                  Map<String, ModelDefinition> moduleDefinitions,
                                  List<String> warnings) {
        Element variablesElem = getFirstChild(modelElem, XmileConstants.VARIABLES);
        if (variablesElem == null) {
            return;
        }

        // Pass 1: Build maps of stock inflows/outflows and collect names
        Map<String, List<String>> stockInflows = new HashMap<>();
        Map<String, List<String>> stockOutflows = new HashMap<>();
        Set<String> stockNames = new HashSet<>();
        Set<String> flowNames = new HashSet<>();
        Set<String> lookupNames = new HashSet<>();

        for (Element stockElem : getChildElements(variablesElem, XmileConstants.STOCK)) {
            String name = stockElem.getAttribute(XmileConstants.ATTR_NAME);
            if (name == null || name.isBlank()) {
                continue;
            }
            stockNames.add(name);
            stockInflows.put(name, getChildTexts(stockElem, XmileConstants.INFLOW));
            stockOutflows.put(name, getChildTexts(stockElem, XmileConstants.OUTFLOW));
        }

        // Pass 2: Build model elements
        for (Element stockElem : getChildElements(variablesElem, XmileConstants.STOCK)) {
            String name = stockElem.getAttribute(XmileConstants.ATTR_NAME);
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                buildStock(stockElem, name, builder, warnings);
            } catch (IllegalArgumentException e) {
                warnings.add("Error processing stock '" + name + "': " + e.getMessage());
            }
        }

        for (Element flowElem : getChildElements(variablesElem, XmileConstants.FLOW)) {
            String name = flowElem.getAttribute(XmileConstants.ATTR_NAME);
            if (name == null || name.isBlank()) {
                continue;
            }
            flowNames.add(name);
            try {
                buildFlow(flowElem, name, timeUnit, stockInflows, stockOutflows,
                        builder, lookupNames, warnings);
            } catch (IllegalArgumentException e) {
                warnings.add("Error processing flow '" + name + "': " + e.getMessage());
            }
        }

        for (Element auxElem : getChildElements(variablesElem, XmileConstants.AUX)) {
            String name = auxElem.getAttribute(XmileConstants.ATTR_NAME);
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                buildAux(auxElem, name, builder, lookupNames, warnings);
            } catch (IllegalArgumentException e) {
                warnings.add("Error processing aux '" + name + "': " + e.getMessage());
            }
        }

        // Process module instances
        for (Element moduleElem : getChildElements(variablesElem, XmileConstants.MODULE)) {
            String name = moduleElem.getAttribute(XmileConstants.ATTR_NAME);
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                buildModule(moduleElem, name, builder, moduleDefinitions, warnings);
            } catch (IllegalArgumentException e) {
                warnings.add("Error processing module '" + name + "': " + e.getMessage());
            }
        }

        checkUnsupportedElements(variablesElem, warnings);

        // Parse views
        Element viewsElem = getFirstChild(modelElem, XmileConstants.VIEWS);
        if (viewsElem != null) {
            List<ViewDef> views = XmileViewParser.parse(
                    viewsElem, stockNames, flowNames, lookupNames, warnings);
            for (ViewDef view : views) {
                builder.view(view);
            }
        }
    }

    private void buildModule(Element moduleElem, String instanceName,
                              ModelDefinitionBuilder builder,
                              Map<String, ModelDefinition> moduleDefinitions,
                              List<String> warnings) {
        // The module name typically matches a named <model> element
        ModelDefinition moduleDef = moduleDefinitions.get(instanceName);
        if (moduleDef == null) {
            // Some XMILE files use a different instance name than the model name.
            // Try to find by looking for a model name that matches.
            // If not found, warn and skip.
            warnings.add("Module '" + instanceName
                    + "' references unknown model definition, skipped");
            return;
        }

        // Parse <connect> elements
        Map<String, String> inputBindings = new HashMap<>();
        Map<String, String> outputBindings = new HashMap<>();

        for (Element connectElem : getChildElements(moduleElem, XmileConstants.CONNECT)) {
            String to = connectElem.getAttribute(XmileConstants.ATTR_TO);
            String from = connectElem.getAttribute(XmileConstants.ATTR_FROM);
            if (to == null || to.isBlank() || from == null || from.isBlank()) {
                continue;
            }

            // In XMILE, a dot prefix on "to" means it's an output binding:
            //   <connect to=".outer_alias" from="inner_var"/>  → output
            //   <connect to="inner_var" from="outer_var"/>     → input
            if (to.startsWith(".")) {
                // Output: inner variable "from" is exposed as "to" (without dot) in parent
                outputBindings.put(from, to.substring(1));
            } else {
                // Input: parent expression "from" feeds into inner port "to"
                inputBindings.put(to, from);
            }
        }

        builder.module(new ModuleInstanceDef(instanceName, moduleDef,
                inputBindings, outputBindings));
    }

    private void checkUnsupportedElements(Element variablesElem, List<String> warnings) {
        for (String tag : UNSUPPORTED_ELEMENTS) {
            List<Element> elems = getChildElements(variablesElem, tag);
            for (Element elem : elems) {
                String name = elem.getAttribute(XmileConstants.ATTR_NAME);
                warnings.add("Unsupported XMILE element <" + tag + "> '"
                        + (name.isEmpty() ? "" : name) + "' skipped");
            }
        }
    }

    // --- XML utility methods ---

    private static Document parseXml(String content)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // Disable external entities for security
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder db = factory.newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(content)));
    }

    private static String getHeaderName(Element root) {
        Element header = getFirstChild(root, XmileConstants.HEADER);
        if (header == null) {
            return null;
        }
        return getChildText(header, XmileConstants.NAME);
    }

    static Element getFirstChild(Element parent, String tagName) {
        // Try namespace-aware first
        NodeList nodes = parent.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, tagName);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName(tagName);
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element elem) {
                return elem;
            }
        }
        return null;
    }

    static String getChildText(Element parent, String tagName) {
        Element child = getFirstChild(parent, tagName);
        if (child != null) {
            return child.getTextContent().strip();
        }
        return null;
    }

    private static List<String> getChildTexts(Element parent, String tagName) {
        List<String> texts = new ArrayList<>();
        NodeList nodes = parent.getElementsByTagNameNS(
                XmileConstants.NAMESPACE_URI, tagName);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName(tagName);
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element elem) {
                String text = elem.getTextContent().strip();
                if (!text.isBlank()) {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    static List<Element> getChildElements(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element elem) {
                String localName = elem.getLocalName();
                if (localName == null) {
                    localName = elem.getTagName();
                }
                if (tagName.equals(localName)) {
                    result.add(elem);
                }
            }
        }
        return result;
    }

    private static double getChildDouble(Element parent, String tagName, double defaultValue) {
        String text = getChildText(parent, tagName);
        if (text != null && !text.isBlank()) {
            try {
                return Double.parseDouble(text.strip());
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return defaultValue;
    }

    private static boolean hasChild(Element parent, String tagName) {
        return getFirstChild(parent, tagName) != null;
    }

    private static double parseDoubleAttr(Element elem, String attrName, double defaultValue) {
        String val = elem.getAttribute(attrName);
        if (val != null && !val.isBlank()) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return defaultValue;
    }

    private static boolean isNumericLiteral(String expr) {
        return expr != null && NUMERIC_PATTERN.matcher(expr.strip()).matches();
    }

    static double[] parseCommaSeparated(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String[] parts = text.split("[,;\\s]+");
        List<Double> values = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                try {
                    values.add(Double.parseDouble(trimmed));
                } catch (NumberFormatException e) {
                    // skip malformed
                }
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    static double[] linspace(double min, double max, int count) {
        if (count < 2) {
            return new double[]{min};
        }
        double[] result = new double[count];
        double step = (max - min) / (count - 1);
        for (int i = 0; i < count; i++) {
            result[i] = min + i * step;
        }
        return result;
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.ROOT)
                + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
