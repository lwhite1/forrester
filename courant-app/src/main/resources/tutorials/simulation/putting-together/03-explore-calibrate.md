## Explore

With a validated model, explore its behavior systematically:

- **Parameter sweeps** identify which parameters the model is most sensitive to. Focus attention on the parameters that matter most.
- **Monte Carlo analysis** quantifies the range of possible outcomes when multiple parameters are uncertain simultaneously. It answers: *how wide is the envelope of behavior?*

Exploration reveals where the model's predictions are robust and where they depend heavily on uncertain assumptions.

## Calibrate

If historical data is available, calibrate the uncertain parameters:

- Import reference data and map it to model variables
- Select the parameters to calibrate and set plausible bounds
- Run the calibrator and evaluate the fit

After calibration, test the model against **holdout data** -- data that was not used during calibration. Good performance on unseen data is the strongest evidence that calibration captured real dynamics, not noise.
