# Glossary and concepts

> A Russian translation is kept in [`glossary.ru.md`](glossary.ru.md). This file is the
> leading copy — change it first.

This document explains every term used in `kdm-renderer/docs`, `KDM spec.md` and the
surrounding code, assuming no prior exposure to Dokka, to Kotlin, or to the JVM.

**How to read it.** Sections 1–7 are ordered by dependency: each one only uses terms
already introduced. Read them in order once, then use the alphabetical index at the
end as a lookup table. Section 8 lists terms that are genuinely ambiguous in this
project — read it even if you skip everything else.

Assumed known: general programming, what a compiler and a syntax tree are, JSON and
schemas, and the idea of a documentation generator. Everything else is defined here.

---

## 1. The problem domain

**API reference.** A generated website (or other artifact) documenting every public
declaration of a library: its name, signature, and prose description. Distinct from a
hand-written guide — an API reference is derived mechanically from source code, so
its structure mirrors the code's structure. `https://kotlinlang.org/api/core/` is one.

**Declaration.** A named entity in source code that a programmer can refer to: a
class, a function, a variable, a type alias. The unit of documentation — an API
reference is, structurally, a list of declarations with text attached.

**Signature.** The part of a declaration that determines how it is called or used:
name, parameter types, return type, modifiers, type parameters. Rendered on the page
as a code-like line. Two functions can share a name but not a signature.

**Documentation comment / doc comment.** A comment in the source code, written in a
special syntax, intended for extraction by a documentation generator rather than for
the reader of the source.

**KDoc.** Kotlin's doc comment format. Markdown-based, written in `/** ... */`, with
block tags such as `@param`, `@return`, `@throws`, `@sample`, `@since`, `@suppress`.
The Kotlin equivalent of Javadoc.

**Javadoc.** Java's doc comment format, and also the name of the tool that renders it
into HTML, and also the name of that HTML layout. All three senses appear in these
documents; context disambiguates.

**Tag.** A labelled section inside a doc comment (`@param name description`). Some
tags carry structured meaning the renderer acts on (`@sample` inlines code,
`@suppress` hides the declaration); others are just prose with a heading.

**API surface.** The set of declarations a library exposes to its users — as opposed
to its internal implementation. What exactly belongs to it is a configuration
decision, not a fact: see *visibility* (§ 2) and F-001 in the feature registry.

**Dokka.** JetBrains' documentation generator for Kotlin. Reads Kotlin (and Java)
source code, produces an API reference in HTML or other formats. The subject of this
repository.

---

## 2. Kotlin vocabulary

Only the concepts that affect the documentation model. This is not a Kotlin tutorial;
for the language itself see `https://kotlinlang.org/docs/`.

**Package.** A namespace, written as a dotted path (`kotlinx.coroutines`). Groups
declarations. Unlike a directory, it is declared in the source file, so package
structure and file structure need not agree.

**Classifier.** Umbrella term for declarations that introduce a type: class,
interface, object, enum class, annotation class, type alias. KDM uses this term
directly (`KdClassifier`).

**Object.** In Kotlin, `object` declares a singleton — a class with exactly one
instance, declared and named in one step. A **companion object** is a singleton tied
to a class, holding what other languages would call static members.

**Property.** A named value on a class or at top level, accessed like a field but
compiled to accessor methods (a getter, and a setter if mutable). The distinction
matters for documentation: a property is one declaration to a Kotlin reader but up to
three to a Java reader. Contrast **field**, which is plain storage with no accessors
— Kotlin exposes fields only via Java interop.

**Extension function.** A function declared outside a class but callable as if it
were a member of it: `fun String.shout() = uppercase() + "!"` is called as
`"hi".shout()`. Crucially, it is *declared* in some package but *belongs*, from the
reader's point of view, to `String`. This mismatch is the origin of feature F-020 and
of gap G-02 — the renderer must show it on the `String` page, which requires an index
built across the whole module.

**Receiver.** The thing an extension is declared on — `String` in the example above.
An extension has an *extension receiver*; a member declared inside a class has an
implicit *dispatch receiver*. KDM models these as `KdReceiverParameter`.

**Context parameter.** An additional implicit parameter a declaration requires at the
call site, beyond the receiver. Modelled as `KdContextParameter`.

**Type alias.** A second name for an existing type: `typealias Handler = (Int) -> Unit`.
Introduces no new type, so documentation must decide whether to show it as its own
declaration, as the aliased type, or both. See F-023.

**Enum entry.** One of the fixed named instances of an `enum class`. Structurally
between a declaration and a value, which is why it gets its own handling.

**Value class.** A class that the compiler erases to its single underlying value
where possible. Relevant because its representation differs between Kotlin and JVM
views of the same API.

**Visibility.** A modifier controlling who may reference a declaration: `public`,
`internal` (visible within the compilation module), `protected`, `private`. Dokka
documents a configurable subset — see `documentedVisibilities`, F-001.

**Type parameter, type argument, variance, projection.** A generic declaration takes
*type parameters* (`class Box<T>`); a use site supplies *type arguments* (`Box<Int>`).
*Variance* declares whether `Box<Sub>` may be used where `Box<Super>` is expected
(`out` / `in`). A *projection* applies variance at the use site rather than the
declaration site. All four appear in signatures, so all four must be in the model as
structure rather than as pre-rendered text — see G-07. KDM: `KdTypeParameter`,
`KdTypeProjection`.

**Nullability.** Kotlin's type system distinguishes `String` from `String?`. Carried
in the model as metadata on a type, and doing double duty for Java interop: it is how
KDM distinguishes Java's `int`, `Integer` and `@NotNull Integer` (see `KDM spec.md § Java`).

**Standard library (stdlib).** Kotlin's own base library (`kotlin.*`,
`kotlin.collections.*`). Mentioned constantly because almost every documented API
links into it, which makes cross-library linking a first-order concern — see F-062.

### Multiplatform

**Target.** A platform a Kotlin project compiles for: `jvm`, `js`, `iosArm64`,
`linuxX64`, and so on. KDM: the `KdTarget` enum.

**Source set.** A group of source files compiled together for one or more targets,
with a declared dependency structure between groups. A typical multiplatform library
has `commonMain` (compiled for every target) plus `jvmMain`, `iosArm64Main`, …
(compiled for one each), where the platform-specific sets *depend on* the common one.
Source sets, not targets, are the unit Dokka configures and documents.

**Multiplatform (MPP).** Kotlin's mechanism for compiling one codebase to several
targets. The reason the documentation model needs a container level above declarations
at all: the same declaration may exist in several source sets with different details.

**expect / actual.** The mechanism for platform-specific implementation of a shared
declaration: `commonMain` declares `expect class Foo`, and each platform source set
provides `actual class Foo`. To a user reading the docs this is *one* API with
per-platform variation; in the source it is N+1 declarations. Reconciling those two
views is the subject of `KDM spec.md § Variants of declarations`, F-082 and Q-017.

An `actual` may also be a *type alias* to an existing platform type, in which case the
`expect` and `actual` sides can have genuinely different models — one of the stated
reasons KDM makes fragments a container level rather than a per-declaration field.

**Android flavor / variant.** Android's build-time variation of the same module
(`debug`/`release`, product flavors). Structurally similar to expect/actual and to
inheritance, which is why `KDM spec.md` asks whether the three can be generalised.

---

## 3. JVM vocabulary

**JVM (Java Virtual Machine).** The runtime Kotlin most commonly targets. Its
concepts leak into the documentation model because Kotlin libraries are consumed from
Java, and because Java sources can be documented alongside Kotlin.

**Classpath.** The list of compiled artifacts available to a compilation. Dokka needs
it to resolve types that are referenced but not declared in the analysed sources.

**API vs ABI.** The *API* is what a programmer writes against — names, signatures,
types. The *ABI* (application binary interface) is what the compiled artifact exposes
— including JVM-specific facts that have no Kotlin-level meaning, such as which class
file a top-level function ended up in. The distinction matters because Google's
tooling needs ABI-level information (file names, multifile facades, `@JvmName`) that
is deliberately not part of the Kotlin API surface — see § 7 and Q-004.

**Mapped type.** A Kotlin type the compiler maps onto a JVM type: `kotlin.String` ↔
`java.lang.String`, `kotlin.collections.List` ↔ `java.util.List`. Not always
one-to-one — Kotlin's `List.size` is a *property* where Java's is a *method*. KDM
stores a reference to the mapped Java declaration so documentation can link into the
JDK (`KDM spec.md § Java`).

**Mapped annotation.** Same idea for annotations: Java's `@Deprecated` and Kotlin's
`@Deprecated` are different declarations with different properties. Resolved in the
spec by promoting deprecation to a first-class concept instead of treating it as an
annotation — see G-09.

**Synthetic property.** A Java getter/setter pair that Kotlin lets you access as if it
were a property. Exists in the Kotlin view of a Java API but not in the Java source,
so the model must be able to represent a declaration that has no source declaration.

**Boxing.** JVM's distinction between primitive `int` and object `Integer`. Visible in
documentation because `void foo(int)` and `void foo(Integer)` are distinct overloads.

---

## 4. The Dokka pipeline

This is the section the feature registry depends on. Dokka is a staged pipeline in
which every stage is replaceable by a plugin:

```
sources ──▶ Documentables ──▶ (merge) ──▶ Pages ──▶ Content ──▶ Output files
        translator        transformers        translator      renderer
```

**Plugin.** A unit of Dokka extension, packaged as a JAR on `pluginsClasspath`.
Dokka's own functionality is itself organised as plugins — `plugin-base` provides the
default pipeline and the HTML output; `plugin-versioning`, `plugin-mathjax` and others
add features on top.

**Extension point (EP).** A named slot a plugin can plug an implementation into.
Declared in `CoreExtensions` (pipeline stages) and in `DokkaBase` (HTML-specific and
finer-grained slots). Listing them is how `50-extension-points.md` enumerates what
third parties can rely on.

**Documentable.** A node in Dokka's model of the analysed source code: the abstract
class `Documentable` and its subclasses, all prefixed `D` — `DModule`, `DPackage`,
`DClass`, `DFunction`, `DProperty`, and so on. This is the "code structure" model:
one tree per module, holding declarations, their signatures, and their parsed doc
comments. **KDM is a replacement for this model** — that is the whole project in one
sentence.

**Source-to-documentable translator.** The stage that runs the Kotlin/Java analyser
and produces `Documentable`s. The point where a KDM producer would attach instead.

**Documentable transformer.** A stage that rewrites the `Documentable` tree. Dokka
ships ~18 of them, and they divide almost evenly into two kinds — see
`30-pipeline-surface.md`:
- *filters* drop declarations (by visibility, deprecation, `@suppress`, …);
- *index builders* add cross-declaration information that no single declaration
  carries (which extensions apply to a type, which classes implement an interface).

The distinction matters because filters can be moved to render time if the model
carries their inputs, and index builders cannot — they need the whole module at once.

**Documentable merger.** Combines the per-source-set trees into one, deciding what
counts as "the same declaration" across source sets. Where expect/actual reconciliation
happens today.

**Page (`PageNode`).** A node in the model of the *output site* structure: one page per
module, package, classlike, function, and so on. `RootPageNode` is the tree root,
`ContentPage` a page carrying content. Note that "page" is an abstract output unit, not
necessarily one HTML file.

**Content model (`ContentNode`).** A format-independent description of what a page
*contains* — groups, tables, code blocks, links, text — sitting between the
declaration model and the concrete output format. `ContentGroup` is the composite
node. Its existence is why one pipeline can emit HTML, Markdown and Javadoc: only the
final renderer differs.

**Page transformer.** A stage that rewrites the page/content tree — adding source
links, resolving `@sample`, merging overload pages, attaching version badges.

**Renderer.** The stage that turns the content model into output files. `HtmlRenderer`
is the one this project replaces. Choosing to implement something in a renderer rather
than a transformer means the other output formats silently do not get it — the reason
`CLAUDE.md` insists on picking the lowest layer that expresses the intent.

**Signature provider.** Produces the rendered signature of a declaration. Reads a
specific set of model fields; enumerating them yields a direct list of model
requirements (F-060).

**DRI (DokkaResourceIdentifier).** Dokka's identifier for a declaration —
`packageName`, `classNames`, `callable`, `target`, `extra`. Used for internal links,
for member anchors, and therefore for the URL of every page and every anchor in the
generated site. Two consequences: DRIs can *clash* (hence `ClashingDriIdentifier`),
and changing the scheme breaks every existing inbound link to the documentation. KDM
needs an equivalent — gap G-01, question Q-013.

**Location provider / resolver.** Turns a DRI into a URL. Internal resolvers handle
declarations in the current build; *external* resolvers handle links into other
libraries' documentation, including Javadoc-format targets.

**`package-list`.** A small index file Dokka publishes next to generated
documentation, listing which packages it contains, so that *other* projects can link
into it without having its sources. The existing mechanism for cross-library links
(F-062, G-06).

**Anchor.** The `#fragment` part of a URL, addressing a specific member within a page.
Derived from the DRI. Looks purely presentational, but is in fact part of the public
URL contract — see the warning at the end of the feature registry.

**Preprocessor (`htmlPreprocessors`).** An HTML-stage extension point used to inject
or rewrite output — how `plugin-mathjax` and `plugin-versioning` attach themselves.

**Source-set bubble.** The small platform label ("JVM", "iOS") shown next to a
declaration in the HTML output when it exists in several source sets. Rendering-level
name for the multiplatform variation described in § 2.

**Post action.** A hook that runs after generation completes.

**Multi-module generation.** Documenting several modules in one site: each module is
generated separately, then an aggregate page is produced and cross-module links are
substituted. Handled by `plugin-all-modules-page` and `plugin-templating`, with
`delayTemplateSubstitution` deferring link resolution until the aggregate step.

---

## 5. KDM

**KDM (Kotlin Documentation Model).** A machine-readable, serializable representation
of a Kotlin library's documented API: the declarations, their signatures, and their
documentation, in a stable published format. It replaces Dokka's in-memory
`Documentable` model with an artifact that can be published, consumed by tools other
than Dokka, and rendered by anything. Tracked in KEEP #484 and KTL-4815.

**KDP.** The same thing, under an earlier name. The model and the design documents say
KDM; the package `org.jetbrains.dokka.base.generation.kdp` and the output directory
`<outputDir>/kdp/*.json` still say KDP. Pure naming drift, no semantic difference —
see Q-006.

**Containment hierarchy.** From the design comment in `KdModule.kt`:

```
project (kotlinx.coroutines)        <- build system only, not in the model
 module (coroutines-core)           KdModule
  fragment (commonMain)             KdFragment
   package (kotlinx.coroutines)     KdPackage      | may be shared between fragments
    [file (Job.kt)]                                 (not currently a model level)
     class (Job)                    KdClass        | may be shared - expect/actual
      declaration (cancel)          KdDeclaration  | may be shared - expect/actual
```

The current scope is one module; `project` is a build-system concept and stays out.

**Fragment (`KdFragment`).** A source set, as a *container* in the model: it holds
packages, and records its own `name`, `dependsOn` (the source sets it builds on) and
`targets`. Placing source sets above declarations rather than tagging each declaration
with them is a deliberate design decision. The reasons, from the source comment:

- an `actual` type alias means the `expect` and `actual` sides can have genuinely
  different models, so one declaration cannot carry both;
- expect/actual for classes is not stable yet, so the model should not bake in an
  assumption about how it evolves;
- it matches how the compiler itself organises declarations;
- for Android flavors, all fragments can be produced once and then *filtered* to
  produce different outputs, rather than re-transforming the whole model;
- it avoids clashes between same-named declarations in different source sets that are
  *not* expect/actual pairs;
- `targets` / `dependsOn` information has to be stored once somewhere regardless.

The comment ends "Maybe it will be a bad idea in the end :)" — treat the design as
provisional. This is the structure behind F-080…F-083 and G-12.

**Model classes.** All prefixed `Kd`, in `kotlin-documentation/kotlin-documentation-model`,
multiplatform and `kotlinx.serialization`-annotated. The main families:

| Family | Members |
|---|---|
| Containers | `KdModule`, `KdFragment`, `KdPackage` |
| Declarations | `KdDeclaration` (sealed) → `KdClassifier` (`KdClass`, `KdTypealias`), `KdCallable` (`KdFunction`, `KdConstructor`, `KdVariable`) |
| Parameters | `KdParameter` (sealed) → `KdValueParameter`, `KdReceiverParameter`, `KdContextParameter`; plus `KdTypeParameter` |
| Types | `KdType` (sealed) → `KdClassifierType`, `KdFunctionalType`, `KdTypeParameterType`, `KdUnresolvedType`; `KdTypeProjection` |
| Identity | `KdClassifierId`, `KdCallableId` |
| Documentation | `KdDocumentationNode` (sealed), `KdLinkReference`, `KdReturns`, `KdThrows` |
| Annotations & constants | `KdAnnotation`, `KdAnnotationArgument`, `KdAnnotationArgumentValue`, `KdConstValue` |
| External | `KdExternalModule`, `KdExternalFragment`, `KdExternalLink` (work in progress) |

**`KdUnresolvedType`.** Worth singling out: the model explicitly represents a type it
could not resolve, rather than failing. Documentation generation has to succeed on
incomplete inputs.

**Encoding.** `encoding.kt` provides `encodeToJson`, `encodeToProtoBuf`, `encodeToCbor`,
and `protoSchema()` which generates a ProtoBuf schema from the model. Which format
becomes the published one is open — `KDM spec.md § Serialization format` weighs JSON
(readable, tool support), ProtoBuf (schema-based, marginally smaller) and Kotlin
metadata (already stable and compatibility-maintained, but never designed as a public
API for external tools).

**Deduplication.** In a multiplatform library the same declaration exists in many
source sets. Storing it once per fragment is what makes the artifact large; the
measurements in `KDM spec.md` for one real module are ~1 MB if each file is analysed
once (Dokka's current approach), ~30 MB if every source set is analysed fully, and
~3 MB with simple deduplication (storing an ID instead of a repeated declaration).
This is why declaration identity (G-01) and deduplication (Q-017) are entangled.

**External module (`KdExternalModule`).** Work-in-progress representation of a library
whose KDM is *not* available — needed because for years most link targets will not
publish one. The KDM-native counterpart of `package-list` (G-06).

**Artifact.** Here, the published KDM file(s) for a library. Introduces the questions
a purely in-memory model never had: forward and backward compatibility, size, and what
a consumer does when it encounters a newer version than it understands
(`KDM spec.md § Backward/forward compatibility guarantees`).

---

## 6. Analysis and build

Needed to contribute to the repository; not needed to read the requirements documents.

**Analysis.** The stage that parses Kotlin sources and resolves names and types — the
compiler frontend, used as a library. Dokka has two interchangeable backends behind
`analysis-kotlin-api`.

**K1 / descriptors.** The older Kotlin frontend. Its resolved-declaration objects are
called *descriptors*. Dokka's implementation: `analysis-kotlin-descriptors`.

**K2 / Analysis API / symbols.** The current Kotlin frontend, exposed to tools through
the *Analysis API* (often abbreviated **AA**); its resolved declarations are called
*symbols*. Dokka's implementation: `analysis-kotlin-symbols`. Because both backends
must keep working, Dokka's tests are split into `testSymbols`, `testDescriptors` and
`testJavaSymbols` rather than a single `test` task.

**PSI (Program Structure Interface).** IntelliJ's syntax-tree abstraction, shared by
the IDE and by these analysis backends. Where Java sources are read from
(`analysis-java-psi`).

**Composite build.** A Gradle arrangement where several independent builds are
combined, via `includeBuild`, into one. In this repository `build-logic`,
`dokka-integration-tests` and each runner under `dokka-runners/` are separate builds —
which is why they are not reachable as `:dokka-runners:…` task paths from the root.

**Runner.** The user-facing entry point that invokes Dokka: the Gradle plugin (new and
classic), the Maven plugin, or the CLI. Each exposes its own configuration DSL over the
same underlying `DokkaConfiguration` — which is why `20-config-surface.md` is not
complete until the runners are surveyed.

**Convention plugin.** A Gradle build script under
`build-logic/src/main/kotlin/dokkabuild.*.gradle.kts` applied across subprojects to
share configuration — toolchains, `explicitApi()`, publishing, the test split.

**`explicitApi()`.** A Kotlin compiler mode requiring explicit visibility modifiers and
explicit return types on public declarations. On for JVM subprojects here, together
with `allWarningsAsErrors` — so a warning fails the build.

**`apiDump` / `.api` files.** A checked-in textual dump of each module's public API.
Any public API change must be accompanied by a regenerated dump (`./gradlew apiDump`),
or the build fails. Makes API changes visible in review.

**`@InternalDokkaApi`.** Marker for declarations that are public for technical reasons
but not part of the supported API.

---

## 7. Ecosystem and stakeholders

**KEEP (Kotlin Evolution and Enhancement Process).** The public process for proposing
changes to Kotlin and its ecosystem. KDM is discussed in KEEP #484 — so parts of this
project's design are decided in public, not in this repository.

**Issue trackers.** YouTrack prefixes appearing in these documents:
`KTL-` (Kotlin libraries — KTL-4815 is this project's meta issue), `KT-` (the Kotlin
language and compiler — KT-88346 documentation serialization, KT-88347 documentation
inheritance), `OSIP-` (Kotlin ecosystem infrastructure projects).

**Dackka.** Google's Dokka plugin, used to generate documentation for Android and
Firebase libraries. Described in `KDM spec.md § Java` as "a HUGE Dokka plugin". It
matters disproportionately because it needs something KDM does not currently promise:
documentation for **Java** consumers of Kotlin libraries, plus JVM/ABI-level facts
(file names, multifile facades, `@JvmName`). Google's options are to adopt KDM, to
fork Dokka, or to migrate to the Analysis API plus a KDoc API that does not yet exist.
This is the substance of Q-004 and G-14.

**kotlinlang.org.** Where Kotlin's own API reference is published, using Dokka with
custom FreeMarker templates — hence a first-party consumer of `templatesDir` (F-107).

---

## 8. Ambiguous terms

Terms that mean different things in different sentences in this project. Worth
checking against this list before assuming.

| Term | Senses |
|---|---|
| **Module** | (a) a Gradle subproject; (b) a Dokka documentation unit, `DModule`, one per generated site section; (c) `KdModule`, the KDM root; (d) a Kotlin *compilation module*, the scope of `internal` visibility. Usually (b)/(c), which mostly coincide. |
| **Fragment** | (a) `KdFragment`, i.e. a source set as a KDM container; (b) the `#anchor` part of a URL. § 5 vs § 4. |
| **Documentable** | Dokka's model type (`Documentable`/`D*`). *Not* a synonym for "declaration" and not related to KDM's `KdDocumented`. |
| **Documentation** | (a) the generated site; (b) the doc-comment content attached to a declaration (`KdDocumentationNode`). |
| **Page** | (a) `PageNode`, an abstract output unit; (b) one HTML file. Not one-to-one — overload sets merge several declarations onto one page. |
| **Model** | (a) Dokka's `Documentable` model; (b) the content model (`ContentNode`); (c) KDM. Three different models in one pipeline. |
| **Javadoc** | (a) Java's doc-comment syntax; (b) the JDK tool; (c) the HTML layout that tool produces, which Dokka can also emit. |
| **KDM / KDP** | The same model. See Q-006. |
| **Target** | (a) a Kotlin compilation target (`jvm`, `iosArm64`); (b) `DRI.target`, which part of a declaration a DRI points at. |
| **Source set** | Kotlin's compilation grouping. In KDM it is called a *fragment*; in Dokka's configuration and HTML output it stays "source set". |
| **Filter** | (a) a documentable transformer that drops declarations at build time; (b) the UI control that hides source sets in the rendered page. F-001 vs F-081. |
| **Index** | (a) a cross-declaration lookup built by a transformer (extensions, inheritors); (b) the search index of the HTML site; (c) `index.html`. |
| **Parity** | Specifically defined in `00-method.md § 2` — do not use it loosely here. |

---

## Alphabetical index

| Term | Section |
|---|---|
| ABI | 3 |
| Analysis API (AA) | 6 |
| Anchor | 4 |
| Android flavor | 2 |
| API reference | 1 |
| API surface | 1 |
| `apiDump` | 6 |
| Artifact (KDM) | 5 |
| Boxing | 3 |
| Classifier | 2 |
| Classpath | 3 |
| Companion object | 2 |
| Composite build | 6 |
| Content model / `ContentNode` | 4 |
| Context parameter | 2 |
| Convention plugin | 6 |
| Dackka | 7 |
| Declaration | 1 |
| Deduplication | 5 |
| Descriptors (K1) | 6 |
| Doc comment | 1 |
| Documentable | 4 |
| Documentable merger | 4 |
| Documentable transformer | 4 |
| Dokka | 1 |
| DRI | 4 |
| Encoding (KDM) | 5 |
| Enum entry | 2 |
| expect / actual | 2 |
| Extension function | 2 |
| Extension point (EP) | 4 |
| External module (`KdExternalModule`) | 5 |
| `explicitApi()` | 6 |
| Field | 2 |
| Fragment (`KdFragment`) | 5 |
| Index builder (vs filter) | 4 |
| `@InternalDokkaApi` | 6 |
| Issue trackers (`KTL-`, `KT-`, `OSIP-`) | 7 |
| Javadoc | 1 |
| JVM | 3 |
| K1 / K2 | 6 |
| `Kd*` classes (model classes) | 5 |
| KDM | 5 |
| KDoc | 1 |
| KDP | 5 |
| KEEP | 7 |
| kotlinlang.org | 7 |
| Location provider | 4 |
| Mapped annotation | 3 |
| Mapped type | 3 |
| Multi-module generation | 4 |
| Multiplatform (MPP) | 2 |
| Nullability | 2 |
| Object | 2 |
| Package | 2 |
| `package-list` | 4 |
| Page (`PageNode`) | 4 |
| Page transformer | 4 |
| Plugin | 4 |
| Post action | 4 |
| Preprocessor | 4 |
| Projection | 2 |
| Property | 2 |
| PSI | 6 |
| Receiver | 2 |
| Renderer | 4 |
| Runner | 6 |
| Signature | 1 |
| Signature provider | 4 |
| Source set | 2 |
| Source-set bubble | 4 |
| Source-to-documentable translator | 4 |
| Standard library (stdlib) | 2 |
| Symbols (K2) | 6 |
| Synthetic property | 3 |
| Tag | 1 |
| Target | 2 |
| Type alias | 2 |
| Type parameter / argument | 2 |
| Value class | 2 |
| Variance | 2 |
| Visibility | 2 |
| YouTrack prefixes → Issue trackers | 7 |

---

## Maintaining this document

- A term belongs here once it appears in more than one document, or once someone has
  had to ask what it means.
- Definitions state what the thing *is* and why it matters *to this project*. If a
  definition could be copied verbatim from the Kotlin documentation, link there
  instead and keep only the project-specific consequence.
- Add new terms to the narrative section they depend on, then to the index. Keep the
  dependency ordering within a section: never use a term before it is introduced.
- When a term turns out to be ambiguous in practice, add it to § 8 rather than
  picking a winner unilaterally — the ambiguity is usually load-bearing somewhere.
