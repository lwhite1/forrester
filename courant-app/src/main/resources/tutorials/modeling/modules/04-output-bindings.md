## Exposing values to the parent

**Output bindings** publish a module's internal values so the parent model can reference them. This is how results flow out of a module.

## How it works

1. Inside the module, select the stock, flow, or variable you want to expose.
2. Mark it as an **output port** -- this publishes it to the parent's namespace.
3. In the parent, draw a connector from the module's output port to any variable that needs the value.

The parent reads the output by name without knowing the internal structure that produced it.

## What to expose

Good outputs are the values that other subsystems need:

- A Production module might expose `Output` and `Inventory`.
- A Population module might expose `Total_Population` and `Growth_Rate`.
- A Finance module might expose `Available_Capital` and `Interest_Rate`.

Keep the number of outputs small. If you're exposing most of a module's internals, the module isn't providing useful encapsulation.

## Inputs and outputs together

A module's **interface** is the combination of its inputs and outputs. A well-designed interface is small, stable, and meaningful -- it describes *what* the module does, not *how* it does it.
