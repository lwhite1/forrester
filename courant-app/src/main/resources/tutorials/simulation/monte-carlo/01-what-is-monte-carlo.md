## From grids to random sampling

**Monte Carlo analysis** runs the model hundreds or thousands of times, each time sampling parameter values randomly from specified distributions.

Unlike sweeps, which test a fixed grid of values, Monte Carlo explores the full parameter space probabilistically. Each run draws a fresh set of parameter values, so the analysis naturally covers combinations that a grid might miss.

The result is not a single answer but a **distribution of outcomes** -- a range of possibilities with associated likelihoods. This lets you answer questions like:

- What is the most likely outcome?
- What is the worst case with 90% confidence?
- How much uncertainty in the inputs translates to uncertainty in the output?
