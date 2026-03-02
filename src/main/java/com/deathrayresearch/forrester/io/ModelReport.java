package com.deathrayresearch.forrester.io;

import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;

import java.util.Map;

/**
 * Generates a plain-text summary report of a model's structure, listing its stocks,
 * inflows, outflows, constants, and variables.
 */
public class ModelReport {

    private static final char LF = '\n';

    /**
     * Creates a human-readable text report describing the given model's structure.
     *
     * @param model the model to report on
     * @return a formatted string summarizing the model's stocks, flows, constants, and variables
     */
    public static String create(Model model) {

        StringBuilder builder = new StringBuilder();
        builder.append("Model Report ")
            .append(LF)
            .append(LF)
            .append("Model: ")
            .append(model.getName())
            .append(LF)
            .append(LF)
            .append("Stocks:")
            .append(LF);
        for (Stock stock : model.getStocks()) {
            builder.append("Stock: ")
                .append(stock.getName())
                .append(LF);
            if (stock.getInflows().isEmpty()) {
                builder.append("No Inflows")
                    .append(LF);
            } else {
                builder.append("Inflows:")
                    .append(LF);
                for (Flow flow : stock.getInflows()) {
                    builder.append(flow.getName())
                        .append(LF);
                }
                builder.append(LF);
            }
            if (stock.getOutflows().isEmpty()) {
                builder.append("No Outflows")
                    .append(LF)
                    .append(LF);
            } else {
                builder.append("Outflows:")
                    .append(LF);
                for (Flow flow : stock.getOutflows()) {
                    builder.append(flow.getName())
                        .append(LF);
                }
                builder.append(LF);
            }
        }
        builder.append("Constants:");
        for (Constant constant : model.getConstants()) {
            builder.append(constant.getName())
                .append(LF);
        }

        builder.append(LF);
        builder.append("Variables:");
        for (Map.Entry<String, Variable> entry : model.getVariableMap().entrySet()) {
            builder.append(entry.getKey())
                .append(": ")
                .append(entry.getValue().getName())
                .append(LF);
        }

        // Modules (hierarchical)
        if (!model.getModules().isEmpty()) {
            builder.append(LF).append("Modules:").append(LF);
            for (Module module : model.getModules()) {
                appendModule(builder, module, "  ");
            }
        }

        return builder.toString();
    }

    private static void appendModule(StringBuilder builder, Module module, String indent) {
        builder.append(indent).append("Module: ").append(module.getName()).append(LF);
        for (Stock stock : module.getStocks()) {
            builder.append(indent).append("  Stock: ").append(stock.getName()).append(LF);
        }
        for (Variable variable : module.getVariables()) {
            builder.append(indent).append("  Variable: ").append(variable.getName()).append(LF);
        }
        for (Flow flow : module.getFlows()) {
            builder.append(indent).append("  Flow: ").append(flow.getName()).append(LF);
        }
        for (Map.Entry<String, Constant> entry : module.getConstants().entrySet()) {
            builder.append(indent).append("  Constant: ").append(entry.getKey()).append(LF);
        }
        for (Module sub : module.getSubModules().values()) {
            appendModule(builder, sub, indent + "  ");
        }
    }

}
