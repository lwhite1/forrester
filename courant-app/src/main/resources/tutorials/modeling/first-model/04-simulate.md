## Configure simulation settings

Go to **Simulate → Simulation Settings** and enter:

  Time step:      `Minute`
  Duration:       `60`
  Duration unit:  `Minute`

This simulates one hour of cooling, one minute at a time.

## Run

Press `Ctrl+R` (or Simulate → Run Simulation).

The dashboard opens at the bottom with two tabs:

- **Table** — a sortable grid with Coffee Temperature at each step
- **Chart** — a line chart plotting the temperature curve

## What you should see

An exponential decay curve: the coffee drops quickly at first (losing ~8°C in the first minute), then slows as it approaches room temperature. After 60 minutes, it's around 20°C.

Right-click the chart and select **Export CSV** to save the data.
