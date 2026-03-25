## Visual inspection

Overlay the calibrated simulation on the reference data. The most important check is visual: does the model capture the **pattern** of the data?

Look for:

- **Shape** -- does the trajectory rise, fall, and curve in the same way?
- **Timing** -- do peaks, troughs, and turning points occur at the right times?
- **Magnitude** -- are the levels approximately right?

## Structural fit vs. numerical fit

A perfect numerical match is not the goal. In fact, it's often a warning sign of overfitting. The goal is **structural fit** -- the model reproduces the observed behavior because it captures the right causal mechanisms.

A structurally valid model will continue to perform well under new conditions. An overfit model will not.

## Holdout validation

If you reserved a portion of data for validation, run the calibrated model against it now. Good performance on unseen data is the strongest evidence that the calibration is meaningful.
