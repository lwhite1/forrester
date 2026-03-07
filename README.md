# Forrester - System Dynamics Modeling Tool

## Overview

Forrester is a Java System Dynamics simulation engine and visual modeling tool. It provides two ways to build models:

- **Visual Editor** — a JavaFX canvas-based GUI for interactively building stock-and-flow diagrams and causal loop diagrams. See [Visual Editor Guide](docs/Visual%20Editor%20Guide.md).
- **Programmable Engine** — a code-first Java API for defining, compiling, and running models programmatically. See [Programmable Engine](docs/Programmable%20Engine.md).

The engine supports creating training simulations, games, scenario testing, and planning models across domains including ecology, project management, software development, business strategy, operations, medicine, and international relations.

## Core Concepts

System Dynamics models are built from these elements:

- **Stocks** — accumulations representing system state (e.g., population, inventory)
- **Flows** — rates of change that add to or drain from stocks
- **Variables** — calculated quantities derived from formulas
- **Constants** — fixed exogenous parameter values
- **Lookup Tables** — piecewise interpolation curves for nonlinear effects
- **Subscripts / Arrays** — dimensions that expand elements into parallel instances (e.g., by region or cohort)

These are connected into feedback loops that drive system behavior over time.

### Causal Loop Diagrams

Forrester also supports **Causal Loop Diagrams (CLDs)** — the qualitative diagramming technique used in early-stage system dynamics modeling:

- **CLD Variables** — qualitative concepts with no equation or unit
- **Causal Links** — directed connections with polarity: positive (+), negative (−), or unknown (?)
- **Automatic Loop Detection** — finds feedback cycles and classifies them as reinforcing (R) or balancing (B)
- **Classification** — CLD variables can be converted into S&F elements (stock, flow, auxiliary, constant)

CLDs and S&F elements share a single canvas and model definition.

## Build & Configuration

- **Language:** Java 21+
- **Build system:** Maven
- **Artifact:** `com.deathrayresearch:dynamics:1.0-SNAPSHOT`

```bash
mvn clean compile    # Build
mvn test             # Run tests
mvn package -DskipTests  # Package JAR
```

### Key Dependencies

| Dependency | Purpose |
|---|---|
| JUnit 5 | Testing |
| Google Guava | Event bus, collections |
| Apache Commons Math | Mathematical functions, optimization |
| OpenCSV | CSV output |
| Logback | Logging |
| OpenJFX 21 | Visual editor and chart visualization |

## Architecture

```
com.deathrayresearch.forrester
├── Simulation.java              # Core simulation engine
├── model/                       # Model elements (Stock, Flow, Variable, Constant, Module, Subscript, etc.)
│   ├── expr/                    # Sealed Expr AST, parser, stringifier, dependency extractor
│   ├── def/                     # Immutable definition records (ModelDefinition, StockDef, etc.)
│   ├── compile/                 # Two-pass ModelCompiler: definition → runnable Model
│   └── graph/                   # Dependency graph, auto-layout, CLD loop detection
├── measure/                     # Dimensional analysis and unit system
├── event/                       # Event-driven communication
├── sweep/                       # Parameter sweep, Monte Carlo, optimization, CSV output
├── io/                          # JSON serialization, Vensim import, XMILE import/export
├── ui/                          # JavaFX chart visualization
└── app/                         # JavaFX visual editor (forrester-app module)
    └── canvas/                  # Canvas rendering, interaction, editing, simulation
```

## Model Import & Export

Forrester can exchange models with other System Dynamics tools:

- **Vensim `.mdl` import** — reads Vensim model files including stocks, flows, auxiliaries, constants, lookup tables, subscripts, sketch data, and simulation settings. See [Vensim Import](docs/Vensim%20Import.md) for supported constructs and limitations.
- **XMILE import & export** — bidirectional exchange with Stella/iThink via the OASIS standard XML format. See [XMILE Import & Export](docs/XMILE%20Import%20Export.md) for supported constructs and limitations.
- **JSON** — native round-trip persistence format used by the visual editor. See the [Programmable Engine](docs/Programmable%20Engine.md#json-serialization) docs for the JSON API.

## Example Models

The demo package contains example models with `main()` entry points:

| Category | Models |
|---|---|
| **Fundamental** | Exponential growth/decay, coffee cooling, bathtub, S-shaped growth, flow time conversion, lookup tables |
| **Delays** | First-order material delay, third-order material delay, FIFO pipeline delay |
| **Feedback & Interaction** | SIR epidemic (+ sweep, multi-sweep, Monte Carlo, calibration variants), multi-region SIR with subscripts, population by region × age, predator-prey, inventory with delays, sales mix |
| **Software Development** | Agile project with rework dynamics, waterfall project with composable modules |

The visual editor also provides 8 bundled example models via File → Open Example.

## Documentation

| Document | Contents |
|---|---|
| [Quickstart Tutorial](userdocs/Quickstart.md) | Build your first model in 10 minutes — a hands-on walkthrough using Newton's law of cooling |
| [Visual Editor Guide](docs/Visual%20Editor%20Guide.md) | GUI features, tools, keyboard shortcuts, simulation, analysis dialogs |
| [Programmable Engine](docs/Programmable%20Engine.md) | Code API: lambda-based models, definitions, compiler, expressions, sweep/Monte Carlo/optimization |
| [Expression Language](userdocs/Expression_Language.md) | Equation syntax, operators, and built-in functions reference |
| [From Vensim PLE](userdocs/From_Vensim_PLE.md) | Migration guide for Vensim PLE users: import workflow, what translates, what to check, and what you gain |
| [Vensim Import](docs/Vensim%20Import.md) | Vensim `.mdl` import: supported constructs, limitations, usage |
| [XMILE Import & Export](docs/XMILE%20Import%20Export.md) | XMILE import/export: supported constructs, limitations, usage |

## Learning System Dynamics

- [Thinking in Systems: A Primer](https://www.chelseagreen.com/product/thinking-in-systems/) by Donella Meadows
- [MIT OCW: Introduction to System Dynamics](https://ocw.mit.edu/courses/15-871-introduction-to-system-dynamics-fall-2013/)
- [MIT OCW: System Dynamics Self Study](https://ocw.mit.edu/courses/15-988-system-dynamics-self-study-fall-1998-spring-1999/)
- [System Dynamics Society: Introduction](https://systemdynamics.org/introduction-to-system-dynamics-modeling/)

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).
