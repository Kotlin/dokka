# Inventory: open YouTrack issues against the current API reference

What the open issues on the current Dokka-based API reference (kotlinlang.org and the
Dokka template) tell us about the KDM-based renderer: which defects it must not
reproduce, and which requirements they add to `10`/`60`.

Like `50` and `70`, this file is **descriptive**. It decides nothing and assigns no
F-/G-/Q- IDs. It only points at existing ones. Rows that belong in the registry or the
gaps get moved there during consolidation (`00-method.md § 5`).

Most of the §1 issues come from the website feedback form. Their descriptions are one
line long and nobody has investigated the root causes, so the "implication" column is
a hypothesis unless it says otherwise.

---

## 0. Source

- Snapshot date: **2026-09-30**. 58 issues matched.
- Query:

```
project: KTL
Subsystem: {kotlinlang.org. Api references}, {Dokka integration}
State: -Fixed, -{Won't fix}, -Obsolete, -Duplicate
```

- Parent meta issues: KTL-4833 (generated output correctness), KTL-4832 (Dokka template
  UI quality), KTL-4834 (build and publishing), KTL-1395 (site integration quality).
- Excluded as work on the renderer itself: KTL-4815, KTL-5111, KTL-5112, KTL-5113.
- Excluded as unrelated to the renderer: KTL-4209, KTL-4017, KTL-1658.
- Excluded as already resolved despite matching the query: KTL-2227 (As designed),
  KTL-1154 (Can't Reproduce), KTL-1112 (As designed).

---

## 1. Missing or wrong content (KTL-4833)

| Issue | Symptom | Implication for KDM / renderer | Refs |
|---|---|---|---|
| KTL-4653 | `List` page doesn't surface inherited and extension members ("Where are all the methods of List?") | ADR-0001's reverse index makes this possible. It conflicts with §2, because `List` has hundreds of stdlib extensions | F-020, F-022, Q-024 |
| KTL-4497 | `List` page shows no way to create one: no `List()`, no `listOf()` | **Not in `10`/`60`.** Needs a type → factory-function relation, either same-name top-level functions or functions returning the type. A reverse index over `returnType.classLikeId` could derive it, but the policy has to be decided | F-020 (analogous) |
| KTL-4366 | `Result` page doesn't list `Companion.failure` | Deciding which companion members to promote onto the owner's page is a renderer policy. The model must mark companion-ness explicitly | E-030 |
| KTL-4133 | `kotlin.time` package page omits declarations that the sidebar shows | The sidebar and the package page disagree. This argues for one page set as the single source of both | F-100, KTL-5111, KTL-5113 |
| KTL-4651 | `copyRecursively` page renders empty | Build-time validation: an empty declaration page should be an error or a warning | F-202 |
| KTL-4475 | kotlinx.datetime package page fails to render | Probably the same as §2 (unverified) | K-13 |
| KTL-4748 | `@Suppress` page doesn't show that the parameter is `vararg` | `vararg` must be in the parameter model. Check it on the PoC, not only in the spec. Fixed in Dokka: dokka#4556 | G-07, F-060 |
| KTL-1427 | `NSDate` in kotlinx-datetime renders as `<Error class: unknown class>` because the reference is built on Linux | **Not in `60`.** The model needs an explicit *unresolved type* representation, and the renderer needs a defined degradation instead of leaking the analyzer's error text | G-07, G-12 |
| KTL-1327 | kotlin-metadata-jvm: source link missing | — | G-05, F-064 |

These pages belong in the parity reference set. Under definition C of `00-method.md`,
parity means not reproducing these defects.

## 2. Performance and page weight

| Issue | Symptom | Implication | Refs |
|---|---|---|---|
| KTL-4154 (+ former KTL-989, KTL-2226) | Large package pages (`kotlin.collections`, `kotlinx.cinterop`) load slowly and freeze the browser on tab switch and window resize. The problem survived every Dokka version including 2.0. The issue explicitly asks whether KTL-4815 fixes this structurally. Public: dokka#3219 | DOM size is an architectural decision, not a styling one. Options: split tabs and platforms at build time instead of hiding them with JS, paginate, load lazily, or split into several pages. This also caps how far KTL-4653 can go | K-13 |
| KTL-2328 | Every page embeds a 10 KB logo, across about 80K pages | No inline assets in the per-page template | K-13 |
| KTL-1320 | CSS caching issue (no details) | Content-hashed asset file names (cache busting) | F-105 |

## 3. URLs, anchors, redirects

| Issue | Symptom | Implication | Refs |
|---|---|---|---|
| KTL-552 (3 votes) | No linkable per-member headers. Anchors look like `#-1580382563%2FClasslikes%2F1975948010`. The request is for Javadoc-style `#join(java.lang.CharSequence,...)` | Readable anchors derived from a stable ID. Changing the anchor scheme breaks existing links, which adds weight to Q-013 | F-063, G-01, Q-013 |
| KTL-1530 | Some `api/latest/jvm/stdlib/...` → `api/core/...` redirects are missing since the 2.0 migration | **Not in `10`/`60`.** If the URL scheme changes, the renderer should emit a redirect map (old URL → new URL) as an output | Q-013 |
| KTL-5131 | E2E test in production for old Core References and their redirects (follows KTL-5077) | The same compatibility surface, now under test | Q-013 |
| KTL-4163 | Remove old unsupported Core API versions, with redirects | Same | F-122 |
| KTL-1467 | On kotlinlang, the anchor target ends up under the site's top bar | Hosts must be able to set the anchor scroll offset (`scroll-margin-top`) | F-063 |

## 4. Host-site integration (kotlinlang.org)

Nothing in `10`–`70` covers `kotlin-web-site`, SEO metadata or the site's E2E tests yet.

| Issue | Symptom | Implication | Refs |
|---|---|---|---|
| KTL-4702, KTL-2932 | kotlin-web-site templates need manual updates on every Dokka release. For 2.1.0 the site needs a `div#resizer`, `[data-item-type]` attributes and `footer.ftl` changes (comment on KTL-2932) | The DOM and CSS classes have become implicit API. The new renderer needs an **explicit template contract**: slots, variables and stable DOM hooks | F-107, Q-004 |
| KTL-4451 | The kotlinlang template fails outside the site: `process.env.DOKKA_NOINDEX` is null in `base.ftl` | Every variable in the template contract needs a default | F-107 |
| KTL-1144 | Screenshot E2E tests (`kotlin-web-site/test/e2e/api-references-template-customization.spec.ts`) are disabled and should come back | Any DOM change breaks them. Count them as a consumer in K-33 | K-33 |
| KTL-1434 | Dokka's `Cmd+K` search shortcut opens a modal that renders incorrectly on kotlinlang | The host must be able to disable or replace keyboard shortcuts and the search UI | F-101 |
| KTL-1756 | No `<link rel="canonical">`, so search engines pick the canonical page incorrectly | **Not in `10`.** Canonical URL per page | F-205 |
| KTL-1755 | Open Graph tags are copied from the Docs site. `og:site_name` should be per reference, e.g. "Kotlin Coroutines API References" | **Not in `10`.** Per-reference site metadata as configuration | F-205 |
| KTL-1773 | A custom `prism.css` puts gray squares over symbols in code blocks | Changing the code-block markup breaks existing `customStyleSheets` users | F-105 |
| KTL-1473 | Global navigation with search on mobile | Header slot owned by the host | F-107 |

## 5. UI state and filters

| Issue | Symptom | Implication | Refs |
|---|---|---|---|
| KTL-1771 (+ former KTL-1772, KTL-4181) | Platform tabs and Types/Functions/Properties tabs are stored globally, so the selection leaks to other pages and is lost when you return. On kotlin-test the tabs don't respond at all | Decide per-page (or URL-encoded) state before building F-080/F-081 | F-080, F-081 |
| KTL-1422 | With a platform filter off, an empty "Functions" header stays. Proposal in the comments: disable the tab rather than hide it | The renderer needs per-section, per-fragment emptiness precomputed into the markup | F-081, G-12 |
| KTL-1412 | Footer isn't at the bottom when all filters are disabled | — | F-081 |

## 6. Navigation, search, offline, versions

| Issue | Symptom | Implication | Refs |
|---|---|---|---|
| KTL-2214 | `navigation.html` is fetched with JS, so navigation doesn't work offline or without JS. Options in the issue: an iframe, or inlining the navigation into every page (optionally) | Directly K-10 | F-100, C-01, S-03, K-10 |
| KTL-4404 | Users want to download the reference for offline use | Nearly free if the output is a self-contained static site (a zip) | K-10 |
| KTL-1684 | Search matches individual characters instead of words | Search index quality is a requirement, not only its location | F-101, G-15, Q-018 |
| KTL-4452 | kotlinx.coroutines has no docs for previous versions. None of the kotlinx libraries do, and the stdlib versioning is described as "quite suboptimal" | Redesign versioning rather than port it | F-122 |
| KTL-2823, KTL-1399 | Library versions in the reference config are updated manually. kotlin-metadata-jvm docs releases aren't aligned with Kotlin releases | Build and publishing, outside the renderer. Versions should come from the input artifacts | F-122 |

## 7. Cosmetics

These don't affect architecture. Keep them as a checklist for the new design:
KTL-1488 (restyle "Since"), KTL-1635 (icons), KTL-1331 (inheritor name width),
KTL-2225 ("Type" wraps), KTL-2235 (footer text moves), KTL-1700 (mobile search
spacing), KTL-1709 (scrollbar), KTL-2881 (version selector width), KTL-1414 (animation
after a version switch).

---

## 8. Not yet covered in `10`/`60`/`questions.md`

To be raised during consolidation:

1. Explicit representation of unresolved types and how the renderer degrades on them (KTL-1427).
2. Type → factory functions / constructors relation (KTL-4497).
3. Page completeness vs page weight, which pulls F-020/F-022 against K-13 (KTL-4653 vs KTL-4154).
4. A redirect map as a renderer output when the URL scheme changes (KTL-1530, KTL-5131, KTL-4163).
5. Host-embedding contract: template variables with defaults, stable DOM hooks,
   disableable shortcuts and search, anchor scroll offset, canonical and OG metadata
   (KTL-4702, KTL-2932, KTL-4451, KTL-1434, KTL-1467, KTL-1756, KTL-1755).
6. Where UI state lives, per page or in the URL (KTL-1771).
7. Asset policy: no inline assets, content-hashed file names (KTL-2328, KTL-1320).
