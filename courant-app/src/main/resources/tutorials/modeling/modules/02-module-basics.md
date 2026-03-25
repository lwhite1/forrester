## What is a module?

A **module** is a self-contained model embedded inside a parent model. On the parent canvas, it appears as a single rectangular box -- a compact representation of an entire subsystem.

## Inside vs. outside

- **Double-click** a module to open it and see its internal structure: stocks, flows, variables, and their connections.
- The module's internal elements are **isolated** from the parent. Variables inside a module cannot directly reference variables in the parent, and vice versa.
- All interaction between the module and its parent happens through defined **bindings** -- explicit input and output connections.

## Why isolation matters

Isolation prevents accidental coupling. When a module's internals are hidden, you can refactor its equations without breaking anything in the parent -- as long as the bindings stay the same. This is the same principle as encapsulation in software engineering.

## Creating a module

Press `6` to switch to the Module tool and click on the canvas to place a new module. Give it a descriptive name that reflects the subsystem it represents.
