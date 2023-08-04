# Core extension points

Core extension points represent the main stages of generating documentation. 

These extension points are plugin and output format independent, meaning it's the very core functionality and as
low-level as can get in Dokka. 

For higher-level extension functions that can be used in different output formats, have a look at the 
[Base plugin](base_plugin.md).

You can find all core extensions in the `CoreExtensions` class:

```kotlin
object CoreExtensions {
    val preGenerationCheck by coreExtensionPoint<PreGenerationChecker>()
    val generation by coreExtensionPoint<Generation>()
    val sourceToDocumentableTranslator by coreExtensionPoint<SourceToDocumentableTranslator>()
    val documentableMerger by coreExtensionPoint<DocumentableMerger>()
    val documentableTransformer by coreExtensionPoint<DocumentableTransformer>()
    val documentableToPageTranslator by coreExtensionPoint<DocumentableToPageTranslator>()
    val pageTransformer by coreExtensionPoint<PageTransformer>()
    val renderer by coreExtensionPoint<Renderer>()
    val postActions by coreExtensionPoint<PostAction>()
}
```

On this page, we'll go over each extension point individually.

## PreGenerationChecker

`PreGenerationChecker` can be used to run some checks and constraints. 

For example, Dokka's Javadoc plugin does not support generating documentation for multi-platform projects, so it uses
`PreGenerationChecker` to check for multi-platform
[source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets), and fails if it finds any.

## Generation

`Generation` is responsible for generating documentation as a whole, utilizing higher-level extensions and extension 
points where applicable.

See [Generation implementations](generation_implementations.md) to learn about the default implementations.

## SourceToDocumentableTranslator

`SourceToDocumentableTranslator` translates any given sources into the Documentable model. 

Kotlin and Java sources are supported by default by the [Base plugin](base_plugin.md), but you can analyze any language 
as long as you can map it to the [Documentable](../data_model/documentable_model.md) model.

For reference, see

* `DefaultDescriptorToDocumentableTranslator` for Kotlin sources translation
* `DefaultPsiToDocumentableTranslator` for Java sources translation

## DocumentableMerger

`DocumentableMerger` merges all `DModule` instances into one. Only one extension of this type is expected to be 
registered.

## DocumentableTransformer

`DocumentableTransformer` performs the same function as `PreMergeDocumentableTransformer`, but after merging source
sets.

Notable example is `InheritorsExtractorTransformer`, it extracts inheritance information from 
[source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets) and creates an inheritance
map.

## DocumentableToPageTranslator

`DocumentableToPageTranslator` is responsible for creating pages and their content. See 
[Page / Content model](../data_model/page_content.md) page for more information and examples.

Output formats can either use the same page structure or define their own.

Only a single extension of this type is expected to be registered. 

## PageTransformer

`PageTransformer` is useful if you need to add, remove or modify generated pages or their content.

Using this extension point, plugins like `org.jetbrains.dokka:mathjax-pligin` can add `.js` scripts to the HTML pages. 

If you want all overloaded functions to be rendered on the same page instead of separate ones,
you can use `PageTransformer` to combine the pages into a single one.

## Renderer

`Renderer` - defines the rules on how to render pages and their content: which files to create and how to display
the content properly. 

Custom output format plugins should use the `Renderer` extension point. Notable examples are `HtmlRenderer`
and `CommonmarkRenderer`.

## PostAction

`PostAction` can be used for when you want to run some actions after the documentation has been generated - for example,
if you want to move some files around or log some informational messages.

Dokka's [Versioning plugin](https://github.com/Kotlin/dokka/tree/master/plugins/versioning) utilizes `PostAction` 
to move generated documentation to the versioned directories.

