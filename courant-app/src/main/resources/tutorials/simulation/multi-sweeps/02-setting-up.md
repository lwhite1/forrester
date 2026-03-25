## Add a second swept parameter

In **Simulate -> Parameter Sweep**, click **Add Parameter** to sweep a second parameter alongside the first.

Each parameter gets its own configuration:

- **Parameter 1**: `Cooling_Rate` -- min `0.05`, max `0.25`, steps `5`
- **Parameter 2**: `Room_Temperature` -- min `10`, max `30`, steps `5`

Click **Run**. The engine runs every combination of values: 5 steps times 5 steps = **25 runs** total.

## How combinations are generated

The sweep forms a grid. Each point in the grid is a unique combination of parameter values. For two parameters with 5 steps each, the grid has 25 cells. For three parameters with 5 steps each, it has 125 cells. Each cell is one simulation run.
