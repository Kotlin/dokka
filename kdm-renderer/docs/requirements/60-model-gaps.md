# Requirements on KDM (output of the requirements stage)

**This is the deliverable.** Everything the HTML renderer needs that KDM must carry,
derived from the *"Requires from KDM"* column of `10-feature-registry.md`.

Each entry states: what the renderer needs · which features depend on it · whether
`KDM spec.md` already covers it · what is still undecided.

Status: **seeded from the code survey only.** `40-page-anatomy.md` and
`50-extension-points.md` will add entries. Nothing here is agreed yet.

Coverage legend: ✅ covered by `KDM spec.md` · 🟡 partially covered · ❌ absent.

---

## G-01 — Stable declaration identity 🟡

**Need.** A stable ID per declaration: for internal links, for member anchors, for
deduplicating expect/actual and shared source sets, and for disambiguating clashes.

**Depends:** F-024, F-025, F-061, F-063.

**Spec:** `§ Links/ids to expect/actuals`, `§ Backward/forward compatibility guarantees`.

**Undecided.** The URL scheme is derived from this. Changing it breaks every inbound
link that exists today — bookmarks, blog posts, StackOverflow answers, IDE links.
Whether the new renderer must reproduce the current URL scheme exactly is a product
decision, not a technical one. See Q-013.

## G-02 — Cross-declaration indices ❌

**Need.** Three reverse indices that today are computed by transformers over the
whole module: receiver → extensions, supertype → inheritors, supertype → inherited
members.

**Depends:** F-020, F-021, F-022.

**Spec:** partially — `§ Inheritance in the absence of a KDM artifact` acknowledges
the cross-artifact case.

**Undecided.** Three options, each with different costs: (a) KDM stores the indices,
(b) the renderer computes them by loading the whole model, (c) a separate derived
index artifact. Option (b) fails for supertypes outside the module, which is the
common case (`Any`, collections, third-party base classes). See Q-012.

**This is the highest-risk gap** — it is the one place where a per-declaration
serialized model is structurally at odds with what the pages show.

## G-03 — Filter predicate inputs 🟡

**Need.** For every filter that today runs before serialization, KDM must carry the
input so that filtering can happen at render time instead: visibility, deprecation
status, `@suppress`, "obvious/synthetic" marker, inherited-from provenance.

**Depends:** F-001, F-002, F-003, F-005, F-006, F-022.

**Undecided.** Where filtering happens is a fundamental design choice. If KDM is
produced already filtered, the artifact cannot serve two consumers with different
visibility needs, and `documentedVisibilities` becomes a producer-side option
forever. If KDM is unfiltered, artifacts grow and private API leaks into published
files. See Q-010.

## G-04 — Documentation representation ❌

**Need.** KDoc content in a form the renderer can lay out: structured, with all tags
preserved, unknown tags surviving round-trip, and inherited documentation resolvable.

**Depends:** F-040, F-041, F-045.

**Spec:** `§ Documentation` is a stub; KT-88346 (serialization of docs) and KT-88347
(documentation inheritance) are both open.

**Undecided.** Everything. See Q-014.

## G-05 — Source information ✅

**Need.** File path and line per declaration, to build VCS source links.

**Depends:** F-064. **Spec:** `§ Source information`.

**Undecided.** Whether the *resolved* remote URL or only the local path + line is
stored — i.e. whether `sourceLinks` config is applied by the producer or the renderer.

## G-06 — External references 🟡

**Need.** A way to reference declarations outside the module — stdlib, JDK, other
libraries — resolvable without the target's KDM being available, since most targets
will not publish one for years.

**Depends:** F-062, and the Java mapped-type story.

**Spec:** `§ Java` covers mapped Java declarations; the general external-link case is
thinner.

**Undecided.** Does the renderer keep consuming Dokka's `package-list` format?
See Q-016.

## G-07 — Structured types and signatures ✅

**Need.** Types as structure, not pre-rendered strings: generics, variance,
nullability, receivers, parameter names and default values.

**Depends:** F-060. **Spec:** `§ Types`, `§ Parameters`.

**Undecided.** Java primitive/boxed distinction is resolved in the spec via
nullability metadata — verify it survives contact with signature rendering.

## G-08 — Annotations with arguments ✅

**Depends:** F-065. **Spec:** `§ Annotations`.

**Undecided.** Which annotations the renderer shows vs hides is renderer policy,
but the model must carry all of them for that policy to be expressible.

## G-09 — Deprecation as a first-class concept ✅

**Depends:** F-002. **Spec:** `§ Deprecations` — already separated from annotations,
deliberately, because Java and Kotlin differ.

## G-10 — Since-version ✅

**Depends:** F-042. **Spec:** `§ Since`. Must be a field, not pre-rendered text.

## G-11 — Samples 🟡

**Need.** `@sample` bodies, either inlined or as a resolvable reference.

**Depends:** F-043, F-108.

**Undecided.** Inlining bloats the artifact; referencing requires the sample sources
at render time, which breaks the "KDM is self-contained" property. See Q-015.

## G-12 — Fragments and variant semantics 🟡

**Need.** Fragment membership per declaration, expect/actual links, and Android
variant handling — plus the deduplication scheme that makes the artifact size
acceptable.

**Depends:** F-080, F-081, F-082, F-083, F-123.

**Spec:** `§ Variants of declarations` — the container-level fragment design exists
precisely for this, and the 1 MB / 3 MB / 30 MB measurements are recorded there.

**Undecided.** Deduplication scheme; whether only leaf source sets are analysed.
See Q-017.

## G-13 — Module and package documentation ✅

**Depends:** F-044. **Spec:** `§ Package`, `§ Module`.

## G-14 — Java projection ❌

**Need.** Enough information to generate a Java-shaped API reference.

**Depends:** F-124, and Google/Dackka as a stakeholder.

**Spec:** `§ Java` — "Java in the output" is explicitly listed as undecided.

**Undecided.** Whether this is in scope at all. Blocks Google's adoption decision.
See Q-004.

## G-15 — Search index ❌

**Need.** Whatever the search UI indexes today.

**Depends:** F-101.

**Undecided.** Derived at build time from KDM, or a separate published artifact?
Note that a KDM-based search index would be reusable by consumers other than this
renderer. See Q-018.

---

## Next step

Once `40` and `50` are filled in, diff this file against `KDM spec.md`:

- entries marked ❌ or 🟡 that are **not** already open questions in the spec are the
  ones to raise on KEEP #484;
- entries marked ✅ should be verified against the actual PoC model in
  `kotlin-documentation/kotlin-documentation-model`, not just against the spec text.
