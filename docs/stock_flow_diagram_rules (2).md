# Stock and Flow Diagram Rules

*Rendering, Layout, and Connection Specification*

---

## 1. Element Types and Shapes

### Stock (Reservoir)

- **Shape:** Rounded rectangle (8px corner radius)
- **Dimensions:** 140w x 80h (resizable, min 80w x 45h). Default dimensions are used during the initial layout run; subsequent user resizes are handled manually and do not trigger re-layout.
- **Border:** 3px solid `#2C3E50`
- **Fill:** White
- **Label:** Centered name in bold 13pt; unit badge in 9pt at bottom-right

### Flow (Rate / Valve)

- **Shape:** Diamond (rotated square, i.e. a rhombus). Bounding box is 30 x 30.
- **Border:** 1.5px solid `#7F8C8D`
- **Fill:** White
- **Label:** Name 4px below the diamond bounding box in 11pt; equation 18px below that in 9pt gray (hidden if zero or null). Label bounds are checked for collision -- see Section 4.

### Auxiliary Variable

- **Shape:** Rounded rectangle (6px corner radius)
- **Dimensions:** 100w x 55h (resizable, min 60w x 35h). Default dimensions are used during the initial layout run.
- **Border:** 1.5px solid `#7F8C8D`
- **Fill:** White
- **Badge:** "fx" in 9pt gray at top-left
- **Label:** Centered name in 12pt (offset -6px from center); equation in 9pt gray (offset +8px)

### Constant

- **Shape:** Rounded rectangle (4px corner radius) with dashed border
- **Dimensions:** 90w x 45h (resizable, min 50w x 30h). Default dimensions are used during the initial layout run.
- **Border:** 1px dashed `#BDC3C7` (6px dash, 4px gap)
- **Fill:** White
- **Badge:** "pin" in 9pt gray at top-left
- **Label:** Centered name in 11pt (offset -6px); value in 9pt gray (offset +8px)

### Lookup Table

- **Shape:** Rounded rectangle (4px corner radius) with dot-dash border
- **Dimensions:** 100w x 50h (resizable, min 60w x 35h). Default dimensions are used during the initial layout run.
- **Border:** 1.5px dot-dash `#7F8C8D` (8/3/2/3 pattern)
- **Fill:** White
- **Badge:** "tbl" in 9pt gray at top-left
- **Label:** Centered name in 11pt (offset -6px); data point count in 9pt gray (offset +8px)

### Module

- **Shape:** Rounded rectangle (6px corner radius)
- **Dimensions:** 120w x 70h (resizable, min 70w x 45h). Default dimensions are used during the initial layout run.
- **Border:** 2px solid `#2C3E50`
- **Fill:** White
- **Badge:** "mod" in 9pt gray at top-left
- **Label:** Centered name in bold 13pt

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
- **Collision fallback:** After computing the candidate position, test it against all element bounding boxes. If it overlaps (within 20px of any element edge), try offsets of 80px, 120px, 160px, and 200px along the same direction, choosing the first that is clear. If none are clear, try both perpendicular directions (positive and negative cross-axis): compute the clearance to the nearest element for each, and choose the direction with greater clearance. If clearance is equal, prefer upward (negative Y). Retry the offset sequence in the chosen perpendicular direction.

---

## 3. Layout Rules

### Overview: Full Delegation to ELK

Layout is fully delegated to the Eclipse Layout Kernel (ELK) layered (Sugiyama) algorithm. **Layout is a one-shot operation.** It runs once at diagram creation or model import, on the complete graph, with all nodes and edges present. It does not run again in response to user edits such as resizing elements or adding connections -- those are accommodated by the user repositioning elements manually. Connector paths (pipes and info links) are recomputed dynamically as elements move, but node positions are not.

ELK must be given the complete graph in a single invocation. Do not run it incrementally or on subgraphs. The quality of the initial layout is the only layout the user will see without manual intervention, so the full constraint and cycle-handling protocol described below must be applied on that single run.

ELK is run with **Direction: RIGHT** (left-to-right). Layers correspond to horizontal positions; layer assignment determines horizontal ordering.

### SD Band Convention

System dynamics convention places auxiliaries above the stock-flow chain and constants, lookup tables, and modules below it. With `Direction: RIGHT`, ELK's `layerConstraint: FIRST/LAST` controls the *main axis* (horizontal position), not the cross-axis (vertical position). Vertical banding therefore cannot be expressed with `layerConstraint` alone and requires a **compound node (hierarchical) approach.**

The graph passed to ELK contains three top-level compound nodes -- fixed, invisible containers that ELK stacks vertically:

| Compound node | Contents | Vertical position |
|---|---|---|
| `band-aux` | All auxiliaries not in any SCC | Top |
| `band-main` | All stocks, flows, and SCC members of any type | Middle |
| `band-lower` | All constants, lookup tables, and modules not in any SCC | Bottom |

ELK lays out the contents of each compound node internally using `Direction: RIGHT`, producing the left-to-right material chain within `band-main` and left-to-right ordering of peripheral nodes within the other bands. The compound nodes themselves are arranged vertically by setting `elk.direction = DOWN` on the root and giving each compound node a fixed sequence via `elk.layered.layerConstraint` at the root level.

**Cycle detection must run before ELK is invoked** (see Section 3.3). Any node that belongs to an SCC of size >= 2 is placed in `band-main` regardless of its element type. This ensures cycle-participant auxiliaries and constants are laid out topologically alongside the stocks and flows they interact with, rather than being visually separated into a band where their connections would cross.

ELK computes all pixel positions from actual element dimensions and spacing parameters. No hardcoded Y values are used.

**Cross-band edges:** Info links that connect nodes in different compound bands (e.g. an auxiliary in `band-aux` to a stock in `band-main`) are *not* routed by ELK. ELK's hierarchical edge routing for cross-compound edges can produce paths that follow compound node borders rather than taking direct routes, which looks unnatural for dependency arrows. Instead, cross-band edges are excluded from the ELK graph entirely. All info links -- both within and across bands -- are rendered as direct center-to-center lines with border clipping (Section 8), computed dynamically from current element positions. Only material-flow edges (which are always within `band-main`) and within-band info links are passed to ELK for the purpose of node placement and ordering.

### Feedback Cycle Handling

Feedback loops are the defining feature of stock and flow models. Every positive or negative feedback loop contains at least one directed cycle in the dependency graph. ELK's Sugiyama algorithm cannot accept cycles; it must break them before layer assignment. If this is not controlled explicitly, ELK may reverse material-flow edges, causing inflows to appear on the wrong side of their stocks. Cycle breaking is purely an internal algorithmic step -- no cycle is broken in the rendered diagram.

**Step 1 -- Detect SCCs before invoking ELK.**

Run Tarjan's algorithm (or Kosaraju's) on the full dependency graph to identify all strongly connected components of size >= 2. Each SCC represents one or more interacting feedback loops. Tag every node and edge that belongs to any SCC. Place all SCC member nodes into `band-main` (see SD Band Convention above) regardless of their element type.

**Step 2 -- Apply port constraints to stock nodes.**

Set `elk.portConstraints = FIXED_SIDE` on every stock node. Assign ports as follows:

- All inflow material-flow edges attach to the stock's **WEST** port (left face).
- All outflow material-flow edges attach to the stock's **EAST** port (right face).
- Info links are not port-constrained; they attach to whichever side ELK determines is best.

This applies to all stocks unconditionally, not only those in cycles. With port sides fixed, ELK will route any back-edge that must arrive at a stock so that it approaches from the left face, matching the SD convention that inflows enter from the left regardless of where the upstream node sits in the diagram.

**Note on multi-outflow stocks:** When a stock has multiple outflows, all outflow edges exit through the EAST port. ELK may stack them vertically on the east face, which can produce edge crossings if the target stocks are vertically spread. If prototyping reveals unacceptable crossings in multi-outflow scenarios, relax the constraint to `FIXED_ORDER` (which preserves the left/right convention but allows ELK to choose port ordering within a side) or remove port constraints from stocks that have three or more material-flow connections.

**Step 3 -- Identify and mark back-edges.**

Within each SCC, perform a depth-first search rooted at the stock with the highest degree (or the node with the highest out-degree if no stock is present). Any edge that points from a descendant back to an ancestor in the DFS tree is a back-edge. Mark these edges with the ELK property `feedbackEdge: true` before building the ELK graph.

Prefer info-link back-edges over material-flow back-edges. If the DFS designates a material-flow edge as the back-edge and a non-material alternative exists in the cycle, choose the non-material edge instead.

**Pure material-flow cycles:** Some feedback loops consist entirely of material-flow edges with no info links (e.g. Stock A -> Flow -> Stock B -> Flow -> Stock A). In this case no non-material alternative exists. Designate the **inflow** edge (flow -> sink stock) of the edge that closes the cycle as the back-edge. With WEST port constraints on the sink stock, ELK routes the reversed edge so that it still approaches the stock's left face. The visual result is a backward-routed pipe that loops from the upstream flow back around to the sink stock's left side, which reads naturally as "material flowing back into the stock."

This choice should be validated during prototyping. If the visual result is unsatisfactory for specific model topologies, the alternative is to reverse the outflow edge (stock -> flow) instead and verify that the EAST port routing produces an acceptable result. The prototype should test both options on representative cycle models (e.g. predator-prey, SIR with reinfection) before committing to one.

**Step 4 -- Configure ELK cycle breaking.**

Set `elk.layered.cycleBreaking.strategy = INTERACTIVE`. In this mode ELK respects the pre-marked `feedbackEdge` flags and does not independently reverse any unmarked edge. This guarantees that all non-designated material-flow edges retain their natural direction.

**Step 5 -- Post-layout back-edge rendering.**

ELK assigns back-edges reversed waypoints (running opposite to the logical edge direction). When drawing these edges, reverse the waypoint order so the arrowhead appears at the correct original target. Back-edges that are info links should be drawn as curved or routed dashed lines; avoid straight diagonal lines that cross the interior of the diagram.

**Step 6 -- Feedback loop overlay.**

After layout, compute the convex hull of all nodes participating in each SCC.

*Polarity:* Each dependency edge may carry a `linkPolarity` annotation (value: `+1`, `-1`, or `0`/absent). When available, this is set by the equation engine from the sign of the partial derivative of the target's equation with respect to the source, evaluated at initial conditions. Link polarity is determined as follows:

- **Arithmetic dependencies:** For equations involving addition, subtraction, multiplication, and division, the sign of the partial derivative can be computed by symbolic differentiation of the expression AST. This covers the majority of SD model equations.
- **Lookup table dependencies:** Lookup tables are piecewise-linear; the local slope at the initial-condition input value determines the sign. If the input falls on a breakpoint where the slope changes sign, or if the table is non-monotonic, set polarity to `0` (unknown).
- **Conditional and discontinuous dependencies:** `IF`/`MIN`/`MAX` and similar discontinuous functions cannot have a well-defined partial derivative at all input values. Set polarity to `0` (unknown) for these.
- **Manual override:** Users may set or override `linkPolarity` on any edge. Manual annotations take precedence over computed values.

Loop polarity is the product of the `linkPolarity` values of all edges in the cycle: a product of `+1` indicates a reinforcing loop (positive feedback); `-1` indicates a balancing loop (negative feedback). If any edge in the loop has polarity `0` or is unset, the loop polarity is shown as "?" (unknown).

Link polarity computation is a separate feature from layout. The layout algorithm does not depend on polarity values. If the equation engine does not yet support symbolic differentiation, all edges default to polarity `0` (unknown) and all loops display "?" -- the layout, overlay geometry, and per-SCC coloring still function correctly.

*Hull rendering:* Each SCC is assigned a color from the loop palette (see Section 6). Draw each hull as a filled polygon with outline. Where the diagram contains multiple SCCs, use distinct colors from the palette to visually distinguish them. No special polygon-clipping logic is needed for overlapping hulls -- if two hulls overlap spatially, simply draw them in palette order; the low fill alpha (0.08) ensures that double-blending is barely perceptible.

*Label:* Place the loop polarity label ("+", "-", or "?") at the centroid of the hull.

### Horizontal Ordering

Two types of graph edges drive ELK's crossing minimisation and layer assignment:

| Edge Type | Priority | Source | Description |
|---|---|---|---|
| Material flow | 10 (high) | `FlowDef.source` / `FlowDef.sink` | Stock-to-flow and flow-to-stock connections form the main left-to-right chain |
| Info link | 1 (low) | `DependencyGraph.allEdges()` | Formula dependencies (excluding stock-flow connections already represented by material edges) |

The high-priority material edges ensure the stock-flow chain dominates ordering: inflows appear to the left of their sink stock, outflows to the right of their source stock, and transfer flows between their source and sink stocks.

### Overlap Resolution

ELK's spacing rules should prevent within-layer overlaps when given correct element dimensions. If overlaps are detected after the layout run -- for example because label extents exceed element bounds -- resolve them as follows before finalising positions:

1. Group elements by their ELK-assigned layer.
2. Sort each group by position along the cross-axis.
3. Scan in order. Record any element whose leading edge is within 20px of the previous element's trailing edge.
4. Redistribute the group symmetrically around its centroid, preserving order and maintaining the 20px minimum gap. Do not cascade elements in one direction only.

This resolver runs once, immediately after the ELK layout pass, as part of the initial layout pipeline. It does not run again after user edits.

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

This conversion must happen in a single clearly identified function. Any code that mixes top-left and center conventions without converting will produce elements offset by half their size.

---

## 4. Label Collision

Label collision resolution runs once, immediately after ELK layout and coordinate conversion, as the final step of the initial layout pipeline.

1. For each element, compute the bounding box of all its labels including sub-labels (equation lines, badge text). Labels placed outside the element shape (e.g. flow names below the diamond) extend the bounding box beyond the element.
2. Test each label bounding box against the bounding boxes of all nearby elements and their labels (limit to elements within 200px to avoid O(n^2) cost on large diagrams).
3. If a collision is detected, nudge the label 8px in the direction that increases separation and repeat (max 3 iterations). If unresolvable, suppress the secondary label (equation or value), set the `labelCollapsed: true` flag on the element's `ElementPlacement` record, and store the full secondary label text in `collapsedLabelText` on the same record. The renderer reads `labelCollapsed` to decide whether to draw the secondary label; on hover, it renders `collapsedLabelText` in a tooltip. Neither flag triggers re-layout.
4. Flow name labels (placed below the diamond) are nudged vertically first, then horizontally if the vertical nudge causes a new collision.

---

## 5. Dependency Graph Rules

- **Formula references:** If element A's formula references element B, an edge is created from B -> A (B influences A).
- **Flow-stock connections:** Flows have edges to their source and sink stocks (representing material movement).
- **Material edge exclusion:** When building info link edges for ELK, any dependency edge that duplicates a stock-flow material connection (in either direction) is excluded, to avoid conflicting layout hints.
- **Cycle membership:** After SCC detection (Section 3), every edge that belongs to a cycle is tagged with its SCC identifier. This tag is used during rendering to determine loop participation for the feedback loop overlay.
- **Link polarity:** Every dependency edge may carry a `linkPolarity` annotation with value `+1`, `-1`, or `0` (unknown/unset). See Section 3, Step 6 for how polarity is determined and used. The annotation defaults to `0` when the equation engine cannot compute it. Users may override the annotation manually. Loop polarity (Section 3, Step 6) is derived from this annotation; it is not computed independently.

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

### Loop Color Palette

Each SCC is assigned a distinct color from the following ordered palette. Colors are assigned in SCC detection order (depth-first discovery order from Tarjan's algorithm). The palette cycles if the number of SCCs exceeds six.

| Loop index | Highlight (0.8a) | Edge (0.6a) | Fill (0.08a) |
|---|---|---|---|
| 1 | `#E74C3C` | `#E74C3C` | `#E74C3C` |
| 2 | `#8E44AD` | `#8E44AD` | `#8E44AD` |
| 3 | `#2980B9` | `#2980B9` | `#2980B9` |
| 4 | `#27AE60` | `#27AE60` | `#27AE60` |
| 5 | `#D35400` | `#D35400` | `#D35400` |
| 6 | `#16A085` | `#16A085` | `#16A085` |

---

## 7. Rendering Order (Back to Front)

1. Canvas background (`#F8F9FA`)
2. Material flow pipes (with clouds and arrowheads)
3. Info link dashed lines (with arrowheads)
4. Feedback loop edges (if active)
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

This section defines both the arrowhead shape and where the line body must end when an arrowhead is present. A common error is to clip the line at the element border and then draw the arrowhead on top -- this causes the line body to extend beneath the arrowhead, making the arrowhead appear to float inside the element. The correct approach terminates the line body at the arrowhead base, not at the element border.

### Arrowhead Dimensions

| Connection type | Length | Width |
|---|---|---|
| Material flow | 14px | 10px |
| Info link | 8px | 6px |

### Termination Sequence (Arrowhead End)

Apply this sequence at every line end that carries an arrowhead (the target end). The source end of info links and the non-arrowhead ends of pipes are clipped normally without this adjustment.

1. Compute the border intersection point **P** by clipping the line from source center to target element center against the target element's border. Use rectangular clipping for stocks, auxiliaries, constants, lookup tables, and modules. Use rhombus clipping for flow diamonds (Section 8). **P** is where the line would touch the element border with no arrowhead.
2. Place the **arrowhead tip at P**. Do not move it inward.
3. Compute the unit direction vector **D** pointing from source toward P.
4. **The line body ends at L = P - D x arrowheadLength.** This is the arrowhead base center. Draw the line from its source to **L**. It must not continue past **L** toward **P**. No part of the line body is drawn between **L** and **P**.
5. Compute the perpendicular vector **Q = (-Dy, Dx)** (D rotated 90 degrees).
6. Draw the filled arrowhead triangle with vertices: tip **P**, left base **L + Q x (width/2)**, right base **L - Q x (width/2)**.

**Summary:** line ends at L; arrowhead occupies the gap L -> P; arrowhead tip P touches the element border.

### Source End (No Arrowhead)

At the source end of info links, clip the line to the source element border using the appropriate formula (Section 8). No arrowhead-length adjustment is applied.

---

## 9. Line Clipping to Element Borders

### Rectangular Clipping (Stocks, Auxiliaries, Constants, Lookups, Modules)

1. Compute direction vector `(dx, dy)` from element center to the external point.
2. Compute `scale = min(halfWidth / |dx|, halfHeight / |dy|)`, guarding against `dx = 0` or `dy = 0`.
3. Clipped point = `center + (dx, dy) x scale`.

### Rhombus Clipping (Flow Diamond)

The flow diamond is a rhombus with half-diagonals `hw = 15` and `hh = 15` (half of the 30 x 30 bounding box). The rhombus boundary satisfies `|x/hw| + |y/hh| = 1`. For a ray from center in direction `(dx, dy)`:

```
scale = 1 / (|dx| / hw + |dy| / hh)
```

Clipped point = `(cx + dx x scale, cy + dy x scale)`.

Guard against `dx = 0` and `dy = 0` as with rectangular clipping. This formula must be used in place of rectangular clipping whenever the connected element is a flow diamond -- including material flow pipes and info links targeting or departing from a flow.

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
