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
| Q-003 | Are `plugin-versioning` and `plugin-all-modules-page` part of parity? | | yes | open |
| Q-004 | Do we commit to current extension-point compatibility, or clean break + migration path? Dackka is the load-bearing case | | yes | open |
| Q-005 | Which reference projects form the acceptance set? | | no | open |
| Q-006 | Naming: model and docs say KDM, packages and output dir say `kdp`. When is this unified? | | no | open |

## Model design

| ID | Question | Owner | Blocking | Status |
|---|---|---|---|---|
| Q-010 | Is KDM produced pre-filtered (visibility, deprecated, `@suppress`) or unfiltered with render-time filtering? (G-03) | | yes | open |
| Q-011 | Does `reportUndocumented` stay a Dokka-pipeline concern, or become a KDM-consumer tool? | | no | open |
| Q-012 | Cross-declaration indices (extensions, inheritors, inherited members): stored in KDM, computed by the renderer, or a separate derived artifact? (G-02) | | yes | open |
| Q-013 | Declaration ID scheme, and whether the current URL scheme must be preserved exactly (G-01) | | yes | open |
| Q-014 | Documentation representation and inheritance — KT-88346, KT-88347 (G-04) | | yes | open |
| Q-015 | `@sample`: inline bodies or resolvable references? (G-11) | | no | open |
| Q-016 | External references: keep Dokka's `package-list` format, or something new? (G-06) | | no | open |
| Q-017 | Fragment deduplication scheme; analyse leaf source sets only? (G-12) | | yes | open |
| Q-018 | Search index: derived at build time, or a published artifact? (G-15) | | no | open |

## Assumptions currently in force

None yet. When a question is set to `assumed`, record here what we are assuming and
what changes if the assumption turns out wrong — so that the cost of a late answer
stays visible.
