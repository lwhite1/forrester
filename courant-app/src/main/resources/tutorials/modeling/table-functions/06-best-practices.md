## Guidelines for effective table functions

**Normalize the output range to 0-1.** A table that outputs 0 to 1 acts as a multiplier. This makes tables reusable and composable. Multiply the normalized output by a maximum value in the equation, not in the table.

**Use descriptive names.** `Effect_of_Pressure_on_Productivity` is better than `Table1`. The name should state the input, the output, or both.

**Test with extreme inputs.** What happens at the boundaries? If your table covers 0-100 but the input reaches 150, the output is clamped to the value at 100. Make sure this behavior makes sense for your model.

**Document the source.** Every table encodes an assumption about the real world. Note where the shape came from:

- Empirical data (best)
- Expert judgment (common)
- Theoretical reasoning (acceptable)
- "It seemed right" (fix this)

**Keep tables simple.** Five to eight points usually capture the shape adequately. More points don't add precision unless you have data to justify them. Excessive points make the table harder to understand and maintain.

**Avoid flat regions in the middle.** A flat section means "no response to changing input" — this is sometimes correct but often signals a mistake in the table design.
