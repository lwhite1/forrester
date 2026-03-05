# Design 4: Maturity-Responsive Interface — Design Specification

## 1. Overview

A single-window application for building, simulating, and analyzing system dynamics models with an integrated LLM. The interface adapts its visual presentation, available affordances, and LLM behavior based on computable properties of the model's current state — its maturity — rather than requiring the user to select modes or workflow stages.

The initial implementation is a JavaFX fat client talking to an LLM API over HTTP. The architecture assumes an eventual web port (likely TypeScript + Canvas/SVG) and keeps rendering logic and business logic separated accordingly. The application also supports a no-AI mode (section 16) where all modeling, simulation, and analysis features work without an LLM connection, ensuring the tool remains fully functional for users who cannot afford or do not need AI integration.

> **Extension: Multi-model windows and module tabs.** This spec describes a single-window application.
> The [Multi-Model and Module Tab Design](Multi-Model%20and%20Module%20Tab%20Design.md) extends this
> architecture with separate windows for multiple models and tabs for module navigation within a model.

### Design principles

1. **The model is the source of truth for interface state.** No user-declared modes. The interface reads the model and reacts.
2. **Nothing is hidden, but emphasis shifts.** Every feature is always reachable. What changes is prominence, default visibility, and LLM behavior.
3. **Honest output.** If output is unreliable (incomplete equations, untested structure), say so visually and verbally. Never suppress output — label it.
4. **The conversation is the documentation.** Every model element traces to the conversation that created or modified it. No separate documentation step.
5. **Friction is informational, not blocking.** Warnings, labels, visual de-emphasis — not disabled buttons or locked panels.

---

## 2. Personas

### 2.1 Student — "Maya"

**Background.** Second-year graduate student in public health. Took one system dynamics course. Understands stocks and flows conceptually but has never built a model from scratch without a textbook walkthrough. Comfortable with software but not a programmer. Has used Excel extensively, R occasionally.

**Goals.** Build a cost-effectiveness model of a screening program for her thesis. Her advisor gave her a reference paper with a Markov model she needs to reproduce and extend. She needs to demonstrate that she understands the model structure, not just the results.

**Behavior patterns.**
- Wants to see output early — she learns by running things and seeing what happens, not by completing the structure first.
- Tends to accept the first formulation that produces "reasonable" output without testing alternatives.
- Asks the LLM broad questions: "is this right?" rather than specific ones.
- Will skip documentation steps if they're not integrated into the building process.
- Needs unit checking to be aggressive — she will connect dollars to people and not notice.

**What the interface needs to do for Maya.**
- Let her run early, but clearly mark what's incomplete so she doesn't anchor on artifacts.
- The LLM should push back on "is this right?" with specific questions: "What do you expect to happen to the Sick population after year 5?"
- Unit mismatches should be visually loud and accompanied by plain-language explanations.
- The conversation history *is* her thesis documentation — it needs to be exportable and coherent.

### 2.2 Researcher — "David"

**Background.** Tenured professor, 20 years of system dynamics experience. Has built 50+ models, published extensively. Currently uses Vensim Professional. Thinks in feedback loops. Can write stock-and-flow equations faster than most people write prose.

**Goals.** Model organizational resilience in healthcare systems. The model will have 40-80 stocks across 6 subsystems. He will present it to hospital administrators who have no SD background. Needs Monte Carlo sensitivity analysis and clear visualizations for non-technical audiences.

**Behavior patterns.**
- Builds structure rapidly — he knows the stocks and flows before he sits down. Does not need Socratic questioning during construction.
- Wants the tool to stay out of his way during building and become useful during analysis and presentation.
- Will be irritated by an LLM that asks "is this a stock or a flow?" when he's already created 30 stocks in the last 10 minutes.
- Skips directly to simulation, runs extreme-condition tests by instinct, modifies structure based on output.
- Cares deeply about dimensional consistency but catches most errors himself.
- Needs module/subsystem support — a flat canvas with 80 stocks is unusable.

**What the interface needs to do for David.**
- Detect rapid expert construction and suppress the questioning LLM posture.
- Provide fast keyboard-driven model construction (command palette, not drag-and-drop).
- Support subsystem grouping and module composition.
- Make Monte Carlo, parameter sweeps, and fan charts accessible without workflow ceremony.
- The LLM becomes useful for David during analysis: "Which parameters have the most influence on burnout onset?" and during presentation: "Generate a plain-language summary of why loop B2 dominates after year 3."

### 2.3 Decision-maker — "Elena"

**Background.** Deputy director of nursing at a 400-bed hospital. MBA, not technical. Has been fighting nurse turnover for three years. Has data (monthly turnover rates, hiring numbers, exit interview summaries) but no modeling skills. Has tried spreadsheet projections that always turn out wrong because they miss feedback effects.

**Goals.** Understand *why* her interventions keep failing. She suspects there's something structural going on but can't articulate it. She does not want to learn system dynamics. She wants answers, or at least better questions. She needs a summary report she can take to her board.

**Behavior patterns.**
- Will never open a command palette or type an equation. Interacts entirely through natural language.
- Asks "what if" questions naturally: "What happens if we hire 20% more?" She does not think in terms of parameters and values.
- Has real data but it's in messy spreadsheets with inconsistent time periods and missing months.
- Will accept the AI's output at face value unless the interface forces her to engage critically. She's used to trusting analytical tools (Excel, BI dashboards) without inspecting methodology.
- Needs uncertainty communicated as ranges, not fan charts. "Between 180 and 240" is meaningful; a percentile band chart is not.

**What the interface needs to do for Elena.**
- Build the model for her. She describes the problem; the AI constructs the structure, writes equations, sets parameters, and runs simulations — all behind the conversation.
- Show results as charts and plain-language summaries, not stock-and-flow diagrams.
- Always make the model inspectable — Elena may never look at the diagram, but it must be there. The "reveal" interaction (see section 15) is the safety net.
- Present uncertainty honestly. Ranges rather than point estimates. Never claim precision the model doesn't support.
- List assumptions explicitly and invite challenge: "This model assumes burnout is purely workload-driven. Is that right, or are there other factors — salary, scheduling, management quality?"

---

## 3. Architecture overview

### 3.1 Component layers

```
┌─────────────────────────────────────────────────────┐
│                   JavaFX Shell                       │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │Conversa- │  │   Canvas     │  │  Behavior     │  │
│  │tion Panel│  │  Component   │  │  Dashboard    │  │
│  └────┬─────┘  └──────┬───────┘  └───────┬───────┘  │
│       │               │                  │           │
│  ┌────┴───────────────┴──────────────────┴────────┐  │
│  │              UI Controller Layer                │  │
│  │   (event routing, selection state, undo/redo)   │  │
│  └────────────────────┬───────────────────────────┘  │
├───────────────────────┼─────────────────────────────┤
│  ┌────────────────────┴───────────────────────────┐  │
│  │            Model Façade / ViewModel             │  │
│  │  (wraps ModelDefinition, tracks maturity,       │  │
│  │   maintains conversation-to-element links,      │  │
│  │   emits change events)                          │  │
│  └────────────────────┬───────────────────────────┘  │
│  ┌────────────────────┴───────────────────────────┐  │
│  │            Existing Forrester Engine             │  │
│  │  ModelDefinition → ModelCompiler → Simulation   │  │
│  │  ParameterSweep, MonteCarlo, RunResult          │  │
│  └────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────┐  │
│  │        LLM Integration Layer (optional)         │  │
│  │  (prompt assembly, posture selection,            │  │
│  │   response parsing, tool-use protocol)           │  │
│  │  Not instantiated in no-AI mode (section 16)     │  │
│  └────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 3.2 Key architectural decisions

**Model Façade wraps ModelDefinition, not Model.** The existing `ModelDefinition` / `ModelDefinitionBuilder` system is the right layer to operate on — it represents model structure declaratively, supports serialization, and the `ModelCompiler` already handles the definition-to-executable-model transformation. The façade adds maturity tracking, provenance (conversation links), and change notification on top of `ModelDefinition`.

**LLM calls are asynchronous and non-blocking.** Every LLM request runs on a background thread. The conversation panel shows a typing indicator. The canvas and simulation controls remain interactive during LLM calls. The LLM layer exposes a `CompletableFuture<LlmResponse>` API.

**Undo/redo operates on ModelDefinition snapshots.** Each user action (adding a stock, changing an equation, reclassifying a variable) produces a new `ModelDefinition` snapshot pushed onto an undo stack. The conversation panel is append-only and not part of undo — undoing a model change does not delete the conversation that led to it.

**Canvas rendering is a custom JavaFX Canvas, not Scene Graph nodes.** For models with 80+ elements, hit-testing and layout on a retained-mode scene graph becomes expensive. A custom `Canvas` with manual hit-testing, viewport culling, and batched redraws will scale better and port more directly to HTML5 Canvas.

> **Problem area: Canvas rendering choice.** Custom Canvas gives performance and portability but loses JavaFX's built-in accessibility, focus management, and animation framework. Scene Graph would be simpler for small models but may not survive the 80-stock researcher model. A hybrid approach — Scene Graph nodes for selected/focused elements overlaid on a Canvas for the rest — adds complexity. **Decision needed before implementation.**

### 3.3 Web port considerations

The architecture is split so that the Model Façade, Maturity System, and LLM Integration Layer have no JavaFX dependencies. They operate on `ModelDefinition` and emit plain Java events. The UI layer (Canvas, Conversation Panel, Behavior Dashboard) is JavaFX-specific and would be rewritten for the web.

For the web port, the likely stack is:
- **Canvas component:** HTML5 Canvas or SVG (SVG for smaller models, Canvas for large ones — same tradeoff as JavaFX)
- **Conversation panel:** Standard web chat UI
- **Behavior dashboard:** A charting library (D3, Observable Plot, or similar)
- **Backend:** The Forrester engine running server-side on JVM, exposed via WebSocket for simulation streaming and REST for model CRUD
- **LLM integration:** Moves server-side; the client sends user messages, the server assembles prompts with model context

> **Problem area: Model Façade portability.** If the Model Façade is in Java, the web client either needs a thin Java backend (fine for single-user, questionable for hosted multi-user) or the façade needs to be reimplemented in TypeScript. Keeping the façade logic minimal and well-documented reduces the porting cost.

---

## 4. The Maturity Signal System

The maturity system is the core differentiator of this design. It continuously evaluates the model and produces signals that drive interface adaptation.

### 4.1 Signal definitions

Each signal is a value computed from the current `ModelDefinition`, updated on every model change.

#### Structural completeness

```java
public record StructuralCompleteness(
    int totalElements,       // stocks + flows + auxiliaries + constants
    int connectedElements,   // elements with at least one connection
    int danglingEndpoints,   // flows with missing source or sink
    int disconnectedSubgraphs, // connected components in the dependency graph
    double ratio             // connectedElements / totalElements, 0.0–1.0
) {}
```

Computed from `DependencyGraph`. A model with 10 stocks and 8 of them connected to flows has `ratio = 0.8`.

#### Equation coverage

```java
public record EquationCoverage(
    int totalRequiringEquations,  // flows + auxiliaries
    int withEquations,            // elements that have a defined expression
    int withPlaceholders,         // elements with placeholder "?" or empty expression
    double ratio                  // withEquations / totalRequiringEquations
) {}
```

#### Unit consistency

```java
public record UnitConsistency(
    int totalChecked,        // connections where both sides have units specified
    int consistent,          // dimensionally compatible connections
    int inconsistent,        // dimensional mismatches
    int unspecified,         // connections where one or both sides lack units
    List<UnitMismatch> mismatches  // specific mismatch details
) {}
```

The `DefinitionValidator` already checks some of this. This signal extends it to be continuously available, not just a one-shot validation.

#### Parameter specification

```java
public record ParameterSpecification(
    int totalParameters,    // constants + initial values
    int specified,          // have numeric values
    int placeholder,        // marked as "TBD" or using default 0
    int withDistributions,  // have uncertainty distributions for Monte Carlo
    double ratio
) {}
```

#### Simulation history

```java
public record SimulationHistory(
    int totalRuns,
    int runsSinceLastStructuralChange,
    boolean hasBeenRun,
    boolean resultsStale,  // model changed since last run
    boolean hasSweepResults,
    boolean hasMonteCarloResults
) {}
```

#### Data attachment

```java
public record DataAttachment(
    int dataSeriesLoaded,
    int dataSeriesMatchedToStocks,  // data columns mapped to model stocks
    boolean comparisonRun           // at least one run compared to data
) {}
```

### 4.2 Composite maturity assessment

The signals combine into a non-numeric qualitative assessment used for LLM posture selection and visual adaptation. This is deliberately not a score — a single number would hide which dimensions are mature and which aren't.

```java
public record MaturityAssessment(
    StructuralCompleteness structure,
    EquationCoverage equations,
    UnitConsistency units,
    ParameterSpecification parameters,
    SimulationHistory simulation,
    DataAttachment data,
    BuildingPace pace          // see 4.3
) {}
```

### 4.3 Building pace detection

For the researcher persona, the system needs to detect rapid expert construction and suppress the questioning LLM posture.

```java
public record BuildingPace(
    int elementsAddedLastMinute,
    int equationsDefinedLastMinute,
    boolean isRapidConstruction    // heuristic threshold, e.g., > 3 elements/minute
) {}
```

> **Problem area: Pace detection thresholds.** "3 elements per minute" is a guess. Too low and the LLM goes quiet for a student who managed to add 4 stocks quickly by following a textbook. Too high and the expert gets pestered. This needs user testing. A safer approach: the threshold starts conservative (high, so the LLM stays quiet more easily) and the LLM only enters questioning posture when there's a clear pause in activity (no model changes for 30+ seconds) *and* the model is structurally incomplete. **This inverts the default: the LLM is quiet unless there's both a gap and a pause.**

### 4.4 Maturity change events

The façade emits events when maturity signals change meaningfully:

```java
public sealed interface MaturityEvent {
    record StructureChanged(StructuralCompleteness before, StructuralCompleteness after)
        implements MaturityEvent {}
    record EquationsDefined(EquationCoverage before, EquationCoverage after)
        implements MaturityEvent {}
    record UnitMismatchDetected(UnitMismatch mismatch)
        implements MaturityEvent {}
    record ResultsStale(SimulationHistory history)
        implements MaturityEvent {}
    record DataAttached(DataAttachment before, DataAttachment after)
        implements MaturityEvent {}
    record PaceChanged(BuildingPace before, BuildingPace after)
        implements MaturityEvent {}
}
```

The UI controller subscribes to these and triggers canvas redraws, LLM posture updates, and dashboard visibility changes.

---

## 5. Window layout

### 5.1 Primary layout

A horizontal split-pane with three regions. Unlike Design 1, the right region (behavior dashboard) is initially collapsed — it appears when there's behavior to show.

```
┌────────────────────────────────────────────────────────────┐
│ ┌──────────┐                                               │
│ │ Cmd+K    │  (command palette — appears on keystroke)      │
│ └──────────┘                                               │
│                                                            │
│ ┌─────────────┬──────────────────────┬───────────────────┐ │
│ │             │                      │                   │ │
│ │ Conversa-   │                      │   Behavior        │ │
│ │ tion        │   Canvas             │   Dashboard       │ │
│ │ Panel       │                      │   (collapsed      │ │
│ │             │                      │    initially)     │ │
│ │             │                      │                   │ │
│ │             │                      │                   │ │
│ │             │                      │                   │ │
│ ├─────────────┤                      │                   │ │
│ │ Maturity    │                      │                   │ │
│ │ Strip       │                      │                   │ │
│ └─────────────┴──────────────────────┴───────────────────┘ │
│ ┌──────────────────────────────────────────────────────────┐│
│ │ Status bar: unit warnings · equation gaps · run status   ││
│ └──────────────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────┘
```

**Conversation panel:** Fixed width (300–400px), left side. Contains the chat interface and, below it, the maturity strip (a compact visualization of the maturity signals — see 5.3).

**Canvas:** Fills the center. Pan, zoom, selection, and direct manipulation of model elements.

**Behavior dashboard:** Right side, initially collapsed to zero width. Expands (animated, 300–400px) when the first simulation completes. The user can manually expand/collapse it at any time via a drag handle or keyboard shortcut.

**Status bar:** Bottom, single line. Shows the most important warnings: unit mismatches, undefined equations, stale results. Clicking a warning navigates to the relevant element on the canvas.

**Command palette:** Overlaid on the window, centered, appears on Ctrl+K / Cmd+K. Dismisses on Escape or action completion.

### 5.2 Panel resizing and visibility

All panels are resizable via drag handles. The conversation panel can be collapsed to an icon strip (for David building rapidly and wanting maximum canvas space). The behavior dashboard can be manually expanded before the first simulation run (for Maya who wants to see what will be there).

> **Problem area: Layout on small screens.** A three-panel layout at 1920px works. At 1366px (common laptop), three panels are cramped. On the web port, mobile is a non-starter for this tool, but laptop screens matter. Consider a tab-based fallback where Conversation and Dashboard are tabs sharing the same side panel, and only one is visible at a time. **This reduces the cross-linking benefit (can't see conversation and output simultaneously) but may be a necessary compromise.**

### 5.3 Maturity strip

A compact horizontal bar at the bottom of the conversation panel showing maturity signals as small segmented indicators:

```
Structure  ████████░░  80%
Equations  ██████░░░░  60%
Units      ██████████  ok
Parameters ████░░░░░░  40%
Simulated  ──────────  not yet
Data       ──────────  none
```

Each segment is clickable — clicking "Equations 60%" lists the elements missing equations, and clicking an element in that list navigates to it on the canvas.

This is *not* a progress bar toward a goal. It's a status display. 40% parameter specification is not "bad" — the user may be intentionally working on structure before parameterizing. The maturity strip never judges; it reports.

---

## 6. Canvas component

### 6.1 Element rendering

The canvas supports two rendering styles for each element, determined by the element's classification state. Elements transition between styles as the user formalizes them.

**Unclassified variable (CLD style):**
```
    ┌─────────────────┐
    │   Staff Level    │
    └─────────────────┘
         ↑
    (plain rounded rectangle, thin border)
```

**Stock:**
```
    ┌═══════════════════┐
    ║   Staff Level     ║  ▁▂▃▄▅▆▅▃  ← sparkline (only when equation
    ║     350           ║             coverage allows simulation)
    ╚═══════════════════╝
         ↑
    (heavy border, value displayed, optional sparkline)
```

**Flow:**
```
         ⟶ ⊳ ⟶         ← valve symbol on the connection line
        Hiring Rate
           12/yr
```

**Auxiliary / Converter:**
```
          ╭───────╮
          │ Effect │
          │ of     │
          │ Fatigue│
          ╰───────╯
              ↑
    (circle/ellipse, smaller than stocks)
```

**Constant:**
```
          ┌ ─ ─ ─ ┐
          │ Target │
          │  500   │
          └ ─ ─ ─ ┘
              ↑
    (dashed border — visually lighter, clearly a parameter not a dynamic element)
```

### 6.2 Visual maturity encoding

Elements on the canvas carry visual signals about their completeness:

| State | Visual treatment |
|---|---|
| Unclassified (CLD variable) | Thin border, neutral fill, no value display |
| Classified but no equation | Typed shape (stock/flow/aux), amber left-edge accent |
| Equation defined, units unspecified | Typed shape, small "?" unit badge |
| Equation defined, unit mismatch on a connection | Red connection line, red badge on both endpoints |
| Fully specified | Typed shape, clean fill, no warnings |
| Equation defined, results stale | Sparkline shown with reduced opacity + "stale" label |

Connections follow the same logic:

| State | Visual treatment |
|---|---|
| CLD arrow (polarity only) | Thin arrow, "+" or "−" label or natural-language annotation |
| Flow (typed) | Thicker line with valve symbol |
| Unit mismatch | Red line with mismatch icon at midpoint |
| Connector (aux → flow) | Thin dashed arrow |

### 6.3 Sparkline behavior

Sparklines appear inside stock elements when:
1. The stock has at least one inflow or outflow with a defined equation.
2. A simulation has been run (even a partial one).
3. Results are not stale (or shown at reduced opacity if stale).

Sparklines are small (60×20px at default zoom), show the full simulation time range, and have no axis labels. They exist to show *shape* — monotonic growth, oscillation, plateau — not precise values. Hovering over a sparkline shows a tooltip with the value at that time point.

> **Problem area: Sparkline on partial models.** If only 3 of 8 flows have equations, the simulation will run with the undefined flows contributing zero. The sparklines will show *something*, but it's an artifact of absent structure, not a real prediction. The reduced-opacity treatment helps, but Maya may still anchor on the shape. **Consider: should sparklines on stocks with upstream undefined equations show a distinct "partial" pattern — e.g., a dotted sparkline — to make the incompleteness visually salient?**

### 6.4 Canvas interaction

**Selection.** Click to select. Shift-click to multi-select. Rubber-band selection for groups. Selected elements show resize handles and a floating context toolbar.

**Context toolbar.** Appears near the selected element(s). Contains only actions relevant to the selection:
- Stock selected: Add Inflow, Add Outflow, Edit Equation, Classify (if unclassified)
- Flow selected: Edit Equation, Set Units, Reverse Direction
- Multiple selected: Group into Subsystem, Align, Distribute

**Direct manipulation.**
- Drag to move elements.
- Drag from a connection port to create a new flow or connector.
- Double-click an element to open inline equation editing (a text field directly on the canvas, not a modal).
- Scroll to zoom. Middle-drag to pan.

**Subsystem grouping.** Select multiple elements → context toolbar "Group" → they collapse into a named rectangle with summary info (number of stocks, number of flows). Double-click the group to expand it in place. This is essential for David's 80-stock models.

> **Problem area: Subsystem grouping and ModelDefinition.** The existing `Module` and `ModuleInstanceDef` classes support compositional model structure, but the UI grouping need not map 1:1 to module boundaries. A visual group might span module boundaries for layout purposes, or a module might be split across visual groups. **Keep visual grouping (layout concern) separate from module structure (model concern). They can be linked but must not be conflated.**

### 6.5 Auto-layout

The canvas uses a force-directed layout with manual override. When elements are first created (via LLM or command palette), auto-layout places them. Once the user manually drags an element, its position is pinned and the auto-layout respects the pin.

`AutoLayout` already exists in the codebase. It will need extension to handle:
- Incremental layout (add one element to an existing arrangement without rearranging everything)
- Subsystem grouping constraints (elements in the same group stay spatially close)
- Flow routing (valve symbols centered on the path between source and sink stocks)

---

## 7. Conversation panel

### 7.1 Message types

The conversation contains multiple message types, visually distinguished:

| Type | Appearance | Source |
|---|---|---|
| User message | Right-aligned bubble, user's text | User input |
| LLM response | Left-aligned bubble, markdown rendered | LLM |
| Model action | Centered, compact, icon-prefixed | System |
| Warning | Amber background, centered | Maturity system |
| Simulation summary | Compact card with mini-charts | Simulation engine |

**Model action messages** are automatically inserted when the model changes, whether via conversation or direct manipulation:

```
  ⊕ Added stock "Sick" (initial: 0, units: People)
  ✎ Changed flow "Recovery" equation: sick * 0.30 → sick * recoveryRate
  ⚡ Simulation run completed: 20 years, 240 steps
```

These create the traceability chain. Every element on the canvas has a history of model action messages. Clicking an element on the canvas scrolls the conversation to the most recent action involving it.

### 7.2 Conversation-to-canvas cross-linking

When the LLM mentions a model element by name, the name is rendered as a clickable chip in the conversation. Clicking it selects the element on the canvas and pans to it.

When the user selects an element on the canvas, the conversation panel shows a subtle "last mentioned" indicator — a small tag in the margin at the scroll position of the most recent message involving that element. The panel does not auto-scroll (auto-scrolling is disorienting); the user clicks the tag to jump to it.

### 7.3 Input modes

The conversation input supports:
- **Natural language.** Default. The LLM interprets and may propose model changes.
- **Command prefix.** Starting with `/` enters command mode (e.g., `/add stock "Burnout"`, `/run`, `/sweep recoveryRate 0.1 0.5 0.01`). Commands are parsed locally and execute immediately without an LLM round-trip. This is David's fast path.
- **Equation input.** Starting with `=` enters equation mode. Typed directly as Forrester expressions, parsed by `ExprParser`, and applied to the currently selected element. Also David's path.

> **Problem area: Command language design.** The command vocabulary needs to be small, memorable, and discoverable. If it's too large, it becomes its own learning curve. If it's too small, David falls back to natural language for everything and the speed advantage disappears. **Start with 10-15 commands max: add/remove/connect/disconnect/run/sweep/montecarlo/undo/redo/group/ungroup/export. Expand based on usage data.**

### 7.4 LLM-proposed model changes

When the LLM proposes a structural change ("I'd suggest adding a Burnout stock with an inflow from Workload"), the proposal appears as a diff card in the conversation:

```
┌─────────────────────────────────────────┐
│ Proposed change                         │
│                                         │
│  + Stock "Burnout" (initial: 0, People) │
│  + Flow "Burnout Accumulation"          │
│    from: Workload effect                │
│    equation: workload * 0.05            │
│                                         │
│  [Apply]  [Modify]  [Dismiss]           │
└─────────────────────────────────────────┘
```

"Apply" executes the change. "Modify" opens the proposed elements for inline editing before applying. "Dismiss" does nothing. The LLM never modifies the model without the user clicking Apply.

This is a hard rule. The LLM proposes; the user disposes. Even for David, who may Apply 20 proposals in quick succession, the explicit approval step ensures the model remains the researcher's artifact, not the LLM's.

> **Problem area: Approval fatigue.** If the LLM proposes 15 changes in a single response (e.g., "here's a complete SIR model"), clicking Apply 15 times is tedious. Support batch proposals — a single "Apply All" button for multi-element proposals — but show the full diff so the user can review. **This is a tension between agency and convenience. Default to batch-apply with full visibility; let the user configure per-element approval if they want it.**

---

## 8. Behavior dashboard

### 8.1 Visibility

The dashboard is collapsed (zero width) initially. It expands automatically on the first completed simulation run. The user can:
- Manually expand it before any run (drag handle or keyboard shortcut).
- Collapse it to focus on structure.
- Pop it out into a separate window (for dual-monitor setups — important for David presenting to stakeholders).

### 8.2 Dashboard contents

**Time series panel.** Line charts showing stock values over time. By default, shows only stocks the user has interacted with (selected, discussed, or edited equation). Stocks can be added/removed from the chart via checkboxes or by dragging a stock from the canvas into the dashboard.

**Run comparison.** When multiple runs exist (from parameter sweeps or manual re-runs), previous runs appear as ghosted lines behind the current run. A run selector lets the user toggle visibility.

**Monte Carlo / fan chart.** When Monte Carlo results exist, the fan chart (already implemented as `FanChart`) appears as an option. Percentile bands at 50%, 75%, 95%.

**Sensitivity summary.** After a parameter sweep or Monte Carlo run, a ranked text summary: "Staff Level is most sensitive to hiring delay (±40%), followed by attrition rate (±25%)." This is the plain-language alternative to a tornado diagram.

**Stale results indicator.** When the model has changed since the last run, the entire dashboard gets a subtle "stale" overlay — a thin amber border and a label: "Model changed since last run. Results may not reflect current structure." Clicking the label offers to re-run.

### 8.3 Dashboard and maturity

The dashboard's default content adapts to maturity:

| Maturity state | Default dashboard content |
|---|---|
| No runs yet | Empty, with a centered prompt: "Run the simulation to see results here." If the model has equation gaps, the prompt notes them. |
| First run, no data | Time series + sensitivity prompt: "Try a parameter sweep to see what matters most." |
| Multiple runs, no data | Time series + run comparison + sensitivity summary |
| Monte Carlo completed | Fan chart + sensitivity summary |
| Data attached | Time series with data overlay, discrepancy highlights |

---

## 9. Command palette

Activated by Ctrl+K / Cmd+K. A floating text field with fuzzy-matched command completion.

### 9.1 Command categories

**Model construction:**
```
add stock <name> [initial] [unit]
add flow <name> from <source> to <sink>
add aux <name>
add constant <name> <value> [unit]
connect <source> <sink>
disconnect <source> <sink>
classify <element> as stock|flow|aux|constant
group <element1> <element2> ... as <name>
ungroup <name>
```

**Simulation:**
```
run [duration] [timestep]
sweep <parameter> <low> <high> [steps]
montecarlo [iterations]
```

**Navigation:**
```
find <element-name>            → select and pan to element
show <element-name> history    → scroll conversation to element's history
focus <subsystem-name>         → zoom canvas to subsystem
```

**Analysis:**
```
sensitivity                    → trigger sensitivity analysis
compare <run1> <run2>          → overlay two runs in dashboard
export csv                     → export current results
export model                   → export ModelDefinition as JSON
```

**LLM shortcut:**
```
ask <natural language>         → same as typing in conversation, but from palette
```

### 9.2 Fuzzy matching

The palette uses prefix matching first, then fuzzy matching on the remainder. Typing "ad st Bur" matches "add stock Burnout". Matching is case-insensitive. Results show the command with the matched portions highlighted.

The palette also searches model element names. Typing a stock name without a command prefix navigates to that element.

> **Problem area: Command discoverability.** A command palette is powerful for users who know the commands and invisible to users who don't. Maya will not discover `/sweep` by herself. **The conversation panel should suggest commands contextually — after a first simulation, the LLM might say "You could run a sensitivity analysis with `/sweep recoveryRate 0.1 0.5`" — but this requires the LLM to know the command vocabulary, adding another dimension to prompt engineering.**

---

## 10. LLM integration

### 10.1 Prompt assembly

Each LLM call assembles a prompt from:

1. **System prompt** — static instructions defining the LLM's role and constraints.
2. **Posture block** — dynamic, selected by the maturity system (see 10.2).
3. **Model context** — a serialized summary of the current model: all element names, types, equations, units, connections, maturity signals. Derived from `ModelDefinition`.
4. **Conversation history** — the last N messages (windowed; see 10.3).
5. **User message** — the current input.

The model context is regenerated on every call. For large models, this may be substantial (several thousand tokens for 80 stocks with equations). The LLM layer manages context window limits by truncating conversation history before truncating model context — the model is always fully represented.

### 10.2 Posture selection

The maturity assessment selects one of six LLM postures, each defined as a system prompt block. The selection logic:

```
if (aiOnlyMode && !userHasOpenedCanvas) → AUTONOMIST
else if (pace.isRapidConstruction) → OBSERVER
else if (structure.ratio < 0.5 && equations.ratio < 0.3) → ELICITOR
else if (equations.ratio < 0.8) → FORMALIZER
else if (!simulation.hasBeenRun || simulation.resultsStale) → ANTICIPATOR
else if (data.dataSeriesLoaded == 0) → INTERPRETER
else → CHALLENGER
```

#### OBSERVER posture

**Trigger:** Rapid construction pace detected.

**Behavior:** The LLM is nearly silent. It acknowledges commands ("Added stock Burnout") but does not ask questions or offer suggestions. It answers direct questions fully. If the user pauses (no model changes for 60+ seconds) and the model has structural gaps, the LLM transitions out of OBSERVER by offering a single observation: "You've built 12 stocks — I notice the Workload subsystem doesn't have any outflows to the rest of the model yet. Want me to take a look?"

**Persona mapping:** This is David's building posture. Maya is unlikely to trigger it.

#### ELICITOR posture

**Trigger:** Low structural completeness and equation coverage, no rapid construction.

**Behavior:** The LLM asks questions about the system being modeled. It focuses on what's missing: "You have patients entering Treatment — what happens to them after treatment? Do they recover, relapse, or transition to a different state?" It does not propose specific stocks or flows unless asked. It asks one question at a time, not five.

It does not comment on simulation output even if the user runs the model, because the structure is too incomplete for output to be meaningful. If the user asks about output, the LLM says so: "The simulation ran, but with only 30% of equations defined, the trajectories mostly reflect what's missing rather than what's there. The Staff Level curve might be worth looking at since that subsystem is complete."

**Persona mapping:** This is Maya's early posture. The LLM helps her think about what belongs in the model.

> **Problem area: ELICITOR quality.** This posture requires the LLM to ask domain-relevant questions, not generic ones. "Have you considered feedback effects?" is useless. "What determines how long patients stay in the Sick state — is there a treatment that speeds recovery?" is useful. The quality of elicitation depends entirely on the LLM's understanding of the domain, which varies. **For well-known domains (epidemiology, supply chains, population dynamics), this will work well because training data is rich. For novel domains (organizational resilience, ecological tipping points), the LLM's questions will be generic. There is no good mitigation short of domain-specific fine-tuning or retrieval augmentation.**

#### FORMALIZER posture

**Trigger:** Reasonable structural completeness but low equation coverage.

**Behavior:** The LLM focuses on translating qualitative relationships into equations. For each undefined relationship, it offers 2–3 qualitative shapes as inline plots (rendered as small text-art or, on the canvas, as thumbnail charts):

"The link from Workload to Quality — how does it behave?
1. **Linear decline** — quality drops proportionally as workload increases
2. **Threshold** — quality holds steady until workload exceeds a critical point, then drops sharply
3. **Diminishing** — quality degrades quickly at first, then the rate of decline slows"

When eliciting parameter values, it uses domain language: "How long does burnout take to build up — are we talking days, weeks, or months?" and translates the answer into a rate constant.

**Persona mapping:** Both personas use this. Maya benefits from the shape selection (she can choose without knowing the math). David uses it selectively — he'll type equations directly for familiar relationships and let the LLM formalize the ones he's less sure about.

#### ANTICIPATOR posture

**Trigger:** Model is fully or nearly fully specified but hasn't been simulated, or results are stale.

**Behavior:** Before simulation, the LLM asks what the user expects: "Before we run — what do you think will happen to the Burnout stock over 5 years? Steady increase? Peak and decline? Oscillation?" This creates a prediction the first run can test, turning it from an observation ("huh, look at that curve") into a test ("my prediction was wrong — why?").

If results are stale (model changed since last run), the LLM notes what changed and asks whether the user expects the change to affect output: "You increased the recovery rate from 0.3 to 0.5. Do you think that's enough to prevent the Sick population from growing?"

**Persona mapping:** Critical for Maya — it forces her to articulate expectations before seeing output, which is the single most effective guard against confirmation bias. Useful but less critical for David, who does this mentally already.

#### INTERPRETER posture

**Trigger:** Simulation has been run, no empirical data loaded.

**Behavior:** The LLM interprets output, identifies dominant feedback loops, explains surprising behavior, and suggests sensitivity analysis. It is careful to frame observations as model predictions, not truths: "The model predicts burnout peaks at month 18 — that's driven by the reinforcing loop between workload and turnover."

It proactively suggests tests: "Have you tried setting initial Staff Level to zero? That would test whether the hiring pipeline alone can sustain the system." These are extreme-condition tests — a standard SD validation technique.

**Persona mapping:** David expects this and will act on it quickly. Maya needs it explained more — the LLM should offer to elaborate on what "dominant feedback loop" means if she asks.

#### CHALLENGER posture

**Trigger:** Empirical data loaded and matched to model stocks.

**Behavior:** The LLM turns adversarial. It identifies specific discrepancies between model output and data: "The model shows a steady decline in Patient Volume from month 6, but the data shows a plateau followed by a sharp drop at month 10. A constant outflow rate can't produce that shape — there may be a threshold effect or a delayed feedback you're not capturing."

It proposes structural explanations, not just parameter adjustments: "Adjusting the attrition rate from 0.05 to 0.08 makes the curve fit better, but a better structural explanation might be that attrition is workload-dependent — high workload causes attrition, which increases workload on remaining staff, which causes more attrition."

It runs and compares variant models: "I built two variants — one with constant attrition and one with workload-dependent attrition. The workload-dependent version matches the data much better in months 8–14. Here's the comparison."

**Persona mapping:** Primarily David. Maya would encounter this only if her thesis involves fitting to real data, in which case she needs explicit guidance on what "structural explanation" means versus "parameter tweak."

> **Problem area: CHALLENGER structural proposals.** When the LLM proposes structural changes (adding a feedback loop, making a constant into a variable), it's doing the hardest part of modeling. If it gets this right, it's enormously valuable. If it gets it wrong, it's actively harmful — the user may accept a plausible-sounding but incorrect structural change because the LLM presented it confidently. **There is no good technical mitigation. The design mitigation is: structural proposals always require explicit Apply, the diff is shown clearly, and the LLM frames proposals as hypotheses to test rather than corrections to make.**

#### AUTONOMIST posture

**Trigger:** AI-only mode active and the user has not opened the canvas. See section 15 for the full AI-only interface design.

**Behavior:** The LLM builds model structure autonomously — no per-element approval required. This is the key difference from all other postures. It explains what it built and why in plain language. It runs simulations and interprets results proactively. It asks calibration questions in domain language: "How many experienced nurses do you have right now? What's your monthly hire rate?"

It combines aspects of every other posture in a single flow: conceptualizes (ELICITOR), formalizes (FORMALIZER), anticipates (ANTICIPATOR), interprets (INTERPRETER), and challenges when data is present (CHALLENGER). The difference is that it does all of this autonomously rather than collaboratively. The user provides domain knowledge; the AI provides modeling expertise.

It still:
- Lists its assumptions explicitly after building the model.
- Shows uncertainty (fan charts or ranges) not point estimates.
- Asks the user whether the output matches their experience (calibration check).
- Periodically offers to reveal the model ("Want to see the diagram?").

It does not:
- Present results without caveats about model limitations.
- Claim precision that the model doesn't support.
- Resist when the user challenges an assumption.
- Proceed without the user confirming that the problem description is understood correctly.

**Transition out of AUTONOMIST:** When the user opens the canvas (via the "reveal" interaction or by manually expanding it), the posture system re-evaluates based on standard maturity signals. If the user starts editing the model directly, they've moved from Elena's workflow to Maya or David's workflow, and the posture adapts accordingly.

**Persona mapping:** This is Elena's posture. Maya and David start with the canvas visible and never enter AUTONOMIST.

> **Problem area: Autonomous model building quality.** This posture asks the LLM to do everything a skilled modeler does — conceptualize, formalize, parameterize, interpret. Current LLMs can produce plausible model structures for well-known domains (epidemiology, supply chains, basic HR dynamics). They will produce mediocre-to-wrong structures for novel domains. **The mitigation is honesty: the AI should say "This is a standard burnout-turnover model. It may not capture what's specific to your hospital. Tell me what's different about your situation." But honesty about limitations is exactly the thing LLMs are worst at.**

### 10.3 Context window management

**Model context is always fully included.** An 80-stock model with equations serializes to roughly 3,000–5,000 tokens. This is non-negotiable — the LLM must see the complete model to give useful responses.

**Conversation history is windowed.** The last 20 messages (user + LLM) are included. Older messages are summarized into a rolling "conversation so far" block: key decisions, open questions, proposed and rejected alternatives. This summary is regenerated every 10 messages by a background LLM call.

**Model action messages are compressed.** Instead of including every "Added stock X" action, the conversation history includes a summary: "Recent model changes: added 3 stocks (A, B, C), defined 2 equations, ran simulation once."

> **Problem area: Context window cost.** Every LLM call includes 3,000–5,000 tokens of model context + 2,000–4,000 tokens of conversation history + 500–1,000 tokens of posture block + the user message. That's 6,000–10,000 tokens per call before the response. At current API pricing, a heavy user (200 LLM calls per session) generates $2–$8 per session in API costs. **This is manageable for a research tool but matters for a student persona if the tool is institutionally funded. Consider caching repeated model context across turns (most calls change only a few elements). Users who cannot afford API costs can use no-AI mode (section 16), which provides the full modeling tool without any LLM calls.**

### 10.4 Tool use protocol

The LLM can propose model changes via a structured tool-use protocol (function calling). The LLM's available tools:

```
add_stock(name, initial_value, unit)
add_flow(name, source_stock, sink_stock, equation)
add_auxiliary(name, equation, unit)
add_constant(name, value, unit)
set_equation(element_name, equation)
remove_element(element_name)
connect(source, sink)
run_simulation(duration, timestep)
run_sweep(parameter, values)
run_monte_carlo(iterations, parameters_with_distributions)
```

Tool calls from the LLM are intercepted by the UI controller, rendered as diff cards in the conversation (see 7.4), and await user approval before execution. The `run_*` tools execute immediately (running a simulation is non-destructive).

---

## 11. Specific workflows

### 11.1 Maya: Building a screening cost-effectiveness model

**Session start.** Maya opens the application. Canvas is empty. Conversation panel shows a greeting and prompt: "Describe the system you'd like to model, or start adding elements with the canvas or command palette."

**Conceptualization.** Maya types: "I need to model a cancer screening program. There's a healthy population, some have undetected cancer, screening moves them to detected, and then they get treatment."

The LLM (ELICITOR posture — model is empty) responds: "That sounds like a state-transition model with at least four health states. A few questions before we sketch it out: Can people who are treated go back to the healthy state, or is there a 'cancer survivor' state? Is there a state for people who die from cancer, or a general death state?"

Maya answers. The LLM proposes a 5-state model (Healthy, Undetected Cancer, Detected Cancer, Treatment, Dead) as a diff card. Maya clicks Apply.

The canvas shows five unclassified variables (CLD-style rounded rectangles) connected by arrows. No sparklines, no simulation output. The maturity strip shows Structure: 100%, Equations: 0%, Units: unspecified.

**Formalization.** The LLM (FORMALIZER — structure complete, equations empty) walks through each flow: "The transition from Healthy to Undetected Cancer — that's the cancer incidence rate. Is this roughly constant for this population, or does it depend on age?"

Maya says it's age-dependent. The LLM offers a lookup table shape (linear increase with age) as a thumbnail plot. Maya approves.

For each flow, the LLM asks for the parameter in domain terms: "How effective is the screening test — what fraction of cancers does it catch?" Maya says "about 80%." The LLM sets sensitivity to 0.8 and writes the equation.

As equations are defined, elements on the canvas transition from CLD-style to stock-and-flow style. The amber "no equation" accent disappears from formalized elements. The maturity strip progresses.

**Premature simulation.** After defining 3 of 7 flow equations, Maya gets impatient and hits Run. The simulation executes. The behavior dashboard expands, showing time series. But: the three undefined flows contributed zero, so Treatment stays empty and Dead barely rises. Sparklines appear on the three stocks with complete upstream equations; the others show dotted sparklines at reduced opacity.

The LLM (still FORMALIZER) comments: "The simulation ran, but the treatment and mortality pathways have no equations yet, so those stocks aren't moving. The Healthy → Undetected → Detected pathway looks active — you can see screening driving detection. Want to continue defining the remaining equations, or does the Detected stock behavior look reasonable?"

This is the key moment. The LLM doesn't block Maya, doesn't scold her, and doesn't pretend the partial output is meaningful. It names what's real and what's not, and redirects to the structural work.

**Analysis.** After full specification, Maya runs a parameter sweep on screening sensitivity (0.5 to 1.0). The dashboard shows the sweep results. The LLM (INTERPRETER) identifies that total cost is most sensitive to treatment cost per cycle, not screening sensitivity — a non-obvious finding that challenges Maya's framing.

**Export.** Maya exports the conversation as a PDF for her thesis appendix. The model action messages form a structured record of every decision, with the LLM's questions and her answers providing the rationale.

### 11.2 David: Building an organizational resilience model

**Session start.** David opens the application and immediately collapses the conversation panel to a narrow strip. He wants maximum canvas space.

**Rapid construction.** David hits Ctrl+K and types `add stock Staff 500 people`. The stock appears on the canvas. He types `add stock Workload 100 tasks`, `add stock Burnout 0 people`, `add flow Hiring staff`, `add flow Attrition staff`, `add flow BurnoutAccumulation burnout`. Six commands in 90 seconds.

The maturity system detects rapid construction (BuildingPace.isRapidConstruction = true). The LLM enters OBSERVER posture. It acknowledges commands in the conversation strip but asks no questions.

David types equations directly: he selects a flow, presses `=`, and types `staff * attritionRate`. For the burnout accumulation flow, he types a more complex expression: `max(0, (workload / staff - targetRatio) * burnoutFactor)`.

**Subsystem grouping.** After building 25 elements, David selects the staffing-related stocks and groups them: `group Staff Hiring Attrition TargetStaff as "Staffing"`. The elements collapse into a labeled rectangle. He builds five more subsystems this way.

**Pause and reflection.** After 40 minutes, David stops adding elements. The pace detector notices the pause. The model has 45 stocks, 60 flows, and equation coverage at 85%. The LLM offers a single observation: "The Burnout subsystem connects to Staffing via attrition, but the Quality subsystem doesn't have any feedback to Workload yet. In your earlier work on hospital systems, that link was usually significant."

David considers this, decides the LLM is right, and adds the missing feedback loop.

> **Problem area: LLM referencing "earlier work."** The LLM doesn't actually know David's publication history unless it's provided as context. This scenario assumes either: (a) David has told the LLM about his prior models in this session, (b) there's a persistent user profile with prior model summaries, or (c) the LLM is hallucinating. **Option (b) would be valuable but adds complexity (profile management, privacy, storage). For v1, the LLM can only reference what's in the current conversation. Flag this as a v2 feature.**

**Monte Carlo.** David runs Monte Carlo with 500 iterations, Latin Hypercube Sampling, distributions on 12 parameters. The fan chart appears in the dashboard. He pops the dashboard into a separate window on his second monitor.

David asks the LLM: "Which parameters contribute most to variance in Staff Level at year 5?" The LLM (INTERPRETER) runs a correlation analysis on the Monte Carlo results and returns a ranked list with partial rank correlation coefficients.

**Presentation mode.** David asks the LLM: "Write a plain-language summary of the Burnout → Attrition → Workload reinforcing loop for a hospital administrator audience." The LLM generates a two-paragraph explanation with no jargon. David copies it into his slide deck.

### 11.3 Elena: Understanding nurse turnover

**Session start.** Elena opens the application and selects "Help me understand a problem" (the AI-only entry point — see section 15). The canvas is hidden. The conversation panel fills the window. A greeting prompts: "Describe the problem you're trying to understand."

**Problem description.** Elena types: "We keep hiring nurses but turnover keeps going up. The more we hire, the worse it seems to get. What's going on?"

The LLM (AUTONOMIST posture) responds with a structural hypothesis: "That pattern — where hiring makes the problem worse — suggests a reinforcing feedback loop. Here's my working theory: New hires increase total staff, but they also increase the workload on experienced nurses through training and mentoring. That extra workload causes burnout, which increases turnover among your experienced staff. Their departure increases workload further."

Behind the scenes, the LLM has called `add_stock` five times, `add_flow` six times, `set_equation` for each flow, and `run_simulation`. Elena sees none of this — she sees the explanation and a chart.

"I built a small model to test this. With some standard assumptions, here's what it predicts over 3 years:"

A time series chart appears inline in the conversation: Experienced Nurses declining, New Hires cycling, Total Workload rising.

"Does this match what you're seeing? And how long does it typically take a new nurse to become fully productive?"

**Calibration.** Elena says "About 6 months. And yes, that's exactly what's happening." The LLM updates the ramp-up parameter and re-runs: "OK, I've adjusted. That makes the trough deeper — experienced nurses drop further before stabilizing."

**Scenario exploration.** The LLM proactively tests interventions: "I tested three scenarios: (1) hire 50% more — marginally worse due to training burden; (2) reduce workload by 20% via float pool — stabilizes at 12 months; (3) structured mentoring program that halves ramp-up time — best long-term outcome but takes 18 months to show effect."

A comparison chart appears inline. Elena asks: "What about raising salaries?" The LLM adds salary competition to the model, re-runs, and reports: "A 10% salary increase reduces voluntary turnover by roughly 15% in the model, but it doesn't break the workload-burnout loop. You'd still see the same pattern, just slower. Combining salary with the mentoring program is the strongest scenario."

**Data upload.** Elena pastes her monthly turnover data. The LLM calibrates the model against it and reports discrepancies: "The model tracks your data well for months 1–8, but your data shows a sharper drop in month 9 than the model predicts. Did something change around that time — a policy shift, a wave of retirements, a seasonal pattern?"

Elena says it was a retirement incentive program. The LLM adds a one-time outflow event and re-calibrates.

**The "reveal" moment.** Elena asks: "Can you show me what this model actually looks like?" The canvas slides open, fully populated with the stocks, flows, and connections the LLM built. Elena doesn't understand every element, but she can see the feedback loop visually — the arrow from Workload to Burnout to Turnover back to Workload. She notices there's no connection from Management Quality to anything and says: "Our unit managers make a huge difference — units with good managers have half the turnover." The LLM adds the structure and re-runs.

Elena has now crossed from the AI-only zone into the middle of the spectrum. She's inspecting the model, noticing omissions, and directing structural changes. The posture system detects canvas interaction and shifts out of AUTONOMIST into the standard maturity-responsive postures.

**Export.** Elena exports a summary report: problem description, model structure (simplified diagram with plain-language labels), key findings, scenario comparison chart, assumptions list, and data fit. She takes this to her board.

> **Problem area: Elena's data quality.** Real-world data from hospital systems is messy — inconsistent time periods, missing months, aggregated across units. The AI needs to handle data cleaning conversationally: "Your data has gaps in March and July. Should I interpolate, or do you know what happened those months?" This is a significant engineering effort (parsing arbitrary CSVs, detecting time columns, handling missing values) that is easy to underestimate.

---

## 12. Problem areas — consolidated

### 12.1 Critical (must resolve before implementation)

**Canvas rendering strategy.** Custom Canvas vs. Scene Graph vs. hybrid. Affects accessibility, performance, animation, and web portability. Needs a spike: build a test canvas with 100 elements in each approach, measure render time, test accessibility, evaluate port effort.

**LLM posture calibration.** Six postures with qualitative behavioral descriptions are easy to write down and hard to implement reliably. LLMs do not consistently follow complex behavioral instructions, especially nuanced ones like "ask one question at a time" or "do not comment on simulation output." Expect to iterate through 20+ prompt revisions per posture. **Mitigation: build a posture evaluation harness — feed each posture a set of scenario transcripts and grade the responses for adherence. Automate this so prompt revisions can be regression-tested.**

**Equation editing UX.** Inline equation editing on the canvas (double-click a flow, type an expression) needs to handle: autocomplete on element names, syntax highlighting, live validation via `ExprParser`, unit checking as you type. This is a substantial widget. If it's bad, David abandons the tool. If it's over-engineered, it delays the entire project. **Start with a plain text field with post-submit validation. Add autocomplete and live checking in a later pass.**

### 12.2 Significant (must resolve before user testing)

**Partial-model sparklines.** Sparklines on incomplete models are potentially misleading. The dotted/reduced-opacity treatment is a visual mitigation but may not be sufficient. User test with the Maya persona: show her a partial-model sparkline and ask what she concludes. If she treats it as a real prediction, the visual treatment needs to be more aggressive (e.g., no sparkline at all until upstream equations are defined, with a placeholder icon instead).

**Approval fatigue on LLM proposals.** If the LLM proposes 10 elements at once, per-element approval is tedious. Batch-apply with full diff visibility is the compromise, but user testing should verify that users actually read the diff before clicking "Apply All."

**Context window cost.** At 200 calls/session with 8K input tokens each, the cost per session is non-trivial. Investigate: delta-based model context (only send what changed since last call), aggressive conversation summarization, or a tiered model where short clarifying questions use a smaller/cheaper model and only substantive queries use the full model.

**Mode transition jitter.** If the user adds an element, the LLM switches from FORMALIZER to ELICITOR (structure ratio dropped). User adds another element and it switches back. Rapid transitions between postures will feel inconsistent. **Mitigation: hysteresis. A posture change requires the maturity signal to be stable for at least 30 seconds before the LLM adopts the new posture. The old posture persists during the transition window.**

### 12.3 Important (should resolve before v1 release)

**Conversation export.** Maya's thesis use case requires exporting the conversation as a structured document: model decisions, rationale, figures, equations. Raw chat transcript is insufficient. This needs a dedicated export pipeline that extracts model action messages, groups them by subsystem, and interleaves the relevant conversation context. Non-trivial but deferrable.

**Collaboration.** Both designs and this spec are single-user. System dynamics modeling in organizations involves group model building — multiple stakeholders around a shared model. This is architecturally different (shared state, conflict resolution, role-based access) and should be deferred entirely to a web-based v2.

**The conceptualization gap.** All designs, including this one, assume the user already has a rough idea of the system they want to model. The hardest step in modeling — going from "I have a problem" to "here are the structural hypotheses" — remains unsupported. This is arguably the highest-value feature and the hardest to build. It requires the LLM to do something it's mediocre at: generate novel structural hypotheses rather than pattern-match against canonical models. **Flag for research, not near-term implementation.**

**Undo across model and conversation.** Undo reverts the model but does not delete the conversation. This means the conversation contains references to model elements that no longer exist. The UI needs to handle dangling references gracefully — stale element references in the conversation should render as struck-through chips with a "removed" tooltip, not as broken links.

**Large-model performance.** David's 80-stock model will stress the canvas renderer (100+ elements with connections), the model context serialization (5K+ tokens per call), and the layout engine. Performance budgets: canvas render < 16ms (60fps), model context serialization < 100ms, auto-layout < 500ms for incremental add.

### 12.4 Unresolved design questions

**Should the LLM have access to simulation results?** If yes, the LLM can reference specific values ("Burnout reaches 250 at month 18"), which makes its interpretations concrete and testable. But it also means the model context grows by the size of the result set, and the LLM may over-fit its explanations to specific numbers. **Recommendation: include summary statistics (min, max, final value, trend direction) for each stock, not full time series. Let the user ask for specifics.**

**Should the canvas support CLD-only mode permanently?** Some researchers use causal loop diagrams as a final product, not a stepping stone to stock-and-flow. If the tool supports CLD as a first-class output (exportable, publishable), it has a wider audience. But it splits the development effort between two diagram types. **Recommendation: support CLD as the natural starting state, allow export of a CLD-stage snapshot, but don't invest in CLD-specific features (loop labeling, archetype detection) in v1.**

**Should the tool integrate with data sources directly?** David's data comes from hospital databases (SQL, CSV exports). Maya's data comes from published papers (tables she manually enters). Direct database connectivity adds complexity and is a web-port headache. CSV import covers both personas adequately. **Recommendation: CSV import only for v1. Database connectivity is a web-v2 feature.**

**Voice input.** Gemini's design mentioned voice notes for documentation. Voice-to-text for the conversation input would benefit Maya (faster than typing for long explanations of domain knowledge) and David (hands-free model narration during presentations). **Recommendation: defer. The LLM integration already handles text input well, and voice-to-text adds an entire technology stack (speech recognition, microphone permissions, noise handling) for marginal benefit in v1.**

**AI-only mode: user never reveals the model.** The design assumes the "reveal" interaction is the safety net for AI-only mode. But if Elena never clicks "Show model" — and she probably won't unless prompted — she's trusting invisible output. How aggressively should the system prompt revelation? Too often is nagging; too rarely defeats the purpose. **Recommendation: after the first simulation result, the LLM offers once: "I can show you the model diagram if you'd like to see the structure behind this." After data upload, it offers again. Beyond that, the status strip's "Show model" button is the passive reminder. Do not nag.**

**AI-only mode: model quality variance.** The AUTONOMIST posture will produce good models for textbook domains and mediocre models for novel ones. There is no reliable way for the system to know which case it's in. **Recommendation: the AUTONOMIST posture always includes a confidence framing in its first structural proposal: "This is a standard [archetype name] model structure. It's a reasonable starting point for [domain], but may miss factors specific to your situation." This at least signals that the model is a hypothesis, not a ground truth.**

---

## 13. Technology decisions

### 13.1 JavaFX fat client (v1)

| Component | Technology | Rationale |
|---|---|---|
| Window management | JavaFX `SplitPane`, `Stage` | Native split pane with drag handles; pop-out via new `Stage` |
| Canvas | JavaFX `Canvas` (immediate mode) | Performance at scale; portability to HTML5 Canvas |
| Charts (dashboard) | JavaFX `LineChart` or custom Canvas | Built-in charts for simple cases; custom for fan chart (already exists) |
| Conversation panel | JavaFX `ListView` or `VBox` in `ScrollPane` | Custom cell rendering for different message types |
| Command palette | Custom floating `TextField` + `ListView` | No JavaFX equivalent; must be built |
| Markdown rendering | `WebView` (embedded) or custom styled `Text` | `WebView` is heavy but handles markdown well; custom `Text` is lighter |
| LLM integration | HTTP client (`java.net.http`) | Async calls to Claude API via `HttpClient` |
| Model persistence | JSON via `ModelDefinitionSerializer` | Already exists |
| Undo/redo | Custom command stack over `ModelDefinition` snapshots | No framework undo system for domain objects |

### 13.2 Web port (v2, tentative)

| Component | Technology candidates | Notes |
|---|---|---|
| Canvas | HTML5 Canvas + custom renderer, or SVG for small models | Same tradeoff as JavaFX — evaluate both |
| Charts | D3.js, Observable Plot, or Chart.js | D3 for custom fan charts; Chart.js for standard line charts |
| Conversation panel | React/Svelte component | Standard chat UI patterns |
| Command palette | cmdk (React) or custom | Well-established libraries exist |
| Backend | Spring Boot or Quarkus serving existing engine | Forrester engine stays on JVM; exposed via WebSocket + REST |
| LLM integration | Server-side (keeps API keys off client) | Same prompt assembly logic, moved to backend |
| State sync | WebSocket for simulation streaming; REST for model CRUD | Real-time stock updates during simulation |

---

## 14. Implementation phases

### Phase 1: Core canvas editor (COMPLETE)

The foundation: a fully functional no-AI canvas editor for building, simulating, and persisting system dynamics models.

**Delivered:**
- Canvas rendering of `ModelDefinition` elements in stock-and-flow style (Layered Flow Diagram notation).
- Element creation via toolbar and keyboard shortcuts (Stock, Flow, Aux, Constant, Module, Lookup Table).
- Inline equation editing with post-submit validation via `ExprParser` and autocomplete for element names and built-in functions.
- Simulation execution wired to existing `Simulation` class with background-thread execution.
- Simulation results dialog with sortable data table and interactive time series chart (per-series toggle, PNG export).
- Model persistence (save/load via `ModelDefinitionSerializer`) with full view layout preservation.
- Undo/redo (100-level snapshot stack), copy/paste/cut, rubber-band selection.
- Connection creation, deletion, hover/selection highlighting, and drag-to-reroute.
- Lookup table editor with inline chart preview, interpolation mode selection.
- Model validation panel (undefined equations, disconnected flows, missing units, algebraic loops, unused elements).
- Feedback loop highlighting.
- Diagram export (PNG, JPEG, SVG).
- Pan, zoom, resize, context-sensitive cursors, status bar.

No LLM integration. No maturity system. This is the "better Vensim" baseline for David — a capable standalone modeling tool.

### Phase 2: No-AI workflow completion

Complete the no-AI experience as a permanent first-class workflow, and add the analysis capabilities that make it useful for serious work.

**Activity log panel.** Replace the conversation panel placeholder with a structured activity log: timestamped entries for model changes, simulation runs, validation warnings. Not interactive (no AI), but provides a record of the session. See section 16.2.

**Integrated behavior dashboard.** Move simulation results from a separate dialog into a collapsible dashboard panel in the main window. The dashboard persists across simulation runs and serves as the home for all analysis output — time series charts, sweep results, Monte Carlo fan charts, optimization progress.

**Analysis integration.** Wire the existing engine analysis tools into the GUI:
- Parameter sweep UI: configure parameter, range, and steps; results displayed in dashboard.
- Multi-parameter sweep UI: configure parameter grid; results in dashboard with CSV export.
- Monte Carlo UI: configure distributions, iteration count, sampling method (random/LHS); fan chart and percentile envelopes in dashboard.
- Optimization UI: configure objective function, parameter bounds, algorithm; progress and results in dashboard.

These build on existing Forrester infrastructure (`ParameterSweep`, `MultiParameterSweep`, `MonteCarlo`, `MonteCarloResult`, `FanChart`, `Optimizer`). The engine work is done — this phase is GUI integration.

**Baseline question:** Can David build a model, run sensitivity analysis, calibrate against data, and export results — all from the GUI — without needing Vensim?

### Phase 3: UI scaffolding for AI readiness

Build the structural UI elements that later phases depend on:
- Three-panel window layout (activity log / conversation left, canvas center, dashboard right) with collapsible panels.
- Command palette (Ctrl+K searchable, the core commands from Appendix B).
- Session start screen with "Build a model" / "Build without AI" / "Open existing model" options.

No LLM integration yet. These are the UI foundations that Phases 4–5 build on top of.

### Phase 4: Maturity system + visual adaptation

Implement the maturity signal computations. Wire them to canvas rendering:
- Amber accents on elements missing equations.
- Red connection lines on unit mismatches.
- Sparklines on simulated stocks.
- Stale-result indicators.
- Maturity strip in conversation panel.
- Status bar warnings.

No LLM yet. Test whether the visual maturity indicators alone change how Maya builds models (does she notice and fix equation gaps without being told?).

### Phase 5: LLM integration — INTERPRETER and FORMALIZER

Add LLM integration with two postures — the safest ones:
- INTERPRETER: Explain simulation output. Low risk of bad advice; the model is already built.
- FORMALIZER: Help translate qualitative relationships to equations. Moderate complexity.

Implement conversation panel with message types, model action messages, and diff cards for LLM proposals.

This is the first version where the LLM is useful. Test with both personas.

### Phase 6: Remaining postures + pace detection

Add ELICITOR, ANTICIPATOR, CHALLENGER, and OBSERVER postures. Implement pace detection and posture transition hysteresis.

Test posture transitions with scripted scenarios. Build the posture evaluation harness for regression testing prompt changes.

### Phase 7: Polish and export

Conversation export. CLD-style rendering for unclassified elements. Cross-linking (click canvas element → scroll to conversation; click conversation element name → select on canvas). CSV data import and overlay. Subsystem grouping.

### Phase 8: AI-only mode

Implement the session-start choice screen ("Build a model" / "Explore a problem"). Implement AUTONOMIST posture with autonomous model construction (no per-element approval). Implement inline chart rendering in conversation. Implement the "reveal" interaction (canvas slide-open from AI-only mode). Implement assumption tracking and display.

This is the Elena-persona entry point. Test with non-modelers: can they describe a problem and get useful insight in under 20 minutes? Do they understand the limitations of the output? Do they ever click "Show model"?

---

## 15. AI-only mode: conversational system dynamics

### 15.1 The argument

The standard SD argument is that the modeler learns by building — the act of deciding what's a stock, what's a flow, where the feedback loops are is where insight happens. Hand the construction to an AI and you lose the learning.

But this argument assumes the user wants to *become a modeler*. Most people don't. A hospital administrator trying to understand why nurse turnover keeps getting worse doesn't want to learn system dynamics — they want to understand their system. A city planner evaluating a housing policy doesn't need to know what a balancing loop is — they need to know whether the policy will backfire.

There's a large population of people who would benefit from SD thinking but will never build a model. Right now they get nothing. The AI-only mode gives them something. The question is whether that something is useful or dangerous.

### 15.2 The spectrum

The real question isn't "AI-only or full modeler interface" — it's where on the spectrum a given user should operate:

```
AI-only ◄──────────────────────────────────────────► Full modeler
  │                                                        │
  │  "Tell me about    "Show me the    "Let me build      │
  │   my system"        model and       the model          │
  │                     explain it"     myself"            │
  │                                                        │
  │  Hospital admin     MBA student     SD researcher      │
  │  City planner       Policy analyst  PhD modeler        │
  │  Startup founder    Consultant      Professor          │
  │                                                        │
  │      Elena              Maya            David          │
```

Design 4's canvas-based interface covers the right side. The AI-only mode covers the left side. The critical design insight is that the middle zone — "show me the model and explain it" — is where users naturally migrate. Elena starts on the left and moves toward the center when she asks "show me what this model looks like." Maya starts on the right but could start on the left for her first model, then move right as her skills develop.

### 15.3 Layout in AI-only mode

The window simplifies: the conversation panel expands to fill the full width. The canvas and behavior dashboard are hidden but present — the model exists in memory, just not displayed.

```
┌────────────────────────────────────────────────────────────┐
│ ┌──────────────────────────────────────────────────────┐   │
│ │                                                      │   │
│ │                  Conversation                        │   │
│ │                                                      │   │
│ │  (charts, summaries, and scenario comparisons        │   │
│ │   rendered inline as cards within the conversation)  │   │
│ │                                                      │   │
│ │                                                      │   │
│ │                                                      │   │
│ └──────────────────────────────────────────────────────┘   │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [Show model]  Assumptions: 12 · Parameters: 8 ·     │   │
│ │ Last run: 2 min ago                                  │   │
│ └──────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
```

The bottom strip replaces the maturity strip with a simpler status: assumption count, parameter count, last run timestamp, and a "Show model" button. This is the "reveal" entry point.

Charts and scenario comparisons appear inline in the conversation as cards — the same chart components used in the behavior dashboard, just embedded in the message flow rather than in a separate panel.

### 15.4 The "reveal" interaction

At any point, the user can click "Show model" or type "show me the model." The canvas slides open from the right, fully populated with whatever the LLM has built. This is the bridge from the left side of the spectrum to the middle.

This is mandatory. The model must always be inspectable. The AI-only mode is a *view* on a real model, not a separate system. The user can always pull back the curtain. This is what distinguishes it from a chatbot — there's actual math behind the prose, and the user can verify it.

This means the AI must build real models via `ModelDefinition`, not just generate prose. Every claim the AI makes ("burnout peaks at month 18") must be backed by an actual simulation run that the user could, in principle, inspect.

Once the canvas is open, the posture system re-evaluates. If the user starts editing the model directly, they've crossed from Elena's workflow into Maya or David's territory, and the standard maturity-responsive postures take over. If they just look and close the canvas, AUTONOMIST resumes.

### 15.5 What the AI does behind the scenes

Each conversational turn may involve multiple engine operations, all executed silently:

1. **Conceptualization.** Identify stocks, flows, and feedback loops from the natural language description. Call `add_stock`, `add_flow`, `connect`.
2. **Formalization.** Write equations, assign units, set initial values. Call `set_equation`, `add_constant`.
3. **Simulation.** Run the model. Call `run_simulation`.
4. **Interpretation.** Read `RunResult`, identify dominant dynamics, explain in domain language.
5. **Scenario analysis.** Modify parameters, re-run, compare. Multiple `run_simulation` calls with different parameter sets.
6. **Calibration.** When the user provides data points, adjust parameters to fit. When the user uploads data, run comparison and report discrepancies.

In the canvas-based interface, each of these would be a diff card requiring approval. In AUTONOMIST mode, they execute immediately. The conversation shows the *result* of these operations (charts, explanations, assumption lists) but not the operations themselves — unless the user asks "what did you change?"

### 15.6 Where it works

**Qualitative insight.** "Hiring more makes it worse because of the training burden" is a genuinely useful insight. The user doesn't need to see the model to understand the feedback structure. The AI explains the loop in words, and the words are the insight.

**Scenario comparison.** "Option A is better than Option B in the long run" is actionable even without understanding the model mechanics. Decision-makers routinely act on analysis they didn't produce — this isn't fundamentally different from hiring a consultant.

**Rapid framing.** A 5-minute conversation that surfaces the right feedback structure is enormously valuable even if the model behind it is crude. The SD community calls this "back of the envelope modeling" and practitioners do it informally all the time. The AI-only mode makes it conversational.

**Education.** A student learning about feedback dynamics could explore systems conversationally before encountering stock-and-flow notation. "What happens if we double the fishing rate?" is a better entry point than "create a stock called Fish Population."

**Triage.** Before investing in a full modeling effort, a quick AI-built model can reveal whether the problem has interesting dynamics at all. Many real-world problems are simple (monotonic growth, simple decay) and don't benefit from a model. The AI-only mode identifies the cases that *do* need deeper work.

### 15.7 Where it breaks

**The model is invisible, so structural assumptions become invisible defaults.** This is the central problem. Every SD practitioner knows that the most important thing about a model is what it *leaves out*. An invisible model can't be interrogated for its omissions.

The AI can list its assumptions ("This model assumes hiring rate is constant, there's no salary competition, and burnout is purely workload-driven"). But a list of assumptions is not the same as a visible structure. Assumptions presented as text are easy to read and nod past. Assumptions visible as structural choices — "there's no arrow from Salary to Turnover" — are harder to ignore because the *absence* is spatially evident.

> **This is the strongest argument against AI-only mode: it turns structural assumptions from visible absences into invisible defaults.** The "reveal" interaction is the primary mitigation, but only works if the user actually uses it.

**The AI will produce canonical models.** LLMs pattern-match against training data. When someone describes nurse turnover, the AI will produce a model that looks like the standard "burnout and turnover" archetype from the SD literature. Correct if the situation matches the archetype. Actively misleading if the situation is novel — and the user has no way to tell the difference because they can't see the model structure.

**Calibration is shallow.** When the user says "about 6 months," the AI sets ramp-up time to 6 months. But real calibration is harder: What distribution? Is it 6 months average with high variance? Is it different for BSN graduates versus experienced hires? The AI-only interface can ask follow-up questions, but can't match the depth of a modeler systematically working through each parameter.

**Confidence is uncalibrated.** The AI will present scenario comparisons with apparent precision ("experienced nurse count drops by 30%"). The user has no framework for assessing whether this precision is meaningful or an artifact of arbitrary parameter choices. A modeler knows that uncalibrated parameters produce only qualitative conclusions. The AI-only user has no way to make this distinction. Hedging in natural language is weak — a fan chart communicates uncertainty better than any number of verbal caveats.

**Trust dynamics are inverted.** In the canvas-based interface, trust is built progressively: the user creates the structure, verifies the equations, runs the simulation, and sees the output emerge from their choices. In AI-only mode, trust is asked for upfront: "I built a model and here's what it says." This is the consultant model, and it works for consultants because they have reputations, credentials, and professional liability. The AI has none of these.

### 15.8 Implementation

**Not a separate application.** AI-only mode is Design 4 with the canvas hidden and the LLM in AUTONOMIST posture. Same Model Façade, same maturity system, same simulation engine, same `ModelDefinition`. One toggle at session start: "Build a model" (canvas visible, standard postures) or "Help me understand a problem" (canvas hidden, AUTONOMIST posture).

**Session start UX.** The application opens with a simple choice. See section 16.4 for the full session start screen including no-AI mode. The two AI-enabled options are shown here:

"Build a model" enters the standard Design 4 interface. "Explore a problem" enters AI-only mode. "Build without AI" enters no-AI mode (section 16). The choice is not permanent — the user can reveal the canvas at any time and switch to full mode, or enable/disable AI mid-session.

**Inline chart rendering.** In AI-only mode, charts that would appear in the behavior dashboard are instead rendered as inline cards in the conversation. These use the same chart components (time series, fan chart, scenario comparison) but sized for the conversation column width (~600px). Cards are interactive: hovering shows values, clicking expands to full size.

**Assumption tracking.** The AUTONOMIST posture must maintain an explicit assumption list — every default value, every structural choice, every omission. This list is shown in the bottom status strip (count) and expandable into a full panel. When the user challenges an assumption, the AI modifies the model and re-runs.

> **Problem area: Assumption completeness.** Every model embodies dozens of implicit assumptions (linearity, constant rates, closed population, etc.) that a skilled modeler knows but doesn't enumerate. The AI's assumption list will be incomplete because it can't enumerate what it doesn't realize it's assuming. The list captures *explicit* choices ("I used a 3% discount rate") but not *implicit* ones ("I assumed the population is homogeneous"). **There is no full solution. Partial mitigation: the LLM includes a standard disclaimer listing common implicit assumptions for the model type (e.g., "This is a cohort model, which assumes the population is homogeneous — it doesn't capture individual variation").**

### 15.9 Risk assessment

| Risk | Severity | Likelihood | Mitigation |
|---|---|---|---|
| User takes AI-built model as truth, makes bad decision | High | Medium | Clear labeling, uncertainty bounds, assumption listing, "reveal" option |
| AI produces canonical model that misses the real dynamics | High | High for novel domains | Explicit "this is a standard archetype" warning; prompt user for what's different |
| User can't distinguish model artifacts from genuine insight | High | High for non-modelers | Fan charts not point estimates; plain-language uncertainty; offer to explain what drives each result |
| AI presents false precision | Medium | High | Suppress specific numbers in early exploration; report ranges; show sensitivity |
| User never inspects the model, treats conversation as sufficient | Medium | High | Periodic "show the model" prompts; summary of key assumptions at each stage |
| Liability: user makes a clinical or policy decision based on an AI-built model | High | Low (initially) | Terms of use; clear "not a validated model" labeling; export includes full assumptions list |

The highest-severity, highest-likelihood risk is: **the AI builds a canonical model that seems to explain the user's situation, but the real dynamics are different, and the user has no way to notice.** No amount of caveating fully addresses this. The structural mitigation — making the model always inspectable — helps, but only if the user actually inspects it. This is the same risk that exists with any expert system, consultant, or analytical tool. The AI-only mode doesn't create this risk; it makes it easier to fall into.

### 15.10 What AI-only mode is not

It is **not** a substitute for proper modeling and should not be presented as one. The interface must make clear that it produces quick structural hypotheses, not validated models. Every export, every summary, every session should carry a label: "This is an exploratory model built from conversation. It shows one possible explanation for what you're observing. A thorough analysis would require deeper data and expert review."

The purpose of AI-only mode is to make SD thinking accessible to people who would otherwise never encounter it. If Elena walks out of a 20-minute session understanding that her hiring strategy triggers a reinforcing burnout loop, that is enormously valuable — even if the model behind it is crude, even if the numbers are wrong, even if a skilled modeler would have built it differently. The insight is in the structure, not the precision.

---

## 16. No-AI mode: full modeling without LLM costs

### 16.1 The case for no-AI mode

The integrated LLM is Design 4's differentiating feature, but it has a hard cost: every LLM call consumes API tokens. At current pricing, a heavy session generates $2–$8 in API costs (see 10.3). For a researcher with grant funding or a corporation, this is negligible. For a graduate student paying out of pocket, an underfunded university program, or a user in a region without easy access to payment infrastructure, it's a barrier to adoption.

More fundamentally: the core value of a system dynamics tool is the ability to build, simulate, and analyze models. The LLM enhances that experience — it doesn't create it. David (section 2.2) builds models faster than the LLM can ask questions. He uses the LLM during analysis and presentation, not construction. A tool that requires an LLM connection to function at all excludes the users who need nothing more than a good canvas, a simulation engine, and clear output.

No-AI mode is Design 4 without the LLM Integration Layer. The maturity system, the canvas, the behavior dashboard, the command palette, simulation, parameter sweeps, Monte Carlo — all of this works without a single API call. What's lost is the conversation panel's AI capabilities and the posture-driven guidance. What's preserved is everything else.

### 16.2 What changes

**Conversation panel becomes a log panel.** Without an LLM, the conversation panel serves no interactive purpose. It becomes a structured activity log: model changes, simulation runs, validation warnings, and maturity signal updates are displayed as timestamped entries. The user cannot type messages to an AI, but they can use the command palette (section 9) for all model operations — the command palette never required the LLM.

**Maturity signals still compute, but don't drive posture selection.** The maturity system (section 4) is entirely local computation — it reads `ModelDefinition` and produces signals. In no-AI mode, these signals still drive visual adaptation on the canvas (amber accents for missing equations, red lines for unit mismatches, sparklines on simulated stocks, stale-result indicators). The posture selection logic (section 10.2) is simply not invoked.

**The session start screen adds a third workflow option.** See 16.4.

**No features are disabled or degraded.** Every canvas operation, every simulation mode, every export function, every keyboard shortcut works identically. No-AI mode is not a "lite" version — it is the full modeling tool without conversation.

### 16.3 What's lost

| Capability | Available in no-AI mode? | Workaround |
|---|---|---|
| Canvas building, editing, drag-and-drop | Yes | — |
| Command palette | Yes | — |
| Simulation, parameter sweeps, Monte Carlo | Yes | — |
| Maturity visual indicators | Yes | — |
| Unit mismatch warnings | Yes | — |
| Model save/load/export | Yes | — |
| Feedback loop detection and highlighting | Yes | — |
| Inline equation editing | Yes | — |
| Undo/redo | Yes | — |
| LLM-guided elicitation (ELICITOR posture) | No | User must decide model structure without guidance |
| Equation formalization help (FORMALIZER) | No | User writes equations manually |
| Pre-simulation prediction prompts (ANTICIPATOR) | No | User must self-discipline about expectations |
| Output interpretation (INTERPRETER) | No | User interprets charts and feedback loops directly |
| Data-driven challenge (CHALLENGER) | No | User compares model output to data manually |
| AI-only mode (AUTONOMIST) | No | Not available — requires LLM by definition |
| Natural language model building | No | Use canvas or command palette |
| Conversational documentation | No | User maintains notes externally |

The losses are real but bounded. Every lost capability is an *enhancement* to a workflow that remains fully functional without it. A user in no-AI mode can still build, simulate, and analyze any model that a user in AI mode can — they just don't get conversational assistance while doing it.

### 16.4 Session start UX

The session start screen (section 15.8) adds a no-AI option:

```
┌─────────────────────────────────────────────┐
│                                             │
│  How would you like to work?                │
│                                             │
│  ┌───────────────────────────────────────┐  │
│  │  Build a model                        │  │
│  │  Construct and test a system dynamics │  │
│  │  model with AI assistance.            │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  ┌───────────────────────────────────────┐  │
│  │  Explore a problem                    │  │
│  │  Describe a problem and let the AI    │  │
│  │  build a model for you.               │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  ┌───────────────────────────────────────┐  │
│  │  Build without AI                     │  │
│  │  Full modeling tools, no AI costs.    │  │
│  │  Canvas, simulation, and analysis     │  │
│  │  without an LLM connection.           │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  ┌───────────────────────────────────────┐  │
│  │  Open existing model                  │  │
│  └───────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘
```

"Build without AI" is not hidden or de-emphasized. It is a first-class workflow. The label avoids negative framing ("offline mode," "free tier," "limited") — it states what the user gets, not what they're missing.

If an API key is not configured, "Build a model" and "Explore a problem" are grayed out with a note: "Requires API key. Configure in Settings." "Build without AI" and "Open existing model" remain available. The application never refuses to launch because an API key is missing.

### 16.5 Enabling AI mid-session

A user who starts in no-AI mode can enable AI at any time by configuring an API key in Settings. The conversation panel activates, the posture system evaluates the current model state, and the LLM picks up wherever the model is — FORMALIZER if equations are missing, INTERPRETER if a simulation has been run, etc. No model state is lost. The transition is seamless.

The reverse is also true: a user can disable AI mid-session (or the API key can expire / the service can become unreachable). The tool continues operating with the conversation panel reverting to log mode. In-flight LLM requests are cancelled gracefully, and the user sees a status message: "AI disconnected. All modeling tools remain available."

### 16.6 Layout in no-AI mode

The window layout mirrors the standard Design 4 layout (section 5) with the conversation panel replaced by a narrower activity log, or optionally hidden entirely to give the canvas more space:

```
┌─────────────────────────────────────────────────────┐
│  Toolbar / Command Palette                          │
├────────┬──────────────────────────┬─────────────────┤
│Activity│                          │    Behavior     │
│  Log   │       Canvas             │   Dashboard     │
│(narrow)│                          │                 │
│        │                          │                 │
│        │                          │                 │
├────────┴──────────────────────────┴─────────────────┤
│ Status bar: maturity indicators │ zoom │ elements   │
└─────────────────────────────────────────────────────┘
```

The activity log panel can be collapsed entirely via a toggle, giving the canvas the full width between the toolbar and the dashboard. The behavior dashboard remains — it displays simulation results, which have nothing to do with the LLM.

### 16.7 Persona mapping

**David** may prefer no-AI mode for construction, then enable AI for analysis and presentation. His workflow: build rapidly without conversational overhead, run simulations, then activate the LLM to generate plain-language summaries for stakeholders.

**Maya** benefits most from AI guidance, but no-AI mode is still functional for her — she can build models using the canvas and command palette, guided by the maturity visual indicators (amber accents on missing equations, etc.). The indicators serve as a silent checklist. She loses the Socratic prompting that helps her think critically, but she can still complete her thesis model.

**Elena** cannot use no-AI mode. Her workflow is entirely conversational and requires AUTONOMIST posture. This is expected — no-AI mode targets users who can build models, not users who need the AI to build for them.

### 16.8 Implementation notes

No-AI mode requires no additional engine work — it is the *absence* of the LLM Integration Layer, not a separate system. Implementation consists of:

1. **Conditional initialization.** The LLM Integration Layer is instantiated only when an API key is present and the user selects an AI-enabled workflow. All other layers (Model Facade, Maturity System, Canvas, Dashboard) initialize unconditionally.
2. **Activity log component.** A simple timestamped list view replacing the conversation panel. Entries are generated by existing change events from the Model Facade. Estimated effort: small — it's a simplified version of the conversation panel without message input or LLM response rendering.
3. **Session start screen update.** Add the "Build without AI" option and API key detection logic.
4. **Graceful degradation.** If the LLM service becomes unreachable mid-session, the UI transitions to no-AI mode automatically with a notification. No data is lost.

The activity log and integrated dashboard are implemented in **Phase 2** (section 14), building on the canvas editor delivered in Phase 1. The session start screen and command palette follow in Phase 3. No-AI mode is not a temporary limitation — it is a permanent first-class workflow that must stand on its own.

---

## Appendix A: Maturity-to-posture mapping (detailed)

```
                    ┌──────────────────────────────────────────────┐
                    │         AI-only mode AND                     │
                    │         canvas not opened?                   │
                    │                                              │
                    │  YES ──────────────────────► AUTONOMIST       │
                    │                                              │
                    │  NO                                          │
                    │   │                                          │
                    │   ▼                                          │
                    │  Pace: Rapid?                                │
                    │                                              │
                    │  YES ──────────────────────► OBSERVER         │
                    │                                              │
                    │  NO                                          │
                    │   │                                          │
                    │   ▼                                          │
                    │  Structure < 50%                             │
                    │  AND Equations < 30%?                        │
                    │                                              │
                    │  YES ──────────────────────► ELICITOR         │
                    │                                              │
                    │  NO                                          │
                    │   │                                          │
                    │   ▼                                          │
                    │  Equations < 80%?                            │
                    │                                              │
                    │  YES ──────────────────────► FORMALIZER       │
                    │                                              │
                    │  NO                                          │
                    │   │                                          │
                    │   ▼                                          │
                    │  Not simulated OR                            │
                    │  results stale?                              │
                    │                                              │
                    │  YES ──────────────────────► ANTICIPATOR      │
                    │                                              │
                    │  NO                                          │
                    │   │                                          │
                    │   ▼                                          │
                    │  Data loaded?                                │
                    │                                              │
                    │  NO ───────────────────────► INTERPRETER      │
                    │                                              │
                    │  YES ──────────────────────► CHALLENGER       │
                    │                                              │
                    └──────────────────────────────────────────────┘
```

Hysteresis: posture changes require the triggering condition to be stable for 30 seconds before taking effect. The previous posture persists during the transition window. Exception: the AUTONOMIST → standard posture transition on canvas open is immediate (user explicitly chose to inspect the model).

## Appendix B: Command palette — complete v1 command list

| Command | Shorthand | Effect |
|---|---|---|
| `add stock <name> [initial] [unit]` | `as` | Create stock |
| `add flow <name> [from] [to]` | `af` | Create flow |
| `add aux <name> [unit]` | `aa` | Create auxiliary |
| `add constant <name> <value> [unit]` | `ac` | Create constant |
| `connect <source> <target>` | `cn` | Add connector |
| `disconnect <source> <target>` | `dc` | Remove connector |
| `remove <element>` | `rm` | Delete element |
| `group <el1> <el2>... as <name>` | `gr` | Visual grouping |
| `ungroup <name>` | `ug` | Dissolve group |
| `find <name>` | `f` | Navigate to element |
| `run [duration] [timestep]` | `r` | Execute simulation |
| `sweep <param> <lo> <hi> [steps]` | `sw` | Parameter sweep |
| `montecarlo [iterations]` | `mc` | Monte Carlo |
| `export csv` | `ec` | Export results |
| `export model` | `em` | Export model JSON |
| `undo` | `u` | Undo last change |
| `redo` | — | Redo |
