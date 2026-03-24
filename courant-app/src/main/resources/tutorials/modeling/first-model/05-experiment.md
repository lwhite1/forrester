## Change a parameter

Click **Cooling Rate** on the canvas. In the Properties panel on the right, change its value from 0.10 to 0.05. Press `Ctrl+R` again.

The curve is flatter — the coffee cools more slowly. A thicker mug, perhaps.

## Run a parameter sweep

Instead of changing values one at a time, sweep the whole range:

1. Go to **Simulate → Parameter Sweep**
2. Select `Cooling Rate` as the parameter
3. Set Start = `0.02`, End = `0.20`, Step = `0.02`
4. Click OK

The dashboard shows a family of curves — one per cooling rate. Toggle individual series with the checkboxes on the right.

You can see exactly which rate produces the behavior you want. What cooling rate reaches 60°C (drinkable) in 10 minutes?

## Multi-parameter sweep

Try **Simulate → Multi-Parameter Sweep** to vary both Cooling Rate and Room Temperature simultaneously. Each combination runs as an independent simulation.
