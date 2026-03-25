## From linear to exponential

The bathtub had constant flow rates -- the stock changed linearly. Now open the **Exponential Growth** model: go to **File → Open Model** and select `Exponential Growth` from the introductory models.

This model has:

- **Population** -- a stock, starting at 100 people
- **Births** -- an inflow: `Population * Birth_Rate`
- **Deaths** -- an outflow: `Population * Death_Rate`
- **Birth Rate** -- 0.04 (4% per day)
- **Death Rate** -- 0.03 (3% per day)

The critical difference: the Births flow depends on the stock itself. The bigger the population, the more births, which makes the population bigger still. This is a **reinforcing loop**.

## Run the simulation

Press `Ctrl+R`. The simulation runs for 365 days.

The population curve is **exponential** -- slow at first, then accelerating. The net growth rate is 1% per day (4% births minus 3% deaths), and compounding makes it curve upward.

## Double the growth rate

Click **Birth Rate** and change it from `0.04` to `0.05`. Press `Ctrl+R`. The curve is steeper -- a 2% net rate doubles the population faster.

## Notice what's missing

There is no balancing loop. No resource limit, no carrying capacity, no crowding effect. Growth never stops. In real systems, exponential growth always encounters limits eventually -- food, space, predators, disease. The next tutorial on feedback loops explores how balancing loops create those limits.
