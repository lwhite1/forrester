# Stock and Flow Diagram Rules

*Rendering, Layout, and Connection Specification*

---

## 1. Element Types and Shapes

### Stock (Reservoir)

- **Shape:** Rounded rectangle (8px corner radius)
- **Dimensions:** 140w x 80h (resizable, min 80w x 45h). Default dimensions are used during the initial layout run; subsequent user resizes are handled manually and do not trigger re-layout.
- **Border:** 3px solid `#2C3E50`
- **Fill:** White
- **Label:** Centered name in bold 13pt, truncated with ellipsis (`…`) if it exceeds (width - 12)px; unit badge in 9pt at bottom-right

### Flow (Rate / Valve)

- **Shape:** Diamond (rotated square, i.e. a rhombus). Bounding box is 30 x 30.
- **Border:** 1.5px solid `#7F8C8D`
- **Fill:** White
- **Label:** Name 4px below the diamond bounding box in 11pt, truncated with ellipsis if it exceeds 100px. Equations are not displayed on the canvas; they are shown in a tooltip on hover (see Section 4).

### Auxiliary Variable

- **Shape:** Rounded rectangle (6px corner radius)
- **Dimensions:** 100w x 55h (resizable, min 60w x 35h). Default dimensions are used during the initial layout run.
- **Border:** 1.5px solid `#7F8C8D`
- **Fill:** White
- **Badge:** "fx" in 9pt gray at top-left
- **Label:** Centered name in 12pt (offset -6px from center), truncated with ellipsis if it exceeds (width - 20)px. Equations are not displayed on the canvas.

### Constant

- **Shape:** Rounded rectangle (4px corner radius) with dashed border
- **Dimensions:** 90w x 45h (resizable, min 50w x 30h). Default dimensions are used during the initial layout run.
- **Border:** 1px dashed `#BDC3C7` (6px dash, 4px gap)
- **Fill:** White
- **Badge:** "pin" in 9pt gray at top-left
- **Label:** Centered name in 11pt (offset -6px), truncated with ellipsis if it exceeds (width - 16)px. Values are not displayed on the canvas.

### Lookup Table

- **Shape:** Rounded rectangle (4px corner radius) with dot-dash border
- **Dimensions:** 100w x 50h (resizable, min 60w x 35h). Default dimensions are used during the initial layout run.
- **Border:** 1.5px dot-dash `#7F8C8D` (8/3/2/3 pattern)
- **Fill:** White
- **Badge:** "tbl" in 9pt gray at top-left
- **Label:** Centered name in 11pt (offset -6px), truncated with ellipsis if it exceeds (width - 16)px. Data point count is not displayed on the canvas.

### Module

- **Shape:** Rounded rectangle (6px corner radius)
- **Dimensions:** 120w x 70h (resizable, min 70w x 45h). Default dimensions are used during the initial layout run.
- **Border:** 2px solid `#2C3E50`
- **Fill:** White
- **Badge:** "mod" in 9pt gray at top-left
- **Label:** Centered name in bold 13pt, truncated with ellipsis if it exceeds (width - 12)px

---

## 2. Connection Types

### Material Flow (Pipe)

Material flows are the thick pipes that carry material between stocks through flow valves.

- **Line width:** 4px solid
- **Color:** `#2C3E50`
- **Path:** Source endpoint -> flow diamond center -> sink endpoint (two straight segments). When stocks and flows share the same vertical position these segments are collinear; the path is effectively a straight horizontal line passing through the diamond. The two-segment structure is retained for correctness in non-standard configurations.
- **Arrowhead:** Filled triangle at the sink end (14px long, 10px wide). See Section 7 for exact termination geometry.
- **Clipping:** Lines are clipped to the border of connected elements. Use rectangular clipping for stocks; use rhombus clipping for flow diamonds -- see Section 8.

### Info Link (Dependency Arrow)

Info links show formula dependencies (which variables influence which).

- **Line width:** 1px
- **Style:** Dashed (5px dash, 4px gap)
- **Color:** `#7F8C8D` at 60% opacity
- **Path:** Direct line from source element to target element
- **Arrowhead:** Small filled triangle at the target end (8px long, 6px wide). See Section 7.
- **Clipping:** Clipped to element borders on both ends. The source end has no arrowhead; clip normally. The target end has an arrowhead; apply arrowhead-aware termination (Section 7).

### Cloud Symbol (Boundary Source/Sink)

Clouds represent material flowing in from or out to the boundary of the model.

- **Shape:** Circle outline with "~" inside
- **Radius:** 12px
- **Line width:** 1.5px
- **Color:** `#BDC3C7`

**Positioning:**

- If the opposite endpoint is connected to a stock, place the cloud 80px from the diamond in the direction away from that stock.
- If neither endpoint is connected, source clouds default to 80px left of the diamond and sink clouds 80px right.

---

## 3. Layout Rules

### Overview: Flat Graph with ELK

Layout is delegated to the Eclipse Layout Kernel (ELK) 0.9.1 layered (Sugiyama) algorithm. **Layout is a one-shot operation.** It runs once at diagram creation or model import, on the complete graph, with all nodes and edges present. It does not run again in response to user edits such as resizing elements or adding connections — those are accommodated by the user repositioning elements manually. Connector paths (pipes and info links) are recomputed dynamically as elements move, but node positions are not.

ELK is run with **Direction: RIGHT** (left-to-right) on a **single flat graph** — all element types (stocks, flows, auxiliaries, constants, lookup tables, modules) are placed in one graph, and their positions are determined entirely by their topological relationships. There are no compound nodes or vertical bands segregating elements by type.

### Layout Pipeline

The layout pipeline runs these steps in order:

1. **Build the flat ELK graph** with all elements as nodes and all edges (material + info link).
2. **Compute material flow chain order** via BFS to establish initial X positions for INTERACTIVE cycle breaking.
3. **Assign non-chain node positions** (constants, auxiliaries, etc.) one step before their earliest consumer in the chain — may produce negative values.
4. **Identify back-edges** within SCCs by comparing chain order (higher → lower = back-edge).
5. **Run ELK** with INTERACTIVE cycle breaking, which respects the pre-marked feedback edges.
6. **Convert coordinates** from ELK's top-left to center coordinates.
7. **Align flows with stocks** (post-processing) so flow pipes run horizontally.
8. **Detect and reposition back-flows** (sink.x < source.x) below the main chain.
9. **Resolve overlaps** by nudging overlapping element pairs apart.

### Material Flow Chain Order (Step 2)

Before invoking ELK, a BFS computes a left-to-right ordering for all elements reachable via material flow edges (source stock → flow → sink stock). Source nodes (those with no incoming material edges) seed the BFS. If no source exists (pure cycle), the first stock in definition order is used. Each node's order is `max(predecessors) + 1`.

This order is set as the initial X position (`order * 200px`) on each ELK node. With `CycleBreakingStrategy.INTERACTIVE`, ELK respects these positions and preserves the natural left-to-right stock-flow ordering without independently reversing edges.

### Non-Chain Node Positioning (Step 3)

Elements not in the material flow chain (constants, auxiliaries, lookup tables that don't appear as flow sources or sinks) are assigned a chain order of `minConsumerOrder - 1`, where `minConsumerOrder` is the lowest chain order among the node's dependents (nodes that reference it in their equations). This may produce negative values, which is intentional — it places inputs to the left of their consumers. Nodes with no consumers default to order 0.

### Feedback Cycle Handling

#### Port Constraints on Stocks (All Stocks)

Every stock node has `PortConstraints.FIXED_SIDE` with two ports:
- **WEST port:** All inflow material-flow edges attach here (left face).
- **EAST port:** All outflow material-flow edges attach here (right face).

This applies unconditionally to all stocks, not only those in cycles. It ensures inflows always enter from the left and outflows exit from the right.

#### Back-Edge Identification (Step 4)

Back-edges are identified within SCCs using the material flow chain order computed in Step 2. For each SCC (found via `DependencyGraph.findSCCs()`), any edge where the source has a higher chain order than the target is marked as a back-edge (`feedbackEdge: true` in ELK). This is simpler and more reliable than DFS-based back-edge detection, which can misidentify forward material-flow edges as back-edges.

#### Post-Layout Flow-Stock Alignment (Step 7)

After ELK assigns positions, each flow's Y coordinate is adjusted to match its connected stock(s):
- **Transfer flows** (source + sink): align with the closer stock by X distance.
- **Source-only or sink-only flows**: align with the connected stock.

This ensures flow pipes run horizontally through the flow diamond.

#### Back-Flow Repositioning (Step 8)

Flows where the sink stock is to the left of the source stock (sink.x < source.x) are "back-flows" — they represent material cycling back. These are repositioned:
- **X:** midpoint between source and sink stocks
- **Y:** `maxStockY + 120px` (below the main stock chain)

This creates a visual loop where the back-flow drops below the main chain and connects back to the upstream stock.

### Horizontal Ordering

Two types of graph edges drive ELK's crossing minimisation and layer assignment:

| Edge Type | Priority | Source | Description |
|---|---|---|---|
| Material flow | 10 (high) | `FlowDef.source` / `FlowDef.sink` | Stock-to-flow and flow-to-stock connections form the main left-to-right chain |
| Info link | 1 (low) | `DependencyGraph.allEdges()` | Formula dependencies (excluding stock-flow connections already represented by material edges) |

The high-priority material edges ensure the stock-flow chain dominates ordering: inflows appear to the left of their sink stock, outflows to the right of their source stock, and transfer flows between their source and sink stocks.

### Overlap Resolution (Step 9)

After all post-processing, a pairwise overlap resolver runs up to 3 passes. For each overlapping pair (bounding boxes within 20px), elements are nudged apart symmetrically along the axis with less overlap. This prevents post-layout adjustments (flow alignment, back-flow repositioning) from creating new overlaps.

### Spacing Parameters

| Parameter | Value |
|---|---|
| Graph padding (all sides) | 100px |
| Node-to-node spacing (`elk.spacing.nodeNode`) | 60px |
| Edge-to-node spacing (`elk.spacing.edgeNode`) | 30px |
| Minimum gap enforced by overlap resolver | 20px |

### Coordinate System

ELK outputs top-left corner positions. All internal element placements use center coordinates `(cx, cy)`. Convert immediately after ELK returns results:

```
cx = x + width / 2
cy = y + height / 2
```

---

## 4. Label Display and Tooltips

### Label Truncation

All element names are truncated with an ellipsis character (`…`) when they exceed the available space. Truncation is font-aware — it uses a JavaFX `Text` node to measure the pixel width of the candidate string at the element's font size, and removes characters from the end until the truncated string plus ellipsis fits within the maximum width.

Maximum label widths per element type:

| Element | Max width | Font |
|---|---|---|
| Stock | element width - 12px | Bold 13pt |
| Auxiliary | element width - 20px | Normal 12pt |
| Constant | element width - 16px | Normal 11pt |
| Lookup | element width - 16px | Normal 11pt |
| Module | element width - 12px | Bold 13pt |
| Flow | 100px (fixed) | Normal 11pt |

### Equations and Values Hidden

Equations, constant values, and lookup data point counts are **not displayed** on the canvas. This prevents label overlap and visual clutter, especially on small elements and in dense diagrams.

### Hover Tooltips

When the mouse hovers over an element, a tooltip appears after 200ms (hides after 8s) showing:
- **Full element name** (untruncated)
- **Equation, value, or description** depending on element type:
  - Flows/Auxiliaries: equation string
  - Constants: numeric value with unit
  - Stocks: initial value with unit
  - Lookup tables: data point count
  - Modules: module type name

Tooltips are styled with 12pt font.

---

## 5. Dependency Graph Rules

- **Formula references:** If element A's formula references element B, an edge is created from B -> A (B influences A).
- **Flow-stock connections:** Flows have edges to their source and sink stocks (representing material movement).
- **Material edge exclusion:** When building info link edges for ELK, any dependency edge that duplicates a stock-flow material connection (in either direction) is excluded, to avoid conflicting layout hints.
- **Cycle membership:** After SCC detection, every edge that belongs to a cycle is tagged with its SCC identifier. This tag is used during rendering to determine loop participation for the feedback loop overlay.
- **Link polarity:** Every dependency edge may carry a `linkPolarity` annotation with value `+1`, `-1`, or `0` (unknown/unset). Loop polarity is the product of all edge polarities in the cycle: `+1` = reinforcing, `-1` = balancing, any `0` = unknown ("?"). The annotation defaults to `0` when the equation engine cannot compute it. Users may override the annotation manually.

---

## 6. Color Palette

| Role | Hex | Alpha | Usage |
|---|---|---|---|
| Stock border / material flow / primary text | `#2C3E50` | 1.0 | Stocks, modules, flow pipes, all primary text |
| Auxiliary / info link border | `#7F8C8D` | 1.0 | Aux borders, lookup borders, flow diamond borders |
| Constant border / cloud | `#BDC3C7` | 1.0 | Constant dashed borders, cloud symbols |
| Info link | `#7F8C8D` | 0.6 | Dashed dependency arrows |
| Secondary text | `#7F8C8D` | 1.0 | Equations, values, badges |
| Canvas background | `#F8F9FA` | 1.0 | Diagram background |
| Element fill | `#FFFFFF` | 1.0 | All element interiors |
| Hover | `#4A90D9` | 0.4 | Hover highlighting |
| Same-direction flow | `#4A90D9` | 1.0 | Flow connection indicator (blue) |
| Opposite-direction flow | `#D97B4A` | 1.0 | Flow connection indicator (orange) |

### Feedback Loop Colors

The current implementation uses a single color for all feedback loop highlighting:

| Role | Color | Alpha |
|---|---|---|
| Loop participant highlight | `#E74C3C` | 0.8 |
| Loop edge highlight | `#E74C3C` | 0.6 |
| Loop fill | `#E74C3C` | 0.08 |

**Future:** Per-SCC coloring with a 6-color palette (see Section 9).

---

## 7. Rendering Order (Back to Front)

1. Canvas background (`#F8F9FA`)
2. Material flow pipes (with clouds and arrowheads)
3. Info link dashed lines (with arrowheads)
4. Feedback loop edge highlights (if active)
5. All elements (stocks, flows, auxiliaries, constants, modules, lookups)
6. Feedback loop participant highlights (if active)
7. Connection highlights (selected or hovered)
8. Element hover indicators
9. Selection indicators
10. Pending flow creation rubber-band line
11. Flow endpoint reattachment rubber-band line
12. Connection reroute rubber-band line
13. Marquee selection rectangle

---

## 8. Arrowhead Geometry and Line Termination

This section defines both the arrowhead shape and where the line body must end when an arrowhead is present. The correct approach terminates the line body at the arrowhead base, not at the element border.

### Arrowhead Dimensions

| Connection type | Length | Width |
|---|---|---|
| Material flow | 14px | 10px |
| Info link | 8px | 6px |

### Termination Sequence (Arrowhead End)

Apply this sequence at every line end that carries an arrowhead (the target end). The source end of info links and the non-arrowhead ends of pipes are clipped normally without this adjustment.

1. Compute the border intersection point **P** by clipping the line from source center to target element center against the target element's border. Use rectangular clipping for stocks, auxiliaries, constants, lookup tables, and modules. Use rhombus clipping for flow diamonds (Section 9). **P** is where the line would touch the element border with no arrowhead.
2. Place the **arrowhead tip at P**. Do not move it inward.
3. Compute the unit direction vector **D** pointing from source toward P.
4. **The line body ends at L = P - D × arrowheadLength.** This is the arrowhead base center. Draw the line from its source to **L**. It must not continue past **L** toward **P**.
5. Compute the perpendicular vector **Q = (-Dy, Dx)** (D rotated 90 degrees).
6. Draw the filled arrowhead triangle with vertices: tip **P**, left base **L + Q × (width/2)**, right base **L - Q × (width/2)**.

**Summary:** line ends at L; arrowhead occupies the gap L -> P; arrowhead tip P touches the element border.

### Source End (No Arrowhead)

At the source end of info links, clip the line to the source element border using the appropriate formula (Section 9). No arrowhead-length adjustment is applied.

---

## 9. Line Clipping to Element Borders

### Rectangular Clipping (Stocks, Auxiliaries, Constants, Lookups, Modules)

1. Compute direction vector `(dx, dy)` from element center to the external point.
2. Compute `scale = min(halfWidth / |dx|, halfHeight / |dy|)`, guarding against `dx = 0` or `dy = 0`.
3. Clipped point = `center + (dx, dy) × scale`.

### Rhombus Clipping (Flow Diamond)

The flow diamond is a rhombus with half-diagonals `hw = 15` and `hh = 15` (half of the 30 × 30 bounding box). The rhombus boundary satisfies `|x/hw| + |y/hh| = 1`. For a ray from center in direction `(dx, dy)`:

```
scale = 1 / (|dx| / hw + |dy| / hh)
```

Clipped point = `(cx + dx × scale, cy + dy × scale)`.

Guard against `dx = 0` and `dy = 0` as with rectangular clipping. This formula must be used in place of rectangular clipping whenever the connected element is a flow diamond — including material flow pipes and info links targeting or departing from a flow.

---

## 10. Hit Testing Zones

- **Cloud endpoints:** 18px radius from cloud center
- **Connected flow endpoints:** 14px radius
- **Flow diamond (for creation):** 55px half-width, 35px half-height rectangular zone

---

## 11. Font Conventions

All fonts use the "System" family (platform default sans-serif).

| Context | Size | Weight |
|---|---|---|
| Stock name | 13pt | Bold |
| Module name | 13pt | Bold |
| Auxiliary name | 12pt | Normal |
| Flow name | 11pt | Normal |
| Constant name | 11pt | Normal |
| Lookup name | 11pt | Normal |
| All badges and secondary labels | 9pt | Normal |

---

## 12. Feedback Loop Analysis

### Detection Algorithm

Feedback loop detection operates at the stock-to-stock level:

1. **Build a stock-to-stock causal graph:** An edge from stock X to stock Y exists when a flow that affects Y (as source or sink) has an equation that depends on X (directly or transitively through auxiliaries), and X ≠ Y. Transitive resolution follows auxiliary equation chains but stops at stocks.
2. **Find SCCs of size ≥ 2** using Tarjan's algorithm. Each SCC represents one or more interacting feedback loops between distinct stocks. Single-stock growth/drain loops are not flagged.
3. **Identify participating flows:** A flow participates in a loop if it creates a causal edge between two different stocks in the same SCC.

### Rendering

When loop highlighting is active (toggled via a menu item), participants are highlighted:
- **Element highlights:** A colored glow/outline (2.5px, `#E74C3C` at 0.8 alpha) with subtle fill (0.08 alpha) is drawn around each participating element. Flow diamonds get a diamond-shaped highlight; all others get a rectangular highlight with 6px padding.
- **Edge highlights:** A thicker colored line (2.5px, `#E74C3C` at 0.6 alpha) is drawn behind each info link and material flow edge that connects loop participants.

---

## 13. Not Yet Implemented

The following items from the original spec are not yet implemented:

- **Compound node (band) layout:** The spec originally called for three compound nodes (`band-aux`, `band-main`, `band-lower`) to segregate element types vertically. The current implementation uses a single flat graph where topology drives positioning. This produces better results for small-to-medium models but may need revisiting for very large models.
- **Per-SCC loop colors:** The spec defined a 6-color palette for distinguishing multiple SCCs. Currently all loops use a single red color. The `FeedbackAnalysis` already returns `loopGroups` (per-SCC groupings), so the renderer could be extended to assign distinct colors.
- **Convex hull loop overlay:** The spec called for drawing convex hulls around SCC participants with loop polarity labels at the centroid. Currently, individual element highlights are drawn instead.
- **Label collision resolution:** The spec defined a post-layout label nudging pass. Currently, equations/values are hidden entirely and names are truncated, which prevents most collisions. A nudging pass could be added for flow name labels that extend below their diamonds.
- **Cloud collision fallback:** The spec defined a multi-step fallback sequence for placing clouds that collide with elements (try 80/120/160/200px, then perpendicular directions). Currently clouds are placed at a fixed 80px offset.
