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
| `KDM spec.md` | The model itself: open questions + draft structure |
| `requirements/00-method.md` | How we gather requirements; definition of parity; acceptance criteria |
| `requirements/10-feature-registry.md` | **Main artifact** — one row per feature, consolidated from the inventories |
| `requirements/20-config-surface.md` | Inventory: user-visible configuration options |
| `requirements/30-pipeline-surface.md` | Inventory: transformers, resolvers, signature providers — implicit behaviour |
| `requirements/40-page-anatomy.md` | Inventory: real generated pages, broken down by page type |
| `requirements/50-extension-points.md` | Inventory: who builds on top of the HTML output and what they need |
| `requirements/60-model-gaps.md` | **Output** — requirements on KDM, derived from the registry |
| `decisions/` | One ADR per decision |
| `questions.md` | Open questions with owner and blocking status |

## Reading order for a newcomer

`problem.md` → `requirements/00-method.md` → `requirements/10-feature-registry.md`
→ `requirements/60-model-gaps.md`.

## Conventions

- Feature IDs are stable: `F-###`. Questions: `Q-###`. Decisions: `ADR-####`.
- Cross-reference by ID, not by prose. The registry links out; discussion lives in
  `questions.md` and `decisions/`, never in the registry itself — otherwise the
  registry stops being scannable.
- Docs are in English, to keep them usable as input to KEEP #484.
