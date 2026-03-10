# XMILE Import & Export

`XmileImporter` reads XMILE XML files (the OASIS standard format used by Stella/iThink) and produces a `ModelDefinition`. `XmileExporter` writes any `ModelDefinition` to valid XMILE 1.0 XML. Together they enable bidirectional model exchange with the Stella/iThink ecosystem.

## Usage

### Import

```java
XmileImporter importer = new XmileImporter();
ImportResult result = importer.importModel(Path.of("model.xmile"));
if (!result.isClean()) {
    result.warnings().forEach(System.out::println);
}
ModelDefinition def = result.definition();

// Compile and run
CompiledModel compiled = new ModelCompiler().compile(def);
Simulation sim = compiled.createSimulation();
sim.execute();
```

### Export

```java
String xml = XmileExporter.toXmile(modelDefinition);
XmileExporter.toFile(modelDefinition, Path.of("model.xmile"));
```

In the visual editor, use File → Open to load `.xmile` files and File → Export XMILE to export.

## Supported Constructs (Import)

| XMILE Construct | Forrester Element |
|---|---|
| `<stock>` with `<eqn>` | `StockDef` (eqn = initial value) |
| `<flow>` with `<eqn>` | `FlowDef` (source/sink from stock `<inflow>`/`<outflow>`) |
| `<aux>` with numeric `<eqn>` | `ConstantDef` |
| `<aux>` with expression `<eqn>` | `AuxDef` |
| `<aux>` or `<flow>` with `<gf>` | `LookupTableDef` + `AuxDef` |
| `<sim_specs>` | `SimulationSettings` (start, stop, dt, time_units) |
| `<views>` / `<view>` | `ViewDef` with element placements and connectors |
| `IF_THEN_ELSE` | `IF` |
| `AND` / `OR` / `NOT` | `&&` / `\|\|` / `!` |
| `=` / `<>` (comparison) | `==` / `!=` |
| `SMTH3` / `SMTH1` | `SMOOTH` (with approximation warning) |
| `Time` | `TIME` |
| `<non_negative>` | `NegativeValuePolicy.CLAMP_TO_ZERO` |

## Import Limitations

- Non-literal stock initial values default to 0 (with warning)
- Start time offset and dt value are not preserved in `SimulationSettings`
- Unsupported elements (`<group>`, `<module>`, `<macro>`, arrays, vendor-specific extensions) emit warnings rather than failing

## Export Limitations

- Constants are exported as `<aux>` with numeric equations (re-imported correctly)
- Start time is always 0
- Subscripts and module instances are not exported

## Key Classes

| Class | Purpose |
|---|---|
| `XmileImporter` | Main entry point implementing `ModelImporter` |
| `XmileExporter` | Static methods: `toXmile(def)`, `toFile(def, path)` |
| `XmileExprTranslator` | Bidirectional expression syntax translation |
| `XmileViewParser` | View XML → `ViewDef` records |
| `XmileViewWriter` | `ViewDef` records → view XML |
