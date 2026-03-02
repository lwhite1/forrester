package com.deathrayresearch.forrester.io.json;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.FlowRoute;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.ModuleInterface;
import com.deathrayresearch.forrester.model.def.PortDef;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.SubscriptDef;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    private final ObjectMapper mapper;

    public ModelDefinitionSerializer() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Serializes a model definition to a JSON string.
     */
    public String toJson(ModelDefinition def) {
        try {
            ObjectNode root = toJsonNode(def);
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize model definition", e);
        }
    }

    /**
     * Deserializes a model definition from a JSON string.
     */
    public ModelDefinition fromJson(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            return fromJsonNode(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize model definition", e);
        }
    }

    /**
     * Writes a model definition to a JSON file.
     */
    public void toFile(ModelDefinition def, Path path) throws IOException {
        String json = toJson(def);
        Files.writeString(path, json);
    }

    /**
     * Reads a model definition from a JSON file.
     */
    public ModelDefinition fromFile(Path path) throws IOException {
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
        if (!def.auxiliaries().isEmpty()) {
            root.set("auxiliaries", serializeAuxiliaries(def.auxiliaries()));
        }
        if (!def.constants().isEmpty()) {
            root.set("constants", serializeConstants(def.constants()));
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
        if (!def.views().isEmpty()) {
            root.set("views", serializeViews(def.views()));
        }
        if (def.defaultSimulation() != null) {
            root.set("defaultSimulation", serializeSimSettings(def.defaultSimulation()));
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
            node.put("unit", s.unit());
            if (s.negativeValuePolicy() != null) {
                node.put("negativeValuePolicy", s.negativeValuePolicy());
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
            if (f.source() != null) {
                node.put("source", f.source());
            }
            if (f.sink() != null) {
                node.put("sink", f.sink());
            }
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeAuxiliaries(List<AuxDef> auxiliaries) {
        ArrayNode arr = mapper.createArrayNode();
        for (AuxDef a : auxiliaries) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", a.name());
            if (a.comment() != null) {
                node.put("comment", a.comment());
            }
            node.put("equation", a.equation());
            node.put("unit", a.unit());
            arr.add(node);
        }
        return arr;
    }

    private ArrayNode serializeConstants(List<ConstantDef> constants) {
        ArrayNode arr = mapper.createArrayNode();
        for (ConstantDef c : constants) {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", c.name());
            if (c.comment() != null) {
                node.put("comment", c.comment());
            }
            node.put("value", c.value());
            node.put("unit", c.unit());
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
                    epNode.put("type", ep.type());
                    epNode.put("x", ep.x());
                    epNode.put("y", ep.y());
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

    // === Deserialization ===

    private ModelDefinition fromJsonNode(JsonNode root) {
        String name = root.get("name").asText();
        String comment = textOrNull(root, "comment");

        ModuleInterface moduleInterface = null;
        if (root.has("moduleInterface")) {
            moduleInterface = deserializeModuleInterface(root.get("moduleInterface"));
        }

        List<StockDef> stocks = new ArrayList<>();
        if (root.has("stocks")) {
            for (JsonNode n : root.get("stocks")) {
                stocks.add(new StockDef(
                        n.get("name").asText(),
                        textOrNull(n, "comment"),
                        n.get("initialValue").asDouble(),
                        n.get("unit").asText(),
                        textOrNull(n, "negativeValuePolicy")));
            }
        }

        List<FlowDef> flows = new ArrayList<>();
        if (root.has("flows")) {
            for (JsonNode n : root.get("flows")) {
                flows.add(new FlowDef(
                        n.get("name").asText(),
                        textOrNull(n, "comment"),
                        n.get("equation").asText(),
                        n.get("timeUnit").asText(),
                        textOrNull(n, "source"),
                        textOrNull(n, "sink")));
            }
        }

        List<AuxDef> auxiliaries = new ArrayList<>();
        if (root.has("auxiliaries")) {
            for (JsonNode n : root.get("auxiliaries")) {
                auxiliaries.add(new AuxDef(
                        n.get("name").asText(),
                        textOrNull(n, "comment"),
                        n.get("equation").asText(),
                        n.get("unit").asText()));
            }
        }

        List<ConstantDef> constants = new ArrayList<>();
        if (root.has("constants")) {
            for (JsonNode n : root.get("constants")) {
                constants.add(new ConstantDef(
                        n.get("name").asText(),
                        textOrNull(n, "comment"),
                        n.get("value").asDouble(),
                        n.get("unit").asText()));
            }
        }

        List<LookupTableDef> lookupTables = new ArrayList<>();
        if (root.has("lookupTables")) {
            for (JsonNode n : root.get("lookupTables")) {
                lookupTables.add(new LookupTableDef(
                        n.get("name").asText(),
                        textOrNull(n, "comment"),
                        jsonToDoubleArray(n.get("xValues")),
                        jsonToDoubleArray(n.get("yValues")),
                        n.get("interpolation").asText()));
            }
        }

        List<ModuleInstanceDef> modules = new ArrayList<>();
        if (root.has("modules")) {
            for (JsonNode n : root.get("modules")) {
                Map<String, String> inputBindings = jsonToMap(n.get("inputBindings"));
                Map<String, String> outputBindings = jsonToMap(n.get("outputBindings"));
                modules.add(new ModuleInstanceDef(
                        n.get("instanceName").asText(),
                        fromJsonNode(n.get("definition")),
                        inputBindings,
                        outputBindings));
            }
        }

        List<SubscriptDef> subscripts = new ArrayList<>();
        if (root.has("subscripts")) {
            for (JsonNode n : root.get("subscripts")) {
                List<String> labels = new ArrayList<>();
                for (JsonNode l : n.get("labels")) {
                    labels.add(l.asText());
                }
                subscripts.add(new SubscriptDef(n.get("name").asText(), labels));
            }
        }

        List<ViewDef> views = new ArrayList<>();
        if (root.has("views")) {
            for (JsonNode n : root.get("views")) {
                views.add(deserializeView(n));
            }
        }

        SimulationSettings defaultSimulation = null;
        if (root.has("defaultSimulation")) {
            JsonNode s = root.get("defaultSimulation");
            defaultSimulation = new SimulationSettings(
                    s.get("timeStep").asText(),
                    s.get("duration").asDouble(),
                    s.get("durationUnit").asText());
        }

        return new ModelDefinition(name, comment, moduleInterface,
                stocks, flows, auxiliaries, constants, lookupTables,
                modules, subscripts, views, defaultSimulation);
    }

    private ViewDef deserializeView(JsonNode n) {
        String name = n.get("name").asText();
        List<ElementPlacement> elements = new ArrayList<>();
        if (n.has("elements")) {
            for (JsonNode ep : n.get("elements")) {
                elements.add(new ElementPlacement(
                        ep.get("name").asText(),
                        ep.get("type").asText(),
                        ep.get("x").asDouble(),
                        ep.get("y").asDouble()));
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
                        cr.get("from").asText(),
                        cr.get("to").asText(),
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
                flowRoutes.add(new FlowRoute(fr.get("flowName").asText(), points));
            }
        }
        return new ViewDef(name, elements, connectors, flowRoutes);
    }

    private ModuleInterface deserializeModuleInterface(JsonNode node) {
        List<PortDef> inputs = new ArrayList<>();
        for (JsonNode p : node.get("inputs")) {
            inputs.add(new PortDef(
                    p.get("name").asText(),
                    textOrNull(p, "unit"),
                    textOrNull(p, "comment")));
        }
        List<PortDef> outputs = new ArrayList<>();
        for (JsonNode p : node.get("outputs")) {
            outputs.add(new PortDef(
                    p.get("name").asText(),
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

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}
