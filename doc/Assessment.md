# Assessment: Forrester as an SD Learning & Modeling Tool

## Strengths

- Excellent for learning SD through code. The API is clean and maps directly to SD concepts —
  stocks, flows, variables, constants, feedback loops. A programmer can build their first model in
  minutes:
  - Stock population = new Stock("Population", 100, PEOPLE);
    Flow births = Flows.exponentialGrowth("Births", DAY, population, 0.04);
    population.addInflow(births);

- Strong demo collection. The 15+ demos cover the core SD curriculum well — exponential
  growth/decay, S-curves, goal-seeking, pipeline delays, material delays, predator-prey, SIR
  epidemiology, inventory oscillations, software project dynamics, parameter sweeps, and Monte
  Carlo uncertainty analysis. Several are drawn directly from Meadows' Thinking in Systems.

- Dimensional analysis is a standout. The measurement system with 8 dimensions, 25+ units,
  automatic rate conversion, and immutable quantities is cleaner than what most commercial SD tools
  offer. Unit mismatches are caught at runtime.

- Lambda-based Flows API eliminates boilerplate. Common patterns are one-liners; custom formulas
  are concise lambdas. This is a genuine ergonomic advantage over older Java-based SD tools.

- Modularity works. The Module system (demonstrated in the Waterfall demo with 4 subsystems)
  supports composing models from reusable parts.

- Parameter sweeps enable sensitivity analysis. The `ParameterSweep` runner iterates an array of
  values, builds a fresh model per value via a factory function, and collects results into CSV
  output. The builder API is clean and the model-factory approach avoids shared mutable state.
  The SIR sweep demo clearly shows how contact rate drives epidemic severity.

- Monte Carlo simulation enables uncertainty analysis. The `MonteCarlo` runner samples multiple
  parameters from probability distributions (Normal, Uniform, Triangular, etc.) using random or
  Latin Hypercube Sampling across hundreds of runs. Results are aggregated into percentile
  envelopes with statistical summaries (percentiles, means) and fan chart visualization. This
  closes the biggest gap between "educational tool" and "useful analysis tool."

## Limitations

- No visual editor. This is the biggest gap for learning. Commercial tools (Vensim, Stella,
AnyLogic) let you draw stock-and-flow diagrams and see feedback structure visually. Here,
learners must infer loop structure from code alone.
- No optimization or goal-seeking. Can't automatically find parameter values that minimize an
  objective function (e.g., "what contact rate keeps peak infections below 100?").
- No arrays/subscripts. Can't model multiple products, regions, or age cohorts without duplicating
  stocks manually.
- Single-level module nesting. No modules-within-modules for very large models.

## Verdict by Audience

| Audience | Suitability |
|---|---|
| Programmers learning SD | Excellent — code-first approach maps concepts clearly |
| SD courses for engineers | Very good — demos cover the standard curriculum |
| Prototyping before Vensim/Stella | Good — quick to iterate, then migrate |
| Deterministic sensitivity analysis | Good — parameter sweeps with CSV output cover single-parameter what-if analysis |
| Uncertainty analysis / research | Good — Monte Carlo with LHS, percentile envelopes, and fan charts cover multi-parameter uncertainty quantification |
| Non-programmers | Poor — no visual editor |
| Production/enterprise modeling | Fair — has sensitivity and uncertainty analysis but lacks optimization |

## Bottom Line

- Forrester is a strong educational tool for programmers — the API is intuitive, the demos are
pedagogically sound, and the dimensional analysis is genuinely excellent. It's well-suited for
learning the mechanics of SD (stocks, flows, feedback, delays) through hands-on coding.

- The addition of parameter sweeps and Monte Carlo simulation moves Forrester from "educational
  only" toward "useful for analysis." A user can now sweep parameters deterministically, run
  Monte Carlo with distribution sampling and Latin Hypercube designs, extract percentile
  envelopes, export CSV, and visualize uncertainty via fan charts — a real workflow that
  commercial tools support.

- It's not a replacement for Vensim or Stella for serious modeling work. The absence of
  optimization and visual diagrams means analysts would hit walls on models that require
  automated calibration or visual feedback-loop analysis.

## What Matters Most Next

Ranked by impact on the gap between "educational tool" and "useful modeling tool":

1. **Multi-parameter sweeps** — Currently sweeps vary one parameter at a time. Supporting
   combinatorial grids (sweep parameter A x parameter B) or correlated sampling would enable
   interaction analysis. The model-factory pattern already supports this — the factory just needs
   to accept a parameter vector instead of a single double.
2. **Visual diagram generation** — The biggest learning gap. Even generating a static DOT/Graphviz
   diagram of the stock-flow structure from a `Model` object would help learners see feedback loops.
   Doesn't need to be interactive to be valuable.
3. **Optimization / calibration** — Automatically finding parameter values that minimize an
   objective function would enable model calibration against historical data and policy
   optimization. Could build on the Monte Carlo infrastructure.
4. **Arrays/subscripts** — Important for real-world models (age cohorts, regions, product lines)
   but a large design effort. Lower priority than the analysis features above because the current
   workaround (duplicate stocks) is ugly but functional for small models.
5. **Nested Modules** — Current support for modules is limited to one level. Necessary for building
   larger, more complex models.
