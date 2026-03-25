## Building hierarchical models

**Composing modules** means connecting multiple modules through their bindings. One module's output becomes another module's input, creating a hierarchy: a top-level model coordinates several sector modules.

## Example: a simple economy

Imagine three modules:

- **Production** -- takes `Labor_Force` and `Capital` as inputs, exposes `Output` and `Wages_Paid`
- **Labor** -- takes `Wages_Paid` as input, exposes `Labor_Force` and `Consumer_Demand`
- **Finance** -- takes `Consumer_Demand` and `Output` as inputs, exposes `Capital` and `Revenue`

The top-level model has just three boxes and a handful of connectors. Each module's internals might have 10-20 elements, but the parent sees only the interface.

## Depth of hierarchy

Modules can contain other modules. A national economy model might have Region modules, each containing Sector modules, each containing process-level stocks and flows. In practice, two or three levels of nesting are sufficient for most models.

## Navigation

- **Double-click** a module to drill into it.
- Use the **breadcrumb bar** at the top of the canvas to navigate back up to the parent.
