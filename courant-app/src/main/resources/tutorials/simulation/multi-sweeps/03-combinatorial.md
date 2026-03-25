## Combinations multiply fast

Adding parameters multiplies the number of runs:

- **1 parameter**, 10 steps = 10 runs
- **2 parameters**, 10 steps each = 100 runs
- **3 parameters**, 10 steps each = 1,000 runs
- **4 parameters**, 10 steps each = 10,000 runs

This **combinatorial explosion** means exhaustive sweeps become impractical beyond 2-3 parameters.

## Practical guidelines

- Start with **3-5 steps** per parameter when sweeping multiple parameters
- If you need more detail, increase the step count for **one parameter at a time** to see where it matters
- For **4 or more** uncertain parameters, consider switching to Monte Carlo analysis (next tutorial) instead of exhaustive grid sweeps
- Watch the total run count displayed in the sweep dialog before clicking **Run**
