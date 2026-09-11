# KDM-based HTML API reference renderer — working docs

Working documentation for the KDM-based HTML API reference renderer.

- KEEP: <https://github.com/Kotlin/KEEP/discussions/484>
- Meta issue: <https://youtrack.jetbrains.com/issue/KTL-4815>

## Current stage: requirements discovery

Goal of this stage: produce a defensible answer to *"what must the new HTML output
do so that it is no less capable than the current one"* — and, more importantly,
split that answer into **what KDM must carry** vs **what the renderer owns**.

The first half is the expensive one: a gap in the model is discovered late and is
costly to fix, because the model is a published, compatibility-constrained artifact.
The second half can be iterated on freely.

## Layout

| Path | What it is |
|---|---|
| `problem.md` | Task framing, links |
| `glossary.md` | Every term used in these documents, defined from zero — Dokka, Kotlin, JVM, KDM, build, ecosystem |
| `KDM spec.md` | The model itself: open questions + draft structure |
| `requirements/00-method.md` | How we gather requirements; definition of parity; acceptance criteria |
| `requirements/10-feature-registry.md` | **Main artifact** — one row per feature, consolidated from the inventories |
| `requirements/20-config-surface.md` | Inventory: user-visible configuration options |
| `requirements/30-pipeline-surface.md` | Inventory: transformers, resolvers, signature providers — implicit behaviour |
| `requirements/40-page-anatomy.md` | Inventory: real generated pages, by page kind and by the entities on each — first pass done against `ui-showcase` |
| `requirements/50-extension-points.md` | Inventory: who builds on top of the HTML output and what they need |
| `requirements/60-model-gaps.md` | **Output** — requirements on KDM, derived from the registry |
| `decisions/` | One ADR per decision |
| `questions.md` | Open questions with owner and blocking status |

## Reading order for a newcomer

`problem.md` → `glossary.md` → `requirements/00-method.md` →
`requirements/10-feature-registry.md` → `requirements/60-model-gaps.md`.

If any term in these documents is unclear, it is in `glossary.md` — including the
terms that are ambiguous in this project (`glossary.md § 8`).

## Conventions

- Feature IDs are stable: `F-###`. Questions: `Q-###`. Decisions: `ADR-####`.
- The page inventory adds local IDs, scoped to `40-page-anatomy.md`: `P-##` page kind,
  `E-###` entity, `C-##` cross-page chrome, `S-##` sidecar artifact, `O-##` observation.
- Cross-reference by ID, not by prose. The registry links out; discussion lives in
  `questions.md` and `decisions/`, never in the registry itself — otherwise the
  registry stops being scannable.
- Docs are in English, to keep them usable as input to KEEP #484.
