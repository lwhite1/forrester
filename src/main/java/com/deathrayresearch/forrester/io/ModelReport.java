package com.deathrayresearch.forrester.io;

import com.deathrayresearch.forrester.model.*;
import com.deathrayresearch.forrester.model.Flow;

import java.util.Map;

public class ModelReport {

    private static final char LF = '\n';

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


        return builder.toString();
    }

}
