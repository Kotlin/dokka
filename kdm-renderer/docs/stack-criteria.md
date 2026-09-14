# Evaluation criteria for the renderer's frontend stack

Status: **skeleton** — the criteria below are proposed; weights, gates and the
candidate list are unfilled. Nothing here picks a stack.

## 1. What this document is for

The renderer has to answer a question that the parity work does not touch: *what
technology produces the HTML, and where does it run?* The constraints as stated
today are:

- the generator runs **on the user's machine**, as part of their Gradle/Maven/CLI build;
- we cannot assume Node.js is installed, and we do not want to ship Node.js in the jar;
- the output must be **a set of static HTML files**; an SPA variant may exist in
  addition, but is not required;
- React is **desirable, not mandatory** — it is where the team's expertise is, and it
  is what makes `resc-ui` (JetBrains' internal UI kit) reusable.

Those four lines already eliminate some options and admit many others. This file
exists so that the survey of options is scored against a list agreed *before* any
candidate is looked at — otherwise the comparison silently reduces to whichever
criterion the last demo happened to exercise.

Scope note: these are criteria for the **rendering/presentation stack**. Criteria for
KDM itself (`60-model-gaps.md`) and for output parity (`10-feature-registry.md`) are
separate and already have their own documents. A stack cannot be blamed for a model
gap, and must not be credited for closing one.

## 2. How to use it

1. Fix the **gates** (§ 4). A candidate that fails a gate is out, and is recorded as
   out with the reason — not silently dropped.
2. Score survivors on the **weighted criteria** (§ 5–§ 9) using the candidate card
   in § 11.
3. Every score needs **evidence**, produced by the spike protocol in § 12. A score
   with no evidence column is an opinion and is marked as such.
4. Gates depend on answers we do not have yet (§ 13). Until then, a gate is written
   as a conditional: *"if baseline is JDK 8, then …"*.

IDs are local to this file: `K-##` criterion, `A-##` candidate approach. `F-###`,
`G-##`, `Q-###`, `ADR-####`, and the `C-##`/`E-###`/`S-##` IDs from
`requirements/40-page-anatomy.md` are the existing project-wide IDs.

Scoring scale for weighted criteria, to keep the numbers comparable:

| Score | Meaning |
|---|---|
| 0 | Blocks the criterion; only a redesign fixes it |
| 1 | Achievable, but by work we would rather not own |
| 2 | Achievable with ordinary effort |
| 3 | Free — the stack gives it without our doing anything |

## 3. Criteria classes

| Class | Effect | Filled by |
|---|---|---|
| `gate` | Pass/fail. One failure eliminates the candidate | § 4 |
| `weighted` | Scored 0–3 × weight | § 5–§ 9 |
| `tiebreak` | Only consulted between otherwise-equal candidates | § 10 |

Weights are TODO — they encode priorities and are therefore not mine to set. Proposal:
weight 3 for *Output quality* and *Build-time behaviour*, 2 for *Maintenance* and
*Extensibility*, 1 for *Ecosystem risk*. Confirm or replace.

## 4. Gates

| ID | Gate | Why it is a gate, not a trade-off | How we check it | Depends on |
|---|---|---|---|---|
| K-01 | Runs with **no toolchain installed by the user** — no `node`, `npm`, `python` on PATH | It is the stated constraint; a build that fails on a clean machine is not shippable | Generate on a container with only a JDK; no network | — |
| K-02 | Runs on the **supported JDK baseline** | Dokka pins `javaToolchain` to 8 today. A stack needing 17/21+ (GraalJS) either forces a baseline bump or is out | Run the generation task under each baseline JDK | Q-030 |
| K-03 | Runs on the **full OS/arch matrix** users build on (Linux/macOS/Windows × x64/arm64, plus CI images) | Native-backed engines (Javet, J2V8, QuickJS bindings) need a per-platform binary; a missing tuple is a hard build failure for those users | Matrix job; check which tuples the candidate publishes | Q-031 |
| K-04 | Works in an **offline / hermetic / proxied build** | Corporate builds have no npm registry, often no internet. A stack that resolves JS deps at generation time is out | Generate with network disabled and an empty Gradle cache | — |
| K-05 | Output is **static files, servable from a plain file tree** | Stated constraint; also what GitHub Pages / S3 / `file://` need | Serve `output/` over `python -m http.server` and open via `file://` | Q-032 |
| K-06 | Everything shipped is **distributable under the artifact's licence** | `resc-ui` is internal; Dokka is Apache-2 and publishes to Maven Central. Fonts, icons and npm transitives all count | Licence inventory of every shipped byte; legal confirmation for `resc-ui` | Q-033 |
| K-07 | **Deterministic, reproducible output** — same KDM in, byte-identical HTML out | Otherwise diffing two versions of the docs is impossible, caching is unsound, and parity tests cannot be snapshot-based | Generate twice, `diff -r`; hash-stable class names, no timestamps/ordering noise | — |
| K-08 | Artifact size stays within budget | A renderer that adds 100 MB+ to every build's dependency graph is a tax on every user. GraalJS ≈ tens of MB; Javet ships a V8 per platform | Measure the resolved dependency set and the published jar | Q-034 |

Gate candidates deliberately **not** listed as gates, with reason: "must be React"
(stated as desirable, not required); "must reuse `resc-ui`" (a benefit, K-24, not a
gate — unless § 13 says otherwise); "must support an SPA mode" (explicitly optional).

## 5. Output quality — what the reader gets

| ID | Criterion | Why | How measured |
|---|---|---|---|
| K-10 | Content present in HTML **without JS execution** | Crawlers, search engines, `curl`, text browsers, corporate JS blocking, offline `file://`. Determines whether build-time prerendering is required at all | `curl` a page, assert signature + KDoc + member list are in the bytes; run the parity checklist with JS disabled |
| K-11 | Full interactive surface reachable | The current output's chrome is not decorative: C-01 TOC, C-02 search, C-03 source-set filter, C-05 version switcher, C-07 theme, C-09 copy, C-10 mobile TOC, plus E-041 platform tabs and E-046 filter-driven visibility | Each `C-##`/`E-0##` row demonstrated in the spike |
| K-12 | Accessibility | F-204 and C-11 — ARIA roles, skip links, keyboard affordances — exist today and are easy to lose in a rewrite. Some stacks give them via the component kit, others make them manual | axe/Lighthouse on the spike's page set; keyboard-only walkthrough |
| K-13 | Page weight and first render on a large page | A 4.8k-token signature page (E-003) with a 1191-element platform-hinted body (E-040) is the realistic worst case; hydration cost scales with it | Bytes per page, LCP/TBT on the worst page in the reference set, on a throttled profile |
| K-14 | URL and anchor stability | G-01, O-02, Q-013. Old links must keep working; the stack must not impose its own routing/asset-hash scheme on page URLs | Diff generated URL set against the current output for a reference project |
| K-15 | Browser matrix incl. no-modern-JS environments | Docs are read from everywhere, including old corporate browsers | Declared target matrix, verified on the spike |
| K-16 | Theming and per-project branding | F-104, F-106, `customAssets`; users restyle Dokka output today | Restyle the spike without forking the renderer |

## 6. Build-time behaviour — what the user's build pays

| ID | Criterion | Why | How measured |
|---|---|---|---|
| K-20 | Throughput at scale | The decisive number for any prerendering approach. `ui-showcase` is 135 pages; the real target is thousands (the 1 MB/3 MB/30 MB KDM figures in `KDM spec.md` came from CK core). Per-page SSR through an interpreted JS engine is the risk case | Pages/second and wall clock for the full reference set, cold and warm |
| K-21 | Memory ceiling | Runs inside the user's Gradle daemon, alongside the Kotlin compiler | Peak RSS / heap for the largest reference project |
| K-22 | Engine/runtime cold start | Paid once per build at best, per worker at worst; dominates small projects | Time to first rendered page on a 1-page project |
| K-23 | Parallelism | Gradle workers, and per-module fan-out. Some engines are single-threaded per context and need one context per thread — which multiplies K-21 | Scaling curve 1→N workers |
| K-24 | Incrementality and cache friendliness | Already flagged in `train-of-thoughts.md`: a one-module change must not regenerate everything. Interacts with ADR-0001 — renderer-side reverse indices are global by nature | Change one declaration; measure regenerated file count and time. Check Gradle build-cache and configuration-cache compatibility |
| K-25 | Failure diagnostics | When rendering breaks, the user sees the error — a JS stack trace from inside an embedded engine is a support burden | Deliberately break a template/component; inspect the reported message |
| K-26 | Non-HTML outputs unaffected | Q-002 — if GFM/Jekyll/Javadoc stay in scope, a stack that only knows how to make HTML pushes them onto a second mechanism | Check the content-model boundary the candidate implies |

## 7. Development and maintenance

| ID | Criterion | Why | How measured |
|---|---|---|---|
| K-30 | Team expertise match | The stated reason React is attractive. A stack nobody on the team can debug is slow forever, not just at the start | Honest count of who can own it, on which side |
| K-31 | Dev loop speed | Iterating on a component must not require a full Dokka run. This is where a React/Vite dev server with a fixture KDM wins outright | Time from source edit to visible change |
| K-32 | Number of languages/toolchains in the repo | A JS toolchain inside a Gradle composite build means npm in Dokka's own CI, lockfiles, two dependency ecosystems, two release processes — even when the user never sees Node | Enumerate what CI must install and keep updated |
| K-33 | Testability | Parity is enforced by tests (`00-method.md § 2`): snapshot HTML, DOM assertions, component tests. Some stacks make HTML snapshots trivial and interaction tests hard, or vice versa | Which of the four test kinds the spike can do, and with what |
| K-34 | Type safety across the KDM boundary | KDM is `kotlinx.serialization` data classes. JVM-side rendering consumes them directly; a TS frontend needs generated types, or it drifts silently | How types reach the view layer; is the generator ours to maintain |
| K-35 | `resc-ui` reuse | Real benefit: components, a11y and design come for free and stay consistent with other JetBrains docs surfaces | Which `C-##`/`E-###` regions map to existing components |
| K-36 | Design-iteration freedom | Informational-equivalence parity (`00-method.md`, definition C) exists so layout can change. The stack should not make redesign expensive | Estimate cost of a nontrivial layout change in the spike |

## 8. Extensibility and compatibility

| ID | Criterion | Why | How measured |
|---|---|---|---|
| K-40 | Third-party extension story | Q-004. `htmlPreprocessors`, `htmlCodeBlockRenderers`, `tabSortingStrategy`, `immediateHtmlCommandConsumer` are exactly the HTML-specific EPs third parties use; Dackka is the load-bearing case (`50-extension-points.md`) | For each EP, what the equivalent is in this stack — or that there is none |
| K-41 | Template and asset overrides | F-121, `customAssets`, FreeMarker templates, and the `<dokka-template-command>` mechanism (O-04) | Can a user override one region without forking |
| K-42 | Sidecar artifact contract | S-01 `package-list` (consumed as *input* by other projects), S-02 search index, S-03 navigation, S-04 source-set graph. These are an output contract regardless of stack | Are they naturally producible, or bolted on |
| K-43 | Multi-module and versioning | Q-003 — `plugin-all-modules-page` and `plugin-versioning` have their own notion of assembling output from several runs; P-01, P-15, C-05 | Does the stack survive the aggregate/archived-copy model |
| K-44 | Optional SPA from the same source | Stated as a nice-to-have. Cheap in some stacks, a second implementation in others | Would the SPA share components, or only data |

## 9. Ecosystem risk

| ID | Criterion | Why | How measured |
|---|---|---|---|
| K-50 | Maturity and support of the load-bearing component | If a JS-in-JVM engine, a WASM runtime, or an internal UI kit is in the critical path, its health is our health | Release cadence, issue latency, who maintains it, is JetBrains among them |
| K-51 | Supply-chain surface | npm transitives inside a build tool that runs on every user's machine is a different risk class from the same deps in a web app | Count of runtime-shipped third-party deps; is there a lockfile and an audit path |
| K-52 | Exit cost | We will be wrong about something. How much is thrown away if this stack is replaced in two years | Which layer would survive: KDM (yes), page/URL model, components, templates |
| K-53 | Precedent | Someone already generating large static API docs this way is evidence the path exists (e.g. Kotlin's own docs, KDoc consumers, rustdoc/docs.rs, Doxygen, Sphinx, Javadoc) | Name the precedent, or record that there is none |

## 10. Tiebreakers

`K-60` community familiarity of the template/component language · `K-61` size of the
change to the current build (`build-logic/` conventions, composite builds) ·
`K-62` whether the same stack could later serve non-API-reference docs.

## 11. Candidate card template

One file or section per candidate. Copy verbatim.

```markdown
### A-##: <name of the approach>

**Shape.** Where each step runs: KDM → ? → HTML. What executes at Dokka release
time (our CI, Node allowed) vs at generation time (user machine, JDK only) vs in
the browser.

**Gates.** K-01 … K-08: pass / fail / conditional, one line of evidence each.

**Scores.** Table of K-1x … K-5x with score, weight, and an evidence link.

**What it makes easy / hard.**

**Unknowns.** What the spike did not answer.
```

### Gate matrix (fill as candidates are surveyed)

| Candidate | K-01 toolchain | K-02 JDK | K-03 OS/arch | K-04 offline | K-05 static | K-06 licence | K-07 determinism | K-08 size | Verdict |
|---|---|---|---|---|---|---|---|---|---|
| A-01 | | | | | | | | | |
| A-02 | | | | | | | | | |

### Weighted summary (fill after the gates)

| Candidate | Output (§5) | Build-time (§6) | Maintenance (§7) | Extensibility (§8) | Risk (§9) | Total |
|---|---|---|---|---|---|---|
| A-01 | | | | | | |

## 12. Spike protocol

No stack is scored on argument alone; K-20, K-13, K-22 and K-31 cannot be reasoned
about, only measured. To keep spikes comparable, each one renders **the same thin
vertical slice** from real KDM:

1. One classlike page (P-05) of a KMP declaration with several fragments — exercises
   E-003 signature tokens, E-040/E-041/E-042 platform panes, E-031 member rows,
   E-021 doc body.
2. One package page (P-03) with the tab strip (E-032).
3. Cross-page chrome: C-01 TOC, C-03 source-set filter, C-07 theme toggle.
4. The whole reference project generated end to end, for K-20/K-21/K-24 numbers.

Inputs: `dokka-integration-tests/gradle/projects/ui-showcase` (KDM already produced
via the `kdp` output) for correctness, plus one large project for scale — TODO,
see Q-005.

Recorded per spike: wall clock, peak RSS, output bytes, page count, dependency-set
size, and the diff-vs-current-HTML for the slice. Same machine, same JDK, numbers in
the candidate card.

## 13. Questions this document waits on

Proposed additions to `questions.md`, numbered from Q-030 to avoid collision.

| ID | Question | Blocks |
|---|---|---|
| Q-030 | Minimum JDK for *running* the new renderer. Today's toolchain baseline is 8; GraalJS needs 17/21+ | K-02, and therefore every pure-JVM JS-engine option |
| Q-031 | How strictly "everything inside the JVM"? Pure bytecode in-process / per-platform native binaries shipped as a dependency / a runtime downloaded on first use | K-01, K-03 — decides whether Javet, QuickJS, native GraalJS are on the table |
| Q-032 | Does "static HTML" mean *readable with JS disabled*, or only *files served without a server*? | K-10, and whether build-time prerendering is required at all |
| Q-033 | Can `resc-ui` ship inside an Apache-2 artifact on Maven Central? The current output already vendors a `ui-kit` (S-06) — confirm what that precedent actually permits | K-06, K-35 |
| Q-034 | Budgets: acceptable generation time and added artifact size, as numbers | K-08, K-20 |
| Q-035 | Who owns the renderer's frontend long-term — the frontend team, the Dokka team, or shared? An answer changes K-30, K-31, K-32 substantially | § 7 weights |
| Q-036 | Is an optional SPA output actually wanted, or merely tolerated? | K-44 |

Existing questions that also gate this file: Q-002 (non-HTML formats → K-26),
Q-003 (versioning/all-modules → K-43), Q-004 (extension-point compatibility → K-40),
Q-005 (reference projects → § 12), Q-013 (identity/URLs → K-14), ADR-0001
(renderer-side indices → K-24).

## 14. Explicitly not criteria

To keep the comparison from drifting: *popularity*, *modernness*, *what the last
demo looked like*, *how much of the current renderer can be copy-pasted*, and
*resume value*. Also not a criterion: how the output looks in the spike — the spike
tests the stack, not the design, and §5 asks whether a redesign stays cheap (K-36).
