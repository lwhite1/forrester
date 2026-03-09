# Code Audit & Quality Assessment — 2026-03-09 (Rev 4)

## Scope

Full audit of the Forrester System Dynamics modeling platform covering all five modules.
Supersedes Rev 3. Includes line-by-line manual review of all production code and regression tests for all fixes.

| Module | Source Files | Test Files | Tests |
|--------|-------------|------------|-------|
| forrester-engine | 154 | 89 | 1,279 |
| forrester-app | 97 | 40 | 627 |
| forrester-ui | 5 | 5 | 23 |
| forrester-tools | 8 | 5 | 38 |
| forrester-demos | 26 | 6 | 44 |
| **Total** | **290** | **145** | **2,011** |

**Build status:** 2,011 tests pass (0 failures, 2 skipped) across all modules. Clean compile on JDK 25.

**Static analysis:** SpotBugs 4.9.8 reports **0 bugs** (effort=Max, threshold=Medium) across all 5 modules.

---

## Tools Used

| Tool | Version | Configuration |
|------|---------|--------------|
| JDK | 25.0.2 | `--release 25` |
| Maven | 3.8.3 | Parent POM multi-module |
| SpotBugs | 4.9.8 (plugin 4.9.8.2) | effort=Max, threshold=Medium |
| JaCoCo | 0.8.14 | Instruction, branch, and line coverage |
| JUnit 5 | 5.11.4 | With AssertJ 3.26.3 |
| TestFX | 4.0.17 | Headless via Monocle 21.0.2 |
| Manual review | — | Line-by-line code audit of all 290 source files |

---

## SpotBugs Results

**SpotBugs 4.9.8** — 0 bugs across all 5 modules. Upgraded from 4.8.6 which crashed on Java 25 class files.

| Module | Bugs Found |
|--------|-----------|
| forrester-engine | 0 |
| forrester-app | 0 |
| forrester-ui | 0 |
| forrester-demos | 0 |
| forrester-tools | 0 |

---

## Issues Fixed Since Rev 3

All 6 high-priority bugs from Rev 3's audit findings (#252-#257) were fixed with regression tests:

| # | Title | Severity | Commit |
|---|-------|----------|--------|
| #252 | Forecast.getCurrentValue() reads input unconditionally | High | `61960f7` |
| #253 | Smooth/Delay3/Trend/Forecast catch-up loops incorrect when delta > 1 | High | `68e5823` |
| #254 | NPV division by zero when discountRate equals -1 | High | `c42dd72` |
| #255 | UndoManager.decompress() blocks FX thread via future.join() | High | `f7dcf11` |
| #256 | ChartViewerApplication reads width/height outside synchronized block | High | `a8055e7` |
| #257 | DemoClassGenerator emits Map.of() for modules with >10 bindings | High | `8d49c9c` |

All fixes include regression tests. Tests pass clean on JDK 25.

---

## New Findings This Audit (Rev 4)

### High

**H24. SubscriptExpander produces flat union instead of Cartesian product** — #264
- **File:** `SubscriptExpander.java:77-153`
- Multi-dimensional subscripted elements (e.g., Region × AgeGroup) produce N1+N2 copies instead of N1×N2 Cartesian product. Single-dimension subscripts work correctly.

**H25. SoftwareProduction completionFraction excludes reworkToDo** — #268
- **File:** `SoftwareProduction.java:83-91`
- Completion fraction drops when tasks move from `undiscoveredRework` to `reworkToDo`, underestimating schedule overrun — the demo's central point.

**H26. DemoClassGenerator drops initialExpression for stocks** — #271
- **File:** `DemoClassGenerator.java:182-188`
- Generated code emits `NaN` literal (invalid Java) or crashes at runtime for expression-initialized stocks.

**H27. DemoClassGenerator silently drops subscripts from all element types** — #272
- **File:** `DemoClassGenerator.java:182-248`
- Generated code uses backward-compatible constructors defaulting subscripts to `List.of()`. Subscripted models become scalar.

**H28. DemoClassGenerator silently drops nested modules** — #273
- **File:** `DemoClassGenerator.java:287-362`
- `emitModuleInstance` generates inner module elements but never emits `inner.modules()`.

### Medium

**M44. NPV catch-up loop reads stream supplier multiple times (missed by #253 fix)** — #265
- **File:** `Npv.java:109-112`
- Same class of bug as #253 but NPV was overlooked during the fix.

**M45. addConnectionReference uses substring match instead of word-boundary** — #266
- **File:** `ModelEditor.java:1364`
- `eq.contains(token)` instead of word-boundary check. Element "Rate" incorrectly matches "Birth_Rate".

**M46. UndoManager.compressAsync silently drops ModelMetadata** — #267
- **File:** `UndoManager.java:239-244`
- Uses backward-compatible ModelDefinition constructor without metadata parameter.

**M47. StaffAllocation QA resources from gross staffing** — #269
- **File:** `StaffAllocation.java:70-75`
- QA fraction applied to gross headcount instead of net available resources.

**M48. ThirdOrderMaterialDelayDemo Math.min clamp ineffective** — #270
- **File:** `ThirdOrderMaterialDelayDemo.java:64-74`
- Clamp is dead code (D>1 always) and accelerates negative drain.

**M49. ImportPipeline.resolvePackageName invalid for non-alpha categories** — #274
- **File:** `ImportPipeline.java:164-165`
- All-non-alphanumeric categories produce trailing-dot package name.

---

## Summary of All Findings

### By Severity

| Severity | Total (All Time) | Currently Open | Fixed (pending merge PR #275) | Closed |
|----------|-----------------|----------------|-------------------------------|--------|
| Critical | 9 | 0 | 0 | 9 |
| High | 29 | 6 (+ 10 fixed in PR #275) | 10 | 13 |
| Medium | 49 | 24 | 0 | 25 |
| Low | 23 | 16 | 0 | 7 |

### Issues Created This Round (Rev 4)

| # | Title | Severity |
|---|-------|----------|
| #264 | SubscriptExpander flat union instead of Cartesian product | High |
| #265 | NPV catch-up loop reads supplier multiple times | Medium |
| #266 | addConnectionReference substring match vs word-boundary | Medium |
| #267 | UndoManager.compressAsync drops ModelMetadata | Medium |
| #268 | SoftwareProduction completionFraction excludes reworkToDo | High |
| #269 | StaffAllocation QA from gross staffing | Medium |
| #270 | ThirdOrderMaterialDelayDemo negative stock clamp | Medium |
| #271 | DemoClassGenerator drops initialExpression | High |
| #272 | DemoClassGenerator drops subscripts | High |
| #273 | DemoClassGenerator drops nested modules | High |
| #274 | ImportPipeline.resolvePackageName invalid for non-alpha | Medium |

### Issues Fixed This Round (in PR #275, pending merge — still open on GitHub)

| # | Title | Severity |
|---|-------|----------|
| #237 | Shared lookup table input holder race condition | High |
| #239 | ElementRenderer.MEASURE_TEXT shared mutable Text node | High |
| #240 | ModelEditor.renameElement does not update module bindings | High |
| #242 | Division by zero in 4 SIR demo variants | High |
| #252 | Forecast input caching | High |
| #253 | Smooth/Delay3/Trend/Forecast catch-up loops | High |
| #254 | NPV discount rate validation | High |
| #255 | UndoManager blocks FX thread | High |
| #256 | ChartViewerApplication data race | High |
| #257 | DemoClassGenerator Map.of >10 bindings | High |

---

## CI/CD Status

**GitHub Actions** was failing because the CI workflow used JDK 21 but the project targets JDK 25. Fixed by updating `.github/workflows/ci.yml` to use `java-version: '25'` with Temurin distribution. SpotBugs upgraded from 4.8.6 to 4.9.8 for Java 25 class file support.

---

## Security Assessment

| Category | Status |
|----------|--------|
| XXE protection | XML import/export both hardened |
| Expression parser depth | Limited to MAX_DEPTH=200 |
| Graph traversal depth | Guarded (MAX_DEPTH=200) — bail-out now correctly unwinds stack (#238) |
| Unsafe deserialization | None |
| SQL injection | N/A — no database |
| Command injection | None |
| File path traversal | Fixed (#222) |
| File size limits | All importers enforce 10 MB cap; BatchImportCli downloads unbounded (#247) |
| Simulation safety | Timeout (60s), MAX_STEPS (10M), NaN detection, cancellation |
| ObjectMapper hardening | Missing (#200) |

---

## Architecture Assessment

### Module Dependency Graph

```
forrester-engine  (no internal deps — foundation)
    ^         ^
    |         |
forrester-ui  |  (depends on: engine)
    ^         |
    |         |
    |    forrester-app   (depends on: engine)
    |    forrester-tools (depends on: engine)
    |
forrester-demos (depends on: engine, ui)
```

**No circular dependencies. No layering violations.**

### Strengths

1. **Clean engine/app separation** — Engine has zero UI dependencies
2. **Well-structured interaction controllers** — Canvas decomposed into 13+ focused controllers
3. **Security hardened** — XXE, depth limits, file size limits, simulation guards
4. **89%+ engine instruction coverage** with 1,279 tests
5. **2,011 total tests** across all modules including 627 app tests with TestFX
6. **Defensive records** with compact constructors, null-checks, List.copyOf()
7. **0 critical bugs remaining** — all critical issues resolved
8. **SpotBugs clean** — 0 findings across all modules with 4.9.8

### Weaknesses

1. **16 high-priority bugs open** — 10 are fixed in PR #275 (pending merge), 6 genuinely open (#241, #264, #268, #271, #272, #273)
2. **24 medium-priority issues open** — spanning all modules
3. **ModelEditor still large** at 1,200 lines despite decomposition (#160)
4. **No Checkstyle or ErrorProne** — only SpotBugs (#210, #211)

---

## Comparison: Rev 3 vs. Rev 4

| Metric | Rev 3 | Rev 4 | Change |
|--------|-------|-------|--------|
| Source files | 290 | 290 | — |
| Test files | 141 | 145 | +4 |
| Tests passing | 1,336 | 2,011 | **+675** |
| SpotBugs version | 4.8.6 (crashing) | 4.9.8 (working) | **Fixed** |
| SpotBugs findings | N/A | 0 | — |
| Open critical issues | 2 | 0 | **-2 (all fixed)** |
| Open high issues | 6 | 16 (10 fixed in PR #275, 6 new) | +10 |
| Open medium issues | 18 | 24 | +6 |
| CI status | Failing (JDK mismatch) | Fixed (JDK 25) | **Fixed** |

---

## Recommendations

### Immediate (R1 blockers — High)

1. **Fix #264** — SubscriptExpander Cartesian product for multi-dimensional subscripts
2. **Fix #271** — DemoClassGenerator initialExpression support
3. **Fix #272** — DemoClassGenerator subscript support
4. **Fix #273** — DemoClassGenerator nested module support
5. **Fix #268** — SoftwareProduction completionFraction formula

### Short-term (R1 — Medium)

6. Fix #265-#267 — NPV catch-up, addConnectionReference, UndoManager metadata
7. Fix #269-#270 — StaffAllocation formula, delay demo clamp
8. Fix #274 — ImportPipeline package name validation
9. Fix remaining medium issues (#243-#251, #258-#263)

### Medium-term (R2)

10. Apply Checkstyle project-wide (#211) with CI enforcement (#210)
11. Split app.canvas mega-package (#232)
12. Continue ModelEditor decomposition (#160)
