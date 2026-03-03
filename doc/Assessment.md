# Assessment: Forrester as an SD Learning & Modeling Tool

## Strengths

- Excellent for learning SD through code. The API is clean and maps directly to SD concepts — stocks, flows, variables, constants, feedback loops. A programmer can build their first model in minutes:
  - Stock population = new Stock("Population", 100, PEOPLE);
    Flow births = Flows.exponentialGrowth("Births", DAY, population, 0.04);
    population.addInflow(births);
  
- Strong demo collection. The 16+ demos cover the core SD curriculum well — exponential growth/decay, S-curves, goal-seeking, pipeline delays, material delays, predator-prey, SIR epidemiology, inventory oscillations, software project dynamics, parameter sweeps, and Monte Carlo uncertainty analysis. Several are drawn directly from Meadows' Thinking in Systems.
  
- Dimensional analysis is a standout. The measurement system with 8 dimensions, 40 predefined units (time, length, mass, volume, temperature, money, items, dimensionless), automatic rate conversion, and immutable quantities is cleaner than what most commercial SD tools offer. Unit mismatches are caught at runtime. All quantity comparison methods (`isLessThan`, `isGreaterThan`, etc.) enforce dimension compatibility — comparing meters to dollars throws `IllegalArgumentException`.
  
- Lambda-based Flows API eliminates boilerplate. Common patterns are one-liners; custom formulas are concise lambdas. This is a genuine ergonomic advantage over older Java-based SD tools.
  
- Modularity works well. The Module system supports composing models from reusable parts with nested sub-modules, constants, and hierarchical structural reports. The Waterfall demo demonstrates runtime composition with 4 subsystems; the definition-based API adds `ModuleInstanceDef` with input/output port bindings for data-driven module reuse.

- Arrays/subscripts enable dimensioned modeling. `Subscript`, `ArrayedStock`, `ArrayedFlow`, and `ArrayedVariable` let users define a dimension (e.g., Region) and expand model elements into one instance per label. Multi-dimensional subscripts (`SubscriptRange`, `MultiArrayedStock`, `MultiArrayedFlow`, `MultiArrayedVariable`) compose multiple dimensions (e.g., Region × AgeGroup) with coordinate access, aggregation (`sumOver`, `slice`), and transparent expansion — matching the subscript capability of Vensim and Stella. Cross-element flows (e.g., migration between regions) work naturally through scalar flows referencing specific array elements. The multi-region SIR demo shows single-dimension arrays; the population region-age demo shows multi-dimensional subscripts with aging, births, deaths, and migration across 9 stocks.

- Intelligent arrays (`IndexedValue`) bring Analytica-style broadcasting to subscripted values. When two `IndexedValue` instances with different dimensions are combined (added, multiplied, etc.), shared dimensions align by name and non-shared dimensions expand via outer product — no manual looping required. `[Region] * [AgeGroup]` automatically produces a `[Region × AgeGroup]` result; `[Region × AgeGroup] + [Region]` broadcasts the region-only value across all age groups. Aggregation functions (`sumOver`) collapse dimensions. Convenience methods on `ArrayedStock`, `ArrayedVariable`, `MultiArrayedStock`, and `MultiArrayedVariable` produce `IndexedValue` snapshots from model state. This closes the gap between Forrester's subscript system and Analytica's array abstraction.
  
- Parameter sweeps enable sensitivity analysis. The `ParameterSweep` runner iterates an array of values, builds a fresh model per value via a factory function, and collects results into CSV output. The builder API is clean and the model-factory approach avoids shared mutable state. The SIR sweep demo clearly shows how contact rate drives epidemic severity.

- Multi-parameter sweeps enable interaction analysis. The `MultiParameterSweep` runner computes the Cartesian product of N parameter arrays and runs every combination — e.g., sweeping contact rate × infectivity produces a full grid of results. The factory signature matches `MonteCarlo`'s, so model factories are reusable across sweep and uncertainty analysis. Time-series and summary CSVs include all parameter columns.
  
- Monte Carlo simulation enables uncertainty analysis. The `MonteCarlo` runner samples multiple parameters from probability distributions (Normal, Uniform, Triangular, etc.) using random or Latin Hypercube Sampling across hundreds of runs. Results are aggregated into percentile envelopes with statistical summaries (percentiles, means) and fan chart visualization. This closes the biggest gap between "educational tool" and "useful analysis tool."

- Optimization and calibration enable automated parameter fitting. The `Optimizer` wraps Apache Commons Math derivative-free optimizers (Nelder-Mead, BOBYQA, CMA-ES) behind a builder API consistent with the sweep and Monte Carlo runners. Users define parameter bounds, a model factory, and an objective function (SSE against observed data, minimize/maximize a stock, target a value, minimize peak). The optimizer runs the simulation repeatedly, tracking the best result. All algorithms respect user-specified parameter bounds — Nelder-Mead clamps parameters to bounds on each evaluation since it doesn't enforce them natively. The `SirCalibrationDemo` demonstrates recovering SIR parameters from synthetic data.

- Data-driven model definitions enable serialization and external tooling. The `model/def` package provides an immutable record hierarchy (`ModelDefinition`, `StockDef`, `FlowDef`, `AuxDef`, `ConstantDef`, `LookupTableDef`, `ModuleInstanceDef`) with a fluent builder and structural validator. The `model/expr` package adds a sealed `Expr` AST with a recursive-descent parser, stringifier, and dependency extractor — formulas are represented as data rather than lambdas. `ModelDefinitionSerializer` provides round-trip JSON persistence via Jackson, and `ModelCompiler` translates definitions into runnable models using two-pass compilation with forward-reference support. This means models can be saved, shared, version-controlled, loaded from external sources, and compiled — a significant step toward interoperability with other SD tools.

- Vensim .mdl import enables model exchange with the most widely used SD tool. `VensimImporter` reads Vensim `.mdl` files and produces a `ModelDefinition` that can be compiled and simulated. Supports stocks (INTEG), constants, auxiliaries, lookup tables (standalone and WITH LOOKUP), subscript ranges, simulation settings (INITIAL TIME, FINAL TIME, TIME STEP), sketch/view data, and expression translation for common Vensim functions (IF THEN ELSE, XIDZ, ZIDZ, SMOOTH3, DELAY1, logical operators). Unsupported constructs (macros, data variables, PULSE, DELAY FIXED) emit warnings rather than failing. The import pipeline has been audited and hardened with fixes for CRLF handling, case-insensitive matching, operator precedence preservation, duplicate name detection, and false-positive avoidance.

- XMILE import and export enables bidirectional model exchange with Stella/iThink and other XMILE-compatible tools. `XmileImporter` reads XMILE XML files (the OASIS standard format for System Dynamics) and produces a `ModelDefinition` that can be compiled and simulated. `XmileExporter` writes any `ModelDefinition` back to valid XMILE 1.0 XML. Supports stocks, flows, auxiliaries, constants, lookup tables (standalone and embedded `<gf>`), simulation settings (`<sim_specs>`), view data, and bidirectional expression translation (IF_THEN_ELSE, AND/OR/NOT, equality/inequality operators, Time variable, SMTH3/SMTH1→SMOOTH approximation). Unsupported constructs (modules, groups, macros, arrays) emit warnings rather than failing. Combined with the existing Vensim `.mdl` importer, Forrester can now exchange models with both major SD tool ecosystems. Round-trip tested: import XMILE → compile → simulate → verify values; export → re-import → compare.

- Dependency graph and auto-layout provide structural analysis. `DependencyGraph` extracts a directed graph from model definitions (which elements influence which), `ConnectorGenerator` auto-generates influence arrows, `AutoLayout` produces layered element placement, and `ViewValidator` checks view integrity. These are the building blocks for visual diagram generation.

- Interactive visual editor closes the biggest learning gap. The `forrester-app` module provides a JavaFX canvas-based editor for creating and editing stock-and-flow diagrams interactively. Users can create stocks, flows, auxiliaries, and constants via toolbar or keyboard shortcuts (1–5), connect flows to stocks with a two-click protocol, edit names/values/equations inline with double-click, drag elements to reposition, and reattach flow endpoints by dragging clouds onto stocks. The editor includes pan (Space+drag, middle/right-drag), zoom (scroll wheel, Ctrl+Plus/Minus/0), rubber-band marquee selection, undo/redo (Ctrl+Z/Shift+Z, 100-level snapshot stack), context-sensitive cursor feedback for all 10 interaction states, and a status bar showing tool/selection/element counts/zoom. Models are saved to and loaded from JSON files with full view layout preservation. Simulation can be run directly from the editor (Ctrl+R) with results displayed in a sortable table. The editor renders the Layered Flow Diagram visual language with distinct shapes for each element type, material flow arrows routed through diamond indicators, dashed info link connectors, and cloud symbols for disconnected endpoints.

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

- The visual editor covers core modeling operations but lacks some polish features found in commercial tools: no context toolbar near selection, no functional resize handles, no hover highlighting or feedback loop highlighting, no simulation results charting/graphing (only a table), and no module/submodel support in the UI. These are refinement gaps rather than architectural gaps — the foundations (rendering, interaction, serialization, simulation integration) are solid.

## Verdict by Audience

| Audience | Suitability |
|---|---|
| Programmers learning SD | Excellent — code-first approach maps concepts clearly |
| SD courses for engineers | Very good — demos cover the standard curriculum |
| Prototyping before Vensim/Stella | Good — quick to iterate; JSON serialization enables model exchange |
| Deterministic sensitivity analysis | Very good — single-parameter and multi-parameter sweeps with CSV output cover what-if and interaction analysis |
| Uncertainty analysis / research | Good — Monte Carlo with LHS, percentile envelopes, and fan charts cover multi-parameter uncertainty quantification |
| Non-programmers | Fair — visual editor enables diagram-based modeling but lacks some polish (no chart view, no feedback loop highlighting) |
| Model calibration / optimization | Good — derivative-free optimization with multiple algorithms and built-in objective functions for fitting to data |
| Subscripted array computation | Very good — intelligent arrays with automatic broadcasting, named-dimension alignment, and aggregation match Analytica semantics |
| Model sharing / interoperability | Very good — JSON round-trip serialization, Vensim .mdl import, XMILE import/export, structural validation, and nested module support enable saving and exchanging models with both major SD tool ecosystems |
| Production/enterprise modeling | Good — has analysis tools, serialization, nested modules, multi-dimensional subscripts, intelligent arrays, and a visual editor with simulation integration |

## Bottom Line

- Forrester is a strong educational tool for programmers — the API is intuitive, the demos are pedagogically sound, and the dimensional analysis is genuinely excellent. It's well-suited for learning the mechanics of SD (stocks, flows, feedback, delays) through hands-on coding.

- The addition of parameter sweeps (single and multi-parameter), Monte Carlo simulation, optimization/calibration, and combinatorial grid analysis moves Forrester from "educational only" toward "useful for analysis." A user can now sweep one parameter, sweep a grid of N parameters to study interactions, run Monte Carlo with distribution sampling and Latin Hypercube designs, extract percentile envelopes, export CSV, visualize uncertainty via fan charts, and calibrate model parameters against observed data using derivative-free optimization — a real workflow that commercial tools support.

- The data-driven definition and serialization pipeline is a significant architectural milestone. Models can now be described as pure data (no lambdas), validated structurally, serialized to/from JSON, and compiled to runnable simulations — with full support for nested modules, expression parsing, dependency extraction, and forward references. This opens the door to external model editors, model exchange between tools, and version-controlled model definitions. The dependency graph and auto-layout infrastructure provide the foundation for visual diagram generation.

- The visual editor is a significant milestone that moves Forrester from "code-only" to "visual + code." Users can now build, edit, save, and simulate models entirely through the GUI — a workflow that was previously only available in commercial tools. The editor's rendering of the Layered Flow Diagram notation, combined with JSON persistence and integrated simulation, makes it a viable standalone modeling tool for small-to-medium models.

## What Matters Most Next

Ranked by impact on the gap between "useful modeling tool" and "competitive with commercial tools":

1. **Simulation results charting** — The results table is functional but charts and time-series graphs are essential for understanding dynamic behavior. Adding line charts (stock trajectories, flow rates) to the results dialog would dramatically improve the analysis workflow.
2. **Feedback loop highlighting** — Hover an element to see which feedback loops it participates in; color-code reinforcing vs balancing loops. This is a core educational feature of commercial SD tools.
3. **Module/submodel support in the UI** — The engine supports nested modules, but the visual editor has no way to create, expand, or navigate submodels. Adding hierarchical model composition to the UI would enable larger models.
4. **Context toolbar and element properties panel** — A floating toolbar near the selection and a side panel for editing all element properties (not just name/equation) would improve editing ergonomics.
