package systems.courant.sd.tools.importer;

import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.SimulationSettings;
import systems.courant.sd.model.def.StockDef;

import java.util.List;
import java.util.Map;

import static systems.courant.sd.tools.importer.JavaSourceEscaper.doubleArrayLiteral;
import static systems.courant.sd.tools.importer.JavaSourceEscaper.escapeString;

/**
 * Generates a Java demo class from a {@link ModelDefinition} and {@link ModelMetadata}.
 *
 * <p>The generated class uses {@code ModelDefinitionBuilder} to reconstruct the model
 * and {@code ModelCompiler} to compile it at runtime.
 */
public class DemoClassGenerator {

    private static final String INDENT = "        ";
    private static final String INDENT2 = "                ";

    private final int copyrightYear;

    /**
     * Creates a generator that uses the given copyright year in license headers.
     *
     * @param copyrightYear the year to emit in Courant copyright headers
     */
    public DemoClassGenerator(int copyrightYear) {
        this.copyrightYear = copyrightYear;
    }

    /**
     * Creates a generator using the current year for copyright headers.
     */
    public DemoClassGenerator() {
        this(java.time.Year.now().getValue());
    }

    /**
     * Generates the full Java source for a demo class.
     *
     * @param definition       the model definition
     * @param metadata         attribution/licensing metadata
     * @param className        the Java class name
     * @param packageName      the fully qualified package name
     * @param sourceFileName   the original file name for provenance
     * @param importWarnings   import warnings to include as comments
     * @param validationErrors validation errors to include as comments
     * @return the complete Java source code
     */
    public String generate(ModelDefinition definition, ModelMetadata metadata,
                           String className, String packageName, String sourceFileName,
                           List<String> importWarnings, List<String> validationErrors) {
        StringBuilder sb = new StringBuilder(4096);

        emitLicenseHeader(sb, metadata);
        emitPackage(sb, packageName);
        emitImports(sb, definition);
        emitClassJavadoc(sb, sourceFileName, metadata, importWarnings, validationErrors);
        emitClassOpen(sb, className);
        emitMainMethod(sb, className);
        emitRunMethod(sb, definition, metadata);
        emitClassClose(sb);

        return sb.toString();
    }

    private void emitLicenseHeader(StringBuilder sb, ModelMetadata metadata) {
        String license = metadata.license();
        if (license != null && license.contains("NC")) {
            sb.append("/*\n");
            sb.append(" * Copyright (c) original author(s). See model metadata for attribution.\n");
            sb.append(" * Licensed under CC-BY-NC-SA-4.0. See THIRD-PARTY-LICENSES for details.\n");
            sb.append(" */\n");
        } else {
            sb.append("/*\n");
            sb.append(" * Copyright (c) ").append(copyrightYear).append(" Courant Systems\n");
            sb.append(" * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.\n");
            sb.append(" */\n");
        }
    }

    private void emitPackage(StringBuilder sb, String packageName) {
        sb.append("package ").append(packageName).append(";\n\n");
    }

    private void emitImports(StringBuilder sb, ModelDefinition definition) {
        sb.append("import systems.courant.sd.Simulation;\n");
        sb.append("import systems.courant.sd.model.ModelMetadata;\n");
        sb.append("import systems.courant.sd.model.compile.CompiledModel;\n");
        sb.append("import systems.courant.sd.model.compile.ModelCompiler;\n");
        sb.append("import systems.courant.sd.model.def.ModelDefinitionBuilder;\n");

        if (!definition.variables().isEmpty()) {
            sb.append("import systems.courant.sd.model.def.VariableDef;\n");
        }
        if (!definition.flows().isEmpty()) {
            sb.append("import systems.courant.sd.model.def.FlowDef;\n");
        }
        if (!definition.lookupTables().isEmpty()) {
            sb.append("import systems.courant.sd.model.def.LookupTableDef;\n");
        }
        if (!definition.stocks().isEmpty()) {
            sb.append("import systems.courant.sd.model.def.StockDef;\n");
        }
        boolean needsList = needsListImport(definition);
        if (!definition.modules().isEmpty()) {
            sb.append("import systems.courant.sd.model.def.ModelDefinition;\n");
            sb.append("import systems.courant.sd.model.def.ModuleInstanceDef;\n");
            sb.append("import systems.courant.sd.model.def.SimulationSettings;\n");
            sb.append("\n");
            sb.append("import java.util.List;\n");
            sb.append("import java.util.Map;\n");
        } else if (needsList) {
            sb.append("\n");
            sb.append("import java.util.List;\n");
        }

        sb.append('\n');
    }

    private void emitClassJavadoc(StringBuilder sb, String sourceFileName,
                                  ModelMetadata metadata,
                                  List<String> importWarnings,
                                  List<String> validationErrors) {
        sb.append("/**\n");
        sb.append(" * Imported from: ").append(escapeHtml(sourceFileName)).append('\n');
        if (metadata.source() != null) {
            sb.append(" * Source: ").append(escapeHtml(metadata.source())).append('\n');
        }
        if (metadata.license() != null) {
            sb.append(" * License: ").append(escapeHtml(metadata.license())).append('\n');
        }
        if (metadata.author() != null) {
            sb.append(" * Author: ").append(escapeHtml(metadata.author())).append('\n');
        }
        sb.append(" *\n");
        sb.append(" * <p>Auto-generated by courant-tools ImportPipeline.\n");

        if (!importWarnings.isEmpty()) {
            sb.append(" *\n");
            sb.append(" * <p>Import warnings:\n");
            sb.append(" * <ul>\n");
            for (String w : importWarnings) {
                sb.append(" *   <li>").append(escapeHtml(w)).append("</li>\n");
            }
            sb.append(" * </ul>\n");
        }
        if (!validationErrors.isEmpty()) {
            sb.append(" *\n");
            sb.append(" * <p>Validation errors (model may need manual fixes):\n");
            sb.append(" * <ul>\n");
            for (String e : validationErrors) {
                sb.append(" *   <li>").append(escapeHtml(e)).append("</li>\n");
            }
            sb.append(" * </ul>\n");
        }

        sb.append(" */\n");
    }

    private void emitClassOpen(StringBuilder sb, String className) {
        sb.append("public class ").append(className).append(" {\n\n");
    }

    private void emitMainMethod(StringBuilder sb, String className) {
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        new ").append(className).append("().run();\n");
        sb.append("    }\n\n");
    }

    private void emitRunMethod(StringBuilder sb, ModelDefinition definition,
                               ModelMetadata metadata) {
        sb.append("    public void run() {\n");
        sb.append(INDENT).append("var builder = new ModelDefinitionBuilder()\n");
        sb.append(INDENT2).append(".name(").append(escapeString(definition.name())).append(")");

        if (definition.defaultSimulation() != null) {
            SimulationSettings sim = definition.defaultSimulation();
            sb.append("\n").append(INDENT2)
                    .append(".defaultSimulation(")
                    .append(escapeString(sim.timeStep())).append(", ")
                    .append(sim.duration()).append(", ")
                    .append(escapeString(sim.durationUnit()));
            if (sim.dt() != 1.0) {
                sb.append(", ").append(sim.dt());
            }
            sb.append(")");
        }
        sb.append(";\n\n");

        // Stocks
        if (!definition.stocks().isEmpty()) {
            sb.append(INDENT).append("// Stocks\n");
            for (StockDef stock : definition.stocks()) {
                emitStockDef(sb, stock, INDENT, "builder");
            }
            sb.append('\n');
        }

        // Constants (literal-valued variables)
        List<VariableDef> literals = definition.variables().stream()
                .filter(VariableDef::isLiteral).toList();
        if (!literals.isEmpty()) {
            sb.append(INDENT).append("// Constants\n");
            for (VariableDef constant : literals) {
                if (constant.subscripts().isEmpty()) {
                    sb.append(INDENT).append("builder.constant(")
                            .append(escapeString(constant.name())).append(", ")
                            .append(constant.literalValue()).append(", ")
                            .append(escapeString(constant.unit()))
                            .append(");\n");
                } else {
                    // Use full VariableDef constructor to preserve subscripts
                    emitVariableDef(sb, constant, INDENT, "builder");
                }
            }
            sb.append('\n');
        }

        // Lookup tables
        if (!definition.lookupTables().isEmpty()) {
            sb.append(INDENT).append("// Lookup tables\n");
            for (LookupTableDef table : definition.lookupTables()) {
                sb.append(INDENT).append("builder.lookupTable(new LookupTableDef(")
                        .append(escapeString(table.name())).append(", ")
                        .append(escapeString(table.comment())).append(", ")
                        .append(doubleArrayLiteral(table.xValues())).append(", ")
                        .append(doubleArrayLiteral(table.yValues())).append(", ")
                        .append(escapeString(table.interpolation()))
                        .append("));\n");
            }
            sb.append('\n');
        }

        // Variables (non-literal only; literals were emitted as constants above)
        List<VariableDef> formulas = definition.variables().stream()
                .filter(a -> !a.isLiteral()).toList();
        if (!formulas.isEmpty()) {
            sb.append(INDENT).append("// Variables\n");
            for (VariableDef v : formulas) {
                emitVariableDef(sb, v, INDENT, "builder");
            }
            sb.append('\n');
        }

        // Flows
        if (!definition.flows().isEmpty()) {
            sb.append(INDENT).append("// Flows\n");
            for (FlowDef flow : definition.flows()) {
                emitFlowDef(sb, flow, INDENT, "builder");
            }
            sb.append('\n');
        }

        // Modules
        if (!definition.modules().isEmpty()) {
            sb.append(INDENT).append("// Modules\n");
            for (ModuleInstanceDef module : definition.modules()) {
                emitModuleInstance(sb, module);
            }
            sb.append('\n');
        }

        // Compile and set metadata
        sb.append(INDENT).append("var definition = builder.build();\n");
        sb.append(INDENT).append("var compiled = new ModelCompiler().compile(definition);\n\n");

        sb.append(INDENT).append("compiled.getModel().setMetadata(ModelMetadata.builder()\n");
        if (metadata.author() != null) {
            sb.append(INDENT2).append(".author(").append(escapeString(metadata.author())).append(")\n");
        }
        if (metadata.source() != null) {
            sb.append(INDENT2).append(".source(").append(escapeString(metadata.source())).append(")\n");
        }
        if (metadata.license() != null) {
            sb.append(INDENT2).append(".license(").append(escapeString(metadata.license())).append(")\n");
        }
        if (metadata.url() != null) {
            sb.append(INDENT2).append(".url(").append(escapeString(metadata.url())).append(")\n");
        }
        sb.append(INDENT2).append(".build());\n\n");

        // Create and run simulation
        sb.append(INDENT).append("Simulation sim = compiled.createSimulation();\n");
        sb.append(INDENT).append("sim.execute();\n");

        sb.append("    }\n");
    }

    private void emitModuleInstance(StringBuilder sb, ModuleInstanceDef module) {
        emitModuleInstance(sb, module, INDENT, "builder");
    }

    private void emitModuleInstance(StringBuilder sb, ModuleInstanceDef module,
                                    String indent, String parentBuilderName) {
        // Generate the inner module definition inline
        String varName = JavaSourceEscaper.toValidIdentifier(module.instanceName()) + "Def";
        ModelDefinition inner = module.definition();

        sb.append(indent).append("{\n");
        String blockIndent = indent + "    ";
        String chainIndent = indent + "            ";
        sb.append(blockIndent).append("var innerBuilder = new ModelDefinitionBuilder()\n");
        sb.append(chainIndent).append(".name(").append(escapeString(inner.name())).append(")");
        if (inner.defaultSimulation() != null) {
            SimulationSettings sim = inner.defaultSimulation();
            sb.append("\n").append(chainIndent).append(".defaultSimulation(")
                    .append(escapeString(sim.timeStep())).append(", ")
                    .append(sim.duration()).append(", ")
                    .append(escapeString(sim.durationUnit()));
            if (sim.dt() != 1.0) {
                sb.append(", ").append(sim.dt());
            }
            sb.append(")");
        }
        sb.append(";\n");

        // Emit inner stocks, constants, lookups, auxes, flows
        for (StockDef stock : inner.stocks()) {
            emitStockDef(sb, stock, blockIndent, "innerBuilder");
        }
        for (VariableDef constant : inner.variables().stream().filter(VariableDef::isLiteral).toList()) {
            if (constant.subscripts().isEmpty()) {
                sb.append(blockIndent).append("innerBuilder.constant(")
                        .append(escapeString(constant.name())).append(", ")
                        .append(constant.literalValue()).append(", ")
                        .append(escapeString(constant.unit()))
                        .append(");\n");
            } else {
                emitVariableDef(sb, constant, blockIndent, "innerBuilder");
            }
        }
        for (LookupTableDef table : inner.lookupTables()) {
            sb.append(blockIndent).append("innerBuilder.lookupTable(new LookupTableDef(")
                    .append(escapeString(table.name())).append(", ")
                    .append(escapeString(table.comment())).append(", ")
                    .append(doubleArrayLiteral(table.xValues())).append(", ")
                    .append(doubleArrayLiteral(table.yValues())).append(", ")
                    .append(escapeString(table.interpolation()))
                    .append("));\n");
        }
        for (VariableDef v : inner.variables().stream().filter(a -> !a.isLiteral()).toList()) {
            emitVariableDef(sb, v, blockIndent, "innerBuilder");
        }
        for (FlowDef flow : inner.flows()) {
            emitFlowDef(sb, flow, blockIndent, "innerBuilder");
        }
        // Nested modules (#273)
        for (ModuleInstanceDef nestedModule : inner.modules()) {
            emitModuleInstance(sb, nestedModule, blockIndent, "innerBuilder");
        }

        sb.append(blockIndent).append("ModelDefinition ").append(varName)
                .append(" = innerBuilder.build();\n");

        // Emit bindings
        sb.append(blockIndent).append(parentBuilderName).append(".module(new ModuleInstanceDef(\n");
        sb.append(chainIndent).append(escapeString(module.instanceName())).append(",\n");
        sb.append(chainIndent).append(varName).append(",\n");
        emitMapLiteral(sb, module.inputBindings(), chainIndent);
        sb.append(",\n");
        emitMapLiteral(sb, module.outputBindings(), chainIndent);
        sb.append("));\n");
        sb.append(indent).append("}\n");
    }

    /**
     * Emits a StockDef constructor call, using the canonical constructor when initialExpression
     * or subscripts are present, and the backward-compatible constructor otherwise.
     */
    private void emitStockDef(StringBuilder sb, StockDef stock, String indent, String builderName) {
        if (stock.initialExpression() != null || !stock.subscripts().isEmpty()) {
            // Use canonical 7-arg constructor to preserve initialExpression and subscripts
            sb.append(indent).append(builderName).append(".stock(new StockDef(")
                    .append(escapeString(stock.name())).append(", ")
                    .append(escapeString(stock.comment())).append(", ")
                    .append(formatDoubleForSource(stock.initialValue())).append(", ")
                    .append(escapeString(stock.initialExpression())).append(", ")
                    .append(escapeString(stock.unit())).append(", ")
                    .append(escapeString(stock.negativeValuePolicy())).append(", ")
                    .append(emitStringList(stock.subscripts()))
                    .append("));\n");
        } else {
            sb.append(indent).append(builderName).append(".stock(new StockDef(")
                    .append(escapeString(stock.name())).append(", ")
                    .append(escapeString(stock.comment())).append(", ")
                    .append(formatDoubleForSource(stock.initialValue())).append(", ")
                    .append(escapeString(stock.unit())).append(", ")
                    .append(escapeString(stock.negativeValuePolicy()))
                    .append("));\n");
        }
    }

    /**
     * Emits a VariableDef constructor call, using the canonical constructor when subscripts
     * are present.
     */
    private void emitVariableDef(StringBuilder sb, VariableDef v, String indent, String builderName) {
        if (!v.subscripts().isEmpty()) {
            sb.append(indent).append(builderName).append(".variable(new VariableDef(")
                    .append(escapeString(v.name())).append(", ")
                    .append(escapeString(v.comment())).append(", ")
                    .append(escapeString(v.equation())).append(", ")
                    .append(escapeString(v.unit())).append(", ")
                    .append(emitStringList(v.subscripts()))
                    .append("));\n");
        } else {
            sb.append(indent).append(builderName).append(".variable(new VariableDef(")
                    .append(escapeString(v.name())).append(", ")
                    .append(escapeString(v.comment())).append(", ")
                    .append(escapeString(v.equation())).append(", ")
                    .append(escapeString(v.unit()))
                    .append("));\n");
        }
    }

    /**
     * Emits a FlowDef constructor call, using the canonical constructor when subscripts
     * are present.
     */
    private void emitFlowDef(StringBuilder sb, FlowDef flow, String indent, String builderName) {
        if (!flow.subscripts().isEmpty()) {
            sb.append(indent).append(builderName).append(".flow(new FlowDef(")
                    .append(escapeString(flow.name())).append(", ")
                    .append(escapeString(flow.comment())).append(", ")
                    .append(escapeString(flow.equation())).append(", ")
                    .append(escapeString(flow.timeUnit())).append(", ")
                    .append(escapeString(flow.source())).append(", ")
                    .append(escapeString(flow.sink())).append(", ")
                    .append(emitStringList(flow.subscripts()))
                    .append("));\n");
        } else {
            sb.append(indent).append(builderName).append(".flow(new FlowDef(")
                    .append(escapeString(flow.name())).append(", ")
                    .append(escapeString(flow.comment())).append(", ")
                    .append(escapeString(flow.equation())).append(", ")
                    .append(escapeString(flow.timeUnit())).append(", ")
                    .append(escapeString(flow.source())).append(", ")
                    .append(escapeString(flow.sink()))
                    .append("));\n");
        }
    }

    /**
     * Formats a double for Java source code. Handles NaN and Infinity which cannot be
     * written as bare literals.
     */
    private static String formatDoubleForSource(double value) {
        if (Double.isNaN(value)) {
            return "Double.NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
        }
        return String.valueOf(value);
    }

    /**
     * Emits a {@code List.of("a", "b")} literal for a list of strings.
     */
    private static String emitStringList(List<String> items) {
        if (items.isEmpty()) {
            return "List.of()";
        }
        StringBuilder sb = new StringBuilder("List.of(");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(escapeString(items.get(i)));
        }
        sb.append(')');
        return sb.toString();
    }

    private void emitMapLiteral(StringBuilder sb, Map<String, String> map, String indent) {
        if (map.isEmpty()) {
            sb.append(indent).append("Map.of()");
        } else if (map.size() <= 10) {
            sb.append(indent).append("Map.of(");
            boolean first = true;
            for (var entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(escapeString(entry.getKey())).append(", ")
                        .append(escapeString(entry.getValue()));
                first = false;
            }
            sb.append(')');
        } else {
            sb.append(indent).append("Map.ofEntries(\n");
            boolean first = true;
            for (var entry : map.entrySet()) {
                if (!first) {
                    sb.append(",\n");
                }
                sb.append(indent).append("    Map.entry(")
                        .append(escapeString(entry.getKey())).append(", ")
                        .append(escapeString(entry.getValue())).append(')');
                first = false;
            }
            sb.append(')');
        }
    }

    private static boolean needsListImport(ModelDefinition definition) {
        // List is needed for subscripts or for the 7-arg StockDef constructor
        // (used when initialExpression is present)
        return definition.stocks().stream().anyMatch(
                        s -> !s.subscripts().isEmpty() || s.initialExpression() != null)
                || definition.variables().stream().anyMatch(v -> !v.subscripts().isEmpty())
                || definition.flows().stream().anyMatch(f -> !f.subscripts().isEmpty());
    }

    static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("*/", "&#42;/");
    }

    private void emitClassClose(StringBuilder sb) {
        sb.append("}\n");
    }
}
