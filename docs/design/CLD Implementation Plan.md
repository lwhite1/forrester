# Causal Loop Diagram (CLD) Implementation Plan

## Motivation

CLD support addresses three gaps:

1. **Market expectation.** Every competing SD tool (Vensim, Stella, Insight Maker) offers CLD. Without it, Shrewd lacks the entry point that most SD practitioners use to begin modeling.
2. **Pedagogical workflow.** SD education starts with CLDs. Students sketch causal structure qualitatively before formalizing into stock-and-flow. A tool that only supports S&F forces users to start at the hard end.
3. **Design 4 alignment.** The Design 4 spec describes "unclassified variables" rendered in "CLD style" that morph into S&F elements. CLD support makes this morphing meaningful — the starting point is a real CLD with loop analysis, not just generic boxes.

## Design Decisions

### Unified canvas, not a separate mode

CLDs and S&F diagrams share a single canvas and a single `ModelDefinition`. A CLD variable is an element type alongside stocks, flows, and auxiliaries — not a separate document. This enables:

- Mixed diagrams during the conceptualization-to-formalization transition
- CLD variables that coexist with already-formalized S&F elements
- A single undo stack, single file format, single serialization path
- The Design 4 morphing workflow: classify a CLD variable as a stock, and it changes shape on the same canvas

### CLD variables are a new element type, not reused auxiliaries

Auxiliaries have equations and units. CLD variables are qualitative — they have a name and optionally a description, but no equation. Reusing AuxDef would require making equations optional across the board, breaking validation assumptions. A dedicated `CldVariableDef` keeps the type system honest.

### Causal links carry polarity, not equations

A CLD link says "A influences B positively (+) or negatively (-)". This is distinct from an info link (which is derived from equation parsing) and from a material flow (which carries substance). CLD links are a third connection type with their own rendering and their own storage.

### Loop analysis is automatic

When the user draws CLD variables and links, the system automatically detects feedback loops, classifies them as reinforcing (R) or balancing (B) by counting negative-polarity links, and displays loop labels. This is the core analytical value of a CLD.

---

## Data Model

### New records (shrewd-engine, model/def/)

```java
// A qualitative variable in a causal loop diagram
public record CldVariableDef(
    String name,
    String comment       // optional description
) {}

// A causal link with polarity between two variables
public record CausalLinkDef(
    String from,         // source variable name
    String to,           // target variable name
    Polarity polarity,   // POSITIVE, NEGATIVE, or UNKNOWN
    String comment       // optional annotation (e.g., "after a delay")
) {
    public enum Polarity { POSITIVE, NEGATIVE, UNKNOWN }
}
```

### ModelDefinition changes

Add two new lists to the `ModelDefinition` record:

```java
List<CldVariableDef> cldVariables,
List<CausalLinkDef> causalLinks
```

These sit alongside the existing stocks, flows, auxiliaries, etc. A model can contain both CLD variables and S&F elements simultaneously.

### ElementPlacement changes

Add `CLD_VARIABLE` to the `ElementType` enum so CLD variables can be positioned and persisted in `ViewDef`.

### ModelEditor changes

Add mutation methods: `addCldVariable()`, `removeCldVariable()`, `renameCldVariable()`, `addCausalLink()`, `removeCausalLink()`, `setCausalLinkPolarity()`.

---

## Graph Analysis

### CldGraph (new class, model/graph/)

Builds a directed graph from `CausalLinkDef` entries. Provides:

- `findLoops()` — SCC detection via Tarjan's (reuse pattern from `DependencyGraph.findSCCs()`)
- `classifyLoop(Set<String> scc)` — count negative-polarity links in the cycle; odd count = balancing (B), even count = reinforcing (R)
- `loopSignature(Set<String> scc)` — canonical loop name for display (e.g., "R1", "B2")

### Integration with FeedbackAnalysis

`FeedbackAnalysis` currently operates on stock-to-stock causal paths derived from equations. For CLD variables, loop detection operates on explicit causal links instead. The two analyses are complementary:

- Pure CLD models: loop analysis from `CldGraph`
- Pure S&F models: loop analysis from `FeedbackAnalysis` (existing)
- Mixed models: both run; loops can span CLD and S&F elements if causal links connect them

---

## Canvas Rendering

### CLD variable shape

- Rounded rectangle with thin border (1px solid `#7F8C8D`)
- Dimensions: 110w x 50h (resizable, min 60w x 35h)
- Fill: white
- Label: centered name in 12pt
- No badge, no equation, no value — visually lighter than all S&F elements

This matches the "unclassified variable (CLD style)" described in the Design 4 spec, Section 6.1.

### Causal link rendering

- Line width: 1.5px solid
- Color: `#2C3E50`
- Curved path (quadratic bezier with auto-computed control point to avoid straight lines that overlap elements)
- Arrowhead: filled triangle at target (10px long, 7px wide)
- Polarity label: "+" or "−" rendered near the arrowhead in bold 12pt
  - Positive: `#27AE60` (green)
  - Negative: `#E74C3C` (red)
  - Unknown: `#BDC3C7` (gray), shown as "?"

### Loop overlay

- Convex hull of loop participants, filled with semi-transparent color
- Loop label ("R1", "B1", etc.) centered in the hull
- Reinforcing: blue tint (`#4A90D9` at 0.08 fill, 0.6 border)
- Balancing: orange tint (`#D97B4A` at 0.08 fill, 0.6 border)
- Toggle visibility via View menu

---

## Interaction

### Creating CLD elements

- New toolbar tool: "Place CLD Variable" (key: 8)
- Click canvas to place; element gets auto-name "Variable N"
- Double-click to rename inline (same as existing elements)

### Creating causal links

- New toolbar tool: "Draw Causal Link" (key: 9)
- Click source variable, drag to target variable
- On release, link created with polarity UNKNOWN
- Click the polarity label ("?") to cycle: ? → + → − → ?
- Or right-click link → Set Polarity submenu

### Classifying CLD variables (the morphing step)

- Right-click a CLD variable → "Classify as..." submenu:
  - Stock, Flow, Auxiliary, Constant, Lookup Table
- Classification creates the corresponding S&F element with the same name and position
- Removes the `CldVariableDef`
- Causal links from/to the classified variable become info links (or material flows if classified as a flow connected to stocks)
- This is the Design 4 morphing operation made concrete

### Promoting an entire CLD to S&F

- Menu action: "Formalize CLD → Stock and Flow" (in Edit or a new Refactor menu)
- Heuristic classification:
  - Variables with only inbound causal links and high connectivity → likely stocks
  - Variables that connect two stocks → likely flows
  - Variables with few connections → likely constants or auxiliaries
- Present suggestions in a dialog; user confirms or overrides each
- With AI enabled, the LLM can make better classification suggestions using domain knowledge from the conversation

---

## Serialization

### JSON

Add `cldVariables` and `causalLinks` arrays to the existing JSON schema. Backward compatible — old files without these fields deserialize to empty lists.

### XMILE

XMILE doesn't have a standard CLD representation. Options:
- Store CLD data in an `<options>` extension element (vendor-specific)
- Or skip CLD in XMILE export (export only S&F elements, warn if CLD variables exist)

### Vensim .mdl

Vensim separates CLD and S&F. CLDs are stored in sketch sections with arrow polarity metadata. The existing `SketchParser` could be extended to detect CLD-only sketches and import them as `CldVariableDef` + `CausalLinkDef`.

---

## Layout

ELK handles CLD layout using the same layered algorithm:

- CLD variables are ELK nodes with their dimensions
- Causal links are ELK edges
- No layer constraints (no band convention for CLDs — all variables are peers)
- Cycle breaking uses the same INTERACTIVE strategy with back-edge marking
- For pure CLD diagrams, consider using `elk.force` (force-directed) instead of `elk.layered`, since CLDs are typically circular/radial rather than hierarchical

Add a layout strategy selector:
- S&F models: `elk.layered` (existing)
- CLD models: `elk.stress` or `elk.force` (better for cyclic graphs)
- Mixed models: `elk.layered` (S&F structure dominates)

---

## Phased Implementation

### Phase 1: Data model and persistence

**Goal:** CLD variables and causal links exist in the model, serialize correctly, round-trip through JSON.

**Work:**
1. Create `CldVariableDef` and `CausalLinkDef` records in `model/def/`
2. Add `CLD_VARIABLE` to `ElementType` enum
3. Add `cldVariables` and `causalLinks` lists to `ModelDefinition`
4. Update `ModelDefinitionBuilder` with `cldVariable()` and `causalLink()` methods
5. Update `ModelDefinitionSerializer` (JSON) to read/write the new fields
6. Update `ModelEditor` with add/remove/rename methods for CLD elements
7. Write unit tests: round-trip serialization, builder usage, editor mutations

**No UI changes.** This phase is engine-only.

**Estimated scope:** ~8 files changed/created, ~15 tests.

### Phase 2: Canvas rendering and basic interaction

**Goal:** Users can place CLD variables and draw causal links on the canvas.

**Work:**
1. Add `drawCldVariable()` to `ElementRenderer`
2. Add causal link rendering to `ConnectionRenderer` (curved lines with polarity labels)
3. Add "Place CLD Variable" tool to `CanvasToolBar` (key: 8)
4. Add "Draw Causal Link" tool to `CanvasToolBar` (key: 9)
5. Update `ModelCanvas.handleKeyPressed()` for DIGIT8 and DIGIT9
6. Implement causal link creation interaction (click-drag between variables)
7. Implement polarity cycling (click polarity label or right-click menu)
8. Update `CanvasState` to track CLD variable positions
9. Update hit-testing for CLD variables and causal links
10. Update `KeyboardShortcutsDialog` with new shortcuts

**Estimated scope:** ~6 files changed, ~2 new files, ~10 tests.

### Phase 3: Loop detection and overlay

**Goal:** Automatic feedback loop identification and visual display on CLD diagrams.

**Work:**
1. Create `CldGraph` in `model/graph/` with SCC detection and loop classification
2. Implement loop polarity calculation (count negative links)
3. Add loop overlay rendering to `CanvasRenderer` (convex hull, R/B labels)
4. Add "Show Loop Overlay" toggle to View menu
5. Update loop detection to handle mixed CLD + S&F models
6. Write tests for loop detection, polarity classification, edge cases (nested loops, shared edges)

**Estimated scope:** ~4 files changed/created, ~12 tests.

### Phase 4: Classification and morphing

**Goal:** Users can classify CLD variables into S&F element types, with causal links converting to appropriate connection types.

**Work:**
1. Add "Classify as..." context menu to CLD variables
2. Implement classification logic in `ModelEditor`:
   - Create the target S&F element (stock/flow/aux/constant/lookup)
   - Transfer name, position, and size
   - Convert causal links to info links (or material flows for flow classification)
   - Remove the `CldVariableDef` and associated `CausalLinkDef` entries
3. Add "Formalize CLD" bulk action with heuristic suggestions dialog
4. Handle edge cases: what happens to a causal link when only one end is classified?
   - Answer: it becomes a mixed-type link rendered as a causal link until both ends are classified
5. Update undo/redo to handle classification as a single undoable action
6. Write tests for all classification paths and link conversion

**Estimated scope:** ~5 files changed, ~1 new dialog, ~15 tests.

### Phase 5: Layout and polish

**Goal:** CLD diagrams lay out well automatically; import/export support.

**Work:**
1. Add CLD-aware layout strategy to `AutoLayout` (force-directed for pure CLD, layered for mixed)
2. Add CLD variable support to Vensim import (`SketchParser` extension)
3. Add CLD data to XMILE export as vendor extension (or warn on export)
4. Add CLD variables to `DiagramExporter` (PNG/SVG export)
5. Add CLD section to `SdConceptsDialog` help content
6. Add CLD variables to `ModelValidator` (warn on disconnected variables, loops with unknown polarity)
7. Polish: selection styling, hover tooltips, drag behavior, resize handles

**Estimated scope:** ~8 files changed, ~8 tests.

### Phase 6: AI integration (Design 4 alignment)

**Goal:** The LLM can create CLD variables from natural language and guide formalization.

**Work:**
1. Add CLD tool-use functions to the LLM integration layer: `add_cld_variable`, `add_causal_link`, `classify_variable`
2. Update ELICITOR posture to create CLD variables (not unclassified S&F elements) during conceptualization
3. Update FORMALIZER posture to guide classification of CLD variables into S&F types
4. The canvas morphing from Design 4 now has a concrete starting point: real CLD elements with loop analysis, not generic boxes
5. AI-assisted bulk formalization: LLM suggests stock/flow/aux classification based on domain understanding

**Estimated scope:** Depends on LLM integration timeline. This phase is blocked on the LLM layer (Design 4 Phases 5+).

---

## Dependencies and Risks

| Risk | Mitigation |
|---|---|
| Mixed CLD + S&F models create confusing diagrams | Clear visual differentiation between CLD and S&F elements; consider a "CLD layer" toggle that dims S&F elements |
| Causal link conversion during classification loses polarity information | Preserve polarity as metadata on the resulting info link; display as a tooltip |
| Force-directed layout for CLDs produces unstable results on large diagrams | Fall back to stress minimization (ELK stress); cap force-directed at ~30 nodes |
| XMILE has no CLD standard | Use vendor extension or accept that CLD data is lost on XMILE export; warn the user |
| Loop detection on large CLDs is slow | Tarjan's is O(V+E); fine for any reasonable model size. Not a real risk. |

## Success Criteria

- A user can build a CLD from scratch, see loops identified automatically, then classify variables into S&F elements and continue to simulation — all without leaving the canvas
- Existing S&F-only models are unaffected (empty CLD lists, no visual changes)
- JSON round-trip preserves all CLD data
- Vensim import can bring in CLD sketches
