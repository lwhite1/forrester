# UI Design Critique

Three interface designs have been proposed for an AI-integrated system dynamics tool. This document summarizes each, assesses strengths and weaknesses, and identifies the real decisions that need to be made.

## Design 1: The Integrated Workspace

**Source:** Claude (earlier pass).

**Summary.** A three-panel layout — conversation, model canvas, behavior dashboard — all visible and interconnected at all times. The simulation runs continuously from the moment the model is valid. Every element cross-links: clicking a variable on the canvas scrolls the conversation; mentioning a variable in chat highlights it on the canvas. Both direct manipulation (drag, drop, sliders) and natural language modify the same model. Auxiliary panels (equation inspector, literature, calibration data, scenario comparison) are available on demand.

**Strengths.**
- Cross-linking between conversation and canvas is a genuinely good idea. Traceability between "why we modeled it this way" and "what we modeled" is a real gap in every existing SD tool.
- Dual-mode interaction (conversational + direct manipulation) respects different working styles. Some people think in diagrams, others in words.
- The always-running simulation provides instant feedback on structural changes, which shortens the build-test loop.

**Weaknesses.**
- The design's own critique is correct: it optimizes for the demo. Three persistent panels, multiple overlay modes, an assumptions ledger, embedded sparklines, scenario ghosting — the cognitive load is immense. Real modeling involves long stretches of staring at one thing, and this design fills the screen with everything.
- Always-running simulation during early model building is actively harmful. A half-built model produces meaningless output, but it *looks* like output. Users will anchor on early trajectories and resist structural changes that invalidate them.
- The dual-mode interaction sounds elegant but doubles the interaction surface to test, maintain, and teach. In practice, users will pick one mode and the other will rot. The design pays the cost of both without the benefit.
- The assumptions ledger is a good idea in principle, but as noted, the LLM grading its own epistemic status is not trustworthy. It becomes a false signal of rigor.

**Verdict.** The cross-linking and traceability ideas are worth keeping. The rest is overdesigned — it tries to solve every problem simultaneously and ends up solving none of them deeply.

---

## Design 2: The Mode-Based Workflow

**Source:** Claude (later pass, built as corrective to Design 1).

**Summary.** Four sequential modes that reshape the interface to match the modeling phase. Mode 1 (Mapping): freeform causal loop diagram, Socratic LLM, no simulation. Mode 2 (Formalization): guided stock/flow classification, qualitative shape selection for relationships, parameter elicitation in domain language. Mode 3 (Exploration): simulation turns on, question-driven parameter exploration, plain-language sensitivity reporting. Mode 4 (Confrontation): data overlay, adversarial LLM challenging model-data discrepancies. The LLM's authority deliberately escalates across modes.

**Strengths.**
- Takes the modeling process seriously. The distinction between "I'm still figuring out what belongs in this model" and "I'm testing whether this model explains the data" is real, and the interface reflecting that distinction is sound.
- Starting with causal loop diagrams instead of stock-and-flow is pedagogically correct. The stock/flow distinction is a formalization step, not a conceptualization step. Forcing it early biases the modeler toward familiar structures.
- Eliciting parameters in domain language ("days, weeks, months?") rather than as raw numbers is a meaningful accessibility gain. Most domain experts have strong intuitions about timescales and magnitudes but can't translate them to parameter values unprompted.
- The escalating LLM authority is a smart design choice. Early in modeling, the researcher's domain knowledge dominates and the LLM should mostly listen. Later, when the model is concrete enough to be tested, the LLM can be more assertive about structural problems.
- Deliberately withholding simulation output until the structure is stable prevents premature anchoring.

**Weaknesses.**
- The mode transitions are the critical design problem and the document doesn't solve them. Who decides when Mapping is "done" and Formalization should begin? If the LLM decides, it needs judgment it doesn't have. If the researcher decides, they'll either transition too early (because they want to see output) or too late (because they're comfortable in the freeform space). A hard gate feels authoritarian; a soft suggestion feels ignorable.
- The Socratic LLM in Mode 1 requires the LLM to ask good questions about what's *missing* from a domain it may not understand. Current LLMs are better at pattern-completing from what's present than identifying what's absent. The LLM will ask generic questions ("have you considered feedback effects?") rather than domain-specific ones ("where does the regulatory approval delay fit?").
- No collaboration story. System dynamics modeling in organizations is inherently social — multiple stakeholders contribute different mental models. A single-researcher tool with a single conversation thread doesn't address this.
- The strict sequencing may not match how experienced modelers actually work. Some modelers sketch a rough stock-and-flow diagram on day one, run it, see that it's wrong, and use the wrongness to guide structural revision. This design would make them wait through two modes before they can do that.

**Verdict.** The best of the three designs conceptually. The phased approach, escalating LLM authority, and deliberate restraint are well-reasoned. The mode transition problem is solvable — soft modes with explicit "you're leaving Mapping, the model has unresolved questions" warnings rather than hard gates. The biggest risk is that it's designed for an ideal modeler who doesn't exist; real users will want to skip ahead.

---

## Design 3: The AI-First Infinite Canvas

**Source:** Gemini.

**Summary.** A command-palette-driven ("Omnisearch") interface where natural language is the primary interaction mode. The AI generates initial model structure from prompts, suggests next variables contextually ("Contextual Ghost"), translates natural language to equations, and runs background Monte Carlo simulations to surface sensitivity information automatically. Scenarios are managed via a git-like branching metaphor. The design benchmarks against Vensim, Stella Architect, and Insight Maker, positioning itself as a "10x" improvement.

**Strengths.**
- The Omnisearch / command palette concept is proven UX (VS Code, Spotlight, Raycast). Unifying search, creation, and AI prompting in one input field reduces interface complexity. This is a concrete, implementable idea.
- Natural language equation entry with bidirectional sync (edit the math, the description updates; edit the description, the math updates) addresses a real documentation gap. Models with stale or missing equation documentation are the norm.
- Inline unit mismatch detection with suggested "bridge variables" is practical and builds on Forrester's existing unit system. This could be implemented today.
- The scenario branching metaphor is intuitive for developers and maps well to the parameter sweep infrastructure already in Forrester.

**Weaknesses.**
- The design treats the AI as a productivity accelerator but never addresses model quality. "Create a model for a startup's cash flow with hiring and churn" will produce a generic textbook model. The hard problem in SD isn't drawing boxes faster — it's figuring out which boxes matter and which causal theories are wrong. This design makes it easier to build models without making it easier to build *good* models.
- "Contextual Ghost" suggestions (hover over empty space, get suggested variables) encourage model bloat. The most common modeling error is including too much, not too little. A tool that eagerly suggests additions will make this worse.
- Background Monte Carlo with automatic sensitivity heatmaps sounds impressive but is computationally expensive and informationally noisy. Running 1,000 simulations on every parameter change in a model with 50 parameters will either be slow or produce sensitivity information that's too dense to act on. And like Design 1's always-running simulation, it presents sophisticated-looking output before the model structure warrants it.
- The "10x" framing and competitor benchmarking is marketing language, not design thinking. Comparing feature checklists (Vensim: modal popups; Us: inline suggestions) describes surface differences, not workflow improvements. The comparison to Stella's AI assistant generating CLD polarities is reasonable, but the proposed "10x" alternative (importing and auto-composing model libraries) assumes a library ecosystem that doesn't exist.
- No consideration of the conceptualization problem. The design assumes the user already knows what to model and just needs a faster way to draw it. This is the least important part of the modeling process to optimize.
- The tone is consistently promotional ("premium," "10x," "Zero-G," "weightless") in a way that substitutes enthusiasm for analysis. Several ideas are asserted as better without examining failure modes.

**Verdict.** Contains two or three good concrete ideas (command palette, inline unit checking, bidirectional equation documentation) buried in a pitch deck. The design would make an excellent interface for someone who already knows exactly what model they want to build. It would be a poor interface for someone trying to figure out whether their mental model of a system is right.

---

## Cross-cutting observations

**The fundamental tension.** Designs 1 and 3 optimize for model construction speed. Design 2 optimizes for model construction quality. These are different goals, and the choice between them depends on who the user is:
- A researcher building a novel model of a system they don't fully understand needs Design 2's deliberate pacing.
- A consultant adapting a known model structure to a new client's data needs Design 3's construction speed.
- A teacher demonstrating feedback dynamics to a class needs Design 1's visual richness.

No single design serves all three well.

**The conceptualization gap.** All three designs start after the hardest part of modeling is already done. The researcher has already decided what system to model, what the key variables are, and roughly how they interact. The LLM's Socratic questioning in Design 2 gestures toward helping with this, but it's the weakest part of that design. Genuine conceptualization support — helping someone go from "my hospital has a burnout problem" to "here are the three feedback structures that might explain it, and here's how we'd distinguish between them" — remains unsolved.

---

## Design 4: Maturity-Responsive Interface

Rather than combining the three designs or choosing between them, there's a different framing that captures Design 2's insights without its modal machinery.

**The core realization.** Design 2's "modes" aren't really workflow stages the user moves through — they're descriptions of model maturity. A model with five disconnected variable names and no equations is in a fundamentally different state than a model with complete structure, calibrated parameters, and empirical data to test against. Design 2 is right that the interface should behave differently in each case. It's wrong that the user should be the one declaring the transition.

**The model knows what state it's in.** At any moment, the model has observable properties: How many variables lack equations? How many connections have unspecified functional forms? Are units consistent? Are there disconnected subgraphs? Has the model ever been simulated? Has it been compared to data? These aren't subjective assessments — they're computable facts. The interface can read them continuously and adapt without asking the user to pick a mode.

### How it works

**The canvas is always present. The conversation is always present.** No panel rearrangement, no mode switches. The user always has the same spatial layout. What changes is emphasis, affordance, and LLM behavior — all driven by model maturity signals.

**Maturity signals, not modes.** The system continuously evaluates the model across several dimensions:

| Signal | What it measures |
|---|---|
| **Structural completeness** | Are all variables connected? Any dangling endpoints? |
| **Equation coverage** | What fraction of relationships have functional forms? |
| **Unit consistency** | Any dimensional mismatches or unspecified units? |
| **Parameter specification** | Are parameter values assigned or still placeholders? |
| **Simulation history** | Has the model been run? How many times? |
| **Data attachment** | Is empirical data loaded for comparison? |

These combine into a rough maturity assessment — not a discrete mode, but a continuous signal the interface responds to.

**What adapts:**

*Canvas rendering.* Early on (low structural completeness), the canvas renders in causal-loop style — variables and arrows, no stock/flow distinction, no sparklines. As the user classifies variables as stocks, flows, and auxiliaries, elements change shape organically. Sparklines appear on stocks only after equations are defined for their flows. This isn't a mode switch — it's the visual representation reflecting what actually exists in the model. You don't *enter* stock-and-flow view; stocks appear when you create stocks.

*Simulation affordances.* The Run button is always present, but its presentation shifts. On a structurally incomplete model, it's subdued with a label like "Run (3 undefined equations)" — not hidden, not blocked, but honest about what you'll get. On a complete model, it's prominent. After the first run, behavior output appears. The user can always choose to run early; the interface just doesn't pretend incomplete output is trustworthy.

*LLM posture.* This is the biggest payoff. Instead of scripting the LLM's behavior per mode, the system prompt adjusts based on maturity:

- **Low structural completeness.** The LLM defaults to asking questions: "You've got hiring rate flowing into staff — what causes people to leave?" It does not volunteer model structure. It asks about what's missing. It does not comment on simulation output even if the user runs the model, because there's nothing structurally stable to interpret.
- **High structural completeness, low equation coverage.** The LLM shifts to formalization: "The link from workload to quality — is that linear, or is there a threshold where things break down?" It offers qualitative shape choices. It elicits parameters in domain language.
- **Full specification, pre-simulation.** The LLM asks what the user expects to see before running: "Before we simulate — if this model is right, what should happen to staff turnover in the first two years?" This creates a prediction to test against, turning the first run into a genuine test rather than a fishing expedition.
- **Post-simulation, no data.** The LLM interprets output but flags the absence of empirical grounding: "The model shows burnout peaking at month 18 — does that match what you've observed?" It suggests sensitivity analysis and extreme-condition tests.
- **Data attached.** The LLM turns adversarial, exactly as Design 2's Mode 4 described: highlighting discrepancies, proposing structural explanations, suggesting alternative formulations.

The key difference from Design 2: the user never "enters Confrontation mode." The LLM simply starts challenging the model when there's data to challenge it against. If the user loads data on day one, the LLM adapts immediately. If they never load data, Mode 4 never appears and nothing feels missing.

*Deliberate friction, not gates.* When the user does something that skips maturity — say, running a simulation when 40% of equations are undefined — the interface doesn't block them. It does three things: (1) shows the simulation output, because the user asked for it; (2) marks undefined regions on the canvas with a clear visual treatment (faded, dashed, whatever communicates "this part is placeholder"); (3) has the LLM preface its interpretation with what's real and what isn't: "The cost trajectory is meaningless right now because the treatment cost equation isn't defined yet. The disease progression dynamics are complete and worth looking at." This respects the user's agency while protecting them from anchoring on noise.

### What this inherits from each design

| Idea | Origin | How it appears |
|---|---|---|
| Cross-linking: conversation ↔ canvas | Design 1 | Every model element traces to the conversation turn that created/modified it. Click a variable, see its history. |
| Command palette as primary input | Design 3 | Omnisearch for creating, finding, and querying model elements. |
| CLD-first visual representation | Design 2 | Canvas starts in CLD style, transitions to stock-and-flow as the model formalizes — driven by model state, not user selection. |
| Escalating LLM authority | Design 2 | LLM posture adapts to maturity signals. Restrained early, assertive late. |
| Domain-language parameter elicitation | Design 2 | LLM asks "days, weeks, or months?" when the model reaches the parameterization stage. |
| Inline unit intelligence | Design 3 | Unit mismatches surfaced inline with suggested bridge variables. |
| Scenario branching | Design 3 | Git-like branch metaphor for strategy comparison. |
| Always-available simulation | Design 1 | Simulation is never blocked, but incomplete models get honest labeling. |

### What this drops

- **Design 1's persistent three-panel layout.** The behavior dashboard appears when there's behavior to show, not before.
- **Design 1's assumptions ledger.** Replaced by conversation-to-model traceability, which serves the same purpose without the LLM grading its own work.
- **Design 2's explicit mode transitions.** Gone entirely. The interface adapts continuously.
- **Design 2's simulation withholding.** The user can always run. The interface is honest about what they'll get, but doesn't refuse.
- **Design 3's "Contextual Ghost."** Suggesting variables on hover encourages model bloat. The LLM can suggest structure when asked; it shouldn't ambient-suggest additions.
- **Design 3's automatic background Monte Carlo.** Computationally expensive, informationally noisy, and presents sophisticated-looking output before the structure warrants it. Sensitivity analysis should be intentional, not ambient.

### The hard parts

**LLM posture calibration.** The system prompt needs to produce reliably different LLM behavior at different maturity levels. This is a prompt engineering challenge, not an architecture challenge — the maturity signals are computable, so the system prompt can be templated. But getting the tone right (curious vs. translating vs. interpreting vs. adversarial) across a continuous spectrum rather than four discrete modes will take iteration.

**Visual transition smoothness.** The canvas evolving from CLD-style to stock-and-flow as the model matures needs to feel organic, not jarring. If the user classifies three variables as stocks in quick succession, the canvas shouldn't dramatically reorganize three times. Batching visual updates and using smooth animations matters here.

**The "expert who wants to skip ahead" problem.** An experienced modeler who knows the structure they want will find the early-maturity LLM behavior (lots of questions, reluctance to interpret output) frustrating. The design needs an escape hatch — perhaps a "draft mode" that lets the user rapidly build structure without the LLM second-guessing every step, then re-engages the maturity-responsive behavior once they've roughed out the skeleton. Alternatively, the LLM could detect fluency (the user is creating stocks, flows, and equations rapidly without pausing) and back off automatically.

**Regression.** Models don't only mature — they regress. A user might delete a key stock, invalidating half the equations. The interface needs to handle de-maturation gracefully: sparklines disappearing from invalidated stocks, the LLM shifting back to structural questions, the behavior dashboard dimming or showing stale results with a clear "model has changed since last run" marker.
