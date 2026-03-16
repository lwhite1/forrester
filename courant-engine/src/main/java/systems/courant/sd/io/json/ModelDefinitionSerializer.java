package systems.courant.sd.io.json;

import static systems.courant.sd.io.json.JsonNodeHelper.requiredText;
import static systems.courant.sd.io.json.JsonNodeHelper.textOrNull;

import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.ConnectorRoute;

import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.FlowRoute;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.ReferenceDataset;
import systems.courant.sd.model.def.PortDef;
import systems.courant.sd.model.def.SimulationSettings;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;
import systems.courant.sd.model.def.ViewDef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Serializes and deserializes {@link ModelDefinition} to/from JSON.
 *
 * <p>Uses a custom serialization approach without Jackson annotations on the records,
 * keeping the model records clean of serialization concerns.
 */
public class ModelDefinitionSerializer {

    private static final Logger log = LoggerFactory.getLogger(ModelDefinitionSerializer.class);
    private static final int MAX_MODULE_DEPTH = 50;

    private final ObjectMapper mapper;

    /**
     * Creates a new serializer with pretty-printing and security hardening enabled.
     */
    public ModelDefinitionSerializer() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(100)
                        .build());
    }

    /**
     * Serializes a model definition to a JSON string.
     *
     * @param def the model definition to serialize
     * @return the JSON string representation
     * @throws IllegalArgumentException if serialization fails
     */
    public String toJson(ModelDefinition def) {
        try {
            ObjectNode root = toJsonNode(def);
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize model definition", e);
        }
    }

    /**
     * Deserializes a model definition from a JSON string.
     *
     * @param json the JSON string to deserialize
     * @return the model definition
     * @throws IllegalArgumentException if the JSON is malformed or missing required fields
     */
    public ModelDefinition fromJson(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            return fromJsonNode(root);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize model definition", e);
        }
    }

    /**
     * Writes a model definition to a JSON file.
     *
     * @param def  the model definition to write
     * @param path the output file path
     * @throws IOException if the file cannot be written
     */
    public void toFile(ModelDefinition def, Path path) throws IOException {
        String json = toJson(def);
        Files.writeString(path, json);
    }

    /**
     * Reads a model definition from a JSON file.
     *
     * @param path the path to the JSON file
     * @return the deserialized model definition
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the JSON is malformed or missing required fields
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    public ModelDefinition fromFile(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            throw new IOException("JSON file exceeds "
                    + (MAX_FILE_SIZE / (1024 * 1024)) + " MB: " + path);
        }
        String json = Files.readString(path);
        return fromJson(json);
    }

    // === Serialization ===

    private ObjectNode toJsonNode(ModelDefinition def) {
        ObjectNode root = mapper.createObjectNode();
        root.put("name", def.name());
        if (def.comment() != null) {
            root.put("comment", def.comment());
        }
        if (def.moduleInterface() != null) {
            root.set("moduleInterface", serializeModuleInterface(def.moduleInterface()));
        }
        if (!def.stocks().isEmpty()) {
            root.set("stocks", serializeStocks(def.stocks()));
        }
        if (!def.flows().isEmpty()) {
            root.set("flows", serializeFlows(def.flows()));
        }
        if (!def.variables().isEmpty()) {
            root.set("variables", serializeVariables(def.variables()));
        }

        if (!def.lookupTables().isEmpty()) {
            root.set("lookupTables", serializeLookupTables(def.lookupTables()));
        }
        if (!def.modules().isEmpty()) {
            root.set("modules", serializeModules(def.modules()));
        }
        if (!def.subscripts().isEmpty()) {
            root.set("subscripts", serializeSubscripts(def.subscripts()));
        }
        if (!def.cldVariables().isEmpty()) {
            root.set("cldVariables", serializeCldVariables(def.cldVariables()));
        }
        if (!def.causalLinks().isEmpty()) {
            root.set("causalLinks", serializeCausalLinks(def.causalLinks()));
        }
        if (!def.comments().isEmpty()) {
            root.set("comments", serializeComments(def.comments()));
        }
        if (!def.views().isEmpty()) {
            root.set("views", serializeViews(def.views()));
        }
        if (def.defaultSimulation() != null) {
            root.set("defaultSimulation", serializeSimSettings(def.defaultSimulation()));
        }
        if (def.metadata() != null) {
            root.set("metadata", serializeMetadata(def.metadata()));
        }
        if (!def.referenceDatasets().isEmpty()) {
            root.set("referenceDatasets", serializeReferenceDatasets(def.referenceDatasets()));
        }
        return root;
    }

    private ArrayNode serializeStocks(List<StockDef> stocks) {
        ArrayNode arr = mapper.createArrayNode();
        for (StockDef s : stocks) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", s.name());
            if (s.comment() != null) {
                node.put("comment", s.comment());
            }
            node.put("initialValue", s.initialValue());
            if (s.initialExpression() != null) {
                node.put("initialExpression", s.initialExpression());
            }
            node.put("unit", s.unit());
            if (s.negativeValuePolicy() != null) {
                node.put("negativeValuePolicy", s.negativeValuePolicy());
            }
            if (!s.subscripts().isEmpty()) {
                ArrayNode subs = mapper.createArrayNode();
                s.subscripts().forEach(subs::add);
                node.set("subscripts", subs);
            }
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeFlows(List<FlowDef> flows) {
        ArrayNode arr = mapper.createArrayNode();
        for (FlowDef f : flows) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", f.name());
            if (f.comment() != null) {
                node.put("comment", f.comment());
            }
            node.put("equation", f.equation());
            node.put("timeUnit", f.timeUnit());
            if (f.materialUnit() != null) {
                node.put("materialUnit", f.materialUnit());
            }
            if (f.source() != null) {
                node.put("source", f.source());
            }
            if (f.sink() != null) {
                node.put("sink", f.sink());
            }
            if (!f.subscripts().isEmpty()) {
                ArrayNode subs = mapper.createArrayNode();
                f.subscripts().forEach(subs::add);
                node.set("subscripts", subs);
            }
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeVariables(List<VariableDef> variables) {
        ArrayNode arr = mapper.createArrayNode();
        for (VariableDef a : variables) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", a.name());
            if (a.comment() != null) {
                node.put("comment", a.comment());
            }
            node.put("equation", a.equation());
            node.put("unit", a.unit());
            if (!a.subscripts().isEmpty()) {
                ArrayNode subs = mapper.createArrayNode();
                a.subscripts().forEach(subs::add);
                node.set("subscripts", subs);
            }
            arr.add(node);
        }
        return arr;
    }


    private ArrayNode serializeLookupTables(List<LookupTableDef> tables) {
        ArrayNode arr = mapper.createArrayNode();
        for (LookupTableDef t : tables) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", t.name());
            if (t.comment() != null) {
                node.put("comment", t.comment());
            }
            node.set("xValues", doubleArrayToJson(t.xValues()));
            node.set("yValues", doubleArrayToJson(t.yValues()));
            node.put("interpolation", t.interpolation());
            if (t.unit() != null) {
                node.put("unit", t.unit());
            }
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeModules(List<ModuleInstanceDef> modules) {
        ArrayNode arr = mapper.createArrayNode();
        for (ModuleInstanceDef m : modules) {
            ObjectNode node = mapper.createObjectNode();
            node.put("instanceName", m.instanceName());
            node.set("definition", toJsonNode(m.definition()));
            if (!m.inputBindings().isEmpty()) {
                node.set("inputBindings", mapToJson(m.inputBindings()));
            }
            if (!m.outputBindings().isEmpty()) {
                node.set("outputBindings", mapToJson(m.outputBindings()));
            }
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeSubscripts(List<SubscriptDef> subscripts) {
        ArrayNode arr = mapper.createArrayNode();
        for (SubscriptDef s : subscripts) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", s.name());
            ArrayNode labels = mapper.createArrayNode();
            for (String label : s.labels()) {
                labels.add(label);
            }
            node.set("labels", labels);
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeViews(List<ViewDef> views) {
        ArrayNode arr = mapper.createArrayNode();
        for (ViewDef v : views) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", v.name());
            if (!v.elements().isEmpty()) {
                ArrayNode elements = mapper.createArrayNode();
                for (ElementPlacement ep : v.elements()) {
                    ObjectNode epNode = mapper.createObjectNode();
                    epNode.put("name", ep.name());
                    epNode.put("type", ep.type().label());
                    epNode.put("x", ep.x());
                    epNode.put("y", ep.y());
                    if (ep.hasCustomSize()) {
                        epNode.put("width", ep.width());
                        epNode.put("height", ep.height());
                    }
                    elements.add(epNode);
                }
                node.set("elements", elements);
            }
            if (!v.connectors().isEmpty()) {
                ArrayNode connectors = mapper.createArrayNode();
                for (ConnectorRoute cr : v.connectors()) {
                    ObjectNode crNode = mapper.createObjectNode();
                    crNode.put("from", cr.from());
                    crNode.put("to", cr.to());
                    if (!cr.controlPoints().isEmpty()) {
                        crNode.set("controlPoints", serializePointList(cr.controlPoints()));
                    }
                    connectors.add(crNode);
                }
                node.set("connectors", connectors);
            }
            if (!v.flowRoutes().isEmpty()) {
                ArrayNode flowRoutes = mapper.createArrayNode();
                for (FlowRoute fr : v.flowRoutes()) {
                    ObjectNode frNode = mapper.createObjectNode();
                    frNode.put("flowName", fr.flowName());
                    if (!fr.points().isEmpty()) {
                        frNode.set("points", serializePointList(fr.points()));
                    }
                    flowRoutes.add(frNode);
                }
                node.set("flowRoutes", flowRoutes);
            }
            arr.add(node);
        }
        return arr;
    }

    private ObjectNode serializeSimSettings(SimulationSettings settings) {
        ObjectNode node = mapper.createObjectNode();
        node.put("timeStep", settings.timeStep());
        node.put("duration", settings.duration());
        node.put("durationUnit", settings.durationUnit());
        if (settings.dt() != 1.0) {
            node.put("dt", settings.dt());
        }
        if (settings.strictMode()) {
            node.put("strictMode", true);
        }
        if (settings.savePer() != 1) {
            node.put("savePer", settings.savePer());
        }
        return node;
    }

    private ObjectNode serializeMetadata(ModelMetadata meta) {
        ObjectNode node = mapper.createObjectNode();
        if (meta.author() != null) {
            node.put("author", meta.author());
        }
        if (meta.source() != null) {
            node.put("source", meta.source());
        }
        if (meta.license() != null) {
            node.put("license", meta.license());
        }
        if (meta.url() != null) {
            node.put("url", meta.url());
        }
        return node;
    }

    private ObjectNode serializeModuleInterface(ModuleInterface iface) {
        ObjectNode node = mapper.createObjectNode();
        ArrayNode inputs = mapper.createArrayNode();
        for (PortDef p : iface.inputs()) {
            ObjectNode pNode = mapper.createObjectNode();
            pNode.put("name", p.name());
            if (p.unit() != null) {
                pNode.put("unit", p.unit());
            }
            if (p.comment() != null) {
                pNode.put("comment", p.comment());
            }
            inputs.add(pNode);
        }
        node.set("inputs", inputs);
        ArrayNode outputs = mapper.createArrayNode();
        for (PortDef p : iface.outputs()) {
            ObjectNode pNode = mapper.createObjectNode();
            pNode.put("name", p.name());
            if (p.unit() != null) {
                pNode.put("unit", p.unit());
            }
            if (p.comment() != null) {
                pNode.put("comment", p.comment());
            }
            outputs.add(pNode);
        }
        node.set("outputs", outputs);
        return node;
    }

    private ArrayNode serializeCldVariables(List<CldVariableDef> vars) {
        ArrayNode arr = mapper.createArrayNode();
        for (CldVariableDef v : vars) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", v.name());
            if (v.comment() != null) {
                node.put("comment", v.comment());
            }
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeCausalLinks(List<CausalLinkDef> links) {
        ArrayNode arr = mapper.createArrayNode();
        for (CausalLinkDef link : links) {
            ObjectNode node = mapper.createObjectNode();
            node.put("from", link.from());
            node.put("to", link.to());
            node.put("polarity", link.polarity().name());
            if (link.comment() != null) {
                node.put("comment", link.comment());
            }
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeComments(List<CommentDef> comments) {
        ArrayNode arr = mapper.createArrayNode();
        for (CommentDef c : comments) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", c.name());
            if (c.text() != null && !c.text().isEmpty()) {
                node.put("text", c.text());
            }
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeReferenceDatasets(List<ReferenceDataset> datasets) {
        ArrayNode arr = mapper.createArrayNode();
        for (ReferenceDataset ds : datasets) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", ds.name());
            node.set("timeValues", doubleArrayToJson(ds.timeValues()));
            ObjectNode cols = mapper.createObjectNode();
            for (Map.Entry<String, double[]> entry : ds.columns().entrySet()) {
                cols.set(entry.getKey(), doubleArrayToJson(entry.getValue()));
            }
            node.set("columns", cols);
            arr.add(node);
        }
        return arr;
    }

    // === Deserialization ===

    private ModelDefinition fromJsonNode(JsonNode root) {
        return fromJsonNode(root, 0);
    }

    private ModelDefinition fromJsonNode(JsonNode root, int depth) {
        if (depth > MAX_MODULE_DEPTH) {
            throw new IllegalArgumentException(
                    "Module nesting depth exceeds maximum of " + MAX_MODULE_DEPTH);
        }
        String name = requiredText(root, "name");
        String comment = textOrNull(root, "comment");

        ModuleInterface moduleInterface = null;
        if (root.has("moduleInterface")) {
            moduleInterface = deserializeModuleInterface(root.get("moduleInterface"));
        }

        List<StockDef> stocks = deserializeStocks(root);
        List<FlowDef> flows = deserializeFlows(root);
        List<VariableDef> variables = deserializeVariables(root);
        List<VariableDef> migratedConstants = deserializeLegacyConstants(root);
        List<LookupTableDef> lookupTables = deserializeLookupTables(root);
        List<ModuleInstanceDef> modules = deserializeModules(root, depth);
        List<SubscriptDef> subscripts = deserializeSubscripts(root);
        List<CldVariableDef> cldVariables = deserializeCldVariables(root);
        List<CausalLinkDef> causalLinks = deserializeCausalLinks(root);
        List<CommentDef> comments = deserializeComments(root);

        List<ViewDef> views = new ArrayList<>();
        if (root.has("views")) {
            for (JsonNode n : root.get("views")) {
                views.add(deserializeView(n));
            }
        }

        SimulationSettings defaultSimulation = deserializeSimulationSettings(root);
        ModelMetadata metadata = deserializeMetadata(root);
        List<ReferenceDataset> referenceDatasets = deserializeReferenceDatasets(root);

        if (!migratedConstants.isEmpty()) {
            ModelDefinition migrated = ModelDefinition.withMigratedConstants(
                    name, comment, moduleInterface,
                    stocks, flows, variables, migratedConstants, lookupTables,
                    modules, subscripts, cldVariables, causalLinks, comments,
                    views, defaultSimulation, metadata);
            if (referenceDatasets.isEmpty()) {
                return migrated;
            }
            return migrated.toBuilder()
                    .clearReferenceDatasets()
                    .referenceDatasets(referenceDatasets)
                    .build();
        }
        return new ModelDefinition(name, comment, moduleInterface,
                stocks, flows, variables, lookupTables,
                modules, subscripts, cldVariables, causalLinks,
                comments, views, defaultSimulation, metadata, referenceDatasets);
    }

    private List<StockDef> deserializeStocks(JsonNode root) {
        List<StockDef> stocks = new ArrayList<>();
        if (root.has("stocks")) {
            for (JsonNode n : root.get("stocks")) {
                stocks.add(new StockDef(
                        requiredText(n, "name"),
                        textOrNull(n, "comment"),
                        requiredDouble(n, "initialValue"),
                        textOrNull(n, "initialExpression"),
                        requiredText(n, "unit"),
                        textOrNull(n, "negativeValuePolicy"),
                        readStringList(n, "subscripts")));
            }
        }
        return stocks;
    }

    private List<FlowDef> deserializeFlows(JsonNode root) {
        List<FlowDef> flows = new ArrayList<>();
        if (root.has("flows")) {
            for (JsonNode n : root.get("flows")) {
                flows.add(new FlowDef(
                        requiredText(n, "name"),
                        textOrNull(n, "comment"),
                        requiredText(n, "equation"),
                        requiredText(n, "timeUnit"),
                        textOrNull(n, "materialUnit"),
                        textOrNull(n, "source"),
                        textOrNull(n, "sink"),
                        readStringList(n, "subscripts")));
            }
        }
        return flows;
    }

    private List<VariableDef> deserializeVariables(JsonNode root) {
        List<VariableDef> variables = new ArrayList<>();
        if (root.has("variables")) {
            for (JsonNode n : root.get("variables")) {
                String unit = textOrNull(n, "unit");
                variables.add(new VariableDef(
                        requiredText(n, "name"),
                        textOrNull(n, "comment"),
                        requiredText(n, "equation"),
                        unit != null ? unit : "",
                        readStringList(n, "subscripts")));
            }
        }
        return variables;
    }

    // Backward compatibility: migrate legacy constants into variables
    private List<VariableDef> deserializeLegacyConstants(JsonNode root) {
        List<VariableDef> migratedConstants = new ArrayList<>();
        if (root.has("constants")) {
            for (JsonNode n : root.get("constants")) {
                double value = requiredDouble(n, "value");
                migratedConstants.add(new VariableDef(
                        requiredText(n, "name"),
                        textOrNull(n, "comment"),
                        VariableDef.formatValue(value),
                        requiredText(n, "unit")));
            }
        }
        return migratedConstants;
    }

    private List<LookupTableDef> deserializeLookupTables(JsonNode root) {
        List<LookupTableDef> lookupTables = new ArrayList<>();
        if (root.has("lookupTables")) {
            for (JsonNode n : root.get("lookupTables")) {
                lookupTables.add(new LookupTableDef(
                        requiredText(n, "name"),
                        textOrNull(n, "comment"),
                        jsonToDoubleArray(requiredNode(n, "xValues")),
                        jsonToDoubleArray(requiredNode(n, "yValues")),
                        requiredText(n, "interpolation"),
                        textOrNull(n, "unit")));
            }
        }
        return lookupTables;
    }

    private List<ModuleInstanceDef> deserializeModules(JsonNode root, int depth) {
        List<ModuleInstanceDef> modules = new ArrayList<>();
        if (root.has("modules")) {
            for (JsonNode n : root.get("modules")) {
                Map<String, String> inputBindings = jsonToMap(n.get("inputBindings"));
                Map<String, String> outputBindings = jsonToMap(n.get("outputBindings"));
                modules.add(new ModuleInstanceDef(
                        requiredText(n, "instanceName"),
                        fromJsonNode(requiredNode(n, "definition"), depth + 1),
                        inputBindings,
                        outputBindings));
            }
        }
        return modules;
    }

    private List<SubscriptDef> deserializeSubscripts(JsonNode root) {
        List<SubscriptDef> subscripts = new ArrayList<>();
        if (root.has("subscripts")) {
            for (JsonNode n : root.get("subscripts")) {
                List<String> labels = new ArrayList<>();
                JsonNode labelsNode = requiredNode(n, "labels");
                for (JsonNode l : labelsNode) {
                    labels.add(l.asText());
                }
                subscripts.add(new SubscriptDef(requiredText(n, "name"), labels));
            }
        }
        return subscripts;
    }

    private List<CldVariableDef> deserializeCldVariables(JsonNode root) {
        List<CldVariableDef> cldVariables = new ArrayList<>();
        if (root.has("cldVariables")) {
            for (JsonNode n : root.get("cldVariables")) {
                cldVariables.add(new CldVariableDef(
                        requiredText(n, "name"),
                        textOrNull(n, "comment")));
            }
        }
        return cldVariables;
    }

    private List<CausalLinkDef> deserializeCausalLinks(JsonNode root) {
        List<CausalLinkDef> causalLinks = new ArrayList<>();
        if (root.has("causalLinks")) {
            for (JsonNode n : root.get("causalLinks")) {
                String polarityStr = textOrNull(n, "polarity");
                CausalLinkDef.Polarity polarity;
                if (polarityStr == null) {
                    polarity = CausalLinkDef.Polarity.UNKNOWN;
                } else {
                    try {
                        polarity = CausalLinkDef.Polarity.valueOf(polarityStr);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown causal link polarity '{}', defaulting to UNKNOWN", polarityStr);
                        polarity = CausalLinkDef.Polarity.UNKNOWN;
                    }
                }
                causalLinks.add(new CausalLinkDef(
                        requiredText(n, "from"),
                        requiredText(n, "to"),
                        polarity,
                        textOrNull(n, "comment")));
            }
        }
        return causalLinks;
    }

    private List<CommentDef> deserializeComments(JsonNode root) {
        List<CommentDef> comments = new ArrayList<>();
        if (root.has("comments")) {
            for (JsonNode n : root.get("comments")) {
                String text = textOrNull(n, "text");
                comments.add(new CommentDef(
                        requiredText(n, "name"),
                        text != null ? text : ""));
            }
        }
        return comments;
    }

    private SimulationSettings deserializeSimulationSettings(JsonNode root) {
        if (!root.has("defaultSimulation")) {
            return null;
        }
        JsonNode s = root.get("defaultSimulation");
        double dt = s.has("dt") ? s.get("dt").asDouble() : 1.0;
        boolean strict = s.has("strictMode") && s.get("strictMode").asBoolean();
        long savePerVal = s.has("savePer") ? s.get("savePer").asLong() : 1;
        return new SimulationSettings(
                requiredText(s, "timeStep"),
                requiredDouble(s, "duration"),
                requiredText(s, "durationUnit"),
                dt,
                strict,
                savePerVal);
    }

    private ModelMetadata deserializeMetadata(JsonNode root) {
        if (!root.has("metadata")) {
            return null;
        }
        JsonNode m = root.get("metadata");
        return ModelMetadata.builder()
                .author(textOrNull(m, "author"))
                .source(textOrNull(m, "source"))
                .license(textOrNull(m, "license"))
                .url(textOrNull(m, "url"))
                .build();
    }

    private List<ReferenceDataset> deserializeReferenceDatasets(JsonNode root) {
        List<ReferenceDataset> referenceDatasets = new ArrayList<>();
        if (root.has("referenceDatasets")) {
            for (JsonNode n : root.get("referenceDatasets")) {
                String dsName = requiredText(n, "name");
                double[] timeValues = jsonToDoubleArray(requiredNode(n, "timeValues"));
                JsonNode colsNode = requiredNode(n, "columns");
                Map<String, double[]> columns = new java.util.LinkedHashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = colsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    columns.put(entry.getKey(), jsonToDoubleArray(entry.getValue()));
                }
                referenceDatasets.add(new ReferenceDataset(dsName, timeValues, columns));
            }
        }
        return referenceDatasets;
    }

    private ViewDef deserializeView(JsonNode n) {
        String name = requiredText(n, "name");
        List<ElementPlacement> elements = new ArrayList<>();
        if (n.has("elements")) {
            for (JsonNode ep : n.get("elements")) {
                double w = ep.has("width") ? ep.get("width").asDouble() : 0;
                double h = ep.has("height") ? ep.get("height").asDouble() : 0;
                elements.add(new ElementPlacement(
                        requiredText(ep, "name"),
                        ElementType.fromLabel(requiredText(ep, "type")),
                        requiredDouble(ep, "x"),
                        requiredDouble(ep, "y"),
                        w, h));
            }
        }
        List<ConnectorRoute> connectors = new ArrayList<>();
        if (n.has("connectors")) {
            for (JsonNode cr : n.get("connectors")) {
                List<double[]> controlPoints = new ArrayList<>();
                if (cr.has("controlPoints")) {
                    controlPoints = deserializePointList(cr.get("controlPoints"));
                }
                connectors.add(new ConnectorRoute(
                        requiredText(cr, "from"),
                        requiredText(cr, "to"),
                        controlPoints));
            }
        }
        List<FlowRoute> flowRoutes = new ArrayList<>();
        if (n.has("flowRoutes")) {
            for (JsonNode fr : n.get("flowRoutes")) {
                List<double[]> points = new ArrayList<>();
                if (fr.has("points")) {
                    points = deserializePointList(fr.get("points"));
                }
                flowRoutes.add(new FlowRoute(requiredText(fr, "flowName"), points));
            }
        }
        return new ViewDef(name, elements, connectors, flowRoutes);
    }

    private ModuleInterface deserializeModuleInterface(JsonNode node) {
        List<PortDef> inputs = new ArrayList<>();
        for (JsonNode p : requiredNode(node, "inputs")) {
            inputs.add(new PortDef(
                    requiredText(p, "name"),
                    textOrNull(p, "unit"),
                    textOrNull(p, "comment")));
        }
        List<PortDef> outputs = new ArrayList<>();
        for (JsonNode p : requiredNode(node, "outputs")) {
            outputs.add(new PortDef(
                    requiredText(p, "name"),
                    textOrNull(p, "unit"),
                    textOrNull(p, "comment")));
        }
        return new ModuleInterface(inputs, outputs);
    }

    // === Utility methods ===

    private ArrayNode doubleArrayToJson(double[] values) {
        ArrayNode arr = mapper.createArrayNode();
        for (double v : values) {
            arr.add(v);
        }
        return arr;
    }

    private double[] jsonToDoubleArray(JsonNode node) {
        double[] arr = new double[node.size()];
        for (int i = 0; i < node.size(); i++) {
            arr[i] = node.get(i).asDouble();
        }
        return arr;
    }

    private ObjectNode mapToJson(Map<String, String> map) {
        ObjectNode node = mapper.createObjectNode();
        for (Map.Entry<String, String> e : map.entrySet()) {
            node.put(e.getKey(), e.getValue());
        }
        return node;
    }

    private Map<String, String> jsonToMap(JsonNode node) {
        Map<String, String> map = new HashMap<>();
        if (node != null && node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                map.put(entry.getKey(), entry.getValue().asText());
            }
        }
        return map;
    }

    private ArrayNode serializePointList(List<double[]> points) {
        ArrayNode arr = mapper.createArrayNode();
        for (double[] point : points) {
            arr.add(doubleArrayToJson(point));
        }
        return arr;
    }

    private List<double[]> deserializePointList(JsonNode node) {
        List<double[]> points = new ArrayList<>();
        for (JsonNode p : node) {
            points.add(jsonToDoubleArray(p));
        }
        return points;
    }

    private List<String> readStringList(JsonNode node, String field) {
        if (!node.has(field)) {
            return List.of();
        }
        JsonNode arr = node.get(field);
        List<String> result = new ArrayList<>();
        for (JsonNode item : arr) {
            result.add(item.asText());
        }
        return result;
    }


    private double requiredDouble(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        if (!child.isNumber()) {
            throw new IllegalArgumentException(
                    "Field '" + field + "' must be a number, got: " + child.getNodeType());
        }
        return child.asDouble();
    }

    private JsonNode requiredNode(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return child;
    }
}
