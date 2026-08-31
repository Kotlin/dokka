# Inventory: page anatomy

The only inventory taken from the *outside*: generate the current HTML for a
reference project and walk the result page type by page type, recording everything a
reader can see or do. This is what catches features that exist in neither the config
nor the model — search behaviour, filter state, keyboard affordances, empty states.

Status: **not started** — requires a generation run. See `00-method.md § 2` for the
reference project list (also TODO).

## How to produce the input

```bash
./gradlew publishToMavenLocal -Pversion=<x>-SNAPSHOT
# then run Dokka on the reference project, or:
scripts/testDokka.sh    # publish + generate + serve in one go
```

## Page types to walk

For each, record: regions present, what each region is derived from, interactive
behaviour, and what breaks if the underlying data is missing.

- [ ] Module page (root `index.html`)
- [ ] Package page
- [ ] Classlike page — class / interface / object / enum / annotation / value class
- [ ] Function page (incl. an overload set merged onto one page)
- [ ] Property page (incl. accessors)
- [ ] Constructor page
- [ ] Typealias page
- [ ] Enum entry page
- [ ] Multi-module aggregate page (`plugin-all-modules-page`) — scope Q-003
- [ ] Search results / no results
- [ ] 404 / broken anchor behaviour

## Cross-page chrome to walk

- [ ] Navigation sidebar: tree state, current-item highlight, persistence
- [ ] Search: what is indexed, ranking, keyboard shortcut, result grouping
- [ ] Source-set filter: default state, persistence across pages, interaction with tabs
- [ ] Breadcrumbs
- [ ] Version switcher — scope Q-003
- [ ] Theme toggle: default, persistence
- [ ] Code blocks: copy button, syntax highlighting, Playground integration
- [ ] Footer, homepage link, library name/version display
- [ ] Anchor links on members; deep-link behaviour on load

## Per-declaration detail to walk

Checklist per declaration entry — each item is a candidate model requirement:

- [ ] Signature: modifiers, generics + variance, parameter names, default values, receiver
- [ ] Annotations shown (and which are hidden)
- [ ] Platform / source-set badges
- [ ] Deprecation presentation (strikethrough, level, replacement, message)
- [ ] since-version badge
- [ ] Source link
- [ ] KDoc: summary vs full description, all supported tags, `@sample` rendering
- [ ] Inherited-from attribution
- [ ] Extension attribution ("extensions for X")
- [ ] Inheritors list
- [ ] Link targets: internal, external (stdlib / JDK / other libs), unresolved

## Output

Findings feed rows into `10-feature-registry.md`. Anything on this page that cannot
be reconstructed from KDM is a `60-model-gaps.md` entry — that is the whole point of
walking the pages rather than only reading the code.
