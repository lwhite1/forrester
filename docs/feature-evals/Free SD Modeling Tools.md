# Free System Dynamics Modeling Tools

The top free SD modeling tools today:

1. **Vensim PLE** — The most widely used free option (free for education/personal learning). Industry-standard stock-and-flow interface, great documentation, large community. Limited compared to the paid Pro version but covers core SD well.

2. **Insight Maker** — Free, browser-based, no install needed. Good for quick prototyping and sharing models. Supports stocks, flows, and agent-based hybrids.

3. **Stella Online** (free tier) — isee systems offers a limited free version of their Stella platform. Polished UI with causal loop and stock-flow diagrams.

4. **Simantics System Dynamics** — Fully open source, desktop app (Windows). More technical but no licensing restrictions.

5. **Minsky** — Open source, focused on economics/monetary modeling with unique Godley Table for stock-flow consistent financial models.

6. **SageModeler** — Free, web-based, open source. Simplified interface aimed at education.

**If I had to pick one:** Vensim PLE is the standard recommendation — it's what most university SD courses use, has the best documentation, and the skills transfer directly to professional SD work.

## Comparison with Python and R Libraries

Beyond standalone modeling tools, there are programming libraries for SD in Python and R. These occupy a different niche — they are scripting/analysis environments rather than visual model-building tools.

### Python: PySD (the leading SD library)

[PySD](https://github.com/SDXorg/pysd) (~440 GitHub stars, MIT license) is the dominant Python library for System Dynamics. It reads Vensim (`.mdl`) and XMILE (`.xml/.stmx`) model files, cross-compiles them into Python, and simulates them using scipy's ODE solvers. Results come back as Pandas DataFrames, making it easy to integrate with matplotlib, scikit-learn, and the broader data science ecosystem.

**Strengths:**
- Bridges SD with Python's data science tools — enables parameter sweeps, sensitivity analysis, Monte Carlo simulation, and ML-based surrogate modeling
- Mature and actively maintained (1,350+ commits, 58 releases)
- Significant academic adoption; well-documented with a PySD Cookbook
- Supports subscripts/arrays, lookup tables, and most common Vensim/XMILE functions

**Limitations:**
- **Not a model-building tool** — it can only simulate models created in Vensim or Stella. There is no visual editor, no drag-and-drop, no interactive canvas
- Does not implement all Vensim/XMILE functions (most common ones covered, but gaps remain)
- XMILE support lags behind Vensim support
- No GUI — command-line and Jupyter notebook only

**Also notable:** [BPTK-Py](https://github.com/transentis/bptk_py) (~27 stars) offers a Python DSL for defining SD models in code and supports hybrid SD + agent-based models, but has a much smaller community.

### R: readsdr + deSolve (the typical R approach)

R has no single dominant SD library. The most SD-specific package is [readsdr](https://github.com/jandraor/readsdr) (~20 stars), which parses XMILE files into R objects compatible with the [deSolve](https://cran.r-project.org/package=deSolve) ODE solver. It can also generate Stan code for Bayesian parameter estimation.

More commonly, R users code SD models manually as ODE systems and solve them with deSolve, following the approach in Jim Duggan's *System Dynamics Modeling with R* (Springer, 2016). deSolve itself is a mature, widely-used general ODE solver but provides no SD-specific abstractions — there are no Stock or Flow objects, just raw differential equations.

**Strengths:**
- deSolve is extremely mature and well-tested
- Full access to R's statistical and visualization ecosystem (ggplot2, tidyverse)
- readsdr's Stan integration enables Bayesian calibration of SD models

**Limitations:**
- **No model-building environment** — models are either imported from Vensim/Stella or hand-coded as equations
- Requires strong R and mathematical skills
- readsdr has a tiny community; Vensim models must be exported to XMILE first
- No visual editor or interactive exploration

### Where Forrester fits

Forrester has two faces: a desktop application with a visual canvas supporting both causal loop diagrams and stock-and-flow models, and a Java engine that can be used as a library (JAR dependency) for programmatic simulation. CLD variables can be classified into S&F elements directly on the canvas, supporting the qualitative-to-quantitative workflow. This gives it broader coverage than tools that are purely visual or purely scripted.

| Capability | PySD (Python) | readsdr/deSolve (R) | Forrester App | Forrester Engine (JAR) |
|---|---|---|---|---|
| Visual model building | No | No | Yes | No |
| Import Vensim (.mdl) | Yes | No | Yes | Yes |
| Import/Export XMILE | Yes (import) | Yes (import) | Yes | Yes |
| Interactive CLD + stock-flow canvas | No | No | Yes | No |
| Batch/programmatic analysis | Excellent | Good | No | Yes (requires JAR dependency) |
| Data science integration | Excellent | Excellent | No | Via JVM data tools |
| Accessible to non-programmers | No | No | Yes | No |

The scripting libraries excel at programmatic analysis of existing models — parameter estimation, optimization, and integration with data science workflows. Forrester's desktop app fills the gap they explicitly leave open: providing a visual, interactive environment where users can build, explore, and simulate models without writing code — starting from causal loop diagrams and progressing to stock-and-flow simulation. Its engine, when used as a JAR dependency, can also serve the programmatic use case for JVM-based workflows.

## Sources

- [System Dynamics Society - Open Source Tools](https://systemdynamics.org/tools/useful-open-source-tools/)
- [Vensim Software](https://vensim.com/software/)
- [Insight Maker](https://insightmaker.com/)
- [Simantics System Dynamics](https://sysdyn.simantics.org/)
- [SageModeler](https://sagemodeler.concord.org/)
- [Minsky](https://sourceforge.net/projects/minsky/)
- [Wikipedia - Comparison of SD Software](https://en.wikipedia.org/wiki/Comparison_of_system_dynamics_software)
- [PySD GitHub](https://github.com/SDXorg/pysd)
- [PySD Documentation](https://pysd.readthedocs.io/)
- [BPTK-Py GitHub](https://github.com/transentis/bptk_py)
- [readsdr GitHub](https://github.com/jandraor/readsdr)
- [deSolve on CRAN](https://cran.r-project.org/package=deSolve)
- [System Dynamics Modeling with R (Springer)](https://link.springer.com/book/10.1007/978-3-319-34043-2)
