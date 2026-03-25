## What you learned

- **Simulation loop** — evaluate flows, update stocks, advance time, repeat
- **Euler integration** — `Stock(t+dt) = Stock(t) + NetFlow * dt`
- **Time Step** — sets the unit of time (Minute, Day, Year)
- **Duration** — sets how many time units to simulate
- **DT** — subdivides each time step for improved accuracy; halving DT doubles computation
- **INITIAL_TIME / FINAL_TIME** — built-in constants available in any equation

## Rules of thumb

- Choose a time step shorter than the fastest important dynamic
- Start with DT = 1.0; reduce only if results change when you halve it
- Use `INITIAL_TIME` and `FINAL_TIME` instead of hard-coding time values in equations

## Try next

Open the coffee cooling model and experiment:

- Run with Duration = 60 minutes, then 120 minutes — watch the temperature approach room temperature
- Change DT from 1.0 to 0.25 and compare — for this gentle system, results should be nearly identical
- Set INITIAL_TIME to 2025 and use `Time` in a variable equation to see it referenced as a year
