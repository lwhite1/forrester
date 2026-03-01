# Assessment: Forrester as an SD Learning & Modeling Tool

## Strengths

- Excellent for learning SD through code. The API is clean and maps directly to SD concepts — stocks, flows, variables, constants, feedback loops. A programmer can build their first model in minutes:
  - Stock population = new Stock("Population", 100, PEOPLE);
    Flow births = Flows.exponentialGrowth("Births", DAY, population, 0.04);
    population.addInflow(births);
  
- Strong demo collection. The 16+ demos cover the core SD curriculum well — exponential growth/decay, S-curves, goal-seeking, pipeline delays, material delays, predator-prey, SIR epidemiology, inventory oscillations, software project dynamics, parameter sweeps, and Monte Carlo uncertainty analysis. Several are drawn directly from Meadows' Thinking in Systems.
  
- Dimensional analysis is a standout. The measurement system with 8 dimensions, 40 predefined units (time, length, mass, volume, temperature, money, items, dimensionless), automatic rate conversion, and immutable quantities is cleaner than what most commercial SD tools offer. Unit mismatches are caught at runtime. All quantity comparison methods (`isLessThan`, `isGreaterThan`, etc.) enforce dimension compatibility — comparing meters to dollars throws `IllegalArgumentException`.
  
- Lambda-based Flows API eliminates boilerplate. Common patterns are one-liners; custom formulas are concise lambdas. This is a genuine ergonomic advantage over older Java-based SD tools.
  
- Modularity works. The Module system (demonstrated in the Waterfall demo with 4 subsystems) supports composing models from reusable parts.

- Arrays/subscripts enable dimensioned modeling. `Subscript`, `ArrayedStock`, `ArrayedFlow`, and `ArrayedVariable` let users define a dimension (e.g., Region) and expand model elements into one instance per label. Multi-dimensional subscripts (`SubscriptRange`, `MultiArrayedStock`, `MultiArrayedFlow`, `MultiArrayedVariable`) compose multiple dimensions (e.g., Region × AgeGroup) with coordinate access, aggregation (`sumOver`, `slice`), and transparent expansion — matching the subscript capability of Vensim and Stella. Cross-element flows (e.g., migration between regions) work naturally through scalar flows referencing specific array elements. The multi-region SIR demo shows single-dimension arrays; the population region-age demo shows multi-dimensional subscripts with aging, births, deaths, and migration across 9 stocks.

- Intelligent arrays (`IndexedValue`) bring Analytica-style broadcasting to subscripted values. When two `IndexedValue` instances with different dimensions are combined (added, multiplied, etc.), shared dimensions align by name and non-shared dimensions expand via outer product — no manual looping required. `[Region] * [AgeGroup]` automatically produces a `[Region × AgeGroup]` result; `[Region × AgeGroup] + [Region]` broadcasts the region-only value across all age groups. Aggregation functions (`sumOver`) collapse dimensions. Convenience methods on `ArrayedStock`, `ArrayedVariable`, `MultiArrayedStock`, and `MultiArrayedVariable` produce `IndexedValue` snapshots from model state. This closes the gap between Forrester's subscript system and Analytica's array abstraction.
  
- Parameter sweeps enable sensitivity analysis. The `ParameterSweep` runner iterates an array of values, builds a fresh model per value via a factory function, and collects results into CSV output. The builder API is clean and the model-factory approach avoids shared mutable state. The SIR sweep demo clearly shows how contact rate drives epidemic severity.

- Multi-parameter sweeps enable interaction analysis. The `MultiParameterSweep` runner computes the Cartesian product of N parameter arrays and runs every combination — e.g., sweeping contact rate × infectivity produces a full grid of results. The factory signature matches `MonteCarlo`'s, so model factories are reusable across sweep and uncertainty analysis. Time-series and summary CSVs include all parameter columns.
  
- Monte Carlo simulation enables uncertainty analysis. The `MonteCarlo` runner samples multiple parameters from probability distributions (Normal, Uniform, Triangular, etc.) using random or Latin Hypercube Sampling across hundreds of runs. Results are aggregated into percentile envelopes with statistical summaries (percentiles, means) and fan chart visualization. This closes the biggest gap between "educational tool" and "useful analysis tool."

- Optimization and calibration enable automated parameter fitting. The `Optimizer` wraps Apache Commons Math derivative-free optimizers (Nelder-Mead, BOBYQA, CMA-ES) behind a builder API consistent with the sweep and Monte Carlo runners. Users define parameter bounds, a model factory, and an objective function (SSE against observed data, minimize/maximize a stock, target a value, minimize peak). The optimizer runs the simulation repeatedly, tracking the best result. All algorithms respect user-specified parameter bounds — Nelder-Mead clamps parameters to bounds on each evaluation since it doesn't enforce them natively. The `SirCalibrationDemo` demonstrates recovering SIR parameters from synthetic data.

## Robustness

The simulation engine and analysis tools have been hardened via a system-wide audit (see `doc/SystemAudit.md`):

- **Simulation engine:** Flow values are computed and recorded exactly once per step (identity-based caching). Sub-second time units (MILLISECOND) work correctly. Simulations are re-entrant — `execute()` resets state. `SimulationEndEvent` is guaranteed via try/finally even when formulas throw. Time step counting uses exact integer arithmetic.
- **SD functions:** `Smooth` and `Delay3` correctly handle multi-step gaps by looping through missed integration steps. `Flows.exponentialGrowthWithLimit` validates that the carrying capacity is positive.
- **Stocks:** NaN and Infinity values are rejected with descriptive errors rather than being silently masked. Flow iteration order is deterministic (LinkedHashSet).
- **Monte Carlo:** Sampled parameter values are preserved in results. Random sampling reseeds distributions once rather than per-draw.
- **Optimizer:** Parameter bounds are enforced for all algorithms including Nelder-Mead. Graceful fallback when no evaluation improves over the initial guess.
- **Measurement:** Fahrenheit-to-Celsius quantity conversion explicitly throws `UnsupportedOperationException` (affine offset incompatible with ratio-based conversion) instead of producing silently wrong results.
- **Visualization:** Fan charts handle negative values and single-step edge cases correctly.
- **Parameter sweeps:** `linspace` validates step > 0 and end >= start.

## Limitations

- No visual editor. This is the biggest gap for learning. Commercial tools (Vensim, Stella,
AnyLogic) let you draw stock-and-flow diagrams and see feedback structure visually. Here, learners must infer loop structure from code alone.
- Single-level module nesting. No modules-within-modules for very large models.

## Verdict by Audience

| Audience | Suitability |
|---|---|
| Programmers learning SD | Excellent — code-first approach maps concepts clearly |
| SD courses for engineers | Very good — demos cover the standard curriculum |
| Prototyping before Vensim/Stella | Good — quick to iterate, then migrate |
| Deterministic sensitivity analysis | Very good — single-parameter and multi-parameter sweeps with CSV output cover what-if and interaction analysis |
| Uncertainty analysis / research | Good — Monte Carlo with LHS, percentile envelopes, and fan charts cover multi-parameter uncertainty quantification |
| Non-programmers | Poor — no visual editor |
| Model calibration / optimization | Good — derivative-free optimization with multiple algorithms and built-in objective functions for fitting to data |
| Subscripted array computation | Very good — intelligent arrays with automatic broadcasting, named-dimension alignment, and aggregation match Analytica semantics |
| Production/enterprise modeling | Fair — has analysis tools, multi-dimensional subscripts, and intelligent arrays but lacks visual diagrams |

## Bottom Line

- Forrester is a strong educational tool for programmers — the API is intuitive, the demos are pedagogically sound, and the dimensional analysis is genuinely excellent. It's well-suited for learning the mechanics of SD (stocks, flows, feedback, delays) through hands-on coding.

- The addition of parameter sweeps (single and multi-parameter), Monte Carlo simulation, optimization/calibration, and combinatorial grid analysis moves Forrester from "educational only" toward "useful for analysis." A user can now sweep one parameter, sweep a grid of N parameters to study interactions, run Monte Carlo with distribution sampling and Latin Hypercube designs, extract percentile envelopes, export CSV, visualize uncertainty via fan charts, and calibrate model parameters against observed data using derivative-free optimization — a real workflow that commercial tools support.

- It's not a replacement for Vensim or Stella for serious modeling work. The absence of
  visual diagrams means analysts would hit walls on models that require visual feedback-loop analysis.

## What Matters Most Next

Ranked by impact on the gap between "educational tool" and "useful modeling tool":

1. **Visual diagram generation** — The biggest learning gap. Even generating a static DOT/Graphviz diagram of the stock-flow structure from a `Model` object would help learners see feedback loops. Doesn't need to be interactive to be valuable.
2. **Nested Modules** — Current support for modules is limited to one level. Necessary for building larger, more complex models.
