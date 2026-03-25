## Confidence is cumulative

No single test validates a model. Confidence is built incrementally by applying multiple tests and documenting the results.

## A testing checklist

Work through these in order:

1. **Units check** (**Ctrl+B**) -- fix all dimensional inconsistencies
2. **Structure review** -- verify every causal relationship against evidence
3. **Extreme conditions** -- test at least 3-5 parameters at extreme values
4. **Behavior reproduction** -- compare output to the reference mode
5. **Sensitivity analysis** -- sweep key parameters to identify critical ones

## Involve domain experts

Share the model and test results with people who know the real system. They can spot implausible assumptions that modelers miss. Their feedback often leads to structural improvements that no automated test would find.

## When is a model ready?

A model that passes structure tests, survives extreme conditions, reproduces reference behavior, and has well-understood sensitivities is ready for policy analysis.

A model that fails any of these needs more work. Resist the temptation to skip testing and jump to policy conclusions -- an untested model can be worse than no model at all, because it creates false confidence.
