## What you learned

- **Modules** -- self-contained subsystems that appear as single boxes in the parent model
- **Isolation** -- internal elements are hidden from the parent and interact only through bindings
- **Input bindings** -- feed external values into a module, letting the same structure serve different contexts
- **Output bindings** -- expose internal results to the parent without revealing internal structure
- **Composition** -- connecting modules through bindings creates hierarchical, multi-level models

## Design principles

- Keep module interfaces small and meaningful.
- Name inputs and outputs for what they represent, not how they're computed.
- Use modules when the model is too large, too repetitive, or too collaborative for a flat structure.
- Avoid modules when simplicity suffices.

## Key insight

Modules are about managing complexity, not adding features. A modular model and a flat model with the same structure simulate identically. The difference is human comprehension -- modules let you think about one subsystem at a time while trusting that the interfaces keep everything connected.

## Try next

The **Aging Chains** tutorial covers a specialized multi-stock structure for modeling populations by age cohort, duration, and vintage.
