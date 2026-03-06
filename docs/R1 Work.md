# Remaining Work for First Free (No-AI) Release

Based on the design spec (sections 14, 16), competitive analysis, assessment, and current codebase state. Organized by priority.

## 1. Bugs to Fix (Open Issues)

These are existing open bugs that should be resolved before release:

| # | Issue | Severity |
|---|---|---|
| 24 | NPE in buildExamplesMenu on malformed catalog entry | High |
| 25 | openExample catches only IOException, misses IllegalArgumentException | High |
| 26 | Examples catalog errors silently swallowed without logging | Medium |
| 52 | VensimExporter denormalizeName corrupts digit-prefixed variable names | Medium |
| 55 | Handle no-extension and unknown-extension files gracefully in Open | Medium |
| 64 | CanvasState.toViewDef() hardcodes view name "Main" | Low |
| 71 | ModelEditor thread safety: no synchronization on mutable state | Medium |
| 79 | Inconsistent parameter validation across sweep dialogs | Low |

## 2. Stability & Quality (Open Enhancement Issues)

| # | Issue | Priority |
|---|---|---|
| 73 | ElementRenderer.truncate() allocates a new Text node per call (performance) | Medium |
| 80 | UndoManager stores full snapshots — high memory for large models | Medium |
| 76 | Remove dead FORCE_RELAYOUT flag and unused equation parameters | Low — but cleanup before release |
| 75 | Clipboard.Entry.elementDef typed as Object loses type safety | Low |
| 74 | CanvasRenderer.render() has 13 parameters | Low |
| 30 | Catalog JSON generation uses string concatenation instead of Jackson | Low |

## 3. Features Not Yet Implemented (Required for Release)

These are features called out in the spec / competitive analysis as necessary for a credible free release.

### 3.1 High Priority — Competitive Differentiation

1. **Command palette (Ctrl+K)** — Spec section 9. Searchable fuzzy-matched command input for rapid model building. Key for the "David" expert workflow. Currently not implemented. The competitive analysis notes this as a differentiator vs. Vensim PLE.

2. **Stale results indicator** — Spec section 8.2. When the model changes after a simulation, the dashboard should show a "stale" indicator. Currently the dashboard just clears results on model change. A "stale but visible" approach with re-run prompt is better UX.

3. **Run comparison / ghost overlays** — Spec section 8.2. Previous simulation runs shown as ghosted lines behind the current run. Currently each new run replaces the previous. Side-by-side or overlay comparison of multiple runs is standard in commercial tools.

4. **CSV data import and overlay** — Spec section 12.3 / competitive analysis. Import CSV time series data and overlay on simulation output for visual comparison. Essential for calibration workflows. No current implementation.

5. **Sensitivity summary (text)** — Spec section 8.2. After a sweep or Monte Carlo run, a ranked plain-language summary of which parameters matter most. Currently sweep results show raw data but no ranked sensitivity summary.

### 3.2 Medium Priority — Expected by Users

6. **Session start screen** — Spec section 16.4. A landing screen with "Build without AI" / "Open existing model" options (omitting the AI options for this release). Currently the app opens directly to a blank canvas.

7. **More example models** — Competitive analysis priority #5. Currently 8 models. Target 12-15 covering project management, business strategy, economics, and more advanced models within existing categories. Issue #49 tracks this partially.

8. **XMILE alternate extensions (.stmx, .itmx)** — Issue #53. Stella/iThink use these extensions. Currently only `.xmile` is recognized.

9. **Improve import/export test coverage** — Issue #54. Critical for trust in interoperability claims.

10. **CLI conversion tool** — Competitive analysis priority #6. `forrester convert model.mdl model.xmile` for headless format conversion. Small effort, high value for interoperability positioning.

11. **Maturity visual indicators (no-AI subset)** — Spec section 4 / 16.2. Even without LLM postures, the canvas should show:
    - Amber accents on elements missing equations
    - Red connection lines on unit mismatches
    - "?" unit badges on elements without units

    These are local computations that help guide users without AI.

12. **Sparklines on stocks** — Spec section 6.3. Small inline behavior indicators inside stock elements showing their simulation trajectory. Significant visual feature that no competitor offers.

### 3.3 Lower Priority — Nice to Have

13. **Pop-out dashboard** — Spec section 8.1. Detach the dashboard into a separate window for dual-monitor setups. Currently the dashboard is embedded in the split pane.

14. **Keyboard shortcuts reference dialog** — Currently the "Keyboard Shortcuts" help menu item is disabled. Should show a simple reference card.

15. **"Getting Started" and "SD Concepts" help** — Help menu items are disabled stubs. Need at least basic content.

16. **Vensim exporter formatting** — Issue #51. Output should follow Vensim conventions more closely.

## 4. Documentation (Non-Code)

The competitive analysis is emphatic: documentation is the single highest-leverage non-code action.

1. **Quickstart tutorial** — "Build your first model in 10 minutes" using the visual editor. Step-by-step with screenshots.

2. **"From Vensim PLE" migration guide** — How to import .mdl, what's supported, what analysis features Forrester adds. Competitive analysis priority #4.

3. **"Why Forrester?" landing page** — Feature comparison table, the zero-cost argument, download link. Competitive analysis calls this out as the single highest-leverage action.

4. **User manual for visual editor** — At minimum: element types, toolbar, keyboard shortcuts, properties panel, simulation, analysis dialogs, import/export.

5. **Document import fidelity** — Which Vensim/XMILE constructs are supported, which emit warnings, which are dropped. Builds trust.

6. **Example model licensing** — Issue #48. Document provenance of bundled models.

## 5. Packaging & Distribution

1. **Publish engine JAR to Maven Central** — Competitive analysis priority #2. Lets programmers add a one-line dependency.

2. **Platform installers** — Native installers (Windows .msi/.exe, macOS .dmg, Linux .deb/.AppImage) or at minimum a fat JAR with bundled JVM (jpackage). Currently requires users to have JDK 21 installed.

3. **README / GitHub project page** — Feature overview, screenshots, download instructions, build instructions.

## 6. Deferred (Post-Release / AI Version)

These are explicitly **out of scope** for the no-AI first release but tracked for reference:

- Causal loop diagrams (spec section 12.4 — large effort, high impact for education)
- LLM integration, conversation panel, all postures (spec phases 5-8)
- AI-only mode / AUTONOMIST posture (spec phase 8)
- Maturity strip in conversation panel (spec section 5.3)
- Web version (competitive analysis priority #7)
- Database connectivity / real-time data feeds
- Interface/dashboard design for stakeholders
- Collaboration / multi-user
- Voice input
- Policy optimization

---

## Summary by Effort

| Category | Count | Typical Effort |
|---|---|---|
| Open bugs to fix | 8 | Small-Medium each |
| Quality/cleanup issues | 6 | Small each |
| New features (high priority) | 5 | Medium-Large each |
| New features (medium priority) | 7 | Small-Medium each |
| New features (lower priority) | 4 | Small each |
| Documentation | 6 | Medium each |
| Packaging/distribution | 3 | Medium each |

The **critical path** items — the things that would make or break a first release — are: fix the open bugs (especially the crash bugs #24/#25), build the command palette, add CSV data import, write the quickstart tutorial, and create platform installers. Everything else enhances the release but isn't blocking.
