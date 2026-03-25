## What you've learned

- **Subscripts** disaggregate a model along named dimensions without duplicating structure
- **Subscript ranges** define the elements of a dimension (North, South, East)
- **Bracket notation** `Population[North]` references specific elements
- **Broadcasting** applies scalar values across all elements automatically
- **SUM** aggregates a subscripted variable back to a scalar
- **Multi-dimensional** subscripts create instances for every combination of elements

## Behavior modes you've seen

- **Divergence** — regions with different parameters evolve differently over time
- **Cross-element flows** — migration between subscript elements creates coupling

## When to use subscripts

- The same structure repeats across categories (regions, cohorts, products)
- You need to compare behavior across categories
- Policy affects categories differently

## When NOT to use subscripts

- Categories have fundamentally different structures (use modules instead)
- You only have 2-3 categories (separate stocks may be simpler)
- The model is already complex enough without disaggregation

Next up: using exogenous inputs to drive models with external signals.
