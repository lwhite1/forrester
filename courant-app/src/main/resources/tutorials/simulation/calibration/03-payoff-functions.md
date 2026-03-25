## Measuring goodness of fit

The **payoff function** quantifies how well the model's output matches the reference data. The calibrator minimizes this function by adjusting parameters.

## Common payoff functions

- **Mean Squared Error (MSE)** -- averages the squared differences between simulated and observed values. Penalizes large errors heavily, making it sensitive to outliers.
- **Mean Absolute Error (MAE)** -- averages the absolute differences. Treats all errors equally, making it more robust to outliers than MSE.
- **Mean Absolute Percentage Error (MAPE)** -- normalizes each error by the observed value. Useful when variables have very different scales or magnitudes.

## Choosing a payoff function

Use **MSE** when large deviations are especially undesirable. Use **MAE** when you want a balanced view of all errors. Use **MAPE** when comparing fit quality across variables with different units or magnitudes.

## Multiple variables

When calibrating against more than one data series, the payoff function combines errors across all mapped variables. Ensure the variables are on comparable scales, or use MAPE to normalize automatically.
