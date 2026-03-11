# Shrewd — System Dynamics Modeling for Everyone

Shrewd is a free, open-source System Dynamics tool for building models that reveal how complex systems behave over time — and why they so often surprise us.

If you've ever struggled with unintended consequences, policy resistance, or the gap between short-term fixes and long-term outcomes, System Dynamics gives you a way to see the whole system at once. Shrewd gives you the tools to do it.

**Free. No license keys. No feature tiers. No signup.**

---

## Sketch your system's causal structure

Start where every SD practitioner starts: with a **causal loop diagram**. Map out the variables in your system and draw the connections between them. Shrewd automatically detects your feedback loops and classifies them as reinforcing or balancing — so you can see the engines of growth, the sources of resistance, and the leverage points where intervention matters most.

When you're ready to make it quantitative, right-click any variable and classify it as a stock, flow, variable, or constant. It transforms in place on the same canvas — no switching tools, no redrawing your diagram.

---

## Build and simulate stock-and-flow models

Place stocks, flows, variables, constants, and lookup tables on a visual canvas. Write equations with autocomplete that suggests variable names and built-in functions as you type. Press Ctrl+R to simulate and see the results immediately in an interactive chart.

Break large models into reusable **modules** with input/output ports. Add **subscripts** to expand elements across dimensions (regions, age groups, product lines) without duplicating your diagram.

---

## Test scenarios and quantify uncertainty

System Dynamics is most valuable when you use it to ask "what if?" — and Shrewd makes that easy.

**Parameter sweeps** let you vary any assumption across a range and see how the system responds. Instead of running one scenario at a time, you see a family of curves that reveals sensitivity and thresholds at a glance.

**Monte Carlo analysis** goes further: assign probability distributions to your uncertain assumptions and run thousands of trials. The result is an envelope of possible futures — not a single prediction, but a realistic picture of the range of outcomes your system can produce. This is how you answer questions like "what's the probability this intervention succeeds?" or "how bad could things get?"

**Optimization** finds the best answer automatically. Define what "better" means — minimize cost, hit a target, fit observed data — and let the optimizer search for the parameter values that achieve it. This is how you calibrate models against reality and identify the highest-leverage interventions.

---

## Import your existing models

Already working in Vensim or Stella? **File > Open** reads Vensim `.mdl` and XMILE files directly. Your model structure, equations, and diagram layout come through intact.

See the [Vensim PLE migration guide](From_Vensim_PLE.md) for details.

---

## How Shrewd compares

|  | Vensim PLE | Stella | Vensim Pro | **Shrewd** |
|---|:---:|:---:|:---:|:---:|
| **Cost** | Free | $249/yr | $1,200/yr | **Free** |
| Stocks, flows, variables | Yes | Yes | Yes | Yes |
| Causal loop diagrams | Separate tool | Separate tool | Separate tool | **Integrated** |
| Subscripts / arrays | No | Yes | Yes | Yes |
| Parameter sweeps | No | Yes | Yes | Yes |
| Monte Carlo | No | Yes | Yes | Yes |
| Optimization / calibration | No | No | Yes | Yes |
| Hierarchical modules | No | Yes | Yes | Yes |
| Vensim .mdl import | N/A | No | N/A | Yes |
| XMILE import/export | No | Native | Limited | Yes |
| Programmable API | No | No | No | **Yes** |

---

## Also a programmable engine

The same engine that powers the visual editor is available as a Java library. Define models in code, run headless simulations, sweep parameters programmatically, or embed System Dynamics in a larger application. Models built in code and models built in the editor use the same format — save from one, open in the other.

See the [Programmable Engine](../docs/Programmable%20Engine.md) documentation.

---

## Get started

```bash
git clone https://github.com/lwhite1/shrewd.git
cd shrewd
mvn clean package -DskipTests
java -jar shrewd-app/target/shrewd-app-*.jar
```

Go to **File > Open Example** and pick a model. Press **Ctrl+R** to run it. Or start from scratch — the [quickstart tutorial](Quickstart.md) walks you through building your first model in 10 minutes.

**Requires Java 21+.**
