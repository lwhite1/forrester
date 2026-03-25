## Matching the model to reality

**Calibration** adjusts model parameters so that simulation output matches historical data. It's a specialized form of optimization where the objective is: *reproduce observed behavior as closely as possible.*

## Why calibrate?

Models contain parameters whose true values are uncertain. Calibration uses real-world data to pin down those values, building confidence that the model captures the dynamics of the system it represents.

## A word of caution

A calibrated model is not necessarily a correct model. If you have enough free parameters, you can fit almost any data -- including noise. The goal is not a perfect numerical match. The goal is a model that reproduces behavior for the right **structural** reasons, so it remains valid under new conditions.
