# Visual Notation Redesign

Design reference for improved system dynamics diagram renderings in the forrester-canvas module.

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

Flows are shown with a valve (bowtie) symbol inherited from Forrester's original hydraulic analogy. Problems:

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

## Research sources

- Booth Sweeney & Sterman (2000), "Bathtub Dynamics: Initial Results of a Systems Thinking Inventory" — stock-flow failure
- Cronin, Gonzalez & Sterman (2009), "Why don't well-educated adults understand accumulation?" — persistence of stock-flow failure
- Richardson (1986), "Problems with causal loop diagrams" — polarity notation critique
- Schaffernicht (2007), "Causality and diagrams for system dynamics" — link/loop polarity
- Graph notation as a gateway (2025), CLD+ enhanced notation — bridging CLD and SFD
- MetaSD (2010), "Are causal loop diagrams useful?" — CLD underspecification critique
- Sterman (2000), "Business Dynamics" — +/- vs s/o recommendation
