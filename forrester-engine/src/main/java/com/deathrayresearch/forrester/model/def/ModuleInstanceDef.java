package com.deathrayresearch.forrester.model.def;

import java.util.Map;

/**
 * Definition of a module instance within a model. The instance references a
 * {@link ModelDefinition} (supporting recursion) and provides bindings for
 * input and output ports.
 *
 * @param instanceName the unique instance name within the parent model
 * @param definition the module's model definition
 * @param inputBindings maps port name → expression string providing the value
 * @param outputBindings maps port name → alias name in the parent model
 */
public record ModuleInstanceDef(
        String instanceName,
        ModelDefinition definition,
        Map<String, String> inputBindings,
        Map<String, String> outputBindings
) implements ElementDef {

    public ModuleInstanceDef {
        if (instanceName == null || instanceName.isBlank()) {
            throw new IllegalArgumentException("Module instance name must not be blank");
        }
        if (definition == null) {
            throw new IllegalArgumentException("Module definition must not be null");
        }
        inputBindings = inputBindings == null ? Map.of() : Map.copyOf(inputBindings);
        outputBindings = outputBindings == null ? Map.of() : Map.copyOf(outputBindings);
    }
}
