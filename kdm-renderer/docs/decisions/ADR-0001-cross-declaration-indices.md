# ADR-0001: Cross-declaration indices are computed by the renderer, not stored in KDM

- **Status:** accepted
- **Date:** 2026-09-11
- **Affects:** F-020, F-021, F-022, G-01, G-02; entities E-030, E-033, E-034, E-035
- **Resolves:** Q-012

## Context

Three reverse relations appear on the pages today, each produced by an after-merge
`documentableTransformer` and attached as an `extra`:

| Relation | Produced by | Consumed as |
|---|---|---|
| receiver → extensions | `ExtensionExtractorTransformer` | `CallableExtensions` extra (E-034) |
| supertype → inheritors | `InheritorsExtractorTransformer` | `InheritorsInfo` extra (E-033) |
| supertype → inherited members | `InheritedEntriesDocumentableFilterTransformer`, `separateInheritedMembers` | member buckets (E-035) |

`saveModule(...)` runs *after* the merge and after those transformers
(`SingleModuleGeneration.kt`: transform-after-merge :44 → `saveModule` :46 →
`createPages` :54), so both extras are physically present on the documentables when
KDM is serialized — and `generation/kdp/transform.kt` reads neither. The drop is
deliberate, not an oversight of ordering.

**What the artifact does carry** (measured on `ui-showcase-result/{jvm,kmp}/kdm`):

- **Extensions are emitted as ordinary callables**, carrying
  `receiverParameter.type.classLikeId` — 4 in `jvm/main`, 3–5 per kmp fragment.
  There is no grouping by receiver, and nothing distinguishes an extension from a
  plain top-level function except the presence of that field.
- **Classes carry forward supertype edges**: `superTypes[].classLikeId`, with
  `typeArguments` for generic supertypes — 6 elements in `jvm/main`. No `inheritors`
  back-edge exists anywhere in the model.
- **Inherited members are materialized**: `kotlin/Any/equals`, `kotlin/Enum/name`
  and friends appear as real declarations listed in `class.callables` — 31 foreign
  elements in `jvm/main`, and 0 dangling references out of 229. No index is needed
  to render them; they are already there.

The decisive force is not cost, it is **possibility**. Both reverse relations are
cross-module by nature. A module's artifact cannot know that a downstream module
extends its types or subclasses its classes: at the time the artifact is produced
that fact does not yet exist. A precomputed forward index in the artifact is
therefore not merely redundant — it is structurally unable to answer the question
users actually ask. Two long-standing requests against current Dokka:

> When combining documentation of multiple modules, Dokka resolves links between
> them […] However, there's no information in the core module documentation that
> dependent modules could have some extensions for its API. I'd expect to see at
> least the following references to the dependent modules: extension
> functions/properties for the types in the core module — for example, `asObservable`
> extensions for `Flow` from `rx2` and `rx3` modules are not shown in the `Flow`
> index page. Beware that since they come from different modules/packages, they
> shouldn't be merged in the same overload group in the index page. […] inheritors
> of types declared in the core modules — for example, the class
> `HandlerDispatcher` should be shown in the list of inheritors of its base class
> `MainCoroutineDispatcher`.

> Currently the inheritor tab only shows subclasses in the same module. It is often
> helpful to see all the inheritors from all modules in a multimodule project. […]
> Like the multi module task version of the HTML format to find inheritors in other
> dependent modules and merge them into the list of inheritors from the partial task.

Current Dokka satisfies neither, because both transformers run per module. Any
design that bakes the index into a per-module artifact inherits that limitation
permanently.

## Options

| Option | Pros | Cons |
|---|---|---|
| A — KDM stores the reverse indices | renderer is trivial; no cross-artifact pass | impossible for the case that motivates the feature: the producing module does not know its downstream extenders. Index is derived data in a compatibility-constrained artifact, invalidated by any change in any other module. Leaks declarations that a consumer would have filtered out |
| B — the renderer computes them over its input set | the only option that answers the multi-module request; index scope equals output scope; filtering happens before indexing | renderer must read every artifact in the set before emitting the first page; correctness depends on joinable ids |
| C — a separate derived index artifact | keeps KDM clean; index can be regenerated independently | a second artifact with its own versioning, staleness and distribution story, for data the renderer can derive in one pass anyway |

## Decision

**The renderer owns cross-declaration indices.** KDM stores only the *forward*
edges that each declaration knows about itself — its receiver type and its
supertypes, as resolvable classifier ids. The renderer builds the reverse maps in a
single pass over **every artifact in its input set**, before page building:

```
extensionsOf: classLikeId → [callable]   from callable.receiverParameter.type.classLikeId
inheritorsOf: classLikeId → [classlike]  from classlike.superTypes[].classLikeId
```

Both maps are scoped to the loaded artifact set rather than to one module, which is
precisely what makes multi-module inheritors and extensions possible.

Inherited members are **not** part of this decision: the artifact materializes them
already, so the renderer needs no index for E-035. Whether that materialization
should remain is a separate question (Q-023).

## Consequences

**Makes easy.** Multi-module inheritors and extensions become expressible, and the
result is strictly a superset of today's per-module output — the two requests above
are satisfied by construction rather than by a special multi-module mode. KDM stays
free of derived, invalidation-prone data: adding an extension in a downstream module
changes no upstream artifact. And because indexing runs after render-time filtering,
the index can never surface a declaration the consumer filtered out — something a
producer-side index cannot guarantee.

**Makes hard.** The renderer must hold enough of every input artifact to complete
the pass before emitting pages. This is bounded: only `id`, `kind`, `name`, the
receiver id and the supertype ids are needed for the index pass, not documentation
or signatures. Completeness is bounded by the input set — an extension declared in
an artifact not handed to the renderer will not appear, and for supertypes outside
the set (`Any`, collections, third-party bases) there is no inheritors list at all,
exactly as today.

**Forecloses / depends on.** The pass joins on classifier ids, so it inherits every
identity defect. The measured collisions make this concrete: `jvm/main` has 238
elements for 132 unique ids, 22 ids duplicated and **3 of them structurally
different** (`CALLABLE:kotlin/Enum/compareTo/1589503459` resolves to two different
declarations, differing in `valueParameters`); every native kmp fragment has 27
duplicated ids, all structurally different. Those collisions corrupt both maps
silently. **G-01 now gates this decision** — see Q-013.

Two rules follow that the renderer must state explicitly: overload groups must not
merge declarations originating in different modules or packages (per the first
request above), and member extensions must get a deliberate rule —
`simpleJvmExtensionWithinClass` (declared inside a class, receiver `kotlin/String`)
falls into `extensionsOf` naturally, though today's HTML does not list it on the
receiver's page (Q-024).

**Compatibility.** No KDM schema change and therefore no artifact compatibility
consequence. It does make `receiverParameter` and `superTypes` **normative**: they
must be present on every extension and classlike, and must carry resolvable
classifier ids. Today they are incidental, and untested for KMP inheritance — the
kmp test project contains no inheritance at all, so `superTypes` never appears in
any of its 12 fragments.

## Follow-ups

- [ ] Rewrite G-02: renderer ownership, requirement narrowed to the forward edges,
      inherited members removed from its scope
- [ ] Q-012 → `answered`
- [ ] F-020, F-021: layer `model` → `renderer`; KDM requirement reduces to the
      forward edge. F-022: layer stays `model` (materialization)
- [ ] File Q-023 (inherited-member materialization: keep or index?) and Q-024
      (member extensions on receiver pages)
- [ ] Add the KMP inheritance/extension coverage hole to `40-page-anatomy.md § 8`
- [ ] KEEP #484 comment: the model carries forward edges with resolvable classifier
      ids and no reverse index; reverse indices are a consumer concern
