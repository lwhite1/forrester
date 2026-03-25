## What you learned

- **Monte Carlo analysis** explores parameter uncertainty probabilistically by running hundreds of simulations with randomly sampled inputs
- **Distributions** (uniform, normal, triangular) encode what you know about each uncertain parameter
- **Confidence bands** show the range of possible outcomes -- wide bands mean high uncertainty, narrow bands mean robust outcomes
- **Sample size** affects precision: 200 runs for exploration, 500-1000 for stable results
- Monte Carlo identifies which uncertainties matter most for the outcome, guiding data collection and risk assessment

## Behavior modes seen

- Stochastic variation in trajectories
- Convergence of confidence bands at equilibrium

## Try next

- Apply Monte Carlo to a model with stronger nonlinearity to see how small input uncertainties can produce large output uncertainty
- Combine Monte Carlo with **optimization** (next tutorial) to find robust strategies that perform well across a range of uncertainties
