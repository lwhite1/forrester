## Configure simulation settings

Go to **Simulate → Simulation Settings** and enter:

  Time step:      `Day`
  Duration:       `56`
  Duration unit:  `Day`

This simulates 8 weeks of the epidemic.

## Run

Press `Ctrl+R`. Switch to the Chart tab in the dashboard.

## What you should see

Three curves:

- **Susceptible** — starts high and drops in an S-shaped curve as people get infected
- **Infectious** — rises exponentially, peaks, then declines (the epidemic curve)
- **Recovered** — rises in an S-shaped curve as people recover

The infectious peak occurs when the susceptible population drops below the threshold needed to sustain exponential growth. This is the moment the reinforcing loop loses dominance to the balancing loop.
