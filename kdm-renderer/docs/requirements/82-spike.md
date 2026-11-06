# Spike: HTML navigation (dokka#3434)

Findings of the spike [Kotlin/dokka#3434](https://github.com/Kotlin/dokka/issues/3434),
recorded as input for the KDM-based renderer's navigation (F-100), search transport
(F-101) and multi-module aggregate (F-120).

**Decision:** the current renderer gets **no** navigation work. Everything the spike
asked about is designed into the new renderer instead. The estimates in § 3 are kept only
so the trade-off stays visible.

Like `80` and `81`, this file is **descriptive**. It assigns no F-/G-/Q- IDs. Items that
belong in the registry, in `questions.md` or in an ADR are listed in § 6 for
consolidation (`00-method.md § 5`).

---

## 0. Source

- The spike asks whether #2150 (page reload on sidebar click), #3133 (`navigation.html`
  too large) and #1795 (unusable without an HTTP server) can be solved in one pass. The
  goal is to avoid rewriting navigation three times.
- The questions in the spike: split `navigation.html` into lazily loaded per-module
  files; avoid page reloads with vanilla JS or a React rewrite; fix offline mode by
  splitting, or by wrapping the payload as `.js`.
- The spike links two of its questions to the wrong issues: #2738 is *"Make
  not-found-version page dynamic"*, #1999 is *"Offline dokka"*.
- Method: reading the code on `master` at `f2caaa149` (2026-09-30). **Nothing was
  measured.** The estimates are expert judgement. Numbers come from the issues.

---

## 1. How navigation works today

| # | Fact | Where |
|---|---|---|
| N-1 | The whole tree goes into one HTML fragment, `navigation.html`: module → packages → classlikes + top-level callables → nested classlikes. Classlikes list only classlike children | `NavigationPage`, `NavigationDataProvider.navigableChildren` |
| N-2 | Every page load does `fetch(pathToRoot + 'navigation.html')` and inserts the result with `innerHTML` (the XSS surface in #2859) | `scripts/navigation-loader.js` |
| N-3 | After insertion the script walks the DOM several times: collapse all, expand the path to the current page, restore state. Expanding the path scans every `toc--part` once per `pageId` prefix, which is O(nodes × depth). Matching uses `includes`, not equality | `expandTocPathToCurrentPage` |
| N-4 | Each node carries heavy markup: `div.toc--part`, a positional `id` (`$navId-$n`), `pageId` (the raw DRI string), `data-nesting-level`, a `button` with an inline `onclick`, icon spans, `<wbr>` breaks and a "Skip to content" link. The same data as JSON would be several times smaller | `NavigationPage.visit` |
| N-5 | Expanded parts and scroll position survive reloads through `sessionStorage`. The keys are the positional ids, so they break when the tree changes. #2150's remaining pain is the reload itself (flicker, re-fetch, re-parse), not lost state | `navigation-loader.js` |
| N-6 | Multi-module: `finish()` merges all module fragments and writes the merged file to the root **and again into every module directory**, with hrefs rewritten to `../`. That is N+1 copies of the largest file, each at its own URL, so the HTTP cache never hits across modules. This is the mechanism behind #3133 (130 MB, AWS SDK) and #4061 (~80 GB) | `AddToNavigationCommandHandler` |
| N-7 | Search has the same shape: `scripts/pages.json` is loaded with `fetch`. Offline, navigation **and** search are broken | `SearchbarDataInstaller`, `components/search/search.tsx` |
| N-8 | There is already a precedent for data wrapped in JS: `scripts/sourceset_dependencies.js` (`sourceset_dependencies='…'`). It works on `file://` | `SourcesetDependencyAppender` |
| N-9 | Per-page scripts (`platform-content-handler.js`, `prism.js`, `navigation-loader.js`) are IIFEs that run once on `DOMContentLoaded`. Nothing can re-initialise them on swapped-in content | `htmlPreprocessors.kt` |
| N-10 | Fonts are already bundled (`@fontsource/inter`). Of #1795's two asks, only the `fetch` calls are left | `plugin-base-frontend/package.json` |

---

## 2. Browser constraints (independent of the renderer)

| # | Constraint | Consequence |
|---|---|---|
| B-1 | Browsers block `fetch`/XHR from `file://` by default | Splitting into `.json` **does not help** offline. The payload format does not matter; the transport does |
| B-2 | `<script src>` works on `file://` | Data wrapped as JS (`register('<id>', JSON.parse('…'))`) works offline, including lazily injected chunks. `JSON.parse('…')` keeps parse cost close to raw JSON for large payloads. Same-origin scripts pass a typical `script-src 'self'` CSP |
| B-3 | Partial-page navigation (intercept a link, `fetch`, swap, `pushState`) needs `fetch` | It cannot work on `file://`. It can only be progressive enhancement, with a normal navigation as the fallback |
| B-4 | Cross-document View Transitions (`@view-transition { navigation: auto; }`) remove the reload flicker without JS. Unsupported browsers ignore the rule | A near-free mitigation for #2150. It does not remove the re-fetch or re-parse. Browser support not verified in the spike |
| B-5 | Inlining the full tree into every page works offline and without JS | The output size multiplies by the page count. At 130 MB of navigation this is not an option |

---

## 3. Answers to the spike's questions

| Spike question | Answer | Effort in the current renderer (not planned) |
|---|---|---|
| Split `navigation.html` into lazily loaded files? | Yes, but **per package**, not only per module: stdlib is one module with 1.7 MB of navigation. Shape: a small root index plus one chunk per package, with the chunk of the current page loaded eagerly. Needs stable node ids, client-side node rendering and chunk-aware state restore | ~2–3 weeks, +1 week for multi-module |
| Avoid page reloads with vanilla JS? | Viable as progressive enhancement (B-3). Needs re-initialisable page scripts (N-9), links resolved against an absolute root instead of `pathToRoot`, and `popstate`, anchor, scroll and focus handling. `customAssets` scripts assume full loads, so it needs an opt-out | ~2–3 weeks; View Transitions ~1 day |
| Rewrite navigation in React? | Not worth it. A real SPA needs page content as data, which is a new renderer. It breaks FreeMarker customisation and no-JS reading, and does not fix `file://`. React for the tree component alone only pays off if the tree must be virtualised | SPA 2–3+ months; tree component ~2–3 weeks |
| Offline: does splitting into JSON help? | No (B-1) | — |
| Offline: wrap the payload as `.js`? | Yes, this is the fix (B-2, N-8). Downsides: a global callback, chunk load errors are coarser than with `fetch`, and third-party tools can't read plain JSON (emit both if needed). Search needs the same treatment (N-7) | ~1 week together with removing N-6 |

---

## 4. Implications for the KDM renderer

What the spike means once navigation moves into the new renderer. Each row points at
the existing ID it feeds.

| # | Implication | Refs |
|---|---|---|
| I-1 | **Navigation is derived from the page set** (KTL-5111, `kdm-pages`), not from a separate provider. The sidebar and the pages can then never disagree (KTL-4133). URLs are computed at build time and the frontend only reads them | F-100, KTL-5113 |
| I-2 | **Sidecar data is loaded as JS, not with `fetch`**, for navigation (S-03) and search (S-02) alike. Otherwise K-05 (`file://`) fails | K-05, S-02, S-03, F-101, Q-018 |
| I-3 | **Navigation is sharded per package** and stored **once per site**. A root index lists modules and packages. The shard format and URLs are fixed from the first commit, because relocating them later changes a public output contract (the same argument ADR-0002 makes for URLs) | S-03, K-42, ADR-0002 |
| I-4 | **No per-module copies.** ADR-0002 takes the whole artifact set as input, so the merged tree has one location. N-6 has no counterpart in the new design | F-120, ADR-0002, #4061 |
| I-5 | **If Q-032/Q-043 resolve to "readable without JS", each page must carry its local branch of the tree in HTML**: the path to the page plus its siblings. The full tree then arrives as enhancement. A design that renders the whole tree in JS (§ 3, row 1) fails K-10. Bounding the branch size matters for large packages (K-13, KTL-4154) | K-10, K-13, Q-032, Q-043 |
| I-6 | **Node ids come from the model**, not from tree position (N-5). They are needed for shard keys, stored UI state and anchors. This is blocked on Q-013: ids are measurably not unique (`jvm/main`: 132 unique ids for 238 elements) | G-01, Q-013 |
| I-7 | **No `innerHTML` with fetched markup.** Nodes are built from data. Check CSP compatibility | #2859 |
| I-8 | **Page scripts are re-initialisable** (`init(root)`, not run-once IIFEs), UI state lives in the URL where it should survive navigation, and links resolve against an absolute root / base URL. This keeps partial navigation (B-3) and View Transitions (B-4) cheap to add later, even if v1 does full page loads | F-205, KTL-1771, #1390 |
| I-9 | **An in-browser SPA is not a navigation requirement.** Reloads are solved by B-3/B-4 on top of static output. Whether an SPA is wanted at all belongs to K-44/Q-036/Q-041, not to F-100 | K-44, Q-036, Q-041 |
| I-10 | **Shards are deterministic**: stable ordering and stable file names, so the same KDM produces byte-identical navigation | K-07 |

---

## 5. Not verified

- No sizes or timings were measured. Before fixing the shard format, measure on stdlib
  and the AWS SDK for Kotlin: navigation bytes (per file and in total), fetch / parse /
  init time of the current loader, and the number of nodes per package. The largest
  package bounds I-5.
- Browser support for B-4 and the exact per-browser `file://` rules for B-1 were not
  checked.
- The claim that JS-wrapped payloads parse about as fast as JSON (B-2) is general
  knowledge, not measured here.

---

## 6. For consolidation

1. F-100 / S-03: record I-1 … I-3 and I-6 as the navigation requirements, with KTL-5113
   as the implementing task.
2. ADR-0002 defers the *"cross-module navigation tree"* as feature surface. I-3 argues
   that its **format** is architecture and must not be deferred. The feature itself can
   still wait.
3. Q-032 now gates the navigation design too (I-5), not only prerendering.
4. Q-013 gates navigation ids (I-6). Add it to the question's "blocks" list.
5. Q-018: the search index transport is decided by B-1/B-2 regardless of where the index
   is built.
6. A new K- or F- row for "no `innerHTML` of fetched content / CSP-compatible client
   code" (I-7). `81 § 9` item 7 already raises it.
