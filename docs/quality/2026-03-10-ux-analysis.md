# User Experience Analysis — Forrester System Dynamics Tool

**Date:** 2026-03-10
**Scope:** Full UX audit grounded in SD cognitive science literature, covering model construction, interpretation, presentation, and internal consistency.

---

## Part I: Literature Foundation

This analysis draws on the core research on cognitive difficulties in system dynamics:

- **Stock-flow failure** (Cronin, Gonzalez & Sterman 2009): Nearly half of MIT graduate students cannot correctly infer stock behavior from inflow/outflow graphs. People apply a "correlation heuristic" — assuming stocks should look like their flows.
- **Misperceptions of feedback** (Sterman 1989): Decision-makers treat closed-loop systems as open-loop, underestimate delays, anchor on current states, and ignore all but the most salient variables.
- **Bathtub dynamics** (Booth Sweeney & Sterman 2000): Accumulation reasoning failures are universal, not attributable to graph illiteracy, and have real-world policy consequences.
- **Cognitive Dimensions of Notations** (Green & Petre 1996): A framework for evaluating visual programming environments across dimensions like viscosity, premature commitment, hidden dependencies, visibility, and progressive evaluation.

These findings define what an SD tool *must* do well to genuinely help modelers think correctly.

---

## Part II: What Forrester Does Well

### 2.1 Progressive Refinement

The system supports a natural modeling progression: CLD variables for qualitative sketching, then stock-flow elements for quantitative modeling, with a "Classify As..." right-click option to promote CLD variables to stocks or auxiliaries. This directly addresses the premature commitment problem — users can sketch structure before committing to equations.

### 2.2 Real-Time Equation Validation

Equations are validated with a 400ms debounce as the user types, with syntax checking, reference checking (with Levenshtein-distance "did you mean?" suggestions), and dimensional analysis feedback. The red/green/orange color coding for unit consistency is exactly what the literature recommends.

### 2.3 Feedback Loop Analysis

Automatic loop identification, classification (reinforcing/balancing/indeterminate), color-coded highlighting, per-loop stepping with `[`/`]` keys, and loop dominance visualization over time (stacked area chart). This directly counteracts the "insensitivity to feedback" bias documented by Sterman.

### 2.4 Behavior Mode Classification

Automatic detection of 9 behavior modes (exponential growth, goal-seeking, oscillation, S-shaped growth, overshoot & collapse, etc.) displayed alongside each series in the results sidebar. This scaffolds the connection between model structure and behavior that the literature identifies as the central learning challenge.

### 2.5 Rich Analysis Suite

Parameter sweeps, multi-parameter sweeps, Monte Carlo with fan charts, optimization, and sensitivity tornado charts — all accessible from a single menu. Few free SD tools offer this breadth. The fan chart (2.5th–97.5th percentile bands) and tornado diagrams follow best practices for uncertainty communication.

### 2.6 Sparklines on Stocks

Post-simulation sparklines rendered inside stock rectangles on the canvas connect structure to behavior *in situ*, reinforcing the rate-level distinction. This is a strong design choice that directly addresses stock-flow failure by showing accumulation behavior at the point of accumulation.

### 2.7 Comprehensive Help Infrastructure

Four help resources (Quickstart tutorial, SD Concepts reference, Expression Language reference, Keyboard Shortcuts), equation templates for common patterns, autocomplete with parameter hints, and a command palette. The SD Concepts dialog covers all foundational topics with formatted text.

---

## Part III: Issues and Recommendations

Issues are organized by theme and rated by priority.

### Theme A: Counteracting Stock-Flow Failure

The literature is emphatic: the single most important cognitive difficulty in SD is stock-flow failure. Forrester has good foundations but several opportunities.

#### A1. No Net Flow Indicator (High Priority)

**Problem:** The research shows that users who see `net flow = inflow - outflow` alongside stock behavior perform dramatically better at accumulation reasoning. Currently, the simulation chart shows stocks and individual flows as separate series, but never the net flow into a stock.

**Recommendation:** Add an option to display "Net flow into [Stock]" as a derived series in the results chart. When a user selects a stock series, show its net flow as a companion series (perhaps dashed). This makes the accumulation relationship `stock change = net flow * dt` visually explicit.

#### A2. No Rate-Level Distinction in Results Chart (Medium Priority)

**Problem:** All variables are plotted on the same Y-axis with the same visual treatment. A stock measured in "Person" and a flow measured in "Person/Day" appear as identically-styled lines, reinforcing the correlation heuristic.

**Recommendation:** Differentiate stocks and flows visually in the chart sidebar — use different line styles (solid for stocks, dashed for flows) or group them in labeled sections ("Levels" and "Rates"). Consider separate Y-axes or a toggle to show rates on a secondary axis.

#### A3. No "Phase Plot" Option (Medium Priority)

**Problem:** Phase plots (stock vs. stock, or stock vs. flow) reveal system behavior modes — limit cycles, spirals, convergence — that are invisible in time series. The literature recommends these for understanding feedback dynamics.

**Recommendation:** Add a "Phase Plot" chart type to the dashboard. Allow users to select X and Y variables to plot against each other. This is especially valuable for oscillatory models where the time-series view shows periodic behavior but the phase plot reveals the attractor structure.

---

### Theme B: Feedback and Delay Comprehension

#### B1. Delays Not Visually Distinct on Canvas (High Priority)

**Problem:** Delay functions (SMOOTH, DELAY, DELAY3) are buried inside equation text. On the canvas, a delayed flow looks identical to an instantaneous one. The literature specifically warns that hiding delays inside equations reinforces the underestimation of delay effects.

**Recommendation:** When a flow or auxiliary equation contains a delay function, render a visual indicator on the canvas — a small clock icon, hash marks on the connecting link, or a "delay" badge analogous to the existing "fx" badge. This makes delays structurally visible without requiring users to read equations.

#### B2. No Causal Tracing (Medium Priority)

**Problem:** Users cannot trace from a variable through the feedback structure to understand what influences it and what it influences. The hidden dependencies dimension is partially addressed by info-link rendering, but there is no way to highlight "all upstream causes of X" or "all downstream effects of Y."

**Recommendation:** Add right-click "Trace Upstream" and "Trace Downstream" options that highlight the causal chain from any selected element. This could use progressive highlighting — direct connections in bold, second-order connections faded — to show the radius of influence.

#### B3. Loop Dominance Chart Lacks Time Cursor (Low Priority)

**Problem:** The loop dominance stacked area chart shows which loops dominate over time, but there is no way to synchronize this with the results time series. The user cannot point to a behavior transition and see which loop took over.

**Recommendation:** Add a shared time cursor across the simulation chart and the loop dominance chart. Clicking a time point on one chart highlights the corresponding point on the other. This connects behavior to structure in real time.

---

### Theme C: Model Construction UX

#### C1. Equation Field is a Single-Line TextField (High Priority)

**Problem:** Equations are entered in a standard single-line TextField. For simple equations (`Population * birth_rate`) this is fine, but complex equations with nested functions and conditionals become unreadable horizontal scrolls. The formula entry UX literature recommends vertical scaling and multi-line support.

**Recommendation:** Replace the equation TextField with a multi-line TextArea that auto-expands vertically (2–5 lines). Preserve the autocomplete and validation behavior. Consider syntax highlighting (function names, operators, variable references in different colors) for readability. Even modest colorization would help parse complex expressions.

#### C2. No Structural Validation Beyond Syntax (Medium Priority)

**Problem:** The validator checks equations for syntax and reference errors, but does not perform structural checks that the SD literature considers essential:
- Orphan variables (defined but never referenced by anything)
- Stocks with no inflows or outflows
- Flows connected to clouds at both ends (leaks or sources with no accumulation target)
- Missing feedback loops (linear causal chains with no closure)

**Recommendation:** Add structural validation rules to the ModelValidator. These should produce warnings (not errors) to avoid blocking the iterative modeling process. Examples: "Stock 'Inventory' has no outflow — is this intentional?" or "Variable 'adjustment_time' is defined but not used by any equation."

#### C3. No Extreme-Condition Testing (Medium Priority)

**Problem:** Forrester and Senge's validation framework emphasizes extreme-condition tests: "Does the model behave reasonably when inputs are zero? Very large? Negative?" The tool has no built-in support for this.

**Recommendation:** Add an "Extreme Conditions" option under Simulate that automatically runs the model with key parameters at 0, at 10x their baseline, and at negative values (where applicable), then flags any NaN, Infinity, or physically unreasonable results. This could be presented as a validation report alongside the existing error/warning system.

#### C4. Lookup Table Editor Lacks Direct Manipulation (Low Priority)

**Problem:** Lookup table data is edited as numeric text fields in rows. The preview chart is read-only. Users cannot click and drag points on the chart to adjust values, which is the standard interaction in other SD tools (Stella, Vensim).

**Recommendation:** Make the lookup preview chart interactive — allow users to click to add points and drag existing points to adjust values. This is a significant implementation effort but is the expected interaction pattern for graphical functions in SD tools.

---

### Theme D: Results Interpretation and Presentation

#### D1. No Reference Mode Comparison (High Priority)

**Problem:** A core SD practice is defining a "reference mode" — the expected or historical behavior pattern — before building the model, then comparing simulation output against it. Forrester has no way to overlay reference data (observed time series) on simulation results.

**Recommendation:** Allow users to import a CSV of observed/expected data and overlay it on the simulation chart. Display it as a distinct series (e.g., dotted line, different color family) with a clear legend label ("Observed" or "Reference"). This is essential for model calibration and validation.

#### D2. Stale Results Warning Could Be More Prominent (Low Priority)

**Problem:** When the model changes after a simulation, results are marked stale with an amber banner. The sparklines fade to 25% opacity. This is good, but the stale state could be missed if the user is focused on the properties panel.

**Recommendation:** Consider adding a subtle visual indicator on the "Dashboard" tab label itself (e.g., an italic font, a warning dot, or dimmed text) when results are stale. This provides ambient awareness without being intrusive.

#### D3. No Scenario Comparison (Medium Priority)

**Problem:** Users can see ghost overlays of up to 5 previous runs, but these are undifferentiated gray lines at 25% opacity with no labels. If a user runs three experiments with different parameters, they cannot tell which ghost is which.

**Recommendation:** Label ghost runs in the sidebar (e.g., "Run 2: growth_rate=0.05") and give each a distinct muted color rather than uniform gray. Allow users to name runs or auto-generate names from changed parameters. This transforms ghost runs from "vague context" into "meaningful comparison."

---

### Theme E: Consistency Issues

#### E1. Inconsistent Validation Error Styling (Medium Priority)

**Problem:** Three different red colors are used for error states:
- Equation validation: `#E74C3C` (Styles.EQUATION_ERROR_BORDER)
- Sweep dialog validation labels: `#cc3333` (inline style)
- Dimension mismatch: `#E67E22` (orange, appropriate for warnings)

The sweep dialogs use inline styles (`-fx-text-fill: #cc3333; -fx-font-size: 11;`) rather than the centralized Styles constants.

**Recommendation:** Consolidate to a single error red (`#E74C3C`) and use `Styles.EQUATION_ERROR_LABEL` everywhere. Replace inline styles in ParameterSweepDialog, MultiParameterSweepDialog, MonteCarloDialog, and OptimizerDialog with centralized style constants.

#### E2. Inconsistent Dialog Sizing (Low Priority)

**Problem:** Dialog widths vary without clear rationale:
- SimulationSettingsDialog: auto-sized (~300px)
- ParameterSweepDialog: auto-sized (~350px)
- MultiParameterSweepDialog: 560px explicit
- MonteCarloDialog: 500px explicit
- OptimizerDialog: 550px explicit
- ValidationDialog: 700x400px (separate window)
- ExpressionLanguageDialog: 680x560px
- SdConceptsDialog: 640x520px

Configuration dialogs should share a consistent width.

**Recommendation:** Standardize configuration dialogs (SimulationSettings, ParameterSweep, MultiParameterSweep, MonteCarlo, Optimizer) at a consistent width (e.g., 520px). Help windows can vary based on content.

#### E3. Inconsistent "OK" Validation Patterns (Low Priority)

**Problem:** Some dialogs disable the OK button when validation fails (SimulationSettingsDialog, ParameterSweepDialog). Others disable the OK button *and* show an Alert dialog on click via an event filter (MultiParameterSweepDialog, MonteCarloDialog, OptimizerDialog). The dual mechanism is redundant and confusing — if the button is disabled, the alert should never fire.

**Recommendation:** Pick one pattern. The cleanest approach: disable the OK button and show inline validation messages (already done). Remove the event filter alerts, which are vestigial.

#### E4. No ID Conventions for Dialog Controls (Low Priority)

**Problem:** Some dialog controls have fx:id values (e.g., `simTimeStep`, `simDuration`) but many do not. The MultiParameterSweepDialog uses indexed IDs (`multiSweepParamName0`, `multiSweepStart0`), while MonteCarloDialog and OptimizerDialog do not assign IDs to their dynamic rows.

**Recommendation:** Assign consistent fx:id values to all dialog controls. This improves testability (TestFX can locate controls by ID) and accessibility.

#### E5. Properties Panel Width May Constrain Equation Entry (Medium Priority)

**Problem:** The properties panel has a preferred width of 250px and a minimum of 180px. Equations are entered in a TextField within this narrow panel. Complex equations are truncated and difficult to edit.

**Recommendation:** Either make the properties panel resizable by the user (drag the divider) or, as suggested in C1, use a multi-line TextArea that wraps text within the available width. Even within 250px, a 3-line TextArea is far more usable than a single-line field for complex equations.

---

### Theme F: Cognitive Dimensions Assessment

Evaluating Forrester against Green & Petre's Cognitive Dimensions framework:

| Dimension | Rating | Notes |
|---|---|---|
| **Viscosity** | Good | Low knock-on viscosity: renaming auto-updates references. Equation changes propagate cleanly. Undo/redo provides safety net. |
| **Premature Commitment** | Good | CLD-first workflow avoids forcing equations before structure is settled. Partial models can be validated. |
| **Hidden Dependencies** | Mixed | Info links show equation dependencies visually. But there is no "where-used" or "what-uses-this" query. If a variable is referenced by 5 equations, the user must inspect each one. |
| **Closeness of Mapping** | Excellent | Standard SD notation (rectangles=stocks, valves=flows, circles=auxiliaries). Matches Forrester/Sterman conventions exactly. |
| **Abstraction** | Good | Module system supports hierarchical decomposition. Breadcrumb navigation for nested modules. |
| **Secondary Notation** | Limited | Free-form layout, but no color-coding of elements, no user annotations, no sector boundaries. Users cannot visually organize the model beyond spatial arrangement. |
| **Visibility** | Good | Properties panel shows equation and units for selected element. Sparklines show behavior on canvas. Status bar shows element counts and validation state. |
| **Progressive Evaluation** | Good | Can simulate partial models. Real-time equation validation. Dimensional analysis as-you-type. Validate command available at any time. |
| **Consistency** | Good | Uniform form structure across element types. Consistent keyboard shortcuts. Same toolbar pattern throughout. |
| **Error Proneness** | Good | Autocomplete reduces typos. Validation catches reference errors. Flash feedback on invalid numeric input. Templates reduce equation errors. |

**Weakest dimension: Secondary Notation.** Users have no way to color-code elements, draw sector boundaries, or add annotations to the diagram. In large models, this limits comprehensibility. Adding even basic element coloring would significantly improve this dimension.

---

### Theme G: Onboarding and Discoverability

#### G1. Quickstart Covers Only One Model Pattern (Low Priority)

**Problem:** The Getting Started tutorial builds a coffee cooling model (single stock, goal-seeking behavior). This is a good starting point but does not expose users to reinforcing feedback, oscillation, or multi-stock models — the cases where cognitive difficulties are most acute.

**Recommendation:** Add 1–2 additional guided tutorials: an SIR epidemic model (reinforcing + balancing feedback, S-shaped behavior) and a supply chain model (delays, oscillation). These expose users to the behavior modes where SD tools provide the most value.

#### G2. No Contextual "What's This?" on Canvas Elements (Low Priority)

**Problem:** The properties panel has help tooltips on field labels, but the canvas itself provides no guidance. A new user who places a stock sees a rectangle but may not understand what it represents or what they need to do next.

**Recommendation:** When a user places their first element of each type, show a brief toast or tooltip: "Stock placed. Set an initial value in the Properties panel." After the first placement, suppress the hint. This is lightweight and non-intrusive.

---

## Part IV: Prioritized Summary

### High Priority
| # | Issue | Theme |
|---|-------|-------|
| A1 | Add net flow indicator to results chart | Stock-flow failure |
| B1 | Make delays visually distinct on canvas | Feedback comprehension |
| C1 | Multi-line equation editor with syntax highlighting | Model construction |
| D1 | Reference mode / observed data overlay | Results interpretation |

### Medium Priority
| # | Issue | Theme |
|---|-------|-------|
| A2 | Differentiate stocks vs. flows in chart presentation | Stock-flow failure |
| A3 | Add phase plot chart type | Stock-flow failure |
| B2 | Causal tracing (upstream/downstream highlighting) | Feedback comprehension |
| C2 | Structural validation (orphans, missing flows, unused vars) | Model construction |
| C3 | Extreme-condition testing support | Model construction |
| D3 | Named/labeled scenario comparison (replace anonymous ghosts) | Results interpretation |
| E1 | Consolidate error styling to centralized constants | Consistency |
| E5 | Resizable properties panel or wrapping equation field | Construction UX |
| F1 | Element color-coding and sector annotations | Secondary notation |

### Low Priority
| # | Issue | Theme |
|---|-------|-------|
| B3 | Synchronized time cursor across charts | Feedback comprehension |
| C4 | Interactive lookup table chart editing | Model construction |
| D2 | Stale indicator on Dashboard tab label | Results interpretation |
| E2 | Standardize dialog widths | Consistency |
| E3 | Remove redundant Alert-on-disabled-OK pattern | Consistency |
| E4 | Assign fx:id to all dialog controls | Consistency/testability |
| G1 | Additional guided tutorials (SIR, supply chain) | Onboarding |
| G2 | First-placement contextual hints | Onboarding |
