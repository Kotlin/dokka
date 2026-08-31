# Method, scope and definition of parity

Status: **draft** — the scope questions below are unanswered and block sizing.

## 1. Why this stage exists

The new renderer consumes KDM instead of Dokka's `Documentable`/`ContentNode` model.
Anything the current HTML output derives from data that KDM does not carry simply
cannot be rendered. So the purpose of this stage is not to produce a feature wish
list — it is to find, before the model is frozen, every place where the current
output depends on information that KDM would drop.

Everything else (layout, CSS, component structure) is renderer-local and can change
later at low cost. The registry therefore always asks two questions per feature:
*is it in scope for parity?* and *what does it demand from the model?*

## 2. Definition of parity

Three candidate definitions, in decreasing strictness:

| | Definition | Consequence |
|---|---|---|
| A | Pixel-equivalent pages | Freezes the current layout. Rejected — redesign freedom is part of the point. |
| B | Functional equivalence against a checklist, on a set of reference projects | Practical, testable |
| C | Informational equivalence — everything reachable on an old page is reachable on a new one, layout free | The actual goal |

**Proposal: C is the goal, B is the acceptance gate.** TODO: confirm — see Q-001.

### Acceptance gate (draft)

Reference projects, each chosen to exercise a different axis:

| Project | Exercises |
|---|---|
| TODO: e.g. `kotlinx.coroutines` | Large MPP, expect/actual, many source sets |
| TODO: a JVM + Java project | Java interop, mapped types, synthetic properties |
| TODO: CK core | Scale; the 1 MB / 3 MB / 30 MB numbers in `KDM spec.md` came from here |
| TODO: multi-module + versioned project | `plugin-all-modules-page`, `plugin-versioning` |

For each, every `must` row in the registry is checked by hand once, then pinned by a test.

## 3. Scope — UNRESOLVED

These three answers change the size of the stage substantially.

- **Q-002 — Non-HTML formats.** Are GFM / Jekyll / Javadoc in scope? If yes, the
  inventory must be taken at the *content model* level rather than the HTML level,
  which is considerably wider.
- **Q-003 — `plugin-versioning` and `plugin-all-modules-page`.** Separate plugins
  with their own multi-module notion. Do they count as part of "the HTML API
  reference" for parity purposes?
- **Q-004 — Third-party plugin compatibility.** Do we commit to keeping the current
  extension points working, or does the new renderer start from a clean API?
  Dackka (Google) is the load-bearing case — see `KDM spec.md § Java` and
  `50-extension-points.md`.

## 4. Four sources of requirements

Collected separately and deliberately, because each one misses a different class of
feature. None of them subsumes the others.

| Source | Inventory | Gives | Blind spot |
|---|---|---|---|
| Configuration | `20-config-surface.md` | Finite, formal, enumerable | Most behaviour has no option |
| Pipeline code | `30-pipeline-surface.md` | Implicit semantics: extension grouping, inheritors, obvious-function suppression, DRI resolution | Says nothing about what the user perceives |
| Generated pages | `40-page-anatomy.md` | Pure UX: search, source-set filter, breadcrumbs, code copying, theming | One project won't exercise every variant |
| External consumers | `50-extension-points.md` | Requirements on extensibility | Not derivable from code; needs conversations |

## 5. Procedure

1. Settle this file: parity definition (Q-001) and scope (Q-002…Q-004).
2. Fill `20`, `30`, `40`, `50` in parallel — they are independent.
3. Consolidate into `10-feature-registry.md`; deduplicate; assign `F-###` IDs.
4. Fill in the *"Requires from KDM"* column for every row. **This is the step the
   whole stage exists for** — steps 2–3 without it produce only a list.
5. Extract `60-model-gaps.md`; diff it against `KDM spec.md`; route the delta to
   KEEP #484 and to `questions.md`.

## 6. Registry row schema

| Column | Meaning |
|---|---|
| ID | `F-###`, stable forever |
| Feature | One line, user-facing phrasing where possible |
| Implemented in | File / extension point / plugin |
| Layer | `model` / `renderer` / `config` / `ep` |
| Consumer | Who actually relies on it |
| Parity | `must` / `should` / `drop` |
| Requires from KDM | The load-bearing column; empty means renderer-local |
| Status | `todo` / `analysed` / `decided` |
| Refs | `Q-###`, `ADR-####` |

Rows are never deleted, only marked `drop` with a reason recorded in an ADR.
