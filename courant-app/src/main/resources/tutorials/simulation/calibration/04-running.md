## Setting up a calibration run

Open **Simulate --> Calibration**. The setup has three parts:

- **Parameters** -- select the parameters to calibrate and set bounds for each
- **Reference data** -- select the imported data series and confirm variable mappings
- **Payoff function** -- choose MSE, MAE, or MAPE

Click **Run** to start.

## What happens during calibration

The calibrator is an optimizer that minimizes the payoff function. It adjusts the selected parameters, runs the simulation, computes the payoff, and repeats. Progress shows:

- The **current best payoff** value
- The **number of runs** completed
- The **current parameter values** being tested

Calibration typically requires hundreds of runs, especially with multiple parameters.

## When to stop

The calibrator stops automatically when the payoff function converges. You can also stop early if the payoff has plateaued at an acceptable level. The best parameter set found so far is preserved.
