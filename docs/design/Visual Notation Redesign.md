# Visual Notation Redesign

Design reference for improved system dynamics diagram renderings in the shrewd-canvas module.

## Context

Standard system dynamics visual notation (stock-and-flow diagrams, causal loop diagrams) has well-documented cognitive problems. The Design 4 canvas is an opportunity to address these rather than faithfully reproducing Vensim/Stella conventions. This document catalogs the known issues from research and proposes design principles for improved renderings.

## Documented cognitive problems

### 1. Stock-flow failure (Sterman, Booth Sweeney 2000)

The most extensively studied problem. When shown graphs of inflows and outflows, fewer than half of highly educated MIT graduate students could correctly sketch the resulting stock trajectory. People apply a "correlation heuristic" — they assume the stock's behavior should look like the net flow pattern, confusing the rate of change with the level.

This is not a notation problem per se, but it has notation implications: if the diagram doesn't make the accumulation relationship visually obvious, even smart people will misread it.

Relevant findings:

- People confuse stocks and flows even in trivially simple systems (one stock, one inflow, one outflow)
- The error persists across education levels and domains
- Visual interventions showing multiple representations of the same data can improve understanding for 6-7 weeks

### 2. Polarity notation (+/- vs s/o)

The "+" and "-" labels on causal links mean "same direction change" and "opposite direction change." This requires an extra cognitive step: you have to think "if A increases, does B increase (+) or decrease (-)?" and separately "if A decreases, does B decrease (+) or increase (-)?" The "+/-" notation is compact but not intuitive — many people read "+" as "good" and "-" as "bad," or as "increase" and "decrease."

The alternative "s" (same) and "o" (opposite) notation was proposed to be more readable but has its own problems — Richardson (1986) showed that "s/o" breaks down for flow-to-stock links. The SD community has debated this for 40 years without resolution, which itself suggests neither notation is good enough.

### 3. Valve symbol confusion

Flows are shown with a valve (bowtie) symbol inherited from Shrewd's original hydraulic analogy. Problems:

- Valves are limiters, not activators — most people don't associate "valve" with "process that drives change"
- Flows are shown as unidirectional even when they're bidirectional
- The valve symbol doesn't communicate what the flow rate depends on
- For non-engineers, the hydraulic metaphor means nothing

### 4. Causal loop diagram underspecification

CLDs leave too much to the imagination. Attempting to formalize a CLD into a model reveals "unstated parameters, aggregation questions, and other leaps of logic." Links without polarity labels are common. Variables are often named ambiguously (concept areas rather than measurable quantities). Complex CLDs become "spaghetti" with crossing lines obscuring the actual structure.

### 5. The CLD-to-SFD gap

The transition from qualitative CLD to quantitative stock-and-flow diagram is a known stumbling block. The two notations look completely different — a variable in a CLD becomes a stock, a flow, or an auxiliary in an SFD, and the visual appearance changes entirely. Students have to re-learn how to read the diagram.

CLD+ (2025/2026 research) addresses this by adding type labels to CLD nodes and distinguishing material flows from information links, making CLDs structurally equivalent to SFDs. This is directly relevant to Design 4's "CLD-style evolving to SFD-style" canvas rendering.

### 6. Feedback loop identification

Determining whether a loop is reinforcing or balancing requires counting negative links (odd = balancing, even = reinforcing). This is error-prone in complex diagrams. People miscount, mislabel, or miss loops entirely. The standard R/B loop labels help but are often missing or ambiguous when loops share links.

## Design principles for improved renderings

### Principle 1: Make accumulation visually obvious

The stock-flow failure research says people confuse levels with rates. The visual design should make the distinction unmistakable:

- **Stocks are containers.** Render them as vessels with a visible "fill level" that changes during simulation. Not a rectangle with a number — a shape that communicates "this holds stuff." The fill level provides an immediate visual analog to the current value.
- **Flows are motion.** Animate flow connections during simulation — particles moving along the pipe, speed proportional to flow rate. Static diagrams use arrow thickness proportional to flow magnitude. The visual should scream "things are moving through here" not "here is a labeled line."
- **The relationship is spatial.** Flows physically connect to stocks. The pipe enters the container. This isn't just a line with an arrowhead — it's plumbing. The visual metaphor should make it impossible to confuse "the thing that accumulates" with "the thing that moves."

### Principle 2: Replace +/- with natural language or color

The +/- polarity notation requires a translation step. Alternatives:

- **Natural language micro-annotations.** Instead of "+", write "more -> more" or a domain phrase: "higher workload -> more burnout." This is what Design 4's CLD mode already proposes. It's wordier but instantly understandable.
- **Color coding.** Same-direction links in one color (e.g., blue), opposite-direction links in another (e.g., orange/red). Color is pre-attentive — the brain processes it before conscious reading. Combined with a small "same" / "opposite" label, this eliminates the +/- ambiguity.
- **Arrow style.** Same-direction links use a standard arrowhead. Opposite-direction links use a distinct arrowhead (e.g., a filled circle/"blob" or a perpendicular bar). This provides a shape-based signal that works even in grayscale.

**Scaling tradeoff:** Natural language annotations don't scale — on a 50-link diagram they create visual clutter. The color + arrow style approach scales better but is less self-explanatory. Recommendation: use natural language in CLD mode (few links, exploration phase) and color + arrow style in SFD mode (many links, formalized model). The transition from one to the other is part of the maturity-responsive visual evolution.

### Principle 3: Retire the valve symbol

Replace the bowtie/valve with something that communicates "process" or "rate of change":

- **A labeled arrow with a rate indicator.** The flow is a thick arrow from source to sink stock. The rate is displayed as a number on the arrow (e.g., "12/yr"). No separate symbol needed.
- **A process node.** For complex flows with multiple inputs, render the flow as a small node (diamond, rounded rectangle) on the connection, with information connectors entering it. This makes clear what the flow rate depends on.
- **On hover/select, expand.** A flow that looks like a simple labeled arrow at rest expands to show its equation, inputs, and units when selected. This keeps the default view clean while making detail accessible.

### Principle 4: Smooth the CLD-to-SFD transition

Following the CLD+ research, Design 4 should make the transition gradual rather than abrupt:

- **Type badges on CLD nodes.** When a variable is classified as a stock, a small "S" badge appears on the CLD node before the shape changes. When classified as a flow, an "F" badge. This foreshadows the SFD shape change.
- **Gradual shape morphing.** When a CLD variable is reclassified as a stock, animate the shape change: the rounded rectangle grows the heavier border, develops the "container" appearance over 300ms. The user sees the transformation, reinforcing the connection between the CLD variable and the SFD stock.
- **Material vs. information links.** Following CLD+, distinguish between material flows (things moving between stocks — thick, solid) and information links (variables influencing flow rates — thin, dashed). This distinction exists in SFDs but not in standard CLDs. Adding it to CLD mode makes the transition seamless.

### Principle 5: Make feedback loops visible on demand

Rather than requiring the user to mentally trace and count links:

- **Loop highlighting.** Hover over any link and the complete loop(s) it belongs to highlight. Other elements dim.
- **Automatic loop labeling.** The system identifies all feedback loops (using the existing `DependencyGraph` infrastructure) and labels them with R/B and a descriptive name derived from the stocks in the loop. These labels are toggle-visible, not always-on.
- **Loop polarity explanation.** When a loop is highlighted, a tooltip explains: "Reinforcing loop: more Workload -> more Burnout -> more Attrition -> more Workload per person -> more Workload." Natural language, tracing the causal chain, with the polarity conclusion stated explicitly.

### Principle 6: Show uncertainty and incompleteness honestly

- **Undefined elements are visually distinct.** Not just dimmed — structurally different. An element without an equation is drawn with a dashed border and a "?" placeholder. It looks *unfinished*, not just *inactive*.
- **Simulation output is qualified.** Sparklines on stocks with incomplete upstream equations use a distinct visual treatment (dotted line, watermark) that communicates "this is based on incomplete structure."
- **Uncertainty has a shape.** When Monte Carlo results exist, stock sparklines show a shaded range (mini fan chart), not just a single line. The visual width of the range communicates how uncertain the value is.

---

## Concrete representation: Layered Flow Diagrams

The principles above are abstract. This section specifies the concrete visual language — what each element looks like, how connections are drawn, how the representation adapts to model maturity and diagram scale.

### Why current SFDs cause cognitive overload

Standard SFDs collapse five independent concerns into a single flat diagram:

1. **Classification** — which elements are stocks, flows, auxiliaries, constants
2. **Structure** — what connects to what
3. **Specification** — equations, units, parameter values
4. **Behavior** — what the model does when simulated
5. **Influence direction** — polarity of causal relationships

The reader must process all five simultaneously. The visual hierarchy doesn't help because everything is rendered at roughly the same size and weight — a stock rectangle, an auxiliary circle, a valve bowtie, and an equation label all compete for attention equally.

The Layered Flow Diagram separates these concerns into visual layers that can be independently emphasized or suppressed, with defaults that match the user's likely task.

### Visual hierarchy

The representation establishes a strict visual weight ordering that matches conceptual importance:

```
Heaviest   ██  Stocks         (thick border, largest area, fill bar)
           ██  Material flows  (wide directional arrows, animated)
           ░░  Auxiliaries     (medium shapes, thin border)
           ··  Constants       (small badges, dashed border)
Lightest   --  Info links      (thin dashed curves, low opacity)
```

The eye is drawn to stocks first, then follows the material flows between them. Auxiliaries and constants are visible but subordinate. Information links are the lightest element — present but not distracting.

This hierarchy makes the "skeleton" of any SD model — stocks connected by flows — immediately visible at a glance, even in a 60-element diagram. The supporting structure (auxiliaries, constants, information links) fills in when you look more closely.

### Element specifications

#### Stocks: The Reservoir

```
    ┌══════════════════════════┐
    ║                          ║
    ║       Staff Level        ║   <- name (bold, centered)
    ║         350              ║   <- current value (lighter weight)
    ║ ┃                        ║
    ║ ┃█████████               ║   <- fill bar (left edge, proportional
    ║ ┃█████████               ║      to value within observed range)
    ║ ┃█████████               ║
    ╚══════════════════════════╝
                       people ─┘   <- unit badge (bottom-right corner)
```

- **Border:** 3px rounded rectangle. Heaviest border of any element.
- **Size:** Largest element on the canvas (~140x90px at default zoom). Stocks dominate visually because they dominate conceptually.
- **Fill bar:** A vertical gradient bar on the left interior edge, rising from bottom. Height encodes current value as a proportion of the stock's observed min-max range during the simulation. Color is a calm blue-to-teal gradient. The fill bar is pre-attentive — the eye reads "70% full" without processing numbers.
- **Behavior watermark:** When simulation data exists, a sparkline curve is rendered as a 12% opacity background watermark across the full interior. Shows trajectory shape (growth, oscillation, plateau) without competing with the name/value text.
- **Value:** Displayed below the name. Updates during simulation playback.
- **Unit badge:** Small, gray, bottom-right corner. Always visible but unobtrusive.
- **Incomplete state:** When no flows are connected, the fill bar is absent and the border uses an amber left-edge accent (per Design 4 maturity encoding). The stock still has its heavy border — classification is clear even before specification is complete.

Why this works for stock-flow confusion: the fill bar makes accumulation *visible as a spatial quantity*. When a simulation runs, the user watches containers filling and emptying. This is fundamentally different from watching a number change or reading a graph — the container metaphor engages spatial cognition, which humans process effortlessly. You cannot look at a filling container and think "that's a rate."

#### Flows: The Stream

```
          ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
    ◈ ════╋══════════════════════════════════════════╋════ ◈
  source   ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛  sink
  stock           Hiring Rate                          stock
                 ╭─────────╮
                 │ 12/yr   │  <- rate badge (pill-shaped)
                 ╰─────────╯
```

- **Arrow:** A wide (4-6px) directional line connecting source stock to sink stock. The arrow physically enters the stock's border — the "pipe enters the container."
- **Width encoding:** During simulation, arrow width scales with flow magnitude relative to other flows. A flow of 50/yr is visually wider than a flow of 5/yr. When no simulation data exists, all flows use uniform width.
- **Animation:** During simulation playback, small dots travel along the arrow at a speed proportional to flow rate. Fast flow = fast dots. Zero flow = no dots. Negative flow = dots reverse direction. This makes material movement *viscerally obvious*.
- **Name:** Displayed alongside the arrow (above or below, based on layout).
- **Rate badge:** A small pill-shaped label showing the current rate and units (e.g., "12 people/yr"). Appears only when simulation data exists or the equation is defined.
- **Process indicator:** When a flow has two or more information inputs, a small rounded diamond appears at the arrow's midpoint. This replaces the valve symbol. It communicates "the rate is calculated here" and serves as the anchor point for incoming information links.
- **No equation defined:** Arrow renders as dashed, at 50% opacity, with an amber "?" badge where the rate badge would be.
- **Cloud symbol:** When a flow has no source or no sink (external inflow or outflow), a small cloud shape (○̃) marks the external end. This is borrowed from standard notation because it works — it means "outside the model boundary."

Why this works: the animated stream makes flows look like *things moving*. Combined with the stock fill bars, the accumulation relationship becomes a physical process you can watch — stuff flows through the pipe into the container, the container fills up. The correlation heuristic breaks down because the flow and the stock look and behave like entirely different visual objects.

#### Auxiliaries: The Calculator

```
          ┌─────────────┐
       fx │ Effect of    │
          │ Fatigue      │
          │   0.73       │
          └─────────────┘
```

- **Border:** 1.5px rounded rectangle. Noticeably lighter than stocks.
- **Size:** ~60% of stock area. Visually subordinate.
- **fx badge:** A small "fx" label in the top-left corner. Communicates "this is a computed value" without requiring domain knowledge.
- **Value:** Displayed only when simulation data exists.
- **No sparkline by default.** Auxiliaries don't accumulate — their sparklines are less informative than stock sparklines. Available on hover or by pinning.
- **Incomplete state:** When no equation is defined, shows dashed border and "?" in the value area.

#### Constants: The Pin

```
          ┌ ╌ ╌ ╌ ╌ ╌ ╌ ┐
        📌  Target Staff
          ╎    500       ╎
          └ ╌ ╌ ╌ ╌ ╌ ╌ ┘
             people
```

- **Border:** 1px dashed. Lightest border of any element.
- **Size:** Smallest element (~50% of stock area). Constants are parameters, not dynamics.
- **Pin icon:** Small icon in the top-left corner. Communicates "this value is fixed."
- **Value:** Always displayed (constants always have a value).
- **Collapsible:** At low zoom levels, constants collapse to a small pill showing just the value, attached visually to the element they influence. This dramatically reduces clutter in large diagrams.

#### Information links: The Whisper

```
          Auxiliary ╌╌╌╌╌╌╌▸ Flow
          (thin, dashed, curved, low opacity)
```

- **Line:** 1px dashed curve. Significantly thinner and lighter than material flows. Renders at 60% opacity by default.
- **Arrowhead:** Small, at the destination end.
- **Polarity encoding** (three redundant channels for accessibility):
  - **Color:** Blue (#4A90D9) for same-direction, amber (#D97B4A) for opposite-direction.
  - **Arrowhead shape:** Standard triangle for same-direction. Circle-dot (●▸) for opposite-direction.
  - **On hover:** Natural language tooltip — "higher Workload → more Burnout" or "more Inventory → less Ordering."
- **Z-order:** Information links render *behind* material flows and element shapes. They are the visual "background" of the diagram.
- **Scaling behavior:** At low zoom, information links fade to near-invisible (20% opacity). At high zoom, they sharpen. In CLD mode (all elements unclassified), information links are the *only* connections and render at full opacity.

Why separate visual treatment for info links: In standard SFDs, the arrow from an auxiliary to a flow looks almost identical to the arrow representing a material flow. This conflates two fundamentally different relationships — "stuff moves" vs. "information influences." The distinct visual treatment makes the diagram's causal structure readable at a glance: thick solid lines are plumbing (material), thin dashed lines are wiring (information).

### Three rendering layers

The canvas renders three conceptual layers. All three are always *available*, but their default visibility depends on context.

**Layer 1: Structure** (always visible)

Everything described above — element shapes, names, connections, classification visual encoding. This layer alone tells you: what are the stocks, how are they connected by flows, what influences the flow rates. The structural skeleton of the model.

**Layer 2: Behavior** (visible after first simulation run)

- Fill bars on stocks (animated during playback)
- Flow width encoding and dot animation
- Rate badges on flows
- Current values on stocks and auxiliaries
- Sparkline watermarks on stocks
- Stale indicator (amber border + "stale" label on the behavior dashboard) when model has changed since last run

This layer answers: what does the model *do*? It is the layer that most directly addresses stock-flow confusion — the fill bars and animated streams make accumulation dynamics visible without reading a single number.

**Layer 3: Detail** (on demand)

- Equations displayed inline below element names
- Unit labels on all connections (not just the unit badge on stocks)
- Unit mismatch warnings (red connection line, red badge on both endpoints)
- Parameter sensitivity indicators (which constants most affect which stocks — derived from sweep/Monte Carlo results)
- Full history data on hover (value at any time point)

This layer answers: *how* does the model work mathematically? It is revealed by:
- Clicking/selecting an element (shows its equation and units)
- Pressing a keyboard shortcut (toggles equations for all visible elements)
- Hovering over a connection (shows units and dimensional analysis)

Why three layers: A first-time viewer sees Structure + Behavior and immediately understands "there are containers, stuff flows between them, some are filling up and others are draining." They don't need to parse equations to get the gist. An expert reviewing the model toggles Detail to audit the math. A presenter showing the model to stakeholders keeps Detail off and narrates the Behavior. Each audience gets the right level of information without the others' noise.

### The CLD-to-SFD transition

The Design 4 spec describes a model that starts as a CLD (all unclassified variables) and gradually formalizes into an SFD. The Layered Flow Diagram handles this as a continuous visual evolution, not a mode switch.

**Stage 1: Unclassified (CLD)**

All elements are identical: thin-bordered (1.5px) rounded rectangles with the variable name inside. Connections are information links (thin, dashed, curved) with natural language polarity annotations ("more A → more B"). No stocks, no flows, no material connections. The diagram looks like a CLD.

```
    ┌─────────────┐   more staff   ┌──────────────┐
    │ Staff Level  │───────────────▸│  Throughput   │
    └─────────────┘                └──────────────┘
          ▴                              │
          │  higher attrition            │  more throughput
          │  fewer staff                 │  less backlog
    ┌─────────────┐                ┌──────────────┐
    │  Attrition   │◂──────────────│   Backlog     │
    └─────────────┘   more backlog └──────────────┘
                      more burnout
                      more attrition
```

**Stage 2: Partial classification**

As elements are classified, they morph individually:

- **Variable → Stock:** Over 300ms, the border thickens from 1.5px to 3px. The shape becomes slightly squarer. The fill bar fade-in begins. Material flow arrows replace the thin information links to/from this element where appropriate.
- **Variable → Flow:** The element *dissolves* into the connection between its source and sink stocks. The thin-bordered rectangle shrinks and disappears as a wide directional arrow grows in its place. The element's name moves to alongside the arrow. If the flow has information inputs, the process diamond fades in at the arrow midpoint.
- **Variable → Auxiliary:** The shape shrinks ~15%, the fx badge fades in.
- **Variable → Constant:** The shape shrinks ~30%, the border becomes dashed, the pin icon fades in.

The diagram at this stage is a *hybrid* — some elements look like CLD variables, others look like SFD elements. This is intentional. The visual difference between classified and unclassified elements reminds the user what formalization work remains. The maturity strip in the conversation panel shows the same information numerically.

**Stage 3: Fully classified (SFD)**

All elements have been classified. The diagram is now a full Layered Flow Diagram with the visual hierarchy described above. No further transition is needed — the SFD emerged naturally from incremental classification.

**Why this works for the CLD-to-SFD gap:** The user never sees two completely different diagrams. They see *one* diagram that gradually changes. The same spatial layout, the same element positions, the same connections — only the visual treatment evolves. The mental model is continuous. There is no moment where "the CLD goes away and the SFD appears" — the CLD *becomes* the SFD.

### Feedback loop rendering

Loops are not rendered by default. They are interactive overlays triggered by user action.

**Hover activation:** Hover over any element or connection for 500ms. The system queries `DependencyGraph` for all feedback loops containing that element. Each loop is assigned a distinct hue from a perceptually-uniform palette (to work for colorblind users, the hues are supplemented with distinct dash patterns).

**Visual treatment:**
- All elements and connections *not* in any highlighted loop dim to 25% opacity.
- Each loop's elements and connections glow with that loop's assigned color.
- A loop label appears near the center of the loop's visual footprint:

```
  R1: Growth
  more Staff → more Throughput → less Backlog → less Burnout → less Attrition → more Staff
```

- The label states the loop type (R = reinforcing, B = balancing), a short name derived from the stocks in the loop, and a one-sentence natural language trace.
- When multiple loops share elements, the shared elements glow with a blend or show alternating color segments.

**Toggle mode:** A keyboard shortcut (e.g., "L") toggles persistent loop overlay. All loops are shown simultaneously with their labels. Useful for presentations and structural review.

**Why this works:** Identifying feedback loops is error-prone because it requires mentally tracing paths and counting polarities. The system does both computations and presents the result visually. The natural language trace makes the causal story explicit — the reader doesn't have to construct it mentally.

### Scaling: 5-element to 80-element models

The representation must work for a teaching example with 3 stocks and also for a professional model with 80 stocks. The approach is **semantic zoom** — the level of detail adapts to the zoom level and element count.

**Small models (3-15 elements):**
- Full rendering of all elements with names, values, fill bars, sparklines.
- Natural language polarity labels on all information links.
- All information links visible at full opacity.
- No grouping needed. The whole model fits on screen.

**Medium models (15-40 elements):**
- At low zoom: elements show only name + fill bar (stocks) or name only (others). Values and sparklines hidden.
- At medium zoom: values appear. Sparkline watermarks appear on stocks.
- Information links use color + arrowhead encoding instead of natural language (saves space).
- Subsystem grouping available: user can select elements and group them into a named container. Groups collapse to show a summary: stock count, aggregate behavior direction (growing/declining/oscillating), and a single compound sparkline.

**Large models (40-80+ elements):**
- Subsystem grouping is the primary organizational tool. The default view shows groups as collapsed summary boxes. Double-click to expand a group in place.
- **Focus + context:** When one group is expanded, surrounding groups shrink to ~60% scale and reduce to outline rendering. The selected group gets full detail.
- **Edge bundling:** Information links between groups are bundled into single annotated lines with a count badge ("12 connections"). Expanding a group reveals the individual links.
- **Minimap:** A small (200x150px) overview in the canvas corner shows the full model at icon scale. The current viewport is a highlighted rectangle. Click to navigate.
- Constants auto-collapse to pill badges attached to their target element.
- The detail layer (equations, units) is completely hidden unless explicitly toggled or an element is selected.

### Static rendering (screenshots, print, export)

Not all features require animation. The representation must also work as a static image:

- Fill bars render at their final (or selected time-point) values.
- Flow widths render at their final magnitudes. No animation dots.
- Sparkline watermarks remain visible as the behavioral signal.
- Polarity uses color + arrowhead shape (both work in static images; color works in color print, arrowhead shape works in grayscale).
- Loop overlays can be rendered as a static layer.
- The three-layer system maps to print: Structure-only for a clean structural diagram, Structure + Behavior for a results summary, Structure + Behavior + Detail for a full technical reference.

### Addressing each cognitive problem — summary

| Cognitive problem | Standard SFD | Layered Flow Diagram |
|---|---|---|
| Stock-flow confusion | Rectangles vs. valve bowties — similar visual weight | Fill-bar containers vs. animated streams — categorically different visual objects |
| Polarity notation | "+/-" or "s/o" symbols requiring mental translation | Three-channel encoding: color (blue/amber), arrowhead shape, natural language on hover |
| Valve symbol | Bowtie from hydraulic engineering — opaque to non-engineers | No valve. Flows are wide directional arrows. Process diamond appears only when a flow has multiple information inputs |
| CLD underspecification | Separate notation that looks nothing like the SFD | Same diagram evolves continuously — CLD variables morph into typed SFD elements in place |
| CLD-to-SFD gap | Two completely different diagram types | One diagram that gradually changes. No transition, no re-learning |
| Feedback loop identification | Manual counting of negative links | Interactive overlay computes loop type and shows natural language trace |
| Information overload | Everything shown at once | Three-layer system: Structure (always), Behavior (after simulation), Detail (on demand) |

### Data requirements from the engine

The representation relies on data already present in the engine's data model:

| Visual feature | Engine data source |
|---|---|
| Element type/shape | `ElementPlacement.type` (stock, flow, aux, constant) |
| Position | `ElementPlacement.x`, `ElementPlacement.y` |
| Fill bar height | `Stock.getValue()` normalized to observed min/max from simulation history |
| Sparkline watermark | `Stock` value history via `Flow.getHistoryAtTimeStep()` and stock integration |
| Flow width | `Flow.quantityPerTimeUnit()` normalized to max across all flows |
| Flow animation speed | `Flow.quantityPerTimeUnit()` |
| Rate badge | `Flow.quantityPerTimeUnit()` + `Flow.getTimeUnit()` |
| Information link polarity | Requires new metadata — polarity is currently implicit in equations. `DependencyGraph.allEdges()` provides the link, but polarity must be computed by evaluating the partial derivative sign (or declared by the user/LLM during CLD construction) |
| Feedback loops | `DependencyGraph` — needs extension for cycle enumeration (currently has `hasCycle()` but not loop listing) |
| Equation text | `FlowDef.equation()`, `AuxDef.equation()` |
| Unit labels | `Stock.getUnit()`, `Variable.getUnit()`, `Constant.getUnit()`, `Flow.getTimeUnit()` |
| Connection routes | `ConnectorRoute` (info links), `FlowRoute` (material flows) |
| Maturity encoding | Computable from `ModelDefinition`: element has equation? has units? has connections? |

**New capabilities needed:**
1. **Polarity computation** — given an equation like `infection_rate * susceptible`, determine the sign of the partial derivative with respect to each input. This is straightforward for most SD equations (products, sums, IF-THEN-ELSE with monotonic branches) and can fall back to "unknown" for complex nonlinear expressions.
2. **Loop enumeration** — extend `DependencyGraph` to find all elementary cycles (Johnson's algorithm or similar). The graph is small enough (typically < 200 nodes) that this is fast.
3. **Simulation history on stocks** — stocks currently don't store their own history (flows do via `Flow.history`). Need to add value history tracking to `Stock` for sparkline watermarks, or compute it by integrating flow histories.
4. **Observed min/max tracking** — for fill bar normalization, need to track each stock's min and max observed values across the simulation run.

## Research sources

- Booth Sweeney & Sterman (2000), "Bathtub Dynamics: Initial Results of a Systems Thinking Inventory" — stock-flow failure
- Cronin, Gonzalez & Sterman (2009), "Why don't well-educated adults understand accumulation?" — persistence of stock-flow failure
- Richardson (1986), "Problems with causal loop diagrams" — polarity notation critique
- Schaffernicht (2007), "Causality and diagrams for system dynamics" — link/loop polarity
- Graph notation as a gateway (2025), CLD+ enhanced notation — bridging CLD and SFD
- MetaSD (2010), "Are causal loop diagrams useful?" — CLD underspecification critique
- Sterman (2000), "Business Dynamics" — +/- vs s/o recommendation
