## Try it yourself

Run these Monte Carlo experiments on the coffee cooling model:

- **Full uncertainty**: Set `Cooling_Rate` to uniform [0.05, 0.20] and `Room_Temperature` to normal [20, 3]. Run 500 times. How wide is the 90% confidence band at the end of the simulation?

- **Reduce one source of uncertainty**: Fix `Cooling_Rate` at its default value and re-run with only `Room_Temperature` uncertain. How much does the band narrow?

- **Compare**: The difference in band width tells you which parameter contributes more uncertainty to the final outcome. This is the Monte Carlo equivalent of sensitivity analysis.

## Questions to consider

- Which parameter drives most of the output uncertainty?
- Does the 90% band include qualitatively different behaviors, or just quantitative variation?
- How would you use these results to prioritize data collection?
