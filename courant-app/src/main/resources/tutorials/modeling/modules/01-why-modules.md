## Taming complexity

As models grow beyond 20-30 elements, they become hard to read, debug, and communicate. A flat model with 50 variables is a wall of arrows -- finding the structure requires tracing every connection.

**Modules** let you package a subsystem into a reusable building block. Think of them like functions in programming:

- They **encapsulate** internal structure, hiding complexity behind a clean interface.
- They can be **reused** -- the same module structure in multiple places, with different inputs.
- They make the **top-level model** a clear diagram of how subsystems interact.

## A concrete example

An economy model might have 80 variables. As a flat diagram, it's unreadable. As three modules -- Production, Labor, and Finance -- the top-level view shows just the relationships between sectors. Each module can be opened and understood independently.

This tutorial covers how to create modules, connect them with bindings, and compose them into hierarchical models.
