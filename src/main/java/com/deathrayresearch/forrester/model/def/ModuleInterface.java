package com.deathrayresearch.forrester.model.def;

import java.util.List;

/**
 * Declares the public interface of a module: which ports accept input values
 * and which ports expose output values.
 *
 * @param inputs the input ports
 * @param outputs the output ports
 */
public record ModuleInterface(
        List<PortDef> inputs,
        List<PortDef> outputs
) {

    public ModuleInterface {
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        outputs = outputs == null ? List.of() : List.copyOf(outputs);
    }
}
