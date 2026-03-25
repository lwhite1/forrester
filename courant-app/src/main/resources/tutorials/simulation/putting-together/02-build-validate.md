## Build

Start with a clear **problem statement** and a **reference mode** -- a sketch of the behavior you're trying to explain. Then translate your dynamic hypothesis into a model:

- Identify the **stocks** that accumulate
- Define the **flows** that change them
- Connect variables through **feedback loops** that drive behavior

Keep the model as simple as possible while still capturing the essential dynamics.

## Validate

Validation builds confidence that the model's structure and behavior are sound:

- **Check units** with `Ctrl+B` -- every equation must be dimensionally consistent
- **Test extreme conditions** -- set parameters to very large or very small values and verify the model behaves sensibly
- **Compare to reference mode** -- does the model reproduce the expected pattern of behavior?

Iterate between building and validating until the model passes all structure and behavior tests. Don't move to exploration until the foundation is solid.
