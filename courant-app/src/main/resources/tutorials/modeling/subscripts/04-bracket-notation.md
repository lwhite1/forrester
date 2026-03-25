## Referencing specific elements

Use bracket notation to refer to a specific subscript element in equations:

- `Population[North]` — the North element only
- `Population[South] + Population[East]` — sum of two elements

This lets you write equations that mix subscripted and element-specific references.

## Cross-element references

A subscripted equation can reference other elements of the same subscript:

  `Migration[Region] = Population[Region] * Migration_Rate`

Each region's migration depends on its own population. The `[Region]` acts as a "for each" — the equation runs once per element.

## Autocomplete

When typing equations, the autocomplete dropdown shows bracket notation options. Type `Pop` and you'll see `Population`, `Population[North]`, `Population[South]`, etc. Press `Tab` to accept.

## SUM and other aggregations

To aggregate across a subscript:

  `Total_Population = SUM(Population[Region!])`

The `!` after the subscript name means "sum over all elements." This collapses the dimension back to a scalar.
