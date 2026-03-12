# Vensim `.mdl` Import

`VensimImporter` reads Vensim `.mdl` model files and produces a `ModelDefinition` that can be compiled and simulated.

## Usage

```java
VensimImporter importer = new VensimImporter();
ImportResult result = importer.importModel(Path.of("model.mdl"));
if (!result.isClean()) {
    result.warnings().forEach(System.out::println);
}
ModelDefinition def = result.definition();

// Compile and run
CompiledModel compiled = new ModelCompiler().compile(def);
Simulation sim = compiled.createSimulation();
sim.execute();
```

In the visual editor, use File → Open to load `.mdl` files directly.

## Supported Constructs

| Vensim Construct | Shrewd Element |
|---|---|
| `INTEG(rate, init)` | `StockDef` + `FlowDef` |
| Numeric literal | `ConstantDef` |
| Unchangeable (`==`) | `ConstantDef` |
| Expression (`=`) | `AuxDef` |
| Standalone lookup table | `LookupTableDef` |
| `WITH LOOKUP(input, data)` | `AuxDef` + extracted `LookupTableDef` |
| Subscript range (`:`) | `SubscriptDef` |
| `IF THEN ELSE` | `IF` |
| `XIDZ` / `ZIDZ` | `IF` with division guards (also available as native `XIDZ`/`ZIDZ` functions) |
| `SMOOTH3` / `SMOOTH3I` | `SMOOTH3` / `SMOOTH3I` (native third-order smoothing) |
| `DELAY1` / `DELAY1I` | `DELAY1` / `DELAY1I` (native first-order delay) |
| `SMOOTHI` | `SMOOTHI` (first-order smoothing with explicit initial) |
| `DELAY_FIXED` | `DELAY_FIXED` (exact pipeline delay) |
| `PULSE` / `PULSE TRAIN` | `PULSE` / `PULSE_TRAIN` |
| `RANDOM NORMAL` / `RANDOM UNIFORM` | `RANDOM_NORMAL` / `RANDOM_UNIFORM` |
| `:AND:` / `:OR:` / `:NOT:` | `&&` / `\|\|` / `!()` |
| Sketch section | `ViewDef` with element placements and connectors |
| Simulation settings | `SimulationSettings` (INITIAL TIME, FINAL TIME, TIME STEP) |

## Limitations

Unsupported constructs (macros, data variables, DELAY N, GET XLS DATA, etc.) emit warnings rather than failing.

## Key Classes

| Class | Purpose |
|---|---|
| `VensimImporter` | Main entry point implementing `ModelImporter` |
| `MdlParser` | Low-level `.mdl` file parser (equations + sketch extraction) |
| `VensimExprTranslator` | Expression syntax translation (Vensim → Shrewd) |
| `SketchParser` | Sketch section → `ViewDef` records |
