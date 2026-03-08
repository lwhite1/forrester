# UX Assessment: Forrester System Dynamics Modeling Application

*Date: 2026-03-08*

## Executive Summary

Forrester is a well-architected JavaFX application with strong fundamentals: a clean visual language, good keyboard support, and a solid simulation pipeline. However, several UX gaps — some general, some specific to the cognitive demands of system dynamics — will limit adoption, particularly among the novice-to-intermediate users who are the primary audience for a free tool competing with Vensim PLE.

The assessment below is organized into three tiers: **cognitive barriers specific to SD modeling**, **general UX/UI issues**, and **strengths to preserve**.

---

## Part 1: Cognitive Barriers in System Dynamics Modeling

The SD education literature (Sterman 2000, Sweeney & Sterman 2000, Cronin et al. 2009) identifies several persistent cognitive difficulties. This section evaluates how the current UI addresses — or fails to address — each one.

### 1.1 Stock-Flow Confusion (Critical)

**The problem:** Even graduate students routinely confuse stocks and flows. People struggle to identify which variables accumulate and which represent rates. The classic "bathtub" experiment shows that most people cannot correctly infer stock behavior from flow graphs.

**Current state:** Forrester uses distinct shapes (heavy rectangle for stocks, diamond for flows) and the toolbar tooltips say "accumulator" and "rate." This is necessary but not sufficient.

**Gaps:**

- **No visual depiction of accumulation.** Stocks look like static boxes. There is no metaphor (fill level, container, reservoir) that visually reinforces "this thing accumulates." Compare Stella's pipe-and-tank metaphor, which — whatever its aesthetic faults — makes the accumulation concept viscerally clear.
- **No inline behavior preview.** The R1 milestone mentions sparklines on stocks (item #12). This is high-value: seeing a rising line inside a stock reinforces that it is accumulating. Prioritize this.
- **Flow direction is ambiguous.** Arrowheads on material flows indicate direction, but there is no visual indication of *magnitude*. A flow of 0.1 looks identical to a flow of 1000. Even a simple thickness or animation cue would help.
- **The "cloud" metaphor for external sources/sinks is unexplained.** New users see a "~" in a circle with no context. The tooltip system could help here.

### 1.2 Feedback Loop Identification (High)

**The problem:** Identifying feedback loops — and especially distinguishing reinforcing from balancing — is the core analytical skill in SD. Novices cannot reliably trace loops in diagrams with more than 4-5 elements.

**Current state:** The LOOPS toggle button highlights feedback loops with colored overlays and R/B labels. This is genuinely good — most competitors don't offer automated loop detection.

**Gaps:**

- **Loop highlighting is all-or-nothing.** In a model with 8 loops, highlighting all of them simultaneously produces visual chaos. Users need to step through loops one at a time, or filter by type (reinforcing vs. balancing).
- **No loop narrative.** The labels say "R1" or "B2" but don't explain *what* the loop does. Even a tooltip like "R1: Population -> Births -> Population (reinforcing)" would dramatically improve comprehension.
- **No connection to behavior.** Users can see loops on the canvas and simulation results on the dashboard, but there is no way to connect the two. "Which loop is dominant right now?" is the key analytical question in SD, and the UI provides no help answering it.

### 1.3 Mental Simulation Errors (High)

**The problem:** People are poor at mentally simulating dynamic systems. They expect linear behavior from nonlinear systems, underestimate delays, and fail to anticipate oscillation. Sterman's "beer game" demonstrated this comprehensively.

**Current state:** The simulation and dashboard are well-structured. Run results display as time-series line charts. Sweep and Monte Carlo analyses are available. Ghost overlays from previous runs are shown behind the current chart, with controls to toggle and clear history.

**Gaps:**

- **No behavior-mode annotations.** The dashboard shows raw curves but never labels them. Exponential growth, goal-seeking, oscillation, S-shaped growth, and overshoot-and-collapse are the canonical behavior modes in SD. Annotating or at least naming the observed pattern would scaffold learning.
- **Stale results banner is good but passive.** The amber "stale" indicator is the right idea. Consider making it more active: "You changed Growth Rate from 0.05 to 0.08. Re-run to see the effect?" This connects the structural change to the behavioral question.

### 1.4 Equation Formulation (Medium-High)

**The problem:** Translating a conceptual understanding ("births depend on population") into a correct equation ("Births = Population * Birth_Rate") is a major stumbling block. Unit consistency adds another layer of difficulty.

**Current state:** The properties panel provides type-specific forms. Equation fields have autocomplete with element names and built-in functions. An expression language reference is available in Help. Real-time syntax and reference validation on equation fields catches errors as users type.

**Gaps:**

- **No dimensional guidance during equation entry.** The unit fields exist on forms, but there is no real-time check that says "this equation produces People/Year, but this flow's unit should be People/Year — OK" or "this equation produces People, but the flow expects People/Year — mismatch." Units are assigned but not *enforced or checked* during editing.
- **No equation templates.** Common SD patterns (exponential decay: `Stock * fractional_rate`, goal-seeking: `(Goal - Stock) / Adjustment_Time`, logistic growth: `Stock * Rate * (1 - Stock/Capacity)`) could be offered as templates when creating elements, reducing the blank-page problem.
- **Autocomplete doesn't show units.** When the popup suggests "Population", it should show "(People)" next to it. This helps users check dimensional consistency as they write.
- **No "explain this equation" affordance.** Even a read-back like "Births equals Population times Birth_Rate" in natural language would help users verify their intent.

### 1.5 Boundary Adequacy (Medium)

**The problem:** Deciding what to include in a model and what to treat as exogenous is a major modeling judgment. Novices either include too little (missing critical feedbacks) or too much (unwieldy models).

**Current state:** No specific support for boundary thinking.

**Gaps:**

- **No model boundary visualization.** A subsystem grouping or color-coding feature would help users think about what is endogenous vs. exogenous. Constants implicitly represent the model boundary, but this is not made explicit.
- **No "what's not connected?" view.** Orphaned elements are flagged in validation, but a visual mode that dims all connected elements and highlights disconnected ones would help during model review.

---

## Part 2: General UX/UI Issues

### 2.1 Discoverability (High)

**Current state:** The toolbar has labeled text buttons (Stock, Flow, Auxiliary, etc.) with tooltips. Number keys 1-9 switch tools. The command palette (Ctrl+K) provides fuzzy search.

**Issues:**

- **The command palette is excellent but hidden.** There is no visual hint that Ctrl+K exists. A search icon or text field in the toolbar ("Search commands...") would surface it.
- **No visible indication of keyboard shortcuts on toolbar buttons.** The tooltips mention shortcuts, but tooltips require hover-and-wait. Showing "S", "2", "3" etc. as small badges on toolbar buttons would aid learning.

### 2.2 Properties Panel (Medium)

**Current state:** Right sidebar shows type-specific forms with inline help icons. When nothing is selected, the panel shows a model summary with editable name/description, element counts, simulation settings, and quick-action buttons.

**Issues:**

- **The Comment field is at the bottom of every form.** In SD, documentation of assumptions is critical to model quality. The comment field's position at the bottom signals "optional afterthought." Consider giving it more prominence, or at least showing a visual indicator when an element has a comment.

### 2.3 Visual Hierarchy and Information Density (Medium)

**Current state:** Clean monochromatic palette with accent colors for semantic meaning. Elements use distinct shapes and badges.

**Issues:**

- **All elements have roughly the same visual weight.** Stocks, auxiliaries, and constants are all light rectangles with dark borders. In a complex model, the eye has no natural entry point. Stocks — as the structural backbone of any SD model — should be visually dominant. Consider filled backgrounds or heavier rendering.
- **Element badges ("fx", "pin", "tbl", "mod") are cryptic.** These are programmer abbreviations, not user-facing labels. "Aux", "Const", "Table", "Module" would be clearer, or use recognizable icons.
- **Connection rendering doesn't scale.** In models with 20+ elements, the info links (dashed gray lines) create a dense web that obscures the stock-flow structure. Consider: connection bundling, hover-to-highlight-dependencies, or a mode that dims info links.

### 2.4 Dashboard and Results (Medium)

**Current state:** Tabbed result viewer with time-series charts (with unit-labeled axes), sweep results, Monte Carlo fan charts, and optimization progress.

**Issues:**

- **Chart-to-model connection is weak.** Clicking a variable name in the chart legend should select and highlight the corresponding element on the canvas. Currently the canvas and dashboard are disconnected views.
- **No variable selection for what to plot.** The simulation appears to plot everything. Users need to choose which variables to display, especially in larger models. Checkboxes exist for toggling visibility but the initial state matters.
- **Fan charts for Monte Carlo are well-designed.** The confidence band visualization is clear and uses appropriate opacity layering. This is a strength.

### 2.5 Error Handling and Recovery (Medium)

**Current state:** Equation fields validate in real-time on keystroke with "did you mean?" suggestions for unresolved references. Full model validation (Ctrl+B) provides structural checks via a dialog table. Status bar shows error/warning counts. Elements with validation issues display red (error) or amber (warning) borders directly on the canvas.

**Issues:**

- **Error messages are structural, not instructional.** "Missing equation for flow Births" tells you *what's* wrong but not *how* to fix it. "Flow 'Births' needs an equation. Double-click to edit, or open the Properties panel." would be actionable.

### 2.6 Undo System (Low-Medium)

**Current state:** Snapshot-based undo with 100-entry stack and labeled history.

**Issues:**

- **Full-snapshot undo is correct but memory-heavy for large models.** The R1 milestone already identifies this (item #80). For UX purposes, the main concern is that undo labels are generic ("Edit", "Move"). More specific labels ("Changed Birth Rate equation", "Moved Population stock") would help users navigate the undo history.

---

## Part 3: Strengths to Preserve

These are things the application does well that should not be lost in future changes:

1. **Command palette (Ctrl+K)** — This is a genuine differentiator vs. Vensim PLE. The fuzzy matching across commands and element names is well-implemented.

2. **Feedback loop detection and highlighting** — Automated loop identification is rare in SD tools. The R/B classification with color coding is valuable.

3. **Multiple analysis modes** — Parameter sweep, multi-sweep, Monte Carlo, sensitivity, and optimization in a single tool is comprehensive. Most free SD tools offer only basic simulation.

4. **Keyboard-first workflow** — Number keys for tool selection, Ctrl+R to run, escape priority chain. This supports the expert "David" persona well.

5. **Clean visual language** — The monochromatic palette with semantic color accents is professional and readable. Resist the temptation to add gratuitous color.

6. **Stale results indicator** — Small but important. Shows awareness of the iterative nature of modeling.

7. **Module system with breadcrumb navigation** — Hierarchical decomposition is essential for larger models and is well-handled.

8. **Activity log** — Provides audit trail and supports reflective practice, which matters for learning.

9. **Start screen with example launcher** — New users land on a guided entry point with New Model, Open Model, and Getting Started cards, plus a difficulty-sorted example gallery. Eliminates the blank-canvas problem.

10. **Context menus** — Right-click on elements, connections, and empty canvas provides standard contextual actions (Edit, Cut, Copy, Delete, Paste, Add elements).

11. **Run comparison overlays** — Ghost traces from up to 5 previous simulation runs behind the current chart, with toggle and clear controls. Directly supports parameter exploration and "what changed?" reasoning.

12. **Real-time equation validation** — Syntax errors and unresolved references flagged on keystroke with Levenshtein-based "did you mean?" suggestions. Catches mistakes before simulation.

13. **Model summary panel** — When nothing is selected, the properties panel shows editable model name/description, element counts, simulation settings, and quick-action buttons instead of dead space.

14. **Canvas error/warning indicators** — Elements with validation issues (missing equations, syntax errors, disconnected flows, missing units) show red or amber borders directly on the canvas, providing immediate spatial context without requiring the validation dialog.

---

## Part 4: Prioritized Recommendations

Ranked by impact on user success, considering both general UX and SD-specific cognition:

| Priority | Recommendation | Addresses |
|----------|---------------|-----------|
| 1 | **Loop-by-loop stepping** in feedback highlighting mode | Feedback loop identification |
| 2 | **Sparklines on stocks** showing simulation trajectory | Stock-flow confusion, behavior preview |
| 3 | **Unit display in autocomplete** suggestions | Dimensional reasoning |

---

## References

The SD-specific observations draw on:

- Sterman, J. (2000). *Business Dynamics: Systems Thinking and Modeling for a Complex World*. Ch. 5 (mental models), Ch. 7-8 (stocks and flows).
- Sweeney & Sterman (2000). "Bathtub dynamics: initial results of a systems thinking inventory." *System Dynamics Review* 16(4).
- Cronin, Gonzalez & Sterman (2009). "Why don't well-educated adults understand accumulation?" *Organizational Behavior and Human Decision Processes* 108(1).
- Groessler (2004). "Don't let history repeat itself -- methodological issues concerning the use of simulators in teaching and experimentation." *System Dynamics Review* 20(3).
