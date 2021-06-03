# Extension points

## Core extension points

We will discuss all base extension points along with the steps, that `DokkaGenerator` does to build a documentation.

### Setting up Kotlin and Java analysis process and initializing plugins

The provided Maven / CLI / Gradle configuration is read.Then, all the `DokkaPlugin` classes are loaded and the extensions are created.
 
 No entry points here.

### Creating documentation models

The documentation models are created.

This step uses `DokkaCore.sourceToDocumentableTranslator` entry point. All extensions registered using this entry point will be invoked. Each of them is required to implement `SourceToDocumentableTranslator` interface:

```kotlin
interface SourceToDocumentableTranslator {
    fun invoke(sourceSet: SourceSetData, context: DokkaContext): DModule
}
```
By default, two translators are created:

* `DefaultDescriptorToDocumentableTranslator` that handles Kotlin files
* `DefaultPsiToDocumentableTranslator` that handles Java files

After this step, all data from different source sets and languages are kept separately.

If you are using Kotlin it is recommended to make use of the asynchronous version, providing you implementation of `invokeSuspending`: 

```kotlin
interface AsyncSourceToDocumentableTranslator : SourceToDocumentableTranslator {
    suspend fun invokeSuspending(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): DModule
}
```

### Pre-merge documentation transform

Here you can apply any transformation to model data before different source sets are merged.

This step uses `DokkaCore.preMergeDocumentableTransformer` entry point. All extensions registered using this entry point will be invoked. Each of them is required to implement `PreMergeDocumentableTransformer` interface:

```kotlin
interface PreMergeDocumentableTransformer {
    operator fun invoke(modules: List<DModule>, context: DokkaContext): List<DModule>
}
```
By default, three transformers are created:

* `DocumentableVisibilityFilter` that, depending on configuration, filters out all private members from declared packages
* `ActualTypealiasAdder` that handles Kotlin typealiases
* `ModuleAndPackageDocumentationTransformer` that creates documentation content for models and packages itself 

### Merging

All `DModule` instances are merged into one.

This step uses `DokkaCore.documentableMerger` entry point. It is required to have exactly one extension registered for this entry point. Having more will trigger an error, unless only one is not overridden.

The extension is required to implement `DocumentableMerger` interface:

```kotlin
interface DocumentableMerger {
    operator fun invoke(modules: Collection<DModule>, context: DokkaContext): DModule
}
```

By default, `DefaultDocumentableMerger` is created. This extension is treated as a fallback, so it can be overridden by a custom one.

### Merged data transformation

You can apply any transformation to already merged data

This step uses `DokkaCore.documentableTransformer` entry point. All extensions registered using this entry point will be invoked. Each of them is required to implement `DocumentableTransformer` interface:

```kotlin
interface DocumentableTransformer {
    operator fun invoke(original: DModule, context: DokkaContext): DModule
}
```

By default, `InheritorsExtractorTransformer` is created, that extracts inherited classes data across source sets and creates inheritance map.

### Creating page models

The documentable model is translated into page format, that aggregates all tha data that will be available for different pages of documentation.

This step uses `DokkaCore.documentableToPageTranslator` entry point. It is required to have exactly one extension registered for this entry point. Having more will trigger an error, unless only one is not overridden.

The extension is required to implement `DocumentableToPageTranslator` interface:

```kotlin
interface DocumentableToPageTranslator {
    operator fun invoke(module: DModule): ModulePageNode
}
```

By default, `DefaultDocumentableToPageTranslator` is created.  This extension is treated as a fallback, so it can be overridden by a custom one.

### Transforming page models

You can apply any transformations to paged data.

This step uses `DokkaCore.pageTransformer` entry point. All extensions registered using this entry point will be invoked. Each of them is required to implement `PageTransformer` interface:

```kotlin
interface PageTransformer {
    operator fun invoke(input: RootPageNode): RootPageNode
}
```
By default, two transformers are created:

* `PageMerger` merges some pages depending on `MergeStrategy`
* `DeprecatedStrikethroughTransformer` marks all deprecated members on every page

### Rendering

All pages are rendered to desired format.

This step uses `DokkaCore.renderer` entry point. It is required to have exactly one extension registered for this entry point. Having more will trigger an error, unless only one is not overridden.
                                    
The extension is required to implement  `Renderer` interface:

```kotlin
interface Renderer {
    fun render(root: RootPageNode)
}
```

By default, only `HtmlRenderer`, that extends basic `DefaultRenderer`, is created, but it will be registered only if configuration parameter `format` is set to `html`. Using any other value without providing valid renderer will cause Dokka to fail. 

## Multimodule page generation endpoints

Multimodule page generation is a separate process, that declares two additional entry points:

### Multimodule page creation

Generation of the page that points to all module for which we generates documentation.

This step uses `CoreExtensions.allModulePageCreator` entry point. It is required to have exactly one extension registered for this entry point. Having more will trigger an error, unless only one is not overridden.

The extension is required to implement  `PageCreator` interface:

```kotlin
interface PageCreator {
    operator fun invoke(): RootPageNode
}
```

By default, `MultimodulePageCreator` is created.  This extension is treated as a fallback, so it can be replaced by a custom one. 

### Multimodule page transformation

Additional transformation that we might apply for multimodule page.

This step uses `CoreExtensions.allModulePageTransformer` entry point. All extensions registered using this entry point will be invoked.  Each of them is required to implement common `PageTransformer` interface.

## Default extensions' extension points

Default core extension points already have an implementation for providing basic Dokka functionality. All of them are declared in `DokkaBase` plugin. If you don't want this default extensions to load, all you need to do is not load Dokka base and load your plugin instead.
 
```kotlin
val customPlugin by configurations.creating

dependencies {
    customPlugin("[custom plugin load signature]")
}
tasks {
    val dokka by getting(DokkaTask::class) {
        pluginsConfig = alternativeAndIndependentPlugins
        outputDirectory = dokkaOutputDir
        outputFormat = "html"
        [...]
    }
}
```
 
 You will then need to implement extensions for all core extension points. 

`DokkaBase` also register several new extension points, with which you can change default behaviour of `DokkaBase` extensions. In order to use them, you need to add `dokka-base` to you dependencies:

```kotlin
compileOnly("org.jetbrains.dokka:dokka-base:<dokka_version>")
```

Then, you need to obtain `DokkaBase` instance using `plugin` function:

```kotlin
class SamplePlugin : DokkaPlugin() {

    val dokkaBase = plugin<DokkaBase>()

    val extension by extending {
        dokkaBase.pageMergerStrategy with SamplePageMergerStrategy order {
           before(dokkaBase.fallbackMerger)
       }
    }
}

object SamplePageMergerStrategy: PageMergerStrategy {
    override fun tryMerge(pages: List<PageNode>, path: List<String>): List<PageNode> {
        ...
    }

}
```

### Following extension points are available with base plugin

| Entry point | Function | Required interface | Used by | Singular | Preregistered extensions
|---|:---|:---:|:---:|:---:|:---:|
| `pageMergerStrategy` |  determines what kind of pages should be merged | `PageMergerStrategy` | `PageMerger` | false | `FallbackPageMergerStrategy` `SameMethodNamePageMergerStrategy`   |
| `commentsToContentConverter` | transforms comment model into page content model | `CommentsToContentConverter` | `DefaultDocumentableToPageTransformer` `SignatureProvider` | true | `DocTagToContentConverter` |
| `signatureProvider`  | provides representation of methods signatures | `SignatureProvider` | `DefaultDocumentableToPageTransformer` | true | `KotlinSignatureProvider` |
| `locationProviderFactory` | provides `LocationProvider` instance that returns paths for requested elements | `LocationProviderFactory` | `DefaultRenderer` `HtmlRenderer` `PackageListService` | true | `DefaultLocationProviderFactory` which returns `DefaultLocationProvider` |
| `externalLocationProviderFactory` | provides `ExternalLocationProvider` instance that returns paths for elements that are not part of generated documentation | `ExternalLocationProviderFactory` | `DefaultLocationProvider` | false | `JavadocExternalLocationProviderFactory` `DokkaExternalLocationProviderFactory` |
| `outputWriter` | writes rendered pages files | `OutputWriter` | `DefaultRenderer` `HtmlRenderer` | true | `FileWriter`|
| `htmlPreprocessors` | transforms page content before HTML rendering | `PageTransformer`| `DefaultRenderer` `HtmlRenderer` | false | `RootCreator` `SourceLinksTransformer` `NavigationPageInstaller` `SearchPageInstaller` `ResourceInstaller` `StyleAndScriptsAppender` `PackageListCreator` |
| `samplesTransformer` | transforms content for code samples for HTML rendering | `SamplesTransformer` | `HtmlRenderer` | true | `DefaultSamplesTransformer` |


