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

## Assumptions currently in force

None yet. When a question is set to `assumed`, record here what we are assuming and
what changes if the assumption turns out wrong — so that the cost of a late answer
stays visible.
