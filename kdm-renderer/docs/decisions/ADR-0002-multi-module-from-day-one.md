# ADR-0002: The renderer is designed multi-module from the first commit

- **Status:** accepted
- **Date:** 2026-09-11
- **Affects:** F-020, F-021, F-061, F-063; G-01, G-02; Q-003, Q-005
- **Resolves:** Q-026

## Context

ADR-0001 already placed the cross-declaration indices in the renderer and scoped
them to *"the whole input artifact set"*. That decision quietly settled the hardest
part of multi-module support; this ADR generalises the same assumption to the rest
of the renderer instead of leaving it to one subsystem.

**Current Dokka bolts multi-module on afterwards.** Per-module "partial" tasks render
HTML, then `plugin-all-modules-page` produces the aggregate page and stitches links
across modules (`AllModulesPageGeneration`, `MultimodulePageCreator`,
`MultimoduleLocationProvider`, `ExternalModuleLinkResolver`,
`ResolveLinkCommandHandler`). It resolves *links* between modules; it does not
re-index *content*. This is exactly why the two feature requests quoted in ADR-0001 —
cross-module extensions on the receiver's page, cross-module inheritors — are still
open. The "single module first, multi-module later" experiment has already been run
in this codebase, and its observed outcome is those two open requests.

*(Caveat: the plugin's file list was read, its internals were not — the claim that it
stitches links without re-indexing content rests on the module structure, on the
`package-list` mechanism recorded in G-06, and on the standing feature requests.
Worth confirming before this paragraph is quoted outside these docs.)*

**A single module is already a multi-artifact situation**, so the simplification
"one module" does not even buy what it appears to buy:

- one module's KDM carries many fragments — 12 in the `kmp` variant of `ui-showcase`;
- `class.callables` references ids the module does not own (`kotlin/Any/*`,
  `kotlin/Enum/*`; 31 such elements in `jvm/main`), so "resolve an id that is not
  mine" is a day-one code path regardless of intent.

## Options

| Option | Pros | Cons |
|---|---|---|
| A — single-module renderer, multi-module bolted on later | fastest first page; smaller surface to get right initially | retrofitting the resolver and the index pass touches every link, anchor and nav entry; adding a module segment to URLs later relocates every page, and URLs are a public contract (F-063). This is Dokka's current shape and the reason for the open requests |
| B — **multi-module architecture, single-module feature scope** | the load-bearing contracts are right while the surface is still small; "one module" is exercised by the same code path as many; the two feature requests are satisfied by construction | the owner-aware resolver must exist before the first page ships; forecloses streaming output (see Consequences) |
| C — full multi-module scope immediately | nothing deferred | pays for the aggregate page, cross-module nav and versioning before a single-module page is even correct; no reason to couple feature scope to architecture |

## Decision

**Option B.** The renderer is built multi-module from the first commit at the level
of four architectural commitments, while its *feature* scope may remain
single-module for as long as convenient:

1. **Input is a set of artifacts.** The loader's input type is a set of KDM modules.
   One module is cardinality 1 — never a separate code path, never a special case
   with its own branch.
2. **The `id → URL` resolver is owner-aware.** It takes the owning artifact of an id
   as an input and must answer for ids it does not own: in-set → a local URL,
   out-of-set → an external link or plain text. No component may assume "this id is
   mine". This is the load-bearing commitment — the resolver sits behind every link
   in a signature, every member anchor and every navigation entry.
3. **Output layout carries the module segment from the start.** Introducing it later
   relocates every page, and URLs are part of the public contract (F-063).
4. **The index pass of ADR-0001 runs over the whole set** before any page is built.

Explicitly deferred, because they are feature surface rather than architecture: the
aggregate module-list page, the cross-module navigation tree, versioning, and
all-modules-page parity (Q-003).

## Consequences

**Makes easy.** The two standing feature requests are satisfied by construction
rather than by a separate multi-module mode, and there is no second plugin to carry
the aggregate case. "Single module" becomes a degenerate input exercised by the same
code path as many modules, so it cannot rot.

**Makes hard.** Nothing ships until the resolver's owner-aware contract exists — the
cheap first page is no longer available. And the index pass requires reading every
input artifact before emitting the first page, which **forecloses streaming or
incremental output**: the renderer is a two-phase process (load-and-index, then
render) by design. If incremental rendering ever becomes a requirement, this decision
is what must be revisited.

**Surfaces G-01 earlier, deliberately.** Cross-artifact joins go by classifier id,
and two modules may declare the same package. A single-module renderer hides the
collisions already measured (`jvm/main`: 132 unique ids for 238 elements, 3 between
structurally different declarations; 27 per native kmp fragment); a multi-module one
hits them immediately. Failing early here is the point, not a side effect.

**Acceptance-set requirement (Q-005).** Both variants of `ui-showcase` are single
-module, so a multi-module regression cannot manifest on today's tests at all. The
acceptance set needs a multi-module project in which a *consumer* module declares
extensions and subclasses of a *producer* module's types — the `Flow` + `rx2`/`rx3`
case from the feature request, in miniature.

**Compatibility.** No KDM schema consequence: this is a renderer-side decision. It
does constrain the renderer's public API — the entry point takes a set, not a module —
which matters for Q-004 (extension-point compatibility).

## Follow-ups

- [ ] Q-005: add a multi-module project with cross-module extensions and inheritors
      to the acceptance set
- [ ] Q-003 narrows: all-modules-page parity is now only the aggregate page and nav,
      not the indices
- [ ] F-061, F-063: record the owner-aware resolver and the module segment in URLs
- [ ] `30-pipeline-surface.md`: note that `plugin-all-modules-page`'s link stitching
      has no counterpart in the new design — the resolver subsumes it. Verify the
      plugin's internals first (see the caveat in Context)
- [ ] Revisit if incremental/streaming output becomes a requirement
