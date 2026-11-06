# Inventory: open GitHub issues on Dokka's HTML output

What the open issues in [Kotlin/dokka](https://github.com/Kotlin/dokka/issues) about the
HTML format and its UX tell us about the KDM-based renderer. This is the companion to
`80-open-issues.md`: that file covers what kotlinlang.org sees, this one covers what
external Dokka users see.

Like `50`, `70` and `80`, this file is **descriptive**. It decides nothing and assigns
no F-/G-/Q- IDs. It only points at existing ones. Rows that belong in the registry or
the gaps get moved there during consolidation (`00-method.md § 5`).

Many issues (#3252–#3307) come from a 2023 UX audit. Their descriptions are a sentence
and a screenshot, so the "implication" column is a hypothesis unless it says otherwise.

---

## 0. Source

- Snapshot date: **2026-09-30**. 95 issues matched.
- Query (GitHub search; the comma inside `label:` is OR):

```
repo:Kotlin/dokka is:issue is:open label:"UX","format: html","html: frontend","feedback: KTL"
```

- Most upvoted: #1795 (27 👍), #2517 (6), #1390 (4), #1888, #1961, #2249 (3 each).
- Duplicates of KTL issues in `80`: #3219 ↔ KTL-4154, #3666 ↔ KTL-1422,
  #3607 ↔ KTL-1488, #3090 ↔ KTL-1331, #3664 ↔ KTL-1421, #2120/#3160 ↔ KTL-1709.
  #2397, #3254 and #3257 are the same state-scoping problem as KTL-1771.
- Excluded as unrelated to the renderer: #3059 (single-target MPP, a runner concern).

---

## 1. Requirements on the KDM model

| Issue | Request | What KDM must carry | Refs |
|---|---|---|---|
| #1300 | Show typealiases in signatures instead of the expanded type | Both the alias and the expanded type in the type model. **Not in G-07**, which implicitly assumes one type | G-07, F-060 |
| #796 | Override hierarchy as in Javadoc ("Overrides", "Specified by") | A callable → overridden callables edge: a third forward edge next to ADR-0001's two, and the basis for F-045 | ADR-0001, F-045, G-04 |
| #1396 | Usages: where a type appears as a parameter or return type | Nothing new. It is a reverse index over every type reference (ADR-0001 pattern), and an expensive one | ADR-0001 |
| #1862, #3922, #3924 | Filter out and de-emphasize `@RequiresOptIn` APIs. Filters for extensions, protected and deprecated members | Annotations alone (G-08) aren't enough: the model must say that an annotation class is itself `@RequiresOptIn`, and at what level. Client-side filters need data attributes in the markup | G-08, F-081 |
| #3853 | Deprecated constructors are not struck through | G-09 must cover constructors too | G-09 |
| #3970 | The deprecation message is visible only on the member's own page, not in the parent's listing | A structured deprecation message, so it can be shown in summary form | G-09 |
| #4251 | Backticked names render inconsistently | The raw identifier plus a "needs quoting" marker, not a pre-quoted string | G-01, G-07 |
| #4330, #3148 | CSS upper-cases source set names ("ANDROID", "WASM") | The fragment display name is taken from the model verbatim. The renderer doesn't guess from "jvm-like" | G-12, F-080 |
| #4177 | The "Web" tab looks selected by default but shows Js content | Intermediate source sets and their relation to leaf ones | F-206, Q-022 |
| #3625 | `@param` names are highlighted but not clickable | A link from the tag to the parameter's declaration, i.e. parameter anchors | G-04, F-063 |

## 2. Documentation representation: the Markdown cluster

| Issue | Symptom |
|---|---|
| #1962 | Links to heading anchors inside Markdown are not recognized |
| #2249 (3 👍) | Make every heading linkable, as on GitHub (depends on #1962) |
| #1907 | No way to write links that open in a new tab |
| #1961 (3 👍) | A valid image URL is rejected as an unrecognized resource type |
| #3330 | HTML inside an indented code block is emitted as markup |
| #3901 | Tables render with an empty header (Dokka 2) |
| #3048 | Blockquotes are unreadable in dark mode |
| #4378 | Inline code gets Kotlin syntax highlighting |
| #2651 | No styling or CSS class for links to code elements |
| #3097 | An `includes` Markdown file that starts with a comment fails the build |

**Implication.** KDoc in KDM should be a lossless CommonMark tree: headings with IDs,
tables with headers, a language on code blocks, inline code kept separate from code
blocks. It should not be pre-rendered HTML or a reduced tree. These issues can serve as
conformance cases for G-04 / Q-014.

## 3. Navigation, running without a server, performance

| Issue | Symptom | Refs |
|---|---|---|
| #1795 (27 👍) | Dokka should be usable without an HTTP server (`file://`, zip). The most upvoted issue in the set | K-10, KTL-2214, KTL-4404 |
| #2964 | Warn users who open pages as files or with JS disabled | K-10 |
| #3133 | `navigation.html` weighs **130 MB** for the AWS SDK for Kotlin and 1.7 MB for stdlib | F-100, S-03, KTL-5113 |
| #4061 | Duplicated per-module `navigation.html` files push a site to **~80 GB** | F-120, ADR-0002 |
| #2150 | Clicking in the sidebar reloads the whole page | F-100 |
| #3254 | Sidebar tree state resets when a package is selected | F-100 |
| #3257 | Switching tabs resets the scroll position | F-080 |
| #3434 | Spike: solve #2150, #3133 and #1795 together, to avoid rewriting navigation three times | F-100 |
| #1601, #3219 | Large pages load slowly and freeze the browser (#3219 = KTL-4154) | K-13 |
| #1855, #3344 | Minify HTML. Remove empty `<span>` elements | K-13 |
| #2859 | DOM-based XSS in `navigation-loader.js`, found by a Coverity scan | — |

**Implication.** These requirements conflict. Inlining the navigation into every page
gives offline and no-JS support, but a 130 MB navigation cannot be inlined. Fetching it
breaks `file://`. The decision has to be made up front, and KTL-5113 is the place to
make it. One possible design: navigation sharded per package, stored once per site, with
only the local branch of the tree inside each page.

Separately, the new client code must not insert fetched HTML with `innerHTML` (#2859).
Check CSP compatibility too.

## 4. URLs and site configuration

| Issue | Request | Refs |
|---|---|---|
| #1390 (4 👍) | A base URL, for hosting under a subpath (`domain/project1`) | F-205 |
| #4180 | A configurable URL slug, because `KBox` becomes `-k-box`. The escaping scheme for capital letters is public and irritating | G-01, Q-013 |
| #1643 | Multi-module output nests each module twice (`image/image`) | F-120, KTL-5112 |
| #1727, #1888 (3 👍) | No module hierarchy for nested Gradle projects | F-120, ADR-0002 |
| #1868 | Module order differs between the sidebar and the index page and can't be configured | F-120 |
| #1305 | Generate a sitemap | — (with KTL-1756) |
| #3761 | Open Graph meta tags | F-205 (with KTL-1755) |
| #2517 (6 👍) | A custom favicon | F-105, F-205 |
| #3882 | A customizable search icon | F-105 |
| #4409 | The default footer uses `Year.now()`, so the output is not reproducible | F-106, K-07 |

## 5. Tabs, filters and page composition

| Issue | Symptom / request | Refs |
|---|---|---|
| #2396 | Defaults to the "android" tab instead of "common" | F-080 |
| #2397 | Select a platform tab through the URL fragment (would also fix KTL-1771) | F-080 |
| #3261 | Merge tabs whose content is identical into one tab labelled with all targets | F-080, G-12 |
| #3294 | Switching the tab doesn't change the content | F-080 |
| #3293 | A filter with a single category is redundant. Non-clickable elements look clickable | F-081 |
| #3666 | Section title stays when the filter leaves it empty (= KTL-1422) | F-081 |
| #3664 | Disabled filter buttons flicker when switching sections | F-081 |
| #3667, #2779 | Rework the tabs component. Redesign the platform selector, whose purpose is unclear | F-080, F-081 |
| #4262 | Tables with line breaks break after a platform tab switch | F-080 |
| #3923 | Package pages hide functions and properties behind tabs, which defeats `Ctrl+F`. The request is a flat page, which conflicts with §3 | K-10, K-13 |
| #3987 | Make "entries" the default tab for enums (KT-74035) | E-030 |
| #3975 | The class page shows only a short property description, and users want the full one | G-04 |
| #4381 | Hide extension functions from the TOC | F-100, F-020 |
| #3789 | An undocumented primary constructor gets an empty page (same as KTL-4651) | F-202 |

## 6. Search

| Issue | Symptom | Refs |
|---|---|---|
| #2864 | Exact matches come last | F-101 |
| #3252, #4038 | Fuzzy matching by subsequence returns many irrelevant results | F-101 (with KTL-1684) |
| #3253 | Packages and modules can't be found | F-101 |

**Implication.** Packages and modules belong in the index. Ranking should put exact
matches first and prefer prefix or substring matches over subsequence ones (G-15, Q-018).

## 7. Accessibility, quality, samples

| Issue | Topic | Refs |
|---|---|---|
| #3277 | Semantic HTML (`<nav>`, `<pre><code>`, `<dfn>`) | F-204 |
| #3282 | Broken links on published sites: the tests need a link checker | K-33 |
| #3579 | Playwright screenshot tests against ui-showcase (KTL-1447) | K-33, KTL-1144 |
| #1281 | `@sample` without Kotlin Playground: offline formatting | G-11, F-108 |

## 8. Cosmetics

These don't affect architecture. Keep them as a checklist for the new design:
#3258, #3259 (symbol indicators), #3268 (table columns), #3271 (tables on mobile),
#3273 (code overflows its container), #3284 (code block margins), #3286 (tab alignment),
#3290 (mark external links), #3292 (tabs jump), #3295 (parameter table capitalization),
#3296 (TOC item consistency), #3297 (breadcrumb line breaks), #3301 (code area styling),
#3303 (segment dividers), #3306 (narrow margins), #3307 (text-based diagrams),
#2120 and #3160 (scrollbar theming), #2359 and #3607 ("Since" block), #2667 (copy-link
icon on wrapped words), #3090 (inheritor name width), #4024 (style discussion),
#4597 (missing table border).

---

## 9. Not yet covered in `10`/`60`/`questions.md`/`80`

To be raised during consolidation:

1. Typealiases in signatures: the alias and the expanded type together (#1300).
2. The "overrides" edge (#796).
3. A `@RequiresOptIn` marker on annotation classes, for filtering and de-emphasis (#1862, #3922, #3924).
4. Documentation as a lossless CommonMark tree (the §2 cluster).
5. Navigation scale as an upper bound for KTL-5113: 130 MB for the AWS SDK, 80 GB of duplicates (#3133, #4061).
6. Base URL and a configurable slug (#1390, #4180).
7. Security of the client-side JS (#2859).
8. Fragment display names taken from the model verbatim (#4330, #3148).
