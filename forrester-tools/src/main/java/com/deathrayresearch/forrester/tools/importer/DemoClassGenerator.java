package com.deathrayresearch.forrester.tools.importer;

import com.deathrayresearch.forrester.model.ModelMetadata;
import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.StockDef;

import java.util.List;
import java.util.Map;

import static com.deathrayresearch.forrester.tools.importer.JavaSourceEscaper.doubleArrayLiteral;
import static com.deathrayresearch.forrester.tools.importer.JavaSourceEscaper.escapeString;

/**
 * Generates a Java demo class from a {@link ModelDefinition} and {@link ModelMetadata}.
 *
 * <p>The generated class uses {@code ModelDefinitionBuilder} to reconstruct the model
 * and {@code ModelCompiler} to compile it at runtime.
 */
public class DemoClassGenerator {

    private static final String INDENT = "        ";
    private static final String INDENT2 = "                ";

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

        emitPackage(sb, packageName);
        emitImports(sb, definition);
        emitClassJavadoc(sb, sourceFileName, metadata, importWarnings, validationErrors);
        emitClassOpen(sb, className);
        emitMainMethod(sb, className);
        emitRunMethod(sb, definition, metadata);
        emitClassClose(sb);

        return sb.toString();
    }

    private void emitPackage(StringBuilder sb, String packageName) {
        sb.append("package ").append(packageName).append(";\n\n");
    }

    private void emitImports(StringBuilder sb, ModelDefinition definition) {
        sb.append("import com.deathrayresearch.forrester.Simulation;\n");
        sb.append("import com.deathrayresearch.forrester.model.ModelMetadata;\n");
        sb.append("import com.deathrayresearch.forrester.model.compile.CompiledModel;\n");
        sb.append("import com.deathrayresearch.forrester.model.compile.ModelCompiler;\n");
        sb.append("import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;\n");

        if (!definition.constants().isEmpty()) {
            sb.append("import com.deathrayresearch.forrester.model.def.ConstantDef;\n");
        }
        if (!definition.auxiliaries().isEmpty()) {
            sb.append("import com.deathrayresearch.forrester.model.def.AuxDef;\n");
        }
        if (!definition.flows().isEmpty()) {
            sb.append("import com.deathrayresearch.forrester.model.def.FlowDef;\n");
        }
        if (!definition.lookupTables().isEmpty()) {
            sb.append("import com.deathrayresearch.forrester.model.def.LookupTableDef;\n");
        }
        if (!definition.stocks().isEmpty()) {
            sb.append("import com.deathrayresearch.forrester.model.def.StockDef;\n");
        }
        if (!definition.modules().isEmpty()) {
            sb.append("import com.deathrayresearch.forrester.model.def.ModelDefinition;\n");
            sb.append("import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;\n");
            sb.append("import com.deathrayresearch.forrester.model.def.SimulationSettings;\n");
            sb.append("\n");
            sb.append("import java.util.List;\n");
            sb.append("import java.util.Map;\n");
        }

        sb.append('\n');
    }

    private void emitClassJavadoc(StringBuilder sb, String sourceFileName,
                                  ModelMetadata metadata,
                                  List<String> importWarnings,
                                  List<String> validationErrors) {
        sb.append("/**\n");
        sb.append(" * Imported from: ").append(sourceFileName).append('\n');
        if (metadata.source() != null) {
            sb.append(" * Source: ").append(metadata.source()).append('\n');
        }
        if (metadata.license() != null) {
            sb.append(" * License: ").append(metadata.license()).append('\n');
        }
        if (metadata.author() != null) {
            sb.append(" * Author: ").append(metadata.author()).append('\n');
        }
        sb.append(" *\n");
        sb.append(" * <p>Auto-generated by forrester-tools ImportPipeline.\n");

        if (!importWarnings.isEmpty()) {
            sb.append(" *\n");
            sb.append(" * <p>Import warnings:\n");
            sb.append(" * <ul>\n");
            for (String w : importWarnings) {
                sb.append(" *   <li>").append(w).append("</li>\n");
            }
            sb.append(" * </ul>\n");
        }
        if (!validationErrors.isEmpty()) {
            sb.append(" *\n");
            sb.append(" * <p>Validation errors (model may need manual fixes):\n");
            sb.append(" * <ul>\n");
            for (String e : validationErrors) {
                sb.append(" *   <li>").append(e).append("</li>\n");
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
                    .append(escapeString(sim.durationUnit())).append(")");
        }
        sb.append(";\n\n");

        // Stocks
        if (!definition.stocks().isEmpty()) {
            sb.append(INDENT).append("// Stocks\n");
            for (StockDef stock : definition.stocks()) {
                sb.append(INDENT).append("builder.stock(new StockDef(")
                        .append(escapeString(stock.name())).append(", ")
                        .append(escapeString(stock.comment())).append(", ")
                        .append(stock.initialValue()).append(", ")
                        .append(escapeString(stock.unit())).append(", ")
                        .append(escapeString(stock.negativeValuePolicy()))
                        .append("));\n");
            }
            sb.append('\n');
        }

        // Constants
        if (!definition.constants().isEmpty()) {
            sb.append(INDENT).append("// Constants\n");
            for (ConstantDef constant : definition.constants()) {
                sb.append(INDENT).append("builder.constant(new ConstantDef(")
                        .append(escapeString(constant.name())).append(", ")
                        .append(escapeString(constant.comment())).append(", ")
                        .append(constant.value()).append(", ")
                        .append(escapeString(constant.unit()))
                        .append("));\n");
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

        // Auxiliaries
        if (!definition.auxiliaries().isEmpty()) {
            sb.append(INDENT).append("// Auxiliaries\n");
            for (AuxDef aux : definition.auxiliaries()) {
                sb.append(INDENT).append("builder.aux(new AuxDef(")
                        .append(escapeString(aux.name())).append(", ")
                        .append(escapeString(aux.comment())).append(", ")
                        .append(escapeString(aux.equation())).append(", ")
                        .append(escapeString(aux.unit()))
                        .append("));\n");
            }
            sb.append('\n');
        }

        // Flows
        if (!definition.flows().isEmpty()) {
            sb.append(INDENT).append("// Flows\n");
            for (FlowDef flow : definition.flows()) {
                sb.append(INDENT).append("builder.flow(new FlowDef(")
                        .append(escapeString(flow.name())).append(", ")
                        .append(escapeString(flow.comment())).append(", ")
                        .append(escapeString(flow.equation())).append(", ")
                        .append(escapeString(flow.timeUnit())).append(", ")
                        .append(escapeString(flow.source())).append(", ")
                        .append(escapeString(flow.sink()))
                        .append("));\n");
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
        // Generate the inner module definition inline
        String varName = JavaSourceEscaper.toPackageSegment(module.instanceName()) + "Def";
        ModelDefinition inner = module.definition();

        sb.append(INDENT).append("{\n");
        sb.append(INDENT).append("    var innerBuilder = new ModelDefinitionBuilder()\n");
        sb.append(INDENT).append("            .name(").append(escapeString(inner.name())).append(")");
        if (inner.defaultSimulation() != null) {
            SimulationSettings sim = inner.defaultSimulation();
            sb.append("\n").append(INDENT).append("            .defaultSimulation(")
                    .append(escapeString(sim.timeStep())).append(", ")
                    .append(sim.duration()).append(", ")
                    .append(escapeString(sim.durationUnit())).append(")");
        }
        sb.append(";\n");

        // Emit inner stocks, constants, lookups, auxes, flows
        for (StockDef stock : inner.stocks()) {
            sb.append(INDENT).append("    innerBuilder.stock(new StockDef(")
                    .append(escapeString(stock.name())).append(", ")
                    .append(escapeString(stock.comment())).append(", ")
                    .append(stock.initialValue()).append(", ")
                    .append(escapeString(stock.unit())).append(", ")
                    .append(escapeString(stock.negativeValuePolicy()))
                    .append("));\n");
        }
        for (ConstantDef constant : inner.constants()) {
            sb.append(INDENT).append("    innerBuilder.constant(new ConstantDef(")
                    .append(escapeString(constant.name())).append(", ")
                    .append(escapeString(constant.comment())).append(", ")
                    .append(constant.value()).append(", ")
                    .append(escapeString(constant.unit()))
                    .append("));\n");
        }
        for (LookupTableDef table : inner.lookupTables()) {
            sb.append(INDENT).append("    innerBuilder.lookupTable(new LookupTableDef(")
                    .append(escapeString(table.name())).append(", ")
                    .append(escapeString(table.comment())).append(", ")
                    .append(doubleArrayLiteral(table.xValues())).append(", ")
                    .append(doubleArrayLiteral(table.yValues())).append(", ")
                    .append(escapeString(table.interpolation()))
                    .append("));\n");
        }
        for (AuxDef aux : inner.auxiliaries()) {
            sb.append(INDENT).append("    innerBuilder.aux(new AuxDef(")
                    .append(escapeString(aux.name())).append(", ")
                    .append(escapeString(aux.comment())).append(", ")
                    .append(escapeString(aux.equation())).append(", ")
                    .append(escapeString(aux.unit()))
                    .append("));\n");
        }
        for (FlowDef flow : inner.flows()) {
            sb.append(INDENT).append("    innerBuilder.flow(new FlowDef(")
                    .append(escapeString(flow.name())).append(", ")
                    .append(escapeString(flow.comment())).append(", ")
                    .append(escapeString(flow.equation())).append(", ")
                    .append(escapeString(flow.timeUnit())).append(", ")
                    .append(escapeString(flow.source())).append(", ")
                    .append(escapeString(flow.sink()))
                    .append("));\n");
        }

        sb.append(INDENT).append("    ModelDefinition ").append(varName)
                .append(" = innerBuilder.build();\n");

        // Emit bindings
        sb.append(INDENT).append("    builder.module(new ModuleInstanceDef(\n");
        sb.append(INDENT).append("            ").append(escapeString(module.instanceName())).append(",\n");
        sb.append(INDENT).append("            ").append(varName).append(",\n");
        emitMapLiteral(sb, module.inputBindings(), INDENT + "            ");
        sb.append(",\n");
        emitMapLiteral(sb, module.outputBindings(), INDENT + "            ");
        sb.append("));\n");
        sb.append(INDENT).append("}\n");
    }

    private void emitMapLiteral(StringBuilder sb, Map<String, String> map, String indent) {
        if (map.isEmpty()) {
            sb.append(indent).append("Map.of()");
        } else {
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
        }
    }

    private void emitClassClose(StringBuilder sb) {
        sb.append("}\n");
    }
}
