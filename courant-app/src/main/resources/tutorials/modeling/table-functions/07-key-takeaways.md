## What you learned

- **Table functions** capture non-linear relationships that linear equations miss
- **Diminishing returns** — output grows quickly at first, then levels off
- **Saturation** — the response flattens when input is large
- **S-shaped curves** — slow start, rapid middle, saturation. Common in adoption and growth
- **LOOKUP syntax** — `LOOKUP(TableName, InputVariable)` reads a value from the curve
- **Normalization** — keep output in the 0-1 range for composability

## Behavior modes seen

- Diminishing returns (aggressive early correction, slow final approach)
- Saturation (response flattening at extremes)
- Sigmoid growth (slow-fast-slow pattern)

## Key insight

Table functions are the modeler's most practical tool for non-linearity. They don't require you to find a mathematical function that fits the data — you draw the relationship directly. Most real system dynamics models use several table functions.

## Try next

The next tutorial, **Units & Dimensional Analysis**, shows how to ensure every equation in your model is dimensionally consistent.
