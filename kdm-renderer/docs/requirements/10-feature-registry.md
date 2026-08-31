# Feature registry

**The main artifact of the requirements stage.** One row per feature, consolidated
from `20`/`30`/`40`/`50`. Column meanings: see `00-method.md § 6`.

Discussion does not belong here — keep it in `questions.md` and `decisions/`, and
reference by ID. The registry has to stay scannable.

Legend — Layer: `model` (needs data in KDM) · `renderer` (presentation only) ·
`config` (user-facing option) · `ep` (extension point).
Parity: `must` · `should` · `drop`.

> Status: seeded, not yet complete. Rows below are the ones that fall out of the
> code survey; `40`/`50` will add more. IDs are provisional until the first
> consolidation pass (step 3 in `00-method.md § 5`).

## Declaration filtering and visibility

| ID | Feature | Implemented in | Layer | Consumer | Parity | Requires from KDM | Status | Refs |
|---|---|---|---|---|---|---|---|---|
| F-001 | Visibility filtering (`documentedVisibilities`) | `DocumentableVisibilityFilterTransformer` | config, model | all | TODO | Visibility on every declaration; and a decision on whether filtering happens at model build or at render time | todo | Q-010 |
| F-002 | `skipDeprecated` | `DeprecatedDocumentableFilterTransformer` | config, model | all | TODO | Deprecation as first-class concept (already in spec § Deprecations) | todo | |
| F-003 | `@suppress` tag | `SuppressTagDocumentableFilter` | model | all | TODO | Either the tag survives into KDM, or suppression is applied before serialization | todo | Q-010 |
| F-004 | `suppressedFiles` / `perPackageOptions.suppress` | `SuppressedByConfigurationDocumentableFilterTransformer` | config | all | TODO | TODO | todo | |
| F-005 | `suppressObviousFunctions` (`equals`/`hashCode`/`toString`, `Enum` members…) | `ObviousFunctionsDocumentableFilterTransformer` | config, model | all | TODO | An "obvious / synthetic" marker, so the renderer can filter instead of the analyzer | todo | |
| F-006 | `suppressInheritedMembers` | `InheritedEntriesDocumentableFilterTransformer` | config, model | all | TODO | "inherited from X" provenance per member | todo | |
| F-007 | Empty package / empty module filtering | `EmptyPackagesFilterTransformer`, `EmptyModulesFilterTransformer` | renderer | all | TODO | — (derivable) | todo | |
| F-008 | `reportUndocumented` | `ReportUndocumentedTransformer` | config | build authors | TODO | Not a rendering feature — decide whether it stays in the Dokka pipeline | todo | Q-011 |
| F-009 | JVM-mapped method filtering | `JvmMappedMethodsDocumentableFilterTransformer` | model | JVM users | TODO | Mapped-declaration references (spec § Java) | todo | |

## Cross-declaration indices

These are computed over the whole module, not per declaration — which makes them the
highest-risk group for a per-declaration serialized model.

| ID | Feature | Implemented in | Layer | Consumer | Parity | Requires from KDM | Status | Refs |
|---|---|---|---|---|---|---|---|---|
| F-020 | Extensions listed on the receiver type's page | `ExtensionExtractorTransformer` | model | all | TODO | Receiver type on extensions + a reverse index, or renderer-side computation over the whole model | todo | Q-012 |
| F-021 | "Inheritors" section on a classlike page | `InheritorsExtractorTransformer` | model | all | TODO | Supertype links + reverse index; cross-artifact inheritors are a known open problem (`KDM spec.md § Inheritance in the absence of a KDM artifact`) | todo | Q-012 |
| F-022 | Inherited members shown on the subclass page | `InheritedEntriesDocumentableFilterTransformer`, `separateInheritedMembers` | model, config | all | TODO | Full supertype resolution, incl. types from outside the module | todo | Q-012 |
| F-023 | Actual typealias handling | `ActualTypealiasAdder` | model | MPP | TODO | Typealias model (spec § Typealias) + expect/actual links | todo | |
| F-024 | Same-name page merging (overloads) | `SameMethodNamePageMergerStrategy` | renderer | all | TODO | Stable declaration IDs that survive overload sets | todo | |
| F-025 | DRI clash disambiguation | `ClashingDriIdentifier` | model | all | TODO | ID scheme + collision rules — directly a KDM concern | todo | Q-013 |

## Documentation content

| ID | Feature | Implemented in | Layer | Consumer | Parity | Requires from KDM | Status | Refs |
|---|---|---|---|---|---|---|---|---|
| F-040 | KDoc rendering (all tags) | `CommentsToContentConverter`, `DocTagToContentConverter` | model | all | TODO | Doc representation — open, see KT-88346 in `KDM spec.md § Serialization format` | todo | Q-014 |
| F-041 | Custom tags | `CustomTagContentProvider` EP | ep | plugin authors | TODO | Unknown tags must survive round-trip rather than being dropped | todo | |
| F-042 | `@since` / `@sinceKotlin` | `SinceKotlinTransformer`, `SinceKotlinTagContentProvider` | model | stdlib, kotlinx | TODO | Since-version as a field, not as pre-rendered text (spec § Since) | todo | |
| F-043 | `@sample` — code samples resolved from `samples` roots | `DefaultSamplesTransformer` | model | kotlinx | TODO | Either resolved sample bodies inline, or a resolvable reference | todo | Q-015 |
| F-044 | Module/package `includes` (`.md` docs) | `ModuleAndPackageDocumentationTransformer` | config, model | all | TODO | Module- and package-level documentation nodes | todo | |
| F-045 | Inherited documentation (from supertype / expect) | — (KT-88347) | model | all | TODO | Explicitly listed as open in `KDM spec.md` | todo | Q-014 |

## Signatures and links

| ID | Feature | Implemented in | Layer | Consumer | Parity | Requires from KDM | Status | Refs |
|---|---|---|---|---|---|---|---|---|
| F-060 | Signature rendering (modifiers, generics, variance, defaults) | `signatures/` | renderer, model | all | TODO | Structured types, not pre-rendered strings (spec § Types, § Parameters) | todo | |
| F-061 | Links between declarations (DRI resolution) | `resolvers/local/` | model | all | TODO | Stable IDs — the central compatibility question | todo | Q-013 |
| F-062 | External documentation links (`package-list`, JDK, stdlib) | `resolvers/external/`, `externalDocumentationLinks` | config, model | all | TODO | External-reference format; must be resolvable without the target's KDM | todo | Q-016 |
| F-063 | Anchors / stable URLs per member | `resolvers/anchors/` | renderer | all users' bookmarks | TODO | Stable IDs again; URL scheme change breaks every existing inbound link | todo | Q-013 |
| F-064 | Source links to VCS | `SourceLinksTransformer`, `sourceLinks` | config, model | all | TODO | File path + line per declaration (spec § Source information) | todo | |
| F-065 | Annotations rendered on declarations | `transformers/pages/annotations/` | model | all | TODO | Annotations with arguments (spec § Annotations) | todo | |

## Multiplatform

| ID | Feature | Implemented in | Layer | Consumer | Parity | Requires from KDM | Status | Refs |
|---|---|---|---|---|---|---|---|---|
| F-080 | Source-set tabs / platform bubbles | `shouldRenderSourceSetBubbles.kt`, `platform-tag` ui-kit | renderer | MPP | TODO | Fragments — already the KDM container level | todo | |
| F-081 | Source-set filter in the UI | `filter-section` ui-kit | renderer | MPP | TODO | Fragment membership per declaration | todo | |
| F-082 | `mergeImplicitExpectActualDeclarations` | `DefaultDocumentableMerger`, config | config, model | MPP | TODO | expect/actual links — open in `KDM spec.md § Links/ids to expect/actuals` | todo | Q-017 |
| F-083 | Source-set merging of pages | `SourceSetMergingPageTransformer` | renderer | MPP | TODO | Deduplication semantics; ties into the 1/3/30 MB tradeoff | todo | Q-017 |

## Site chrome and UX

| ID | Feature | Implemented in | Layer | Consumer | Parity | Requires from KDM | Status | Refs |
|---|---|---|---|---|---|---|---|---|
| F-100 | Navigation tree / sidebar | `NavigationDataProvider`, `NavigationPage`, `toc-tree` | renderer | all | TODO | Package/declaration hierarchy | todo | |
| F-101 | Full-text search | `SearchbarDataInstaller`, `components/search` | renderer | all | TODO | Decide: derived index artifact, or built from KDM at build time | todo | Q-018 |
| F-102 | Breadcrumbs | `breadcrumbs` ui-kit | renderer | all | TODO | — | todo | |
| F-103 | Copy-to-clipboard on code blocks | `copy-tooltip`, `code-block` | renderer | all | TODO | — | todo | |
| F-104 | Dark / light theme | `ui-kit/_tokens` | renderer | all | TODO | — | todo | |
| F-105 | Custom stylesheets / assets | `customStyleSheets`, `customAssets` | config | all | TODO | — | todo | |
| F-106 | Footer message, homepage link | `footerMessage`, `homepageLink` | config | all | TODO | — | todo | |
| F-107 | Custom page templates (FreeMarker) | `templatesDir`, `renderers/html/innerTemplating` | config, ep | JetBrains sites, Google | TODO | — | todo | Q-004 |
| F-108 | Kotlin Playground samples | `plugin-kotlin-playground-samples` | renderer | kotlinx | TODO | Sample bodies (see F-043) | todo | |
| F-109 | MathJax | `plugin-mathjax` | renderer | few | TODO | — | todo | |

## Multi-module and versioning

| ID | Feature | Implemented in | Layer | Consumer | Parity | Requires from KDM | Status | Refs |
|---|---|---|---|---|---|---|---|---|
| F-120 | Multi-module aggregate page | `plugin-all-modules-page` | renderer | multi-module builds | TODO | Module-level model + cross-module links | todo | Q-003 |
| F-121 | Template substitution across modules | `plugin-templating`, `delayTemplateSubstitution` | renderer | multi-module builds | TODO | — | todo | Q-003 |
| F-122 | Version switcher / older versions | `plugin-versioning` | renderer | kotlinx, JetBrains | TODO | — | todo | Q-003 |
| F-123 | Android-specific documentation | `plugin-android-documentation` | renderer | Android | TODO | Android variants — see `KDM spec.md § Variants of declarations` | todo | |
| F-124 | Kotlin-as-Java view | `plugin-kotlin-as-java` | model | Google/Dackka | TODO | Java projection — major open question (spec § Java) | todo | Q-004 |
