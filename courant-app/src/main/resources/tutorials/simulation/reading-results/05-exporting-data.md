## Exporting to CSV

To save simulation results for external analysis:

1. Go to **File --> Export --> CSV**
2. Choose a file name and location
3. Click **Save**

You can also right-click the chart and select **Export CSV** from the context menu.

## CSV format

The exported file contains:

- **First column** — time values (one row per recorded time step)
- **Remaining columns** — one column per variable that was recorded during the simulation
- **Header row** — variable names

Example:

    Time, Coffee_Temperature, Cooling, Room_Temperature
    0,    95.0,               7.5,     20.0
    1,    87.5,               6.75,    20.0
    2,    80.75,              6.075,   20.0

## Using exported data

Import the CSV into your preferred analysis tool:

- **Excel / Google Sheets** — pivot tables, custom charts, what-if formulas
- **Python (pandas)** — `pd.read_csv("results.csv")` for statistical analysis or plotting with matplotlib
- **R** — `read.csv("results.csv")` for econometric or statistical modeling

Exporting is especially useful when you need to apply analysis techniques not built into the simulation tool.
