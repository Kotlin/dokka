# Inventory: page anatomy

The only inventory taken from the *outside*: generate the current HTML and walk the
result page kind by page kind, recording everything a reader can see or do. This is
what catches features that exist in neither the config nor the model — empty states,
filter state, URL shapes, sidecar artifacts.

Status: **first pass done** against the `ui-showcase` reference project (below).
Every row is an observation from the generated HTML, not from reading renderer code.

Grouping is deliberately two-level, as requested: **page kind** → **entities that
appear on that page kind**. A page kind is a distinct template with its own URL
shape; an entity is a repeatable region inside it. The same entity recurs across page
kinds, so entities are defined once in § 4 and referenced by ID from § 3.

IDs: `P-##` page kind, `E-###` entity, `C-##` cross-page chrome, `S-##` sidecar
artifact, `O-##` observation worth a decision. `F-###` / `G-##` / `Q-###` are the
existing IDs from `10-feature-registry.md`, `60-model-gaps.md` and `questions.md`.

## 1. Input used

Reference project — chosen because it exists precisely for this purpose; its README
states the goal as "as much variety of UI elements in one project as possible, so
that during refactorings we can compare the outputs between different versions of
Dokka":

```
dokka-integration-tests/gradle/projects/ui-showcase
```

Two Gradle modules: `jvm` (single-platform, Kotlin + Java sources) and `kmp`
(`commonMain`/`jvmMain`/`jsMain`/`linuxMain`/`macosMain`, expect/actual, cinterop,
context parameters), plus `previousDocVersions/{0.9,1.0,1.5,1.8,1.9}` so the
versioning plugin is exercised.

Generated output walked:

```
dokka-integration-tests/gradle/build/ui-showcase-result
```

Regeneration:

```bash
export DOKKA_TEST_OUTPUT_PATH="build/ui-showcase-result"
./gradlew :dokka-integration-tests:gradle:testUiShowcaseProject
```

Shape of the output: 19 MB, 1064 files — **135 HTML pages** outside `older/`
(797 including the five bundled previous versions), 161 SVG, 42 JS, 36 CSS, 12 JSON,
10 font files, one `package-list` per version.

Coverage caveat: `ui-showcase` does **not** exercise value classes, `@MustBeDocumented`
annotation arguments of every shape, MathJax, Kotlin Playground samples,
`kotlin-as-java`, custom FreeMarker templates, or `customAssets`. Those stay TODO and
need a second reference project — see `00-method.md § 2`.

## 2. Page kinds observed

| ID | Page kind | URL shape in the output | Count | Entities present |
|---|---|---|---|---|
| P-01 | Multi-module aggregate | `/index.html` | 1 | E-001, E-002, E-037 (module rows) |
| P-02 | Module page | `/<module>/index.html` | 2 | E-001, E-002, E-036 (package rows, platform-tagged) |
| P-03 | Package page | `/<module>/<package>/index.html` | 11 | E-001, E-002, E-020, E-030..E-032 (Types / Properties / Functions buckets) |
| P-04 | Package page, degenerate | `/<module>/<package>.html` | 1 | E-001, E-002 only — tab strip and body render **empty** (O-01) |
| P-05 | Classlike page | `/<module>/<package>/-<class>/index.html` | 29 | E-001..E-009, E-020..E-026, E-030..E-035, E-040..E-046 |
| P-06 | Nested / inner / companion classlike page | `…/-<outer>/-<inner>/index.html` (2 levels deep observed) | 6 | as P-05; nesting is expressed only by the path |
| P-07 | Enum entry page | `…/-<enum>/-<e-n-t-r-y>/index.html` | 3 | as P-05 — a full classlike page (O-06); parent carries the `Entries` header |
| P-08 | Typealias page | `…/-typealias-kotlin-class/index.html` | 1 | E-001, E-002, E-003 (expansion inside the signature) |
| P-09 | Member page — constructor | `…/-<class>/-<class>.html` (directory name == file name) | 23 | E-001..E-009, E-021, § 4.5 doc blocks |
| P-10 | Member page — top-level function or property | `/<module>/<package>/<name>.html` | 28 | as P-09, plus E-047 for extension receivers |
| P-11 | Member page — member function or property | `…/-<class>/<name>.html` | 20 | as P-09 |
| P-12 | Member page, platform-disambiguated | `…/[jvm]shared.html`, `…/[macos]print-pointer-raw-value.html` | 6 | as P-10 — **the DRI-clash escape hatch is visible in the URL** (O-02) |
| P-13 | Navigation payload | `/navigation.html`, `/<module>/navigation.html` | 3 | C-01 only; injected client-side, not a browsable page |
| P-14 | Version-not-found page | `/not-found-version.html` | 1 (+1 per archived version) | chrome only, empty `#content` (O-03) |
| P-15 | Archived version snapshot | `/older/<version>/**` | 5 full copies | a frozen copy of P-01..P-14 |

Total: 135 HTML files, of which 131 are content pages carrying the full chrome
(§ 3) — the remaining four are the three P-13 payloads and P-14.

Not present in this output and therefore still unwalked: search-results state (search
is client-side over S-02, there is no results page), a 404 page for a dead
declaration URL, and any per-page "on this page" outline.

## 3. Cross-page chrome

Identical on all 131 content pages; every item is a candidate `renderer`-layer row.

| ID | Chrome element | Evidence in the output | Registry row |
|---|---|---|---|
| C-01 | Navigation sidebar (TOC tree) | `navigation.html` → `div.toc--part[pageid][data-nesting-level]`, injected by `scripts/navigation-loader.js`; per-node expand button, `toc--skip-link` | F-100 |
| C-02 | Search | `navigation-controls--btn_search` + `scripts/pages.json` | F-101 |
| C-03 | Source-set / target filter | `ul.filter-section` with one `button.platform-tag[data-filter=":kmp/jvmMain"]` per source set, a dropdown mirror, and `data-filterable-current` / `data-filterable-set` on every filterable element (392 occurrences) | F-081 |
| C-04 | Breadcrumbs | `div.breadcrumbs` — module → package → classlike → member, `span.current` for the leaf | F-102 |
| C-05 | Version switcher | `div.versions-dropdown` listing `2.0` + `older/{1.9,1.8,1.5,1.0,0.9}`, wrapped in `<dokka-template-command …ReplaceVersionsCommand>` (O-04) | F-122, Q-003 |
| C-06 | Library name + version header | `library-name--link`, `library-version` | F-106 |
| C-07 | Theme toggle | inline blocking script reading `localStorage["dokka-dark-mode"]`, `prefers-color-scheme` fallback | F-104 |
| C-08 | Footer, homepage link, copyright | `div.footer--content`, "Generated by", "© 2026 Copyright" | F-106 |
| C-09 | Copy-to-clipboard | `copy-popup-wrapper` on every code block, sample and anchor (295 anchor wrappers) | F-103 |
| C-10 | Mobile TOC dropdown | `dropdown--list_toc-list`, `theme-dark_mobile` | F-100 |
| C-11 | Skip links / ARIA | `toc--skip-link`, `role="listbox"`, `aria-expanded`, `aria-pressed` throughout | new — see § 6 |
| C-12 | Page title and favicon | `<title>SimpleKotlinClass</title>`, `images/logo-icon.svg` | new — see § 6 |

## 4. Entities

The repeatable regions. "Needs from KDM" is the point of the whole exercise; the last
column is the gap that must cover it.

### 4.1 Headers and identity

| ID | Entity | Rendered as | Needs from KDM | Gap |
|---|---|---|---|---|
| E-001 | Breadcrumb trail | `div.breadcrumbs` | Parent chain of a declaration, and a stable URL per ancestor | G-01 |
| E-002 | Page heading | `h1.cover`, with `<wbr>` inserted at camel-case boundaries | Display name separate from identity; the word-break hints are renderer-local | G-01 |
| E-003 | Declaration signature | `div.symbol.monospace` with `span.token.{keyword,punctuation,operator,function,constant,string}` (4.8k tokens total) and links on every type | Structured signature: modifiers, generics + variance, parameter names, default values, receiver, return type — not a pre-rendered string | G-07 |
| E-004 | Platform badges | `span.platform-tag.{jvm,js,native,common}-like` (487) | Fragment membership per declaration, plus the platform→CSS-class mapping | G-12 |
| E-005 | Source link | `span.source-link[data-element-type]` → `https://github.com/…/<file>` | File path (+ line) per declaration per fragment | G-05 |
| E-006 | Anchor + copy-link | `div.anchor-wrapper[data-name]` + `anchor-icon` | Stable, URL-safe per-member anchor derived from identity | G-01 |
| E-007 | Deprecation banner | `div.deprecation-content` → `h3` "Deprecated (with error)", message paragraph, `h4` "Replace with" + a code block | Deprecation level, message and `ReplaceWith` as fields | G-09 |
| E-008 | Annotations on a declaration | rendered inline into E-003 | Annotations with arguments, plus the rule for which are shown | G-08 |
| E-009 | `@since` / `@SinceKotlin` badge | `h4` "Since" inside the doc block | Since-version as a field | G-10 |

### 4.2 Documentation body

| ID | Entity | Rendered as | Needs from KDM | Gap |
|---|---|---|---|---|
| E-020 | Brief description | `div.brief` in list rows | First-sentence extraction — or a pre-split brief/detail | G-04 |
| E-021 | Full description | `p.paragraph` and friends inside `div.content` | Doc tree, not HTML | G-04 |
| E-022 | Markdown constructs | headings `h1`–`h4`, `ul`/`ol`/`li`, `em`/`strong`, `blockquote`, inline `code`, tables as `div.table--container` + `div.table-row`, images, links | The whole `KdDocumentationNode` vocabulary, tables included | G-04 |
| E-023 | Code blocks in KDoc | `div.sample-container` → `pre > code.monospace.block.lang-kotlin[theme=idea]`, highlighted by `scripts/prism.js` | Language tag on code blocks | G-04 |
| E-024 | `@sample` | `div.sample-container` with the *resolved body* of the referenced function (7 occurrences on 5 pages) | Either inlined sample bodies or a resolvable reference | G-11 |
| E-025 | Links in KDoc | `<a>` to internal declarations, and to external stdlib/JDK targets | `KdLinkReference` + external reference resolution without the target's KDM | G-06 |
| E-026 | Custom / unknown tags | `span.kdoc-tag` (15) — e.g. "Author", "Additional info" | Unknown tags must round-trip, not be dropped | G-04 |

### 4.3 Member lists (the bulk of every index page)

| ID | Entity | Rendered as | Needs from KDM | Gap |
|---|---|---|---|---|
| E-030 | Section header | `h2.tableheader` / `h4.tableheader`. Observed vocabulary and counts: Functions 46, Constructors 23, Types 12, Properties 9, Type Parameters 2, Companion functions 2, Companion properties 1, Inheritors 1, Entries 1 | The classification that produces each bucket: callable kind, companion-ness, enum-entry-ness, inheritor-ness | G-02, G-07 |
| E-031 | Member row | `div.table-row.table-row_content` → `div.main-subrow.keyValue` = name + anchor + signature + brief | Per-member: name, signature, brief, fragment set, URL | G-01 |
| E-032 | Tab strip | `div.tabs-section` with `button.section-tab[data-togglable]`. Observed tab sets: `CONSTRUCTOR,TYPE,PROPERTY,FUNCTION` (38 pages), plus `EXTENSION_FUNCTION` / `EXTENSION_PROPERTY` variants, i.e. the "Members" vs "Members & Extensions" split | Declaration kind, and an extension flag on each member | G-07 |
| E-033 | Inheritors list | one page only: `-simple-kotlin-interface/index.html` | Forward `superTypes[].classLikeId`; the reverse index is the renderer's, over its whole input set | G-02, ADR-0001 |
| E-034 | Extensions listed on the receiver's page | `EXTENSION_FUNCTION` tabs (10) | Forward `receiverParameter.type.classLikeId`; the reverse index is the renderer's | G-02, ADR-0001, Q-024 |
| E-035 | Inherited members | `equals`/`hashCode`/`toString` appear in the Functions bucket | Already materialized in `class.callables`; still needs "inherited from X" provenance and an obvious/synthetic marker | G-03, ADR-0001, Q-023, Q-025 |
| E-036 | Package rows on a module page | `div.table-row.table-row_platform-tagged` (12) | Package list per module + fragment set per package | G-13 |
| E-037 | Module rows on the aggregate page | `div.table-row.table-row_multimodule` | Module list + module documentation | G-13 |

### 4.4 Platform divergence

| ID | Entity | Rendered as | Needs from KDM | Gap |
|---|---|---|---|---|
| E-040 | Platform-hinted block | `div.platform-hinted[data-platform-hinted]` — 1191 occurrences on 127 pages | Every documented thing is per-fragment, not per-declaration | G-12 |
| E-041 | Platform tab bar | `div.platform-bookmarks-row` → `button.platform-bookmark[data-toggle=":kmp/jsMain"]`, one per fragment, `data-active` on the default | Fragment identity + ordering + display name (`":kmp/jsMain"` → "js") | G-12 |
| E-042 | Per-fragment content pane | `div.content.sourceset-dependent-content[data-togglable=":kmp/commonMain"]` — 350 panes for `:jvm/main` alone | Signature *and* documentation independently per fragment | G-12 |
| E-043 | expect/actual presentation | one tab per fragment, `expect class` on common and `actual class` on each target, each with its own description text ("Common description", "JS description", …) | expect/actual linkage; open in the spec | G-12 |
| E-044 | Merged-declaration row | `data-filterable-set=":kmp/commonMain,:kmp/jsMain,:kmp/jvmMain,:kmp/linuxMain,:kmp/macosMain"` — one row standing for five fragments | The dedup rule: when are two fragments' declarations "the same"? | G-12 |
| E-045 | Source-set dependency graph | `scripts/sourceset_dependencies.js`: `{":kmp/jvmMain":[":kmp/commonMain"], ":kmp/macosMain":[":kmp/appleMain"], …}` — 13 source sets, including intermediate ones (`appleMain`, `nativeMain`, `webMain`) that have **no** page-level presence | Fragment dependency edges, intermediate fragments included | G-12 |
| E-046 | Filter-driven visibility | `data-filterable-current` vs `data-filterable-set` on 392 elements | Per-element fragment set, so the client can hide rows | G-12 |
| E-047 | Context / receiver parameters | `Receiver` heading (2), context-parameter sample in `kmp/common/ContextReceiversExample.kt` | `KdReceiverParameter` / `KdContextParameter` | G-07 |

### 4.5 Doc-block sub-entities on member pages

Observed KDoc-derived headings and counts, each a section that must be
reconstructible: `Return` 5, `Parameters` 5 (as a table of name → description),
`Throws` 5 (type → description), `See also` 4 (link → description), `Samples` 4,
`Author` 4, `Since` 4, `Replace with` 2, `Receiver` 2, `Type Parameters` 2.

Maps to G-04 for the doc tree, G-07 for the typed parts (`KdReturns`, `KdThrows`,
parameter identity), G-06 for `See also` link targets, G-11 for samples.

## 5. Sidecar artifacts

Not pages, but part of the output contract — a consumer breaks if they change.

| ID | Artifact | Content | Needs from KDM | Gap |
|---|---|---|---|---|
| S-01 | `package-list` | `$dokka.format:html-v1`, `$dokka.linkExtension:html`, then one `$dokka.location:<DRI>/<relative url>` line per clashing declaration and one line per package | The DRI-to-URL mapping, in a format other projects already consume as an *input* | G-06, G-01 |
| S-02 | `scripts/pages.json` | 142 entries × `{name, description, location, searchKeys}`; `searchKeys` = short name, short name again, fully-qualified name | Search index — derived artifact or built from KDM? | G-15 |
| S-03 | `scripts/navigation-loader.js` + `navigation.html` | the TOC tree, `pageid` = raw DRI string | Hierarchy + stable IDs | G-01 |
| S-04 | `scripts/sourceset_dependencies.js` | see E-045 | Fragment graph | G-12 |
| S-05 | `version.json` | `{"version":"2.0"}` | — (config) | — |
| S-06 | Static assets | `styles/` (5 CSS), `ui-kit/` (min css/js + 10 fonts), `images/` (30 SVG incl. per-kind icons `class-kotlin.svg`, `enum.svg`, `abstract-class.svg`, …), `scripts/` (7 JS) | The declaration-kind → icon mapping is data, not styling | G-07 |

## 6. New registry rows this pass proposes

Nothing here exists in `10-feature-registry.md` yet. Numbered from F-200 to avoid
colliding with the code-survey block.

| ID | Feature | Layer | Parity | Why it only shows up from the outside |
|---|---|---|---|---|
| F-200 | Declaration-kind icons in the TOC and member lists | renderer, model | TODO | 30 SVGs keyed by kind + origin (Kotlin vs Java); needs a kind enum, not a rendered label |
| F-201 | Camel-case word-break hints (`<wbr>`) in names and packages | renderer | TODO | Pure presentation, but it is why long names are readable — `LongElementNames.kt` exists to test it |
| F-202 | Degenerate flat page for a package with no visible members | renderer | TODO | O-01 — an empty state nobody designed |
| F-203 | Platform-prefixed file names for clashing DRIs | renderer, model | TODO | O-02 — the clash escape hatch is baked into public URLs |
| F-204 | ARIA roles, skip links, keyboard affordances | renderer | TODO | Not visible in the model or the config at all |
| F-205 | `<title>` / favicon / `pathToRoot` per page | renderer | TODO | Needed for tab titles and for any relocatable output |
| F-206 | Intermediate source sets present in the dependency graph but absent from the UI | model | TODO | E-045 — asymmetry between what is modelled and what is shown |
| F-207 | Blocking inline theme script (no flash of light theme) | renderer | TODO | An ordering constraint on the generated `<head>` |

## 7. Observations needing a decision

| ID | Observation | Suggested owner |
|---|---|---|
| O-01 | `org.jetbrains.dokka.uitest.emptypackage` renders as a flat `<package>.html` with an empty tab strip and empty body — a package whose only member is a private class. Is this intended output, or should the package be filtered (F-007)? | new question |
| O-02 | Six member pages carry a platform prefix in the file name (`[jvm]shared.html`) because their DRIs clash. Any identity scheme change relocates these URLs. | Q-013, G-01 |
| O-03 | `not-found-version.html` has chrome but a completely empty `#content`. | Q-003 |
| O-04 | The version dropdown ships wrapped in an unresolved-looking `<dokka-template-command data="…ReplaceVersionsCommand…">` element on 131 of 135 pages. Confirm whether that element is meant to survive into the final output. | Q-003, F-121 |
| O-05 | `searchKeys` duplicates the short name twice per entry (`["ONE","ONE","…SimpleKotlinEnumClass.ONE"]`). If the search index is a KDM-derived artifact, its schema should be designed rather than inherited. | Q-018, G-15 |
| O-06 | Enum entries get their own directory *and* page (`-o-n-e/index.html`), i.e. they are full classlike pages. Confirm that KDM models an enum entry as a declaration with members. | new question |

## 8. What is still unwalked

- Interactive behaviour: nothing here was verified in a browser. Search ranking,
  filter persistence across pages, tab-selection persistence, and deep-link-to-anchor
  on load all need a run with a real browser.
- Value classes, `kotlin-as-java`, MathJax, Playground, custom templates and assets —
  absent from `ui-showcase` (§ 1 caveat).
- A dead-URL / broken-anchor case.
- `older/` snapshots were treated as opaque copies; the versioning plugin's rewriting
  behaviour was not inspected.
- **KMP inheritance and extensions are unverified.** The `kmp` variant of
  `ui-showcase` contains no inheritance at all: `superTypes` appears in none of its
  12 fragments. So the multiplatform behaviour of E-033 (inheritors) and of fragment
  deduplication for E-034 (extensions) rests on no observation. Acceptance needs a
  KMP project with an `expect`/`actual` hierarchy and extensions declared in an
  intermediate source set. This matters more since ADR-0001, which puts both indices
  in the renderer.
- **Whether a page is even owed** was not settled for materialized external
  declarations: `jvm/main` lists 31 `kotlin/Any/*` and `kotlin/Enum/*` declarations as
  real elements inside `class.callables`, and no field marks them foreign (Q-025).

## Output

Rows from § 4 and § 6 feed `10-feature-registry.md`; the "Needs from KDM" column and
the gap references feed `60-model-gaps.md`. Anything on a page here that cannot be
reconstructed from KDM is by definition a gap entry — that is the whole reason for
walking the pages instead of only reading the code.
