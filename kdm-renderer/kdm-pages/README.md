# kdm-pages

A prototype that answers one question: **given KDM json, can we derive the set of page
URLs the renderer is supposed to produce?**

No HTML, no frontend. Input — the `kdm/model.json` files produced as a side effect of a
normal Dokka run; output — a sorted text file, one URL per line, diffable against the
current Dokka output.

Why Kotlin and not TS: URL/identity is not a frontend concern. It is the model's contract
(G-01, Q-013, S-01 `package-list`, S-02 search index, S-03 navigation) and it is produced
at build time — the frontend reads URLs, it never computes them. Writing it in Kotlin
keeps types over KDM for free (K-34) and leaves the frontend-stack decision
(`../docs/stack-criteria.md`) open. The module is deliberately plain `kotlin("jvm")`;
moving the logic to `commonMain` of a KMP module is a directory move if the browser
ever needs it.

## Running

One entry point: `src/main/kotlin/main.kt`: a raw-JSON inventory print, then a typed read into
the real `KdBundle` model — vendored under `src/main/kotlin/org/jetbrains/kotlin/documentation`,
copied verbatim from the `kotlin-documentation-model` prototype (KEEP #484). From the IDE —
the gutter icon next to `main`, no run configuration needed; with no arguments it reads
`models/kmp/model.json` (the richer of the two showcase modules — every page-set rule meets
there), and a path can be passed as the first argument. The raw print still tolerates the
older shapes on disk (the branch `saveModule` output, the old `Kd*` serialization in
`samples/kdp/`); the typed read only accepts the real `KdBundle` shape.

From the command line (Gradle needs a JDK 11+ launcher, so `JAVA_HOME` is set explicitly —
a shell pointing at JDK 8 fails with `Error resolving plugin [id: 'dokkasettings']`):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -q :kdm-renderer:kdm-pages:run
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -q :kdm-renderer:kdm-pages:run \
  --args="$PWD/kdm-renderer/samples/kdp/kdm-sample.json"
```

The default input is migrated from the branch `saveModule` output, which only a Dokka run
from this branch writes — a site generated from `master` has no `kdm/` directory. Regenerate
as below, then run `./migrate-model.py`, before running without arguments.

Regenerating the reference site, which also destroys whatever is there (the integration
test copies with `copyRecursively` and fails on an existing directory):

```bash
rm -rf dokka-integration-tests/gradle/build/ui-showcase-result
DOKKA_TEST_OUTPUT_PATH="build/ui-showcase-result" JAVA_HOME=$(/usr/libexec/java_home -v 21) \
  ./gradlew :dokka-integration-tests:gradle:testUiShowcaseProject
```

## Models in the real model shape

`models/{jvm,kmp}/model.json` are the two `ui-showcase-result/*/kdm/model.json` files rewritten
into the shape of the real `KdBundle` model — the same types the prototype itself compiles
against — so the page-set work runs on the real model before Dokka emits it natively.
`samples/simpleKmp.kdm.json` holds one genuine analyzer-produced bundle, copied from the
Kotlin repo, as a cross-check. Rebuild with `./migrate-model.py [path to ui-showcase-result]`;
the list of what it changes is at the top of the script.

Some of it is inferred, because the branch output does not carry the information:

- expect/actual — there is no `actuality` in the input; a declaration that differs from the one
  in its `dependsOn` fragment is taken as an actual of it. On the showcase this finds exactly
  `KmpClass`, `asyncWithDelay` and `getCurrentDate`, actual in js/jvm/linux/macos.
- the kmp input repeats declarations of `dependsOn` fragments without their documentation
  (108 copies, the same analyzer artifact as below); those copies are dropped.
- inherited members with a substituted signature (`Enum.compareTo(other: E)` in each enum) can't
  share one original, so they stay full elements under the owner's id — 6 in jvm.
- the originals of inherited members (`kotlin/Any`, `kotlin/Enum`, `kotlin/Throwable` — 19 in jvm)
  go to `embeddedElements`, because no stdlib KDM exists yet to reference them in.
- `@since` KDoc tags are not in the input at all; `since` comes only from `@SinceKotlin`.
- `documentation` is dropped: `KdDocumentation` is an empty placeholder in the model. For some
  expect/actual pairs the docs are the only difference, so the comparisons inside
  `migrate-model.py` keep them and only the written output strips them.
- the input carries no visibility and no flags like `isError`, `isLateinit`, `getter`/`setter`
  or `overridens`; the output writes `PUBLIC` / `FINAL` / `false` / empty defaults.

## Baseline

`expected.txt` — 131 lines, one per content page of the reference output, written the way a
browser shows the URL: `index.html` becomes a trailing slash, `.html` is dropped, segments
are percent-encoded.

```
/
/kmp/org.jetbrains.dokka.uitest.kmp/
/kmp/org.jetbrains.dokka.uitest.kmp/%5Bjs%5Dmy-extension
```

Excluded: `older/` (P-15 archived snapshots), the three `navigation.html` payloads (P-13)
and `not-found-version.html` (P-14) — 135 html files, 131 content pages, matching
`../docs/requirements/40-page-anatomy.md`.

Rebuild with `./expected.sh [path to the site]`. Never hand-roll it: the first version of
this baseline was taken by hand from a directory generated by this branch, and every
multiplatform row in it was wrong.

The reference is one multi-module site — root `/`, then `/jvm/` and `/kmp/`. Exactly six of
its pages carry a platform prefix (`%5Bjvm%5Dshared`, `%5Bmacos%5Dprint-pointer-raw-value`, …);
those are genuine DRI clashes and the only place a source set reaches a URL. Platform
divergence everywhere else lives *inside* a page, as `data-filterable-set` /
`data-togglable` attributes — so a URL identifies a declaration, never a (declaration,
fragment) pair.

**The reference site must be generated from `master`.** This branch's
`DefaultSymbolToDocumentableTranslator` drops `filterSymbolsInSourceSet`, so every source
set re-emits every declaration it can see; the HTML that comes out has one page per
declaration *per source set* (`[apple]-foo/`, `[common]-foo/`, …, 113 of 121 pages) and the
KDM it writes is inflated the same way. Useful as input to look at, useless as a baseline.

## Steps

| # | Step | Verified by | Status |
|---|---|---|---|
| 0 | Inventory of the input | `out/inventory-*.txt` read by hand | done, but measured on branch-generated KDM |
| 1 | Baseline of expected URLs | `expected.txt`, 131 lines, from a master-generated site | done |
| 2 | Module / package / classlike pages, no name sanitization | `out/urls-*.txt` vs the container rows of `expected.txt` | redo — the numbers on record were measured against the wrong baseline |
| 3 | `identifierToFilename` ported from `DokkaLocationProvider` | P-05, P-06 converge | todo |
| 4 | Member pages: constructors (P-09), top-level (P-10), members (P-11) | overload collisions become a number | todo |
| 5 | What owes no page: materialized externals (Q-025), P-04, P-07, P-08 | diff shrinks to the fragment question | todo |
| 6 | Fragments: one page per declaration (Q-017, E-044) | kmp diff closes | todo |
| 7 | Multi-module aggregate (ADR-0002) | `/`, `/jvm/`, `/kmp/` under one root | todo |
