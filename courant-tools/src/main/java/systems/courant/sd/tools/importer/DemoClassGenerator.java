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
        JavaCodeBuilder cb = new JavaCodeBuilder();

        emitLicenseHeader(cb, metadata);
        emitPackage(cb, packageName);
        emitImports(cb, definition);
        emitClassJavadoc(cb, sourceFileName, metadata, importWarnings, validationErrors);
        cb.line("public class " + className + " {");
        cb.blankLine();
        cb.indent();
        emitMainMethod(cb, className);
        emitRunMethod(cb, definition, metadata);
        cb.dedent();
        cb.line("}");

        return cb.toString();
    }

    private void emitLicenseHeader(JavaCodeBuilder cb, ModelMetadata metadata) {
        String license = metadata.license();
        if (license != null && license.contains("-NC")) {
            cb.raw("/*\n");
            cb.raw(" * Copyright (c) original author(s). See model metadata for attribution.\n");
            cb.raw(" * Licensed under CC-BY-NC-SA-4.0. See THIRD-PARTY-LICENSES for details.\n");
            cb.raw(" */\n");
        } else {
            cb.raw("/*\n");
            cb.raw(" * Copyright (c) " + copyrightYear + " Courant Systems\n");
            cb.raw(" * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.\n");
            cb.raw(" */\n");
        }
    }

    private void emitPackage(JavaCodeBuilder cb, String packageName) {
        cb.line("package " + packageName + ";");
        cb.blankLine();
    }

    private void emitImports(JavaCodeBuilder cb, ModelDefinition definition) {
        cb.line("import systems.courant.sd.Simulation;");
        cb.line("import systems.courant.sd.model.ModelMetadata;");
        cb.line("import systems.courant.sd.model.compile.ModelCompiler;");
        cb.line("import systems.courant.sd.model.def.ModelDefinitionBuilder;");

        if (!definition.variables().isEmpty()) {
            cb.line("import systems.courant.sd.model.def.VariableDef;");
        }
        if (!definition.flows().isEmpty()) {
            cb.line("import systems.courant.sd.model.def.FlowDef;");
        }
        if (!definition.lookupTables().isEmpty()) {
            cb.line("import systems.courant.sd.model.def.LookupTableDef;");
        }
        if (!definition.stocks().isEmpty()) {
            cb.line("import systems.courant.sd.model.def.StockDef;");
        }
        boolean needsList = needsListImport(definition);
        boolean needsSimSettings = needsFullSimulationConstructor(definition.defaultSimulation());
        if (!definition.modules().isEmpty()) {
            cb.line("import systems.courant.sd.model.def.ModelDefinition;");
            cb.line("import systems.courant.sd.model.def.ModuleInstanceDef;");
            cb.line("import systems.courant.sd.model.def.SimulationSettings;");
            cb.blankLine();
            cb.line("import java.util.List;");
            cb.line("import java.util.Map;");
        } else {
            if (needsSimSettings) {
                cb.line("import systems.courant.sd.model.def.SimulationSettings;");
            }
            if (needsList) {
                cb.blankLine();
                cb.line("import java.util.List;");
            }
        }

        cb.blankLine();
    }

    private void emitClassJavadoc(JavaCodeBuilder cb, String sourceFileName,
                                  ModelMetadata metadata,
                                  List<String> importWarnings,
                                  List<String> validationErrors) {
        cb.raw("/**\n");
        cb.raw(" * Imported from: " + escapeHtml(sourceFileName) + "\n");
        if (metadata.source() != null) {
            cb.raw(" * Source: " + escapeHtml(metadata.source()) + "\n");
        }
        if (metadata.license() != null) {
            cb.raw(" * License: " + escapeHtml(metadata.license()) + "\n");
        }
        if (metadata.author() != null) {
            cb.raw(" * Author: " + escapeHtml(metadata.author()) + "\n");
        }
        cb.raw(" *\n");
        cb.raw(" * <p>Auto-generated by courant-tools ImportPipeline.\n");

        if (!importWarnings.isEmpty()) {
            cb.raw(" *\n");
            cb.raw(" * <p>Import warnings:\n");
            cb.raw(" * <ul>\n");
            for (String w : importWarnings) {
                cb.raw(" *   <li>" + escapeHtml(w) + "</li>\n");
            }
            cb.raw(" * </ul>\n");
        }
        if (!validationErrors.isEmpty()) {
            cb.raw(" *\n");
            cb.raw(" * <p>Validation errors (model may need manual fixes):\n");
            cb.raw(" * <ul>\n");
            for (String e : validationErrors) {
                cb.raw(" *   <li>" + escapeHtml(e) + "</li>\n");
            }
            cb.raw(" * </ul>\n");
        }

        cb.raw(" */\n");
    }

    private void emitMainMethod(JavaCodeBuilder cb, String className) {
        cb.line("public static void main(String[] args) {");
        cb.indent();
        cb.line("new " + className + "().run();");
        cb.dedent();
        cb.line("}");
        cb.blankLine();
    }

    private void emitRunMethod(JavaCodeBuilder cb, ModelDefinition definition,
                               ModelMetadata metadata) {
        cb.line("public void run() {");
        cb.indent();
        String chainIndent = cb.indentAt(2);

        cb.line("var builder = new ModelDefinitionBuilder()");
        cb.raw(chainIndent + ".name(" + escapeString(definition.name()) + ")");

        if (definition.defaultSimulation() != null) {
            emitDefaultSimulation(cb, definition.defaultSimulation(), chainIndent);
        }
        cb.raw(";\n");
        cb.blankLine();

        // Stocks
        if (!definition.stocks().isEmpty()) {
            cb.line("// Stocks");
            for (StockDef stock : definition.stocks()) {
                emitStockDef(cb, stock, "builder");
            }
            cb.blankLine();
        }

        // Constants (literal-valued variables)
        List<VariableDef> literals = definition.variables().stream()
                .filter(VariableDef::isLiteral).toList();
        if (!literals.isEmpty()) {
            cb.line("// Constants");
            for (VariableDef constant : literals) {
                if (constant.subscripts().isEmpty()) {
                    cb.line("builder.constant("
                            + escapeString(constant.name()) + ", "
                            + constant.literalValue() + ", "
                            + escapeString(constant.unit())
                            + ");");
                } else {
                    // Use full VariableDef constructor to preserve subscripts
                    emitVariableDef(cb, constant, "builder");
                }
            }
            cb.blankLine();
        }

        // Lookup tables
        if (!definition.lookupTables().isEmpty()) {
            cb.line("// Lookup tables");
            for (LookupTableDef table : definition.lookupTables()) {
                emitLookupTableDef(cb, table, "builder");
            }
            cb.blankLine();
        }

        // Variables (non-literal only; literals were emitted as constants above)
        List<VariableDef> formulas = definition.variables().stream()
                .filter(a -> !a.isLiteral()).toList();
        if (!formulas.isEmpty()) {
            cb.line("// Variables");
            for (VariableDef v : formulas) {
                emitVariableDef(cb, v, "builder");
            }
            cb.blankLine();
        }

        // Flows
        if (!definition.flows().isEmpty()) {
            cb.line("// Flows");
            for (FlowDef flow : definition.flows()) {
                emitFlowDef(cb, flow, "builder");
            }
            cb.blankLine();
        }

        // Modules
        if (!definition.modules().isEmpty()) {
            cb.line("// Modules");
            for (ModuleInstanceDef module : definition.modules()) {
                emitModuleInstance(cb, module, "builder");
            }
            cb.blankLine();
        }

        // Compile and set metadata
        cb.line("var definition = builder.build();");
        cb.line("var compiled = new ModelCompiler().compile(definition);");
        cb.blankLine();

        cb.raw(cb.currentIndent() + "compiled.getModel().setMetadata(ModelMetadata.builder()\n");
        if (metadata.author() != null) {
            cb.raw(chainIndent + ".author(" + escapeString(metadata.author()) + ")\n");
        }
        if (metadata.source() != null) {
            cb.raw(chainIndent + ".source(" + escapeString(metadata.source()) + ")\n");
        }
        if (metadata.license() != null) {
            cb.raw(chainIndent + ".license(" + escapeString(metadata.license()) + ")\n");
        }
        if (metadata.url() != null) {
            cb.raw(chainIndent + ".url(" + escapeString(metadata.url()) + ")\n");
        }
        cb.raw(chainIndent + ".build());\n");
        cb.blankLine();

        // Create and run simulation
        cb.line("Simulation sim = compiled.createSimulation();");
        cb.line("sim.execute();");

        cb.dedent();
        cb.line("}");
    }

    private void emitModuleInstance(JavaCodeBuilder cb, ModuleInstanceDef module,
                                    String parentBuilderName) {
        String varName = JavaSourceEscaper.toValidIdentifier(module.instanceName()) + "Def";
        ModelDefinition inner = module.definition();

        cb.line("{");
        cb.indent();
        String chainIndent = cb.indentAt(2);
        cb.line("var innerBuilder = new ModelDefinitionBuilder()");
        cb.raw(chainIndent + ".name(" + escapeString(inner.name()) + ")");
        if (inner.defaultSimulation() != null) {
            emitDefaultSimulation(cb, inner.defaultSimulation(), chainIndent);
        }
        cb.raw(";\n");

        // Emit inner stocks, constants, lookups, auxes, flows
        for (StockDef stock : inner.stocks()) {
            emitStockDef(cb, stock, "innerBuilder");
        }
        for (VariableDef constant : inner.variables().stream().filter(VariableDef::isLiteral).toList()) {
            if (constant.subscripts().isEmpty()) {
                cb.line("innerBuilder.constant("
                        + escapeString(constant.name()) + ", "
                        + constant.literalValue() + ", "
                        + escapeString(constant.unit())
                        + ");");
            } else {
                emitVariableDef(cb, constant, "innerBuilder");
            }
        }
        for (LookupTableDef table : inner.lookupTables()) {
            emitLookupTableDef(cb, table, "innerBuilder");
        }
        for (VariableDef v : inner.variables().stream().filter(a -> !a.isLiteral()).toList()) {
            emitVariableDef(cb, v, "innerBuilder");
        }
        for (FlowDef flow : inner.flows()) {
            emitFlowDef(cb, flow, "innerBuilder");
        }
        // Nested modules (#273)
        for (ModuleInstanceDef nestedModule : inner.modules()) {
            emitModuleInstance(cb, nestedModule, "innerBuilder");
        }

        cb.line("ModelDefinition " + varName + " = innerBuilder.build();");

        // Emit bindings
        cb.raw(cb.currentIndent() + parentBuilderName + ".module(new ModuleInstanceDef(\n");
        cb.raw(chainIndent + escapeString(module.instanceName()) + ",\n");
        cb.raw(chainIndent + varName + ",\n");
        emitMapLiteral(cb, module.inputBindings(), chainIndent);
        cb.raw(",\n");
        emitMapLiteral(cb, module.outputBindings(), chainIndent);
        cb.raw("));\n");
        cb.dedent();
        cb.line("}");
    }

    private static boolean needsFullSimulationConstructor(SimulationSettings sim) {
        return sim != null && (sim.strictMode() || sim.savePer() != 1 || sim.initialTime() != 0.0);
    }

    private void emitDefaultSimulation(JavaCodeBuilder cb, SimulationSettings sim,
                                       String chainIndent) {
        if (needsFullSimulationConstructor(sim)) {
            cb.raw("\n" + chainIndent
                    + ".defaultSimulation(new SimulationSettings("
                    + escapeString(sim.timeStep()) + ", "
                    + sim.duration() + ", "
                    + escapeString(sim.durationUnit()) + ", "
                    + sim.dt() + ", "
                    + sim.strictMode() + ", "
                    + sim.savePer() + ", "
                    + sim.initialTime()
                    + "))");
        } else {
            cb.raw("\n" + chainIndent
                    + ".defaultSimulation("
                    + escapeString(sim.timeStep()) + ", "
                    + sim.duration() + ", "
                    + escapeString(sim.durationUnit()));
            if (sim.dt() != 1.0) {
                cb.raw(", " + sim.dt());
            }
            cb.raw(")");
        }
    }

    /**
     * Emits a StockDef constructor call, using the canonical constructor when initialExpression
     * or subscripts are present, and the backward-compatible constructor otherwise.
     */
    private void emitStockDef(JavaCodeBuilder cb, StockDef stock, String builderName) {
        if (stock.initialExpression() != null || !stock.subscripts().isEmpty()) {
            // Use canonical 7-arg constructor to preserve initialExpression and subscripts
            cb.line(builderName + ".stock(new StockDef("
                    + escapeString(stock.name()) + ", "
                    + escapeString(stock.comment()) + ", "
                    + formatDoubleForSource(stock.initialValue()) + ", "
                    + escapeString(stock.initialExpression()) + ", "
                    + escapeString(stock.unit()) + ", "
                    + escapeString(stock.negativeValuePolicy()) + ", "
                    + emitStringList(stock.subscripts())
                    + "));");
        } else {
            cb.line(builderName + ".stock(new StockDef("
                    + escapeString(stock.name()) + ", "
                    + escapeString(stock.comment()) + ", "
                    + formatDoubleForSource(stock.initialValue()) + ", "
                    + escapeString(stock.unit()) + ", "
                    + escapeString(stock.negativeValuePolicy())
                    + "));");
        }
    }

    /**
     * Emits a VariableDef constructor call, using the canonical constructor when subscripts
     * are present.
     */
    private void emitVariableDef(JavaCodeBuilder cb, VariableDef v, String builderName) {
        if (!v.subscripts().isEmpty()) {
            cb.line(builderName + ".variable(new VariableDef("
                    + escapeString(v.name()) + ", "
                    + escapeString(v.comment()) + ", "
                    + escapeString(v.equation()) + ", "
                    + escapeString(v.unit()) + ", "
                    + emitStringList(v.subscripts())
                    + "));");
        } else {
            cb.line(builderName + ".variable(new VariableDef("
                    + escapeString(v.name()) + ", "
                    + escapeString(v.comment()) + ", "
                    + escapeString(v.equation()) + ", "
                    + escapeString(v.unit())
                    + "));");
        }
    }

    /**
     * Emits a FlowDef constructor call, using the canonical constructor when subscripts
     * are present.
     */
    private void emitFlowDef(JavaCodeBuilder cb, FlowDef flow, String builderName) {
        if (!flow.subscripts().isEmpty()) {
            cb.line(builderName + ".flow(new FlowDef("
                    + escapeString(flow.name()) + ", "
                    + escapeString(flow.comment()) + ", "
                    + escapeString(flow.equation()) + ", "
                    + escapeString(flow.timeUnit()) + ", "
                    + escapeString(flow.source()) + ", "
                    + escapeString(flow.sink()) + ", "
                    + emitStringList(flow.subscripts())
                    + "));");
        } else {
            cb.line(builderName + ".flow(new FlowDef("
                    + escapeString(flow.name()) + ", "
                    + escapeString(flow.comment()) + ", "
                    + escapeString(flow.equation()) + ", "
                    + escapeString(flow.timeUnit()) + ", "
                    + escapeString(flow.source()) + ", "
                    + escapeString(flow.sink())
                    + "));");
        }
    }

    /**
     * Emits a LookupTableDef constructor call, including unit when present.
     */
    private void emitLookupTableDef(JavaCodeBuilder cb, LookupTableDef table, String builderName) {
        String line = builderName + ".lookupTable(new LookupTableDef("
                + escapeString(table.name()) + ", "
                + escapeString(table.comment()) + ", "
                + doubleArrayLiteral(table.xValues()) + ", "
                + doubleArrayLiteral(table.yValues()) + ", "
                + escapeString(table.interpolation());
        if (table.unit() != null) {
            line += ", " + escapeString(table.unit());
        }
        cb.line(line + "));");
    }

    private static String formatDoubleForSource(double value) {
        return JavaSourceEscaper.formatDoubleForSource(value);
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

    private void emitMapLiteral(JavaCodeBuilder cb, Map<String, String> map, String indent) {
        if (map.isEmpty()) {
            cb.raw(indent + "Map.of()");
        } else if (map.size() <= 10) {
            cb.raw(indent + "Map.of(");
            boolean first = true;
            for (var entry : map.entrySet()) {
                if (!first) {
                    cb.raw(", ");
                }
                cb.raw(escapeString(entry.getKey()) + ", "
                        + escapeString(entry.getValue()));
                first = false;
            }
            cb.raw(")");
        } else {
            cb.raw(indent + "Map.ofEntries(\n");
            boolean first = true;
            for (var entry : map.entrySet()) {
                if (!first) {
                    cb.raw(",\n");
                }
                cb.raw(indent + "    Map.entry("
                        + escapeString(entry.getKey()) + ", "
                        + escapeString(entry.getValue()) + ")");
                first = false;
            }
            cb.raw(")");
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
}
