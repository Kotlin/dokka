# ADR-0003: Keep Dokka's existing `package-list` format for external references

- **Status:** accepted
- **Date:** 2026-09-14
- **Affects:** G-06; F-061, F-063
- **Resolves:** Q-016
- **Related:** KT-88421 (cross-link resolution across consumers), KT-88237 (unique declaration identifier)

## Context

KT-88421 asks how consumers of KDM should resolve a declaration identifier into a
URL, and puts two options on the table: reuse Dokka's existing `package-list`, or
introduce something in the spirit of Sphinx's `objects.inv`.

What Dokka's format carries today (per KT-88421): package names, module names in the
multi-module case, and — *in some cases* — the string representation of the DRI plus
a relative path, for declarations whose file location does not follow from the URL
algorithm. That last part is load-bearing rather than incidental: it is the escape
hatch that makes platform-prefixed file names such as `[jvm]shared.html` (P-12, 6
pages in `ui-showcase`) linkable from the outside at all.

Two forces make replacement unattractive:

- **Back-compat is required regardless.** A consumer must keep resolving links into
  Javadoc's `package-list`, Javadoc's newer `element-list`, AndroidX's convention and
  Dokka's own — today handled by subclasses of `ExternalLocationProvider`. Adopting a
  new format would therefore be *additive* work on top of all of those, not a
  replacement for any of them.
- **No unmet requirement has been identified.** The MkDocs example raised in KT-88421
  is answered by having the Markdown output follow Dokka's naming convention, which
  MkDocs then maps to HTML directly. And a new format does not help the
  `kotlinx-knit` case at all: as the 2026-09-07 comment on that issue notes, if the
  unique declaration identifier is uninterpreted — a hash — then a consumer without
  compiler context cannot construct the identifier in the first place, whatever the
  index file looks like.

The same comment reaches the same conclusion independently: resolution belongs on
the consumer side, the existing mechanism should simply be reused, and a new scheme
can be introduced later without blocking anything.

## Options

| Option | Pros | Cons |
|---|---|---|
| A — **keep Dokka's `package-list`** | zero migration; every library that publishes one today stays linkable; the renderer inherits the existing conventions; nothing here blocks on KT-88421 | inherits the format's quirks, including DRI strings embedded in a published file |
| B — new `objects.inv`-style artifact | uniform, well-specified, tool-friendly | purely additive: the four existing conventions must still be supported; solves no identified problem; does not help the uninterpreted-identifier case |

## Decision

**Keep the existing Dokka `package-list` format, unchanged**, as the external
-reference artifact of the new HTML renderer — in both roles, as producer and as
consumer. Do not introduce an `objects.inv`-style artifact within this project.
Revisit only against a concrete requirement the current format demonstrably cannot
express.

## Consequences

**Makes easy.** No migration for existing published documentation. The renderer can
adopt the conventions already encoded in `ExternalLocationProvider` subclasses rather
than inventing a resolution layer, and this project does not block on KT-88421 being
decided.

**Constraint kept, not added.** Because the format may carry a DRI plus an explicit
relative path, external linking is deliberately *not* a pure function of declaration
identity: the resolver must accept a per-declaration path override. That is the same
shape ADR-0002 already requires of the owner-aware resolver, so it costs nothing
extra here — but it does mean "URL = f(structural fields)" is a rule with a
documented exception, and the exception is part of the public contract.

**Forecloses (the important one).** The file embeds DRI strings, which couples a
*published* artifact to Dokka's identity scheme. If G-01 / Q-013 changes declaration
identity, the emitted `package-list` must still carry the **old** DRI strings for
inbound compatibility. In other words this file is a compatibility surface, not an
internal detail — cheap to note now, expensive to discover after the identity scheme
moves.

**Keeps G-06 closed for now**, and reopening is cheap: nothing else in the design
depends on the choice.

## Follow-ups

- [ ] Q-016 → `answered`
- [ ] G-06: record the decision and the compatibility-surface consequence
- [ ] Verify what Dokka actually writes into `package-list` — format version, and
      exactly when the DRI + relative-path lines appear. **Not verified in this
      session** (repository access was unavailable); the description above follows
      KT-88421
- [ ] The URL oracle from the next-step plan must diff `package-list` too, not only
      page paths — it is equally a public contract
