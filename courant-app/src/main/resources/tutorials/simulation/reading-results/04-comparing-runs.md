## Why compare runs?

Changing a parameter and re-running replaces the previous results. To see both runs side by side, use run comparison.

## Overlaying results

1. Run the simulation with the current parameter values (`Ctrl+R`)
2. Change a parameter — for example, set **Cooling_Rate** to 0.05
3. Go to **Simulate --> Compare Runs** or toggle the comparison control in the dashboard
4. Run again (`Ctrl+R`)

The chart now shows both runs overlaid: the original in its normal color, and the new run in a contrasting style. This makes it easy to see exactly where and how much the behavior changes.

## Example: fast vs. slow cooling

Run the coffee cooling model twice:

- **Run 1:** Cooling_Rate = 0.20 (thin ceramic mug)
- **Run 2:** Cooling_Rate = 0.05 (insulated travel mug)

With comparison enabled, you can see that the travel mug keeps coffee above 60 degrees C far longer. The curves diverge immediately and the gap persists throughout the simulation.

## Clearing comparisons

To discard previous comparison runs and start fresh, use **Simulate --> Clear Comparisons** or close the comparison panel.
