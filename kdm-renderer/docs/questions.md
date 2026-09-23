# Open questions

One row per question. `Blocking` means work cannot proceed without an answer, as
opposed to work that can proceed under a stated assumption.

Status: `open` · `assumed` (proceeding on an assumption, recorded below) ·
`answered` (→ ADR).

## Scope and process

| ID | Question | Owner | Blocking | Status |
|---|---|---|---|---|
| Q-001 | Definition of parity: informational equivalence as goal, checklist as gate? (`00-method.md § 2`) | | yes | open |
| Q-002 | Are non-HTML formats (GFM / Jekyll / Javadoc) in scope? If yes the inventory must be taken at content-model level | | yes | open |
| Q-003 | Are `plugin-versioning` and `plugin-all-modules-page` part of parity? Narrowed by ADR-0002: only the aggregate page and cross-module navigation are still in question — the indices are the renderer's either way | | yes | open |
| Q-004 | Do we commit to current extension-point compatibility, or clean break + migration path? Dackka is the load-bearing case | | yes | open |
| Q-005 | Which reference projects form the acceptance set? ADR-0002 adds a hard requirement: a multi-module project in which a consumer module declares extensions and subclasses of a producer module's types. Both `ui-showcase` variants are single-module, so a multi-module regression cannot manifest on today's tests | | yes | open |
| Q-006 | Naming: model and docs say KDM, packages and output dir say `kdp`. When is this unified? | | no | open |

## Model design

| ID | Question | Owner | Blocking | Status |
|---|---|---|---|---|
| Q-010 | Is KDM produced pre-filtered (visibility, deprecated, `@suppress`) or unfiltered with render-time filtering? (G-03) | | yes | open |
| Q-011 | Does `reportUndocumented` stay a Dokka-pipeline concern, or become a KDM-consumer tool? | | no | open |
| Q-012 | Cross-declaration indices (extensions, inheritors, inherited members): stored in KDM, computed by the renderer, or a separate derived artifact? (G-02) | | yes | **answered** → ADR-0001 |
| Q-013 | Declaration ID scheme, and whether the current URL scheme must be preserved exactly (G-01). **Now gates ADR-0001** — the renderer's reverse indices join on classifier ids, and ids are measurably not unique | | yes | open |
| Q-014 | Documentation representation and inheritance — KT-88346, KT-88347 (G-04) | | yes | open |
| Q-015 | `@sample`: inline bodies or resolvable references? (G-11) | | no | open |
| Q-016 | External references: keep Dokka's `package-list` format, or something new? (G-06) | | no | **answered** → ADR-0003 |
| Q-017 | Fragment deduplication scheme; analyse leaf source sets only? (G-12) | | yes | open |
| Q-018 | Search index: derived at build time, or a published artifact? (G-15) | | no | open |

## Output surface (from the page inventory, `40-page-anatomy.md`)

| ID | Question | Owner | Blocking | Status |
|---|---|---|---|---|
| Q-019 | A package whose only member is invisible renders as a flat `<package>.html` with an empty body (O-01). Intended output, or should the package be filtered out? | | no | open |
| Q-020 | Enum entries get their own directory and full classlike page (O-06). Does KDM model an enum entry as a declaration with members? | | no | open |
| Q-021 | Declaration-kind icons (30 SVGs keyed by kind + Kotlin/Java origin, F-200): is the kind enum part of KDM, or a renderer-side classification? | | no | open |
| Q-022 | Intermediate source sets appear in `sourceset_dependencies.js` but have no page-level presence (F-206). Must KDM carry intermediate fragments? | | no | open |
| Q-023 | Inherited members are materialized into the artifact (`kotlin/Any/equals` and friends listed in `class.callables`, 31 foreign elements in `jvm/main`, `kotlin/Any/equals` repeated 30× identically in one fragment). Keep the materialization, or carry only the supertype edge and let the renderer expand it? (ADR-0001) | | no | open |
| Q-024 | Member extensions (an extension declared inside a class) land in the renderer's `extensionsOf` index naturally, but today's HTML does not list them on the receiver type's page. Which behaviour is correct? (ADR-0001) | | no | open |
| Q-025 | How is a materialized inherited or otherwise external declaration distinguished from one the module owns? No field marks provenance; the only discriminator today is string surgery on the id's package prefix against `module.packages`, which breaks for a module documenting package `kotlin` | | yes | open |
| Q-026 | Single-module renderer first with multi-module added later, or multi-module architecture from the first commit? | | yes | **answered** → ADR-0002 |

## klibs.io integration (`requirements/70-klibs-io.md`)

Scope as decided by the owner: hosting API reference is the primary use; KDM behind
klibs.io's API/MCP is wanted; API search and the artifact-registry role are undecided;
cross-version diffs are out. KDM reaches klibs.io from Maven Central, or submitted
directly by an author; klibs.io will not generate it itself. Requirements are wanted
now; implementation is **not** part of v1.

| ID | Question | Owner | Blocking | Status |
|---|---|---|---|---|
| Q-040 | Which problem does a browser-rendered (SPA) mode actually solve for klibs.io — generation/storage cost, cost of *re*-generation after a renderer release, features static output cannot give (cross-library search, view-time link resolution), or reuse of their Next.js frontend? Each has a cheaper alternative; lazy generation + an S3 cache covers the first two. Proposal: state the reason as a number, and separate "KDM is the primary storage" (likely yes) from "rendering happens in the browser" (likely not needed). Gates the stack survey via K-44 (O-70-02) | | yes | open |
| Q-041 | If browser rendering is needed, where is the seam: (a) prerender on the klibs.io backend — the same code, a second place to run it, cost ≈ 0; (b) static shell + per-page JSON; (c) full client-side render from KDM. Cost of (c) = page/URL model in `commonMain` (cheap, O-70-03) + an isomorphic view layer from the first commit (expensive to retrofit) + a second parity test suite, after which K-07 byte-determinism covers only half the product. Proposal: (a) normative, (b) an allowed evolution, (c) only if Q-040 answers "features" — and then it is a stack *gate*, not a criterion | | yes | open |
| Q-042 | Does klibs.io serve KDM to the browser, or a presentation-shaped transform of it? A separate format frees KDM from being convenient to render (R-70-06) but adds a third versioned artifact. Proposal: yes to a presentation format, but as the renderer's **private** contract — otherwise two formats carry compatibility guarantees instead of one. Possibly the same artifact as Q-047(b) (R-70-13) | | no | open |
| Q-043 | Must klibs.io-hosted API reference be readable and indexable **without JS**? klibs.io exists for discovery; pages invisible to search-engine and LLM crawlers subtract from its purpose (O-70-09). Proposal: yes, and as a gate — which closes `stack-criteria.md` Q-032 in favour of "readable with JS disabled" and therefore closes Q-041 in favour of (a)/(b) | | yes | open |
| Q-044 | What exactly is the "shared FQN→URL resolver" from KTL-4815: (a) a pure function `(coordinates, element id) → URL` in `commonMain`, shared by the renderer and klibs.io; (b) a sidecar file — today's `package-list` (ADR-0003, S-01) or its replacement; (c) a hosted redirect service. Proposal: (a)+(b) for builds; (c) is a separate product (permanent links, MCP/AI) and must never be called at generation time, because that violates gate K-04 (offline/hermetic builds). (a) is impossible until Q-013/G-01 is answered (R-70-03) | | yes | open |
| Q-045 | Who aggregates? klibs.io's unit is a GitHub project with N Maven packages; KDM arrives per module, independently, possibly at mismatched versions (O-70-07). Options: (a) klibs.io aggregates by calling the renderer with an artifact set; (b) an aggregated KDM is published (by whom?); (c) no aggregation — one package, one reference. Proposal: (a), which makes "arbitrary, possibly inconsistent artifact set → site" a renderer requirement (R-70-10, R-70-11) that ADR-0002 does not yet cover, since a single build's module set is consistent by construction | | yes | open |
| Q-046 | How does klibs.io discover that KDM exists, and under which coordinates? (a) a classifier convention detectable from the Maven Central index — no artifact download, the same place `kotlin-tooling-metadata.json` is read today; (b) the `klibs-io-notifier` Gradle plugin (KTL-2138) also reporting a KDM publication; (c) direct author upload. Proposal: (a) primary, (b) an accelerator, (c) the manual path. Requires a fixed classifier (R-70-04, so Q-006 becomes blocking) and a size budget (R-70-02) | | yes | open |
| Q-047 | What does klibs.io's API/MCP expose over KDM: (a) the artifact as a blob; (b) point queries (signature + KDoc for one declaration); (c) signature search? Today there are two MCP tools and no declaration-level entity in the schema. Proposal: (b) as the target, (a) as a first step — (a) alone is not a product at 3–30 MB per answer, and (b) is most of the way to (c), so the "API search: undecided" line is effectively decided by whether (b) happens (O-70-08, R-70-13) | | no | open |

## Assumptions currently in force

None yet. When a question is set to `assumed`, record here what we are assuming and
what changes if the assumption turns out wrong — so that the cost of a late answer
stays visible.
