package com.deathrayresearch.forrester.model.def;

/**
 * Definition of a module interface port.
 *
 * @param name the port name
 * @param unit the unit name
 * @param comment optional description
 */
public record PortDef(
        String name,
        String unit,
        String comment
) {

    public PortDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Port name must not be blank");
        }
    }

    /**
     * Creates a port definition without a comment.
     *
     * @param name the port name
     * @param unit the unit name
     */
    public PortDef(String name, String unit) {
        this(name, unit, null);
    }
}
