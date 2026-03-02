# XMILE Import/Export Audit Report

## Scope

Audit of the 7 production files in `io/xmile/`:

| File | Lines | Purpose |
|------|-------|---------|
| `XmileConstants.java` | 80 | Namespace URIs, element/attribute names |
| `XmileExprTranslator.java` | 165 | Bidirectional expression translation |
| `XmileImporter.java` | 546 | XMILE XML → ModelDefinition |
| `XmileExporter.java` | ~485 | ModelDefinition → XMILE XML |
| `XmileViewParser.java` | 228 | Parse view XML → ViewDef |
| `XmileViewWriter.java` | 143 | Write ViewDef → view XML |
| `package-info.java` | 12 | Package documentation |

Test coverage: 65 tests across 5 test classes + 2 XMILE test resource files (teacup, SIR).

## Issues Found and Fixed

### Fixed (3 issues)

1. **Dead code in `XmileExporter`** (LOW) — `writeLookupNameForEmbedded()` was a no-op method called in the flow export loop. Removed the method and the call.

2. **Missing `Locale.ROOT` in `toLowerCase()`** (LOW) — `XmileExporter.writeSimSpecs()` and `XmileViewWriter.mapTypeToTag()` called `toLowerCase()` without `Locale.ROOT`, risking locale-dependent behavior (e.g., Turkish locale converting `I` to `ı`). Fixed both to use `Locale.ROOT`.

3. **Unused field `NOT_OP_PATTERN`** (LOW) — `XmileExprTranslator` declared a compiled `Pattern` for `!` that was never referenced (the `translateNotOperator` method uses character iteration instead). Removed the unused field.

## Known Limitations (by design, not bugs)

### Import Limitations

1. **Start time not preserved** — `SimulationSettings` stores only duration, not start time. An XMILE file with `<start>10</start><stop>110</stop>` imports as duration=100 but the start time offset is lost. On re-export, `<start>` is always 0.

2. **DT not preserved** — XMILE's `<dt>` value is noted in a warning but not stored in `SimulationSettings` (Forrester uses fixed time steps). Re-exported files always have `<dt>1</dt>`.

3. **Non-literal stock initial values default to 0** — XMILE allows expression-based initial values (e.g., `<eqn>other_stock * 0.5</eqn>`). The importer only handles numeric literals; expressions emit a warning and default to 0.0.

4. **Unsupported XMILE elements skipped with warnings:**
   - `<group>` — element grouping
   - `<module>` — submodels
   - `<macro>` — user-defined macros
   - `<event_poster>` — event triggers
   - Array/dimension constructs
   - Conveyor, queue, and oven stocks
   - Vendor-specific `isee:` namespace extensions

5. **SMTH3/SMTH1 approximated as SMOOTH** — XMILE's third-order and first-order smoothing functions are mapped to Forrester's SMOOTH (first-order), with warnings emitted about the semantic difference.

### Export Limitations

1. **Constants exported as `<aux>` with numeric equation** — XMILE has no separate `<constant>` element. On re-import, these are correctly recognized as constants (numeric literal detection).

2. **Lookup tables referenced by auxiliaries are embedded** — When an aux's equation is `LOOKUP(name, input)`, the lookup data is embedded as `<gf>` inside the `<aux>`. Standalone lookups (not referenced by any aux) are exported as `<aux>` elements with `<gf>` but no `<eqn>`.

3. **Subscripts and modules not exported** — XMILE modules and array dimensions are out of scope. Models with subscripts or module instances export only their top-level non-subscripted elements.

### Expression Translation Limitations

| XMILE → Forrester | Status |
|---|---|
| `IF_THEN_ELSE(c, t, f)` → `IF(c, t, f)` | Supported |
| `AND`/`OR`/`NOT` → `&&`/`\|\|`/`!` | Supported |
| `=` → `==`, `<>` → `!=` | Supported (preserves `<=`, `>=`, `!=`) |
| `Time` → `TIME` | Supported |
| `SMTH3`/`SMTH1` → `SMOOTH` | Supported (with approximation warning) |
| `DELAY3` | Pass-through (same name) |
| `EXP`, `LN`, `ABS`, `SQRT`, `MIN`, `MAX` | Pass-through |

| Forrester → XMILE | Status |
|---|---|
| `IF(c, t, f)` → `IF_THEN_ELSE(c, t, f)` | Supported |
| `&&`/`\|\|`/`!` → `AND`/`OR`/`NOT` | Supported |
| `==` → `=`, `!=` → `<>` | Supported |
| `TIME` → `Time` | Supported |

Not translated (no XMILE equivalent): `XIDZ`, `ZIDZ` (Vensim-specific division guards already translated to `IF` on Vensim import).

## Security

- **XXE protection**: `XmileImporter.parseXml()` disables doctype declarations and external entity resolution via `DocumentBuilderFactory` feature flags. This prevents XML External Entity attacks.
- **No user-controlled code execution**: Expression translation is purely text-based regex replacement; no eval or reflection.

## Code Quality

- All classes are `final` (utility classes) or have private constructors (preventing instantiation)
- All XML element/attribute names use `XmileConstants` — no string literals for XML names in processing code
- Namespace-aware parsing with non-namespace fallback for compatibility
- Consistent error handling: `IllegalArgumentException` caught per-element with warnings emitted
- `getChildElements()` correctly iterates direct children only (avoids cross-level name collisions)
- `getFirstChild()` uses `getElementsByTagNameNS` (descendant search) which works correctly for XMILE's fixed-depth structure

## Test Coverage

| Test Class | Tests | Coverage Area |
|---|---|---|
| `XmileExprTranslatorTest` | 30 | toForrester (18), toXmile (10), round-trip (2) |
| `XmileImporterTest` | 15 | Element parsing (6), stock-flow linkage (3), sim settings (2), teacup integration (1), SIR integration (1), expression translation (1), compile+simulate round-trip (1) |
| `XmileExporterTest` | 12 | Basic export (5), export→import round-trip (4), lookup helpers (3) |
| `XmileViewParserTest` | 4 | Placements, connectors, type resolution, default view name |
| `XmileViewWriterTest` | 4 | Write+parse-back: placements, connectors, flow routes, empty list |
| **Total** | **65** | |

## Remaining Items

None — all issues found during audit have been fixed. The implementation is suitable for the supported XMILE subset.
