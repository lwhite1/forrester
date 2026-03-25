## Historical data for comparison

**Reference data** is the historical time series you want the model to match. It provides the ground truth that the calibrator measures against.

## Importing reference data

Import reference data via **File --> Import --> Reference Data**. The expected format is CSV with:

- A **time column** matching the model's time units
- One or more **data columns**, each corresponding to a model variable

## Mapping data to variables

After import, map each data column to a model variable. The calibrator will compare the simulated value of that variable to the reference value at each data point.

## Data quality matters

- Ensure the time column aligns with the model's time horizon
- Check for missing values or obvious outliers
- If possible, hold out a portion of the data for validation -- use one subset for calibration and the other to test whether the calibrated model predicts well
