# Forrester — System Dynamics Modeling for Everyone

Build feedback models visually. Simulate them instantly. Analyze them seriously — with parameter sweeps, Monte Carlo, and optimization. All free, no signup, no license keys.

Forrester is an open-source System Dynamics tool that gives you the modeling and analysis power of professional SD software without the professional price tag.

---

## Build models the way you think about systems

Start with a **causal loop diagram** — sketch out the feedback structure of your system with variables and polarity-labeled links. Forrester automatically detects your feedback loops and classifies them as reinforcing or balancing.

When you're ready to simulate, right-click any variable and classify it as a stock, flow, auxiliary, or constant. It transforms in place — no switching tools, no redrawing.

Or jump straight into **stock-and-flow modeling**: place elements with keyboard shortcuts or the toolbar, connect flows with two clicks, and write equations with autocomplete that suggests variable names and built-in functions as you type.

---

## Analyze without limits

Run analysis directly from the Simulate menu. Results appear in an interactive dashboard with charts and CSV export.

**Parameter sweep** — pick any constant, set a range, and see a family of simulation curves side by side. Answer "what if?" questions in seconds instead of running one scenario at a time.

**Multi-parameter sweep** — vary two or more parameters simultaneously. Every combination runs as an independent simulation. See how parameters interact.

**Monte Carlo** — assign probability distributions to your uncertain parameters and run hundreds or thousands of trials. See percentile envelopes that show the range of possible outcomes. Answer questions like "what's the probability this project finishes on time?"

**Optimization** — fit your model to observed data, or find the parameter values that minimize cost, maximize throughput, or hit a target. Three algorithms (Nelder-Mead, BOBYQA, CMA-ES) search the parameter space for you.

---

## How Forrester compares

|  | Vensim PLE | Stella | Vensim Pro | **Forrester** |
|---|:---:|:---:|:---:|:---:|
| **Cost** | Free | $249/yr | $1,200/yr | **Free** |
| Stocks, flows, auxiliaries | Yes | Yes | Yes | Yes |
| Lookup tables | Yes | Yes | Yes | Yes |
| Causal loop diagrams | Separate tool | Separate tool | Separate tool | **Integrated** |
| Subscripts / arrays | No | Yes | Yes | Yes |
| Parameter sweeps | No | Yes | Yes | Yes |
| Monte Carlo | No | Yes | Yes | Yes |
| Optimization / calibration | No | No | Yes | Yes |
| Hierarchical modules | No | Yes | Yes | Yes |
| Vensim .mdl import | N/A | No | N/A | Yes |
| XMILE import/export | No | Native | Limited | Yes |
| SVG diagram export | No | No | No | **Yes** |
| Programmable API | No | No | No | **Yes** |

Forrester gives you the analysis capabilities of Vensim Pro and the modeling features of Stella — plus a programmable engine and integrated CLD support — at no cost.

---

## Everything you need in the editor

- **Equation autocomplete** — element names and built-in functions suggested as you type
- **Inline editing** — double-click any element to rename it or edit its equation directly on the canvas
- **Lookup table editor** — edit data points and see a live chart preview with linear or cubic spline interpolation
- **Validation** — Ctrl+B checks for undefined equations, disconnected flows, algebraic loops, and unused elements
- **Feedback loop highlighting** — toggle to see which elements participate in feedback cycles, with reinforcing/balancing classification
- **100-level undo** with action labels and a visual history panel
- **Copy/paste** across windows with automatic name remapping and equation adjustment
- **Pan and zoom** — scroll wheel, keyboard shortcuts, or Space+drag
- **Diagram export** — PNG, JPEG, or SVG at 2x resolution
- **8 bundled example models** covering epidemiology, ecology, population dynamics, and supply chains

---

## Import your existing models

Already have models in Vensim or Stella? **File > Open** reads `.mdl` and XMILE files directly. Your stocks, flows, equations, lookup tables, simulation settings, and diagram layout come through intact. Any constructs that need attention are flagged in the activity log — nothing fails silently.

See the [Vensim PLE migration guide](From_Vensim_PLE.md) for a detailed walkthrough.

---

## Also a programmable engine

The same engine that powers the visual editor is available as a Java library. Define models in code, run headless simulations, sweep parameters programmatically, or embed System Dynamics in a larger application.

```java
ModelDefinition def = new ModelDefinitionBuilder()
    .name("SIR Epidemic")
    .stock("Susceptible", 990, "Person")
    .stock("Infectious", 10, "Person")
    .stock("Recovered", 0, "Person")
    .flow("Infection",
          "Susceptible * Infectious / (Susceptible + Infectious + Recovered)"
          + " * contact_rate * infectivity",
          "Day", "Susceptible", "Infectious")
    .flow("Recovery", "Infectious * recovery_rate",
          "Day", "Infectious", "Recovered")
    .constant("contact_rate", 8.0, "1/Day")
    .constant("infectivity", 0.10, "Dimensionless")
    .constant("recovery_rate", 0.05, "1/Day")
    .build();

CompiledModel compiled = new ModelCompiler().compile(def);
compiled.createSimulation().execute();
```

Models built in code and models built in the visual editor use the same format. Save from code, open in the editor. Build in the editor, load from code.

---

## Get started

```bash
git clone https://github.com/lwhite1/forrester.git
cd forrester
mvn clean package -DskipTests
java -jar forrester-app/target/forrester-app-*.jar
```

Open the editor, go to **File > Open Example**, and pick a model. Press **Ctrl+R** to run it. Or start from scratch — the [quickstart tutorial](Quickstart.md) walks you through building your first model in 10 minutes.

**Requires Java 21+.**
