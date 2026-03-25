## Overfitting

Too many free parameters can fit any data, including noise. Every additional calibrated parameter adds flexibility that may not be warranted. Keep the number of calibrated parameters to a minimum -- calibrate only the parameters you genuinely don't know.

## Multiple fits

Different parameter combinations can produce nearly identical fit to the data but generate very different forecasts. Test robustness by calibrating from **multiple starting points**. If different starting points yield different parameter values but similar fit quality, the model may be structurally ambiguous.

## Compensating errors

One wrong parameter value can be masked by another wrong parameter value. The fit looks good, but the individual parameters are meaningless. Guard against this by:

- Validating individual parameter values against independent data or expert judgment
- Checking that each parameter falls within a physically plausible range
- Examining whether the model's internal behavior (not just its output) makes sense

## The cure

Fewer free parameters, stronger bounds, and independent validation are the best defenses against all three pitfalls.
