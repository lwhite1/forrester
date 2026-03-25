## Open simulation settings

Go to **Simulate --> Simulation Settings**. Two fields control the time horizon:

- **Time Step** — the unit of time (Day, Week, Month, Year)
- **Duration** — how many time units to simulate

## Example

Set the following:

    Time Step:      Minute
    Duration:       60
    Duration Unit:  Minute

This simulates one hour of coffee cooling, one minute at a time. The engine will execute 60 iterations of the simulation loop.

## Choosing a time step

Shorter time steps give more resolution — you see finer-grained behavior — but the simulation takes longer because there are more iterations.

- **Too coarse** — you miss rapid changes and get inaccurate results
- **Too fine** — the simulation runs slowly with no improvement in insight

A good rule of thumb: the time step should be shorter than the fastest important dynamic in your model. For coffee cooling measured in minutes, a one-minute step works well. For population models spanning decades, a one-year step is usually sufficient.
