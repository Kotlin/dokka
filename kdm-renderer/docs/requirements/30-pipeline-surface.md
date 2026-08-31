# Inventory: pipeline surface

Behaviour that has no configuration option and is invisible in the rendered page's
markup, but which nonetheless determines what appears. This is where the model gaps
usually hide.

Pipeline stages (`CoreExtensions`):

```
Input → sourceToDocumentableTranslator → documentableTransformer* → documentableMerger
      → documentableToPageTranslator → pageTransformer* → renderer → postActions
```

Orchestrated by `SingleModuleGeneration` in `plugin-base`.

## Documentable transformers

`dokka-subprojects/plugin-base/src/main/kotlin/org/jetbrains/dokka/base/transformers/documentables/`

| Transformer | What it does | Registry ID |
|---|---|---|
| `DocumentableVisibilityFilterTransformer` | Drops declarations below the configured visibility | F-001 |
| `DeprecatedDocumentableFilterTransformer` | `skipDeprecated` | F-002 |
| `SuppressTagDocumentableFilter` | `@suppress` | F-003 |
| `SuppressedByConfigurationDocumentableFilterTransformer` | `suppressedFiles`, per-package suppress | F-004 |
| `SuppressedByConditionDocumentableFilterTransformer` | Base class for the above | F-004 |
| `ObviousFunctionsDocumentableFilterTransformer` | Hides `equals`/`hashCode`/`toString`, inherited `Enum`/`Any` members | F-005 |
| `InheritedEntriesDocumentableFilterTransformer` | `suppressInheritedMembers` | F-006 |
| `EmptyPackagesFilterTransformer`, `EmptyModulesFilterTransformer` | Prunes empties | F-007 |
| `ReportUndocumentedTransformer` | Diagnostics only | F-008 |
| `JvmMappedMethodsDocumentableFilterTransformer` | Java-mapped method handling | F-009 |
| `ExtensionExtractorTransformer` | Builds the receiver → extensions index | F-020 |
| `InheritorsExtractorTransformer` | Builds the supertype → inheritors index | F-021 |
| `ActualTypealiasAdder` | expect/actual typealiases | F-023 |
| `ClashingDriIdentifier` | Disambiguates colliding DRIs | F-025 |
| `KotlinArrayDocumentableReplacerTransformer`, `DocumentableReplacerTransformer` | Type substitution | TODO |
| `ModuleAndPackageDocumentationTransformer` | Attaches `includes` docs | F-044 |
| `DefaultDocumentableMerger` | Merges source sets | F-082 |

**Note the shape of this list.** Roughly half are *filters* and half are *index
builders*. Filters can move to render time if KDM carries the predicate inputs
(visibility, deprecation, provenance). Index builders cannot — they need the whole
module at once, which is a structural question for KDM, not a rendering one.

## Page transformers

| Transformer | What it does | Registry ID |
|---|---|---|
| `SourceLinksTransformer` | VCS links | F-064 |
| `DefaultSamplesTransformer` | Resolves `@sample` | F-043 |
| `SinceKotlinTransformer` | since-version badges | F-042 |
| `CommentsToContentConverter` / `DocTagToContentConverter` | KDoc → content model | F-040 |
| `SameMethodNamePageMergerStrategy`, `FallbackPageMergerStrategy`, `PageMerger` | Overload page merging | F-024 |
| `SourceSetMergingPageTransformer` | MPP page merging | F-083 |

## Resolvers

`resolvers/local/` (internal links), `resolvers/external/` + `resolvers/external/javadoc/`
(`package-list`, Javadoc-format targets), `resolvers/anchors/` (member anchors),
`resolvers/shared/`.

This subtree is the single biggest compatibility risk: it defines the URL scheme and
therefore every inbound link that exists today. See F-061, F-063, Q-013.

## HTML renderer internals

`renderers/html/`: `HtmlRenderer`, `HtmlContent`, `Tags.kt`, `htmlFormatingUtils.kt`,
`htmlPreprocessors.kt`, `NavigationDataProvider` + `NavigationPage` (F-100),
`SearchbarDataInstaller` (F-101), `shouldRenderSourceSetBubbles.kt` (F-080),
`HtmlCodeBlockRenderer`, `command/` (multi-module template commands),
`innerTemplating/` (FreeMarker, F-107).

## Signature providers

`signatures/` — renders modifiers, generics, variance, defaults, receivers. TODO:
enumerate what information each one reads, since that is a direct list of required
model fields (F-060).

## Open

- Ordering between transformers is significant in places. TODO: identify where, so
  the new pipeline does not silently reorder.
- Which of these become *renderer* concerns, which become *KDM producer* concerns,
  and which disappear? That split is essentially the design of the new pipeline.
