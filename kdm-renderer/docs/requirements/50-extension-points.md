# Inventory: extension points and external consumers

Who builds on top of the current HTML output, and what they would need from a
KDM-based renderer. Not derivable from code alone — this file needs conversations.

Governed by Q-004 (do we commit to extension-point compatibility at all?). Until
that is answered, this inventory is descriptive, not a requirements list.

## Core extension points (`CoreExtensions`)

| EP | Purpose | Survives in a KDM world? |
|---|---|---|
| `preGenerationCheck` | Validation before generation | TODO |
| `generation` | Whole-pipeline replacement | TODO |
| `sourceToDocumentableTranslator` | Input → model | Becomes "produce KDM" |
| `documentableMerger` | Source-set merging | Becomes a KDM-producer concern |
| `documentableTransformer` | Model rewriting | The big question — see below |
| `documentableToPageTranslator` | Model → pages | Renderer-internal |
| `pageTransformer` | Page rewriting | Renderer-internal |
| `renderer` | Output format | The new renderer plugs in here |
| `postActions` | After-generation hooks | TODO |

## `plugin-base` extension points (`DokkaBase`)

`preMergeDocumentableTransformer`, `pageMergerStrategy`, `commentsToContentConverter`,
`customTagContentProvider`, `signatureProvider`, `locationProviderFactory`,
`externalLocationProviderFactory`, `outputWriter`, `htmlPreprocessors`,
`htmlCodeBlockRenderers`, `tabSortingStrategy`, `immediateHtmlCommandConsumer`,
plus the analysis-facing `kotlinAnalysis`, `externalDocumentablesProvider`,
`externalClasslikesTranslator`.

Note how many of these are *HTML-specific* (`htmlPreprocessors`,
`htmlCodeBlockRenderers`, `tabSortingStrategy`, `immediateHtmlCommandConsumer`).
Those are precisely the ones third parties use to customise the HTML output, and
precisely the ones a rewritten renderer would not naturally reproduce.

## External consumers

### Dackka (Google) — the load-bearing case

Already analysed in `KDM spec.md § Java`. Summary of what makes it hard:

- Generates documentation for **both** Kotlin and Java consumers.
- Relies heavily on Dokka's API: own filters, transformers and renderers.
- Needs JVM/ABI-level information that is not part of the Kotlin API surface —
  file names, JVM multifile facade and `@JvmName` file annotations (today read from
  `Documentable`'s `source` property).
- Their options: adopt KDM · fork Dokka and maintain it · migrate to Analysis API +
  a new KDoc API (which does not exist yet for Javadoc at all).
- Java API generation is a permanent requirement for them.

Open: does KDM need to carry enough for a Java projection (F-124), or is
`plugin-kotlin-as-java` out of scope?

### First-party consumers

| Consumer | Depends on | Registry ID |
|---|---|---|
| `plugin-versioning` | Page structure, `htmlPreprocessors`, output layout | F-122 |
| `plugin-all-modules-page` | Multi-module page model, templating commands | F-120 |
| `plugin-templating` | `command/`, `delayTemplateSubstitution` | F-121 |
| `plugin-android-documentation` | Android variant filtering | F-123 |
| `plugin-mathjax` | `htmlPreprocessors` | F-109 |
| `plugin-kotlin-playground-samples` | `htmlCodeBlockRenderers` | F-108 |
| kotlinlang.org / JetBrains sites | `templatesDir` (FreeMarker), custom assets | F-107 |

### Community plugins

TODO: survey. At minimum, establish whether any widely used plugin depends on the
HTML-specific extension points above, because that decides Q-004 in practice.

## Open

- Q-004: compatibility commitment, or clean break with a migration path?
- If clean break: what is the minimum extensibility the new renderer must ship with
  on day one so that the first-party plugins above can be ported?
