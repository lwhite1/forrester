package com.deathrayresearch.forrester.model.def;

import java.util.List;

/**
 * Definition of a subscript (dimension label set) in a model.
 *
 * @param name the subscript name
 * @param labels the ordered list of labels
 */
public record SubscriptDef(
        String name,
        List<String> labels
) {

    public SubscriptDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subscript name must not be blank");
        }
        labels = List.copyOf(labels);
    }
}
