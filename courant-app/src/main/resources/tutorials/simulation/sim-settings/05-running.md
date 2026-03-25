## Start a simulation

Press `Ctrl+R` to run the simulation. You can also use **Simulate --> Run Simulation** from the menu bar.

The status bar at the bottom of the window shows progress as the engine steps through the simulation loop.

## Viewing results

When the simulation completes, the dashboard opens with two tabs:

- **Chart** — a line chart plotting stocks over time
- **Table** — a sortable grid showing values at each time step

Switch between tabs to explore the output from different angles.

## Cancelling a run

To cancel a running simulation, press `Escape`. This stops the engine immediately and discards partial results.

Long simulations — millions of steps or complex models — show a progress indicator. If a run is taking too long, cancel it and consider increasing DT or using a coarser time step.

## Re-running after changes

After changing any parameter or equation, press `Ctrl+R` again to re-run. The dashboard updates with fresh results. Previous results are replaced unless you use run comparison (covered in the next tutorial).
