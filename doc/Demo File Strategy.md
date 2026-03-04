# Demo File Strategy: Bundled Example Models

## 1. Resource Location

Bundle models inside the `forrester-app` JAR as classpath resources:

```
forrester-app/src/main/resources/models/
├── catalog.json
├── introductory/
│   ├── exponential-growth.json
│   ├── teacup-cooling.json
│   ├── goal-seeking.json
│   └── bathtub.json
├── epidemiology/
│   ├── sir-basic.json
│   └── sir-vaccination.json
├── ecology/
│   ├── predator-prey.json
│   └── ...
├── economics/
│   ├── bass-diffusion.json
│   └── ...
├── supply-chain/
│   └── inventory-oscillation.json
└── project-management/
    └── software-project.json
```

Being inside the JAR means they ship automatically — no separate install step.

## 2. Catalog File

A single `catalog.json` at the root of the models folder. This avoids loading every model JSON just to display a list:

```json
{
  "models": [
    {
      "id": "sir-basic",
      "name": "SIR Epidemic Model",
      "description": "Classic Susceptible-Infected-Recovered compartmental model. Demonstrates feedback between infection rate and susceptible depletion.",
      "category": "epidemiology",
      "difficulty": "introductory",
      "path": "epidemiology/sir-basic.json",
      "source": {
        "author": "Kermack & McKendrick (1927), adapted",
        "url": "https://en.wikipedia.org/wiki/Compartmental_models_in_epidemiology",
        "license": "Public domain (mathematical model)",
        "notes": "Implementation is original; the SIR structure is standard epidemiological theory"
      },
      "tags": ["feedback", "stocks-and-flows", "nonlinear"],
      "elements": { "stocks": 3, "flows": 2, "auxiliaries": 2 }
    }
  ]
}
```

Key metadata per model: name, description, category, difficulty level, source attribution, license, and a lightweight element count so the UI can show model complexity at a glance.

## 3. Sourcing Models

Three tiers, in order of effort:

**Tier 1 — Convert what already exists.** The `forrester-demos` module has 16+ coded demos (exponential growth, SIR, predator-prey, inventory oscillation, software project, etc.). Build each programmatically, serialize to JSON via `ModelDefinitionSerializer`, add view layout via `AutoLayout`. These are already Forrester's IP — no licensing issues.

**Tier 2 — Import from test resources.** The Vensim `.mdl` and XMILE `.xmile` test files (teacup, SIR) can be imported and saved as JSON. These are simple standard models.

**Tier 3 — Source from open repositories.** Classic models that are mathematical common knowledge (Lotka-Volterra, Bass diffusion, Meadows' bathtub, World3 simplified, logistic growth, Goodwin business cycle). The *model structures* are published science — not copyrightable. The specific *implementations* need to be original Forrester JSON, which they would be since you're building them in your own tool.

**Licensing approach:** For each model, distinguish between:
- The mathematical model (public domain / published science)
- The implementation (original work, your license)
- Any adapted source file (credit the source, note the original license)

## 4. Categories

Based on standard SD curriculum and what the demos already cover:

| Category | Models | Source |
|---|---|---|
| **introductory** | Exponential growth, exponential decay, bathtub, goal-seeking, S-curve, teacup cooling | Demos + Meadows |
| **epidemiology** | SIR basic, SIR with vaccination, SIR multi-region | Demos |
| **ecology** | Predator-prey (Lotka-Volterra) | Demos |
| **economics** | Bass diffusion, Goodwin cycle | Standard models |
| **supply-chain** | Inventory oscillation, beer game simplified | Demos + Sterman |
| **project-management** | Software project dynamics | Demos |
| **climate** | Simple carbon cycle, temperature feedback | Standard models |
| **population** | Logistic growth, age-structured population | Demos (region-age) |

Start with 10-15 models. Grow over time.

## 5. App Integration

Add a **"File > Open Example..."** menu item (with `Ctrl+Shift+O` or similar) that opens a dialog:

```
┌─ Example Models ──────────────────────────────────────┐
│                                                        │
│  Categories          Model                             │
│  ┌──────────────┐   ┌───────────────────────────────┐ │
│  │▸ All          │   │ ● SIR Epidemic Model          │ │
│  │  Introductory │   │   Susceptible-Infected-       │ │
│  │▸ Epidemiology │   │   Recovered compartmental     │ │
│  │  Ecology      │   │   model. 3 stocks, 2 flows.   │ │
│  │  Economics    │   │                               │ │
│  │  Supply Chain │   │   Source: Kermack & McKendrick │ │
│  │  Climate      │   │   License: Public domain      │ │
│  │  ...          │   │                               │ │
│  └──────────────┘   └───────────────────────────────┘ │
│                                                        │
│                              [ Cancel ]  [ Open ]      │
└────────────────────────────────────────────────────────┘
```

When the user clicks Open, load the JSON from classpath resources into the editor as an unsaved copy (no file path set — forces Save As if they want to modify it). This protects the bundled originals.

## 6. Implementation Order

1. **Create the catalog schema and a `ModelCatalog` class** in the engine module that reads `catalog.json` from the classpath and returns model metadata
2. **Generate the first batch of JSON models** — write a one-time conversion tool (or test) that runs each demo model, serializes to JSON, and writes to the resources folder
3. **Build the `ExampleModelsDialog`** in the app module — JavaFX dialog that reads the catalog, shows categorized list, and loads the selected model
4. **Wire into the File menu** — add "Open Example..." menu item that opens the dialog
5. **Populate remaining models** — fill out categories with 10-15 total models

The heaviest work is step 2 (building and laying out the models). The catalog infrastructure and dialog are straightforward.
