## Passing values into a module

**Input bindings** connect a parent variable to a module port, replacing an internal constant or variable with the parent's value. This is how context flows into a module.

## How it works

1. Inside the module, create a variable that will receive the external value (e.g., `Demand`).
2. Mark it as an **input port** -- this tells the system that the variable's value comes from outside.
3. In the parent, draw a connector from a parent variable to the module's input port.

The internal variable now takes its value from whatever the parent provides. The module's equations reference it by its internal name, unaware of where the value originates.

## Why inputs matter

Input bindings let the **same module** behave differently depending on context:

- A "Production" module takes `Demand` as input. In one part of the model, demand comes from domestic consumers. In another copy, demand comes from exports.
- A "Population" module takes `Birth_Rate` as input. Different regions provide different rates.

The module's structure stays the same. Only the inputs change.
