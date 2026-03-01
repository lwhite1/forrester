# Decision Analysis with Forrester

## Overview

Decision Analysis (DA) is a related but distinct field from System Dynamics. The two share a concern with modeling uncertain outcomes over time, but differ in structure: SD models are feedback-driven simulations with stocks and flows, while DA models are typically decision trees, influence diagrams, or Markov cohort models evaluated by backward induction or forward simulation.

This document assesses how much of Forrester's existing infrastructure can be reused for DA modeling, identifies gaps, and outlines what would need to be built.

## Markov Cohort Models: ~80% Reuse

Markov cohort models are the workhorse of health decision analysis and cost-effectiveness research. They track a population (cohort) moving through discrete health states over time, accumulating costs and health outcomes (QALYs) along the way. Structurally, they are almost identical to SD models.

### Direct mapping

| DA concept | Forrester equivalent | Status |
|---|---|---|
| Health/decision states | `Stock` | Native |
| Transition rates | `Flow` (outflow of source state, inflow of destination state) | Native |
| Population conservation | Same `Flow` wired as outflow + inflow | Native (structural) |
| Cycle-based time stepping | `Simulation` with configurable `TimeUnit` | Native |
| Absorbing states (e.g., Death) | `Stock` with inflows only, no outflows | Native |
| State-dependent transitions | `Flow.create()` lambda reading multiple stocks | Native |
| Probabilistic sensitivity analysis | `MonteCarlo` with LHS + distributions | Native |
| Deterministic sensitivity analysis | `ParameterSweep`, `MultiParameterSweep` | Native |
| Percentile envelopes | `MonteCarloResult.getPercentileSeries()` | Native |
| Fan chart visualization | `FanChart` | Native |
| CSV export | `SweepCsvWriter`, `MonteCarloResult` | Native |
| Competing risks (multiple exits) | Multiple `addOutflow()` on one stock | Native |

The SIR model already in the demo collection **is** a 3-state Markov cohort model. The same structure — with different state names and transition rates — would model cancer progression, HIV treatment pathways, or surgical vs. medical management strategies.

### What works today with no changes

**State-transition modeling.** A treatment cost-effectiveness model might look like:

```java
Stock healthy = new Stock("Healthy", 1000, PEOPLE);
Stock sick    = new Stock("Sick", 0, PEOPLE);
Stock dead    = new Stock("Dead", 0, PEOPLE);

Flow onset = Flows.exponentialGrowth("Disease Onset", YEAR, healthy, 0.02);
healthy.addOutflow(onset);
sick.addInflow(onset);

Flow recovery = Flows.exponentialGrowth("Recovery", YEAR, sick, 0.30);
sick.addOutflow(recovery);
healthy.addInflow(recovery);

Flow diseaseDeath = Flows.exponentialGrowth("Disease Death", YEAR, sick, 0.05);
sick.addOutflow(diseaseDeath);
dead.addInflow(diseaseDeath);
```

**Cost accumulation via accumulator stocks.** DA models attach per-cycle costs and health utilities to each state. Forrester has no built-in reward mechanism, but the idiom is natural:

```java
Stock totalCost = new Stock("Total Cost", 0, US_DOLLAR);
totalCost.setNegativeValuePolicy(NegativeValuePolicy.ALLOW);

Flow costAccrual = Flow.create("Cost Accrual", YEAR, () -> {
    double annualCost = sick.getValue() * 5000 + healthy.getValue() * 200;
    return new Quantity(annualCost, US_DOLLAR);
});
totalCost.addInflow(costAccrual);
```

After simulation, `totalCost.getValue()` gives the undiscounted cumulative cost.

**Discounting via step index.** Standard in health economics (typically 3% annual):

```java
double discountRate = 0.03;

Flow discountedCost = Flow.create("Discounted Cost", YEAR, () -> {
    double t = sim.getCurrentStep(); // cycle number
    double discount = 1.0 / Math.pow(1 + discountRate, t);
    double annualCost = sick.getValue() * 5000 + healthy.getValue() * 200;
    return new Quantity(annualCost * discount, US_DOLLAR);
});
discountedCostAccum.addInflow(discountedCost);
```

**Strategy comparison via model factory.** The `ParameterSweep` and `MonteCarlo` builders already accept a factory function that builds a fresh model per run. Different strategies (e.g., treatment A vs. B) can be expressed as different factory functions or as a parameter that controls flow wiring inside the factory:

```java
Model buildModel(double treatmentEffectiveness) {
    // Higher effectiveness → higher recovery rate
    Flow recovery = Flows.exponentialGrowth("Recovery", YEAR, sick,
        0.30 * treatmentEffectiveness);
    ...
}

// Compare strategies by sweeping effectiveness
SweepResult result = ParameterSweep.builder()
    .parameterName("Treatment Effectiveness")
    .parameterValues(new double[]{1.0, 1.5, 2.0}) // baseline, drug A, drug B
    .modelFactory(this::buildModel)
    .timeStep(YEAR)
    .duration(Times.years(20))
    .build()
    .execute();
```

**Monte Carlo on a Markov model.** Uncertainty in transition probabilities, costs, and utilities is handled by the existing `MonteCarlo` builder — sample from distributions, run hundreds of iterations, extract percentile envelopes.

### Caveats with the current engine

**Competing risks and large step sizes.** When multiple outflows leave a state simultaneously, each flow is computed from the pre-update stock value but subtracted sequentially. If the combined outflows exceed the stock, `NegativeValuePolicy.CLAMP_TO_ZERO` silently truncates. This is correct for small transition probabilities (typical in annual Markov cycles) but can cause conservation errors with large rates. Manual guards (like the SIR demo's `min(infected, susceptible)` check) are needed for high-rate transitions.

**Euler integration only.** The simulation engine uses simple Euler forward stepping. For Markov cohort models with discrete annual cycles, this is fine — Markov models are inherently discrete. For continuous-time models or very short time steps, numerical accuracy may suffer.

**No half-cycle correction.** Standard Markov convention assumes transitions occur mid-cycle. The engine applies transitions at the start of each cycle. A half-cycle correction (add half a cycle's reward at the start and end) would need to be applied as a post-processing adjustment on accumulator stocks.

## Small additions needed (utility-level)

These are convenience methods and small classes — days of work, not weeks.

### 1. Reward/cost convenience methods

A `Rewards` utility class (analogous to `Flows`) for common accumulation patterns:

```java
// One-liner for state-dependent cost accumulation
Flow cost = Rewards.perCycle("Treatment Cost", YEAR, sick, 5000, US_DOLLAR);

// Discounted accumulation
Flow discCost = Rewards.discounted("Discounted Cost", YEAR, sick, 5000, US_DOLLAR,
    0.03, sim::getCurrentStep);

// QALY accumulation (utility weight per state)
Flow qalys = Rewards.qualityAdjusted("QALYs", YEAR,
    Map.of(healthy, 1.0, sick, 0.6, dead, 0.0), sim::getCurrentStep);
```

### 2. Half-cycle correction

A static utility that adjusts an accumulator stock's final value:

```java
double corrected = HalfCycleCorrection.apply(totalCost, firstCycleReward, lastCycleReward);
```

### 3. Summary statistics on RunResult

Extend `RunResult` (or add a companion class) with time-series aggregation:

```java
result.getCumulativeStockValue("Total Cost");   // sum across all steps
result.getTimeWeightedAverage("Sick");          // mean state occupancy
```

### 4. Incremental cost-effectiveness ratio (ICER)

A comparison utility that takes two strategy results and computes:

```
ICER = (Cost_B - Cost_A) / (Effect_B - Effect_A)
```

```java
double icer = ICER.compute(
    resultA.getFinalStockValue("Discounted Cost"),
    resultA.getFinalStockValue("Discounted QALYs"),
    resultB.getFinalStockValue("Discounted Cost"),
    resultB.getFinalStockValue("Discounted QALYs"));
```

### 5. Tornado diagrams

Parameter sweeps already generate the data. What's missing is:
- A convention for recording "base case outcome" vs. "outcome at low/high parameter value"
- Sorting parameters by influence width (high minus low)
- Visualization (horizontal bar chart)

The data generation is a thin wrapper around `ParameterSweep`; the chart is a new JavaFX view.

## Medium additions needed

### 6. Cost-effectiveness acceptability curves (CEACs)

From Monte Carlo output: at each willingness-to-pay threshold lambda, compute the fraction of iterations where strategy B has positive net monetary benefit vs. strategy A:

```
NMB = lambda * delta_Effect - delta_Cost
P(cost-effective) = fraction of iterations where NMB > 0
```

The raw data exists in `MonteCarloResult` (per-run cost and effect values). The aggregation logic and plotting are new.

### 7. Cost-effectiveness plane scatter plots

Plot each Monte Carlo iteration as a point on (delta_Cost, delta_Effect) axes. Shows the joint distribution of incremental costs and effects. Quadrant analysis indicates dominance.

### 8. Net monetary benefit analysis

For a given willingness-to-pay threshold, convert health outcomes to monetary terms and compare strategies on a single scale. Useful for probabilistic sensitivity analysis where ICERs can be undefined (negative denominators).

## What doesn't map — new engines needed

### 9. Decision trees

Fundamentally different structure. A decision tree has:
- **Decision nodes** — the modeler chooses the branch
- **Chance nodes** — branches weighted by probabilities
- **Terminal nodes** — payoffs (cost, QALYs, utility)

Evaluation is **backward induction** (fold from leaves to root), not forward simulation. Nothing in Forrester's simulation engine applies here. This would be a new `decisiontree`package with its own `Tree`, `Node`, and `TreeEvaluator` classes.

However, the Monte Carlo sampling infrastructure (`MonteCarlo`, distributions, LHS) could be reused to run probabilistic decision trees.

### 10. Influence diagrams

Directed acyclic graphs evaluated by variable elimination or arc reversal. Different topology (no feedback loops) and different evaluation algorithm. New engine.

### 11. Value of Information (VOI)

Requires comparing expected value of a decision with vs. without a diagnostic test or signal. Conceptually: "run the tree twice with different information structures and compare." Needs the decision tree engine first.

## Reuse summary

| Component | Markov DA | Decision tree DA |
|---|---|---|
| `Stock` (states) | Direct reuse | Not applicable |
| `Flow` (transitions) | Direct reuse | Not applicable |
| `Simulation` (time stepping) | Direct reuse | Not applicable |
| `MonteCarlo` (sampling + LHS) | Direct reuse | Reuse sampling only |
| `ParameterSweep` / `MultiParameterSweep` | Direct reuse | Reuse for sensitivity |
| `MonteCarloResult` (percentiles) | Direct reuse | Partial reuse |
| `FanChart` (visualization) | Direct reuse | Not applicable |
| Measurement system (units) | Direct reuse | Direct reuse |
| CSV output | Direct reuse | Direct reuse |
| `Flows` utility patterns | Direct reuse | Not applicable |
| Decision tree evaluation | Not applicable | New engine needed |
| Influence diagram evaluation | Not applicable | New engine needed |

## Recommended implementation path

1. **Reward utilities** — `Rewards` class with `perCycle()`, `discounted()`,`qualityAdjusted()` convenience methods. Enables Markov cost-effectiveness models as clean one-liners. Small effort, high impact.
   
2. **ICER and strategy comparison** — utility class that takes sweep/Monte Carlo results for two strategies and computes ICERs, net monetary benefit, and probability of cost-effectiveness. Unlocks the core DA output.
   
3. **Tornado diagram** — thin wrapper around `ParameterSweep` + a JavaFX horizontal bar chart. The most-requested DA visualization after fan charts.
   
4. **CEAC and CE plane** — statistical aggregation on Monte Carlo output + scatter plot visualization. Completes the standard probabilistic sensitivity analysis toolkit.
   
5. **Decision tree engine** — if needed. This is a separate module with its own evaluation algorithm. The Monte Carlo and distribution infrastructure carries over; everything else is new.

Steps 1-4 are incremental additions to the existing codebase. Step 5 is a new subsystem.
