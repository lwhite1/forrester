# UX Assessment: Shrewd System Dynamics Modeling Application

*Date: 2026-03-10 (updated post chart-to-model linking and badge labels)*

## Executive Summary

Shrewd is a well-architected JavaFX application with strong fundamentals: a clean visual language, good keyboard support, and a solid simulation pipeline. However, several UX gaps — some general, some specific to the cognitive demands of system dynamics — will limit adoption, particularly among the novice-to-intermediate users who are the primary audience for a free tool competing with Vensim PLE.

The assessment below is organized into three tiers: **cognitive barriers specific to SD modeling**, **general UX/UI issues**, and **strengths to preserve**.

---

## Part 1: Cognitive Barriers in System Dynamics Modeling

The SD education literature (Sterman 2000, Sweeney & Sterman 2000, Cronin et al. 2009) identifies several persistent cognitive difficulties. This section evaluates how the current UI addresses — or fails to address — each one.

### 1.1 Stock-Flow Confusion (Critical)

**The problem:** Even graduate students routinely confuse stocks and flows. People struggle to identify which variables accumulate and which represent rates. The classic "bathtub" experiment shows that most people cannot correctly infer stock behavior from flow graphs.

**Current state:** Shrewd uses distinct shapes (heavy rectangle for stocks, diamond for flows) and the toolbar tooltips say "accumulator" and "rate." This is necessary but not sufficient. 

**The "cloud" metaphor for external sources/sinks now has tooltips.** Hovering over a cloud shows an explanatory tooltip distinguishing sources ("Material flows into the model from outside") from sinks ("Material flows out of the model"), making the model boundary more explicit.

**Gaps:**

- **No visual depiction of accumulation.** Stocks look like static boxes. There is no metaphor (fill level, container, reservoir) that visually reinforces "this thing accumulates." Compare Stella's pipe-and-tank metaphor, which — whatever its aesthetic faults — makes the accumulation concept viscerally clear.
- **Inline sparklines are implemented but subtle.** Stocks display a 16px-tall polyline trajectory in their lower portion after simulation (blue at 70% opacity, fading to 25% when stale). This is the right idea, but the small size and low contrast mean users may not notice them. Consider making them more prominent — taller, or with a labeled axis hint — to better reinforce the accumulation concept.
- **Flow magnitude is ambiguous.** Arrowheads on material flows indicate direction, but there is no visual indication of *magnitude*. A flow of 0.1 looks identical to a flow of 1000. Even a simple thickness or animation cue would help.

### 1.2 Feedback Loop Identification (High)

**The problem:** Identifying feedback loops — and especially distinguishing reinforcing from balancing — is the core analytical skill in SD. Novices cannot reliably trace loops in diagrams with more than 4-5 elements.

**Current state:** The LOOPS toggle button highlights feedback loops with colored overlays and R/B labels. This is genuinely good — most competitors don't offer automated loop detection. Recent additions have substantially closed the gaps in this area:

- **Loop step-through with type filtering is well-implemented.** The `LoopNavigatorBar` provides Previous/Next buttons (`[`/`]` keys) and an "All" button for stepping through loops one at a time. Reinforcing/Balancing filter toggles let users focus on one loop type, with the step counter reflecting the filtered set. A help icon (?) explains loop terminology in-place. Elementary cycle enumeration ensures each distinct feedback path is navigable individually, even within larger strongly-connected components.
- **Loop narratives are behavioral and clear.** Loop tooltips generate natural-language narratives that trace cause-and-effect through causal polarities — e.g., "As Population rises, Births increase, further raising Population (reinforcing growth)." This scaffolds understanding of feedback mechanisms far better than bare variable chains.
- **Loop dominance analysis connects loops to behavior.** A dedicated "Loop Dominance" tab in the Dashboard shows a stacked area chart of per-loop activity over time, answering the key SD question: "Which loop is dominant right now?" Loops are color-coded (green for reinforcing, blue for balancing) with a collapsible "How to read this chart" explanation.

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

- **No model boundary visualization.** A subsystem grouping or color-coding feature would help users think about what is endogenous vs. exogenous. Literal-valued auxiliaries (parameters) implicitly represent the model boundary, but this is not made explicit.
- **No "what's not connected?" view.** Orphaned elements are flagged in validation, but a visual mode that dims all connected elements and highlights disconnected ones would help during model review.

---

## Part 2: General UX/UI Issues

### 2.1 Discoverability (High)

**Current state:** The toolbar has labeled text buttons (Stock, Flow, Auxiliary, etc.) with tooltips. Number keys 1-8 switch tools. The command palette (Ctrl+K) provides fuzzy search.

**Issues:**

- **No visible indication of keyboard shortcuts on toolbar buttons.** The tooltips mention shortcuts, but tooltips require hover-and-wait. Showing "S", "2", "3" etc. as small badges on toolbar buttons would aid learning.

### 2.2 Properties Panel (Medium)

**Current state:** Right sidebar shows type-specific forms with inline help icons. When nothing is selected, the panel shows a model summary with editable name/description, element counts, simulation settings, and quick-action buttons.

### 2.3 Visual Hierarchy and Information Density (Medium)

**Current state:** Clean monochromatic palette with accent colors for semantic meaning. Elements use distinct shapes and badges.

**Issues:**

- **Visual weight hierarchy exists but may not be strong enough.** Stocks use 3px borders, modules 2px, formula auxiliaries/lookups 1.5px (solid), and literal-valued auxiliaries (parameters) 1.5px (dashed). Lookups use dot-dash borders. This is a meaningful hierarchy, but all elements still share white fills. In complex models, the eye may still lack a strong entry point. Consider filled or tinted backgrounds for stocks to make them even more visually dominant.
- **Element badges communicate type clearly.** Formula auxiliaries show an "fx" badge, literal-valued parameters display their numeric value, lookup tables show "Table", and modules show "Module". All badges are now readable without prior knowledge of abbreviations.
- **Connection rendering doesn't scale.** In models with 20+ elements, the info links (dashed gray lines) create a dense web that obscures the stock-flow structure. Consider: connection bundling, hover-to-highlight-dependencies, or a mode that dims info links.

### 2.4 Dashboard and Results (Medium)

**Current state:** Tabbed result viewer with time-series charts (with unit-labeled axes), sweep results, Monte Carlo fan charts, and optimization progress. Clicking a variable name in the chart sidebar selects and centers the corresponding element on the canvas, bridging the gap between behavioral results and model structure. Fan charts for Monte Carlo are well-designed. The confidence band visualization is clear and uses appropriate opacity layering. This is a strength.

**Issues:**

****

- **Variable selection is inconsistent across result types.** Sweep and Monte Carlo panes provide a ComboBox to select which variable to plot. However, the main Simulation result pane plots all stocks and variables with no selection control. In larger models, this produces cluttered charts. Adding variable selection or toggle checkboxes to the Simulation pane would match the pattern already established in Sweep and Monte Carlo.

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

2. **Feedback loop detection, highlighting, and dominance analysis** — Automated loop identification with behavioral narratives, type filtering, and a loop dominance chart answering "which loop is dominant?" is a genuine differentiator. No free SD tool offers this depth of loop analysis.

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
| 1 | **Unit display in autocomplete** suggestions | Dimensional reasoning |
| 2 | **Variable selection in Simulation result pane** to match Sweep/Monte Carlo | Dashboard usability |
| 3 | **Actionable error messages** with fix guidance, not just structural descriptions | Error recovery |

*Note: Loop-by-loop stepping, stock sparklines, behavioral loop narratives, loop type filtering, loop dominance analysis, cloud tooltips, chart-to-model linking, and full-word element badges — the top priorities from earlier versions of this assessment — have all been implemented.*

---

## References

The SD-specific observations draw on:

- Sterman, J. (2000). *Business Dynamics: Systems Thinking and Modeling for a Complex World*. Ch. 5 (mental models), Ch. 7-8 (stocks and flows).
- Sweeney & Sterman (2000). "Bathtub dynamics: initial results of a systems thinking inventory." *System Dynamics Review* 16(4).
- Cronin, Gonzalez & Sterman (2009). "Why don't well-educated adults understand accumulation?" *Organizational Behavior and Human Decision Processes* 108(1).
- Groessler (2004). "Don't let history repeat itself -- methodological issues concerning the use of simulators in teaching and experimentation." *System Dynamics Review* 20(3).
