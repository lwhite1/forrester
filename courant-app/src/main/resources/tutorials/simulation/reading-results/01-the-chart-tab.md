## The chart view

After running a simulation (`Ctrl+R`), switch to the **Chart** tab in the dashboard at the bottom of the window.

The chart displays:

- **X-axis** — time, from INITIAL_TIME to FINAL_TIME
- **Y-axis** — variable values, auto-scaled to fit the data

By default, all stocks in the model are plotted. Each stock gets its own colored line with a legend entry.

## Navigating the chart

- **Hover** over a point to see its exact time and value in a tooltip
- **Scroll** the mouse wheel to zoom in or out on the time axis
- **Click and drag** to pan across the time range

## Auto-scaling

The y-axis automatically adjusts to show the full range of plotted variables. If you plot variables with very different scales (e.g., temperature 0-100 and a rate 0-1), the smaller variable may appear flat. The next step shows how to control which variables are displayed.
