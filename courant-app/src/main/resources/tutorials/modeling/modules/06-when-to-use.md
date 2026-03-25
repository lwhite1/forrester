## When modules help

Use modules when:

- **Reuse** -- the same structure appears in multiple places. A regional model with five identical sectors benefits from one module replicated five times.
- **Encapsulation** -- a subsystem has a clear interface. If you can describe what goes in and what comes out, it's a good module candidate.
- **Organization** -- the model is too large to view at once. Modules let you work at one level of detail at a time.
- **Collaboration** -- different people work on different parts. Modules with defined interfaces let teams work independently.

## When modules don't help

Avoid modules for simple models. If your entire model has fewer than 20 elements, packaging it into modules adds overhead without benefit:

- Extra navigation (drilling in and out).
- More concepts to manage (bindings, ports).
- Harder to see the full picture at a glance.

## A rule of thumb

If you can print the model on a single page and read every variable name, you probably don't need modules. If you're scrolling, squinting, or losing track of feedback loops, it's time to decompose.
