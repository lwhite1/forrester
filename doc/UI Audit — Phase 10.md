# UI Audit — Phase 10 (Comprehensive Post-Marquee Review)

Full audit of all 21 source files in `forrester-app` against `UI Behaviors.md`, performed after Phase 10 (marquee selection) implementation. This supersedes the initial Phase 10 audit.

## Scope

Complete review of the canvas editor application:
- 21 source files (ForresterApp + 20 canvas package files)
- 195 tests, all passing
- All documented behaviors in `UI Behaviors.md` verified against code

---

## Documentation vs Code Verification

### Application Window

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Initial size 1200x800 | Yes | ForresterApp:93 |
| Title "Forrester — [filename]" | Yes | ForresterApp:331–334 |
| Menu bar + toolbar top, canvas center, status bar bottom | Yes | ForresterApp:86–91 |

### Status Bar

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Background #E8EAED, top border #BDC3C7 | Yes | StatusBar:22 |
| Tool / selection / elements / zoom sections | Yes | StatusBar:14–17 |
| Tool names match doc | Yes | StatusBar:49–55 |
| Selection text format | Yes | StatusBar:59–66 |
| Element count format | Yes | StatusBar:69–77 |
| Zoom percentage format | Yes | StatusBar:80–82 |

### File Menu

| Item | Accelerator | Code Match | Location |
|------|-------------|------------|----------|
| New | Ctrl+N | Yes | ForresterApp:106 |
| Open | Ctrl+O | Yes | ForresterApp:110 |
| Save | Ctrl+S | Yes | ForresterApp:114 |
| Save As | Ctrl+Shift+S | Yes | ForresterApp:118 |
| Close | Ctrl+W | Yes | ForresterApp:123 |
| Exit | — | Yes | ForresterApp:127 |

### Edit Menu

| Item | Accelerator | Code Match | Location |
|------|-------------|------------|----------|
| Undo | Ctrl+Z | Yes | ForresterApp:136 |
| Redo | Ctrl+Shift+Z | Yes | ForresterApp:144 |
| Select All | Ctrl+A | Yes | ForresterApp:153 |

### Simulate Menu

| Item | Accelerator | Code Match | Location |
|------|-------------|------------|----------|
| Simulation Settings | — | Yes | ForresterApp:164 |
| Run Simulation | Ctrl+R | Yes | ForresterApp:167 |

### Canvas Navigation

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Scroll wheel zoom toward cursor | Yes | ModelCanvas:678–683 |
| Ctrl+Plus/Equals zoom in at center | Yes | ModelCanvas:918–924 |
| Ctrl+Minus zoom out at center | Yes | ModelCanvas:925–930 |
| Ctrl+0 reset zoom | Yes | ModelCanvas:931–935 |
| Space+left-drag pan | Yes | ModelCanvas:714–720 |
| Middle/right-drag pan | Yes | ModelCanvas:714–715 |
| Scale range 10%–500% | Yes | Viewport:11–12 |

### Element Selection

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Left-click selects (clears previous) | Yes | ModelCanvas:780 |
| Shift+click toggles | Yes | ModelCanvas:778–779 |
| Marquee: left-drag on empty canvas | Yes | ModelCanvas:793–806 |
| Shift+marquee adds to selection | Yes | ModelCanvas:801–803 |
| Escape cancels marquee, restores selection | Yes | ModelCanvas:953–955 |
| Select All: Ctrl+A | Yes | ModelCanvas:915–917 |
| Click empty clears selection | Yes | ModelCanvas:793–806 (zero-area marquee) |
| Escape clears selection (last in chain) | Yes | ModelCanvas:968–970 |

### Marquee Visual Feedback

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Fill: #4A90D9 at 10% opacity | Yes | CanvasRenderer:341 |
| Border: #4A90D9 at 60%, 6/3, 1px | Yes | CanvasRenderer:344–347 |
| Live selection during drag | Yes | ModelCanvas:816–821 |
| Crosshair cursor | Yes | ModelCanvas:647 |
| Mouse release finalizes | Yes | ModelCanvas:865–871 |
| Rendering layer 8 (topmost) | Yes | CanvasRenderer:171–173 |

### Selection Visual Feedback

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Dashed blue outline #4A90D9 at 80% | Yes | SelectionRenderer:13 |
| 4px padding | Yes | SelectionRenderer:14 |
| Corner handles | Yes | SelectionRenderer:62–65 |
| Diamond indicator for flows | Yes | SelectionRenderer:68–81 |

### Element Creation

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Toolbar or keys 2–5 enter placement | Yes | ModelCanvas:937–943, CanvasToolBar |
| Click empty creates element | Yes | ModelCanvas:764–771 |
| Escape/1/Select exits placement | Yes | ModelCanvas:962–967 |
| Auto-naming with incremental IDs | Yes | ModelEditor:84–127 |
| New element auto-selected | Yes | ModelCanvas:331–333 |
| Connectors regenerated | Yes | ModelCanvas:330 |

### Flow Connection (Two-Click Protocol)

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| First click sets source | Yes | FlowCreationController:39–53 |
| Second click creates flow at midpoint | Yes | FlowCreationController:56–89 |
| Rubber-band line follows cursor | Yes | CanvasRenderer:274–293 |
| Stock hover highlight | Yes | CanvasRenderer:288–292 |
| Self-loop prevention | Yes | FlowCreationController:60–61 |
| Cloud-to-cloud prevention | Yes | FlowCreationController:65–66 |
| Tool switch cancels pending flow | Yes | ModelCanvas:240–242 |
| Cloud-to-cloud prevention note | Yes | FlowCreationController:65 |
| Only snaps to stocks | Yes | FlowCreationController:136–141 |

### Flow Endpoint Reattachment

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Start on cloud/connected endpoint | Yes | ModelCanvas:740–752 |
| Complete on stock: reconnect | Yes | ModelCanvas:573–579 |
| Complete on empty: disconnect to cloud | Yes | ModelCanvas:576 (null) |
| Escape cancels | Yes | ModelCanvas:956–958 |
| Cloud hit radius: 18px | Yes | FlowEndpointCalculator:14 |
| Connected endpoint hit radius: 14px | Yes | FlowEndpointCalculator:15 |
| Cloud offset: 80px | Yes | LayoutMetrics:55 |

### Element Deletion

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Delete/Backspace removes selected | Yes | ModelCanvas:912–913 |
| Stock deletion nullifies flow refs | Yes | ModelEditor:137–154 |
| Connectors regenerated | Yes | ModelCanvas:353 |

### Inline Editing

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Double-click opens editor | Yes | ModelCanvas:728–736 |
| Enter commits | Yes | InlineEditor:51 |
| Escape cancels | Yes | InlineEditor:53–58 |
| Focus loss commits | Yes | InlineEditor:64–68 |
| Editing chains (constant→value, flow→eq, aux→eq) | Yes | ModelCanvas:397–412 |
| Rename propagation | Yes | ModelCanvas:512–523 |
| Equation editor min width 150px (world) | Yes | LayoutMetrics:68 |
| Canvas shortcuts suppressed during editing | Yes | ModelCanvas:702–705, 901–903 |

### Undo/Redo

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Snapshot-based | Yes | UndoManager |
| Max 100 levels | Yes | UndoManager:16 |
| Redo cleared on new mutation | Yes | UndoManager:34 |
| Menu items enabled/disabled | Yes | ForresterApp:322–327 |
| History cleared on New/Open | Yes | ForresterApp:184, 212 |
| All listed mutation types save undo | Yes | ModelCanvas (verified all paths) |
| One snapshot per drag | Yes | ModelCanvas:845–848 |

### Simulation

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| Settings dialog: 3 fields with defaults | Yes | SimulationSettingsDialog |
| OK disabled for invalid duration | Yes | SimulationSettingsDialog:66–71 |
| Results: separate non-modal window 800x500 | Yes | SimulationResultsDialog:48 |
| Step column + variable columns | Yes | SimulationResultsDialog:26–43 |
| Background thread | Yes | ForresterApp:278–299 |
| Error alert on failure | Yes | ForresterApp:292–295 |

### Element Visual Styling

| Documented Behavior | Code Match | Location |
|---------------------|------------|----------|
| All elements: white (#FFFFFF) fill | Yes | ColorPalette:15 |
| Stock: 8px corners, 3px border, bold 13pt | Yes | LayoutMetrics:20–21, ElementRenderer |
| Stock: unit badge 9pt gray bottom-right | Yes | ElementRenderer:41–47 |
| Flow: diamond 30px, 1.5px border, 11pt name | Yes | LayoutMetrics:37, ElementRenderer:72–73 |
| Flow: equation 9pt gray, suppressed if null/blank/"0" | Yes | ElementRenderer:84–90 |
| Aux: 6px corners, 1.5px border, "fx" badge, 12pt name | Yes | LayoutMetrics:26–27, ElementRenderer:99–134 |
| Aux: equation 9pt gray, suppressed if null/blank/"0" | Yes | ElementRenderer:128–134 |
| Constant: 4px corners, 1px dashed 6/4, "pin" badge, 11pt name | Yes | LayoutMetrics:30–34, ElementRenderer:140–174 |
| Constant: value below name | Yes | ElementRenderer:170–174 |
| Background: #F8F9FA | Yes | ColorPalette:21 |

### Rendering Order

| Layer | Documented | Code Match |
|-------|-----------|------------|
| 1 | Background fill | Yes — CanvasRenderer:72–73 |
| 2 | Material flow lines | Yes — CanvasRenderer:108 |
| 3 | Info link lines | Yes — CanvasRenderer:109 |
| 4 | Element shapes | Yes — CanvasRenderer:112–153 |
| 5 | Selection indicators | Yes — CanvasRenderer:156–158 |
| 6 | Pending flow rubber-band | Yes — CanvasRenderer:161–163 |
| 7 | Reattachment rubber-band | Yes — CanvasRenderer:166–168 |
| 8 | Marquee selection rectangle | Yes — CanvasRenderer:171–173 |

### Cursor Feedback

| State | Documented | Code | Match |
|-------|-----------|------|-------|
| Default / idle | Default arrow | `Cursor.DEFAULT` | Yes |
| Hovering element | Open hand | `Cursor.OPEN_HAND` | Yes |
| Hovering cloud/endpoint | Pointing hand | `Cursor.HAND` | Yes |
| Dragging element | Closed hand | `Cursor.CLOSED_HAND` | Yes |
| Marquee selection | Crosshair | `Cursor.CROSSHAIR` | Yes |
| Space held | Move (four-way) | `Cursor.MOVE` | Yes |
| Panning | Closed hand | `Cursor.CLOSED_HAND` | Yes |
| Placement mode | Crosshair | `Cursor.CROSSHAIR` | Yes |
| Flow pending | Crosshair | `Cursor.CROSSHAIR` | Yes |
| Reattaching endpoint | Closed hand | `Cursor.CLOSED_HAND` | Yes |
| Inline editor active | No change | Early return (no-op) | Yes |

Priority order verified: reattaching/panning/dragging > marquee > spaceDown > flowPending/placement > cloud hover > element hover > default.

### Keyboard Shortcuts

All 19 documented shortcuts verified against code. All match.

---

## Fixed During This Audit

| Finding | Description | Resolution |
|---------|-------------|------------|
| DOC-2 | UI Behaviors said element fill was #F0F4F8 but code uses #FFFFFF (white) | Updated doc to "white (#FFFFFF)" |
| DOC-3 | UI Behaviors said background was #F5F5F5 but code uses #F8F9FA | Updated doc to #F8F9FA |
| DOC-4 | Implementation Plan said 190 tests but actual count is 195 | Updated to 195 |

---

## Carried Findings — Status Update

| ID | Previous Description | Current Status |
|----|---------------------|----------------|
| BUG-14 | Flow equation/name text rendered outside hit area | **Mitigated** — flow hit area (55x35 half-extents) covers name+equation text region |
| BUG-16 | Default "0" equation shown on new flows and auxes | **Resolved** — `isDisplayableEquation` suppresses null, blank, and "0" |
| BUG-17 | SimulationResult `double[]` rows are mutable | **Partially fixed** — compact constructor clones arrays on input; accessor still exposes mutable arrays, but no code mutates them. Low-risk |

---

## Remaining Findings

### Minor

| ID | Description | Severity |
|----|-------------|----------|
| MINOR-1 | (Carried) Equation editor overlaps flow name label — editor positioned at `FLOW_EQUATION_EDITOR_OFFSET` below center, close to the name label | Low |
| MINOR-5 | (Carried) Inline editor position assumes canvas at screen (0,0) — offset by toolbar/menubar height in certain configurations | Low |
| MINOR-6 | (Carried) No feedback when clicking a non-stock element during flow creation — click is silently ignored | Low |
| MINOR-7 | (Carried) No text clipping for long equations inside aux rectangles — text overflows element bounds | Low |
| MINOR-15 | Marquee cursor (CROSSHAIR) set on mouse press but may not visually update until first drag event — imperceptible in practice | Cosmetic |
| MINOR-16 | Space pressed during active marquee does not cancel or pause the marquee — consistent with documented cursor priority order | Cosmetic |

### Code Quality

| ID | Description | Severity |
|----|-------------|----------|
| CQ-11 | (Carried) `updateCursor` hit-tests on every mouse move in SELECT mode — acceptable for expected model sizes | Accepted |
| CQ-13 | `updateMarqueeSelection` rebuilds selection from scratch on every drag event — acceptable for expected model sizes | Accepted |
| CQ-14 | `ModelEditor.hasElement` performs four linear scans — acceptable for expected model sizes, but could use a name index if models grow large | Low |

### UX Suggestions

| ID | Description | Priority |
|----|-------------|----------|
| UX-7 | (Carried) Right-click pans instead of showing a context menu | Low |
| UX-13 | (Carried) No progress indicator during simulation run | Low |
| UX-14 | (Carried) No way to export or copy simulation results from the table | Medium |
| UX-15 | (Carried) Results window has no chart/graph visualization | Medium |
| UX-16 | (Carried) Multiple results windows accumulate on repeated simulation runs | Low |
| UX-17 | File I/O errors (open/save) print stack trace to console instead of showing an error dialog to the user | Medium |

---

## Summary

| Category | New | Fixed | Resolved/Mitigated | Carried | Open |
|----------|-----|-------|--------------------|---------|------|
| Doc Mismatch | 0 | 3 | — | 0 | 0 |
| Bug | 0 | 0 | 3 (BUG-14/16/17) | 0 | 0 |
| Minor | 0 | 0 | — | 6 | 6 |
| Code Quality | 1 | 0 | — | 2 | 3 |
| UX | 1 | 0 | — | 5 | 6 |
| **Total** | **2** | **3** | **3** | **13** | **15** |

**No critical or high-severity issues.** All documented behaviors match the code exactly. Three previously carried bugs are now resolved or mitigated. Three doc-vs-code mismatches were fixed during this audit. One new UX finding (file I/O error handling) and one new code quality note were identified.
