[//]: # (title: Dokka Gradle configuration options)

Dokka has many configuration options to customize your and your reader's experience.

Below are detailed descriptions for each configuration section and some examples. 
You can also find an example
with [all configuration options](#complete-configuration) applied.

For more details on applying configuration blocks for single-project and multi-project builds,
see [Configuration examples](dokka-gradle.md#configuration-examples).

### General configuration

Here is an example of the general Dokka Gradle plugin configuration: 

* Use the top-level `dokka {}` DSL configuration.
* In DGP, you declare Dokka publication configurations in the `dokkaPublications{}` block.
* The default publications
are [`html`](dokka-html.md) and [`javadoc`](dokka-javadoc.md).

* The syntax of `build.gradle.kts` files differs from regular `.kt`
files (such as those used for Kotlin custom plugins) because Gradle's Kotlin DSL uses type-safe accessors.

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}

dokka {
    dokkaPublications.html {
        moduleName.set(project.name)
        moduleVersion.set(project.version.toString())
        // Standard output directory for HTML documentation
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        failOnWarning.set(false)
        suppressInheritedMembers.set(false)
        suppressObviousFunctions.set(true)
        offlineMode.set(false)
        includes.from("packages.md", "extra.md")
        
        // Output directory for additional files
        // Use this block instead of the standard when you 
        // want to change the output directory and include extra files
        outputDirectory.set(rootDir.resolve("docs/api/0.x"))
        
        // Use fileTree to add multiple files
        includes.from(
            fileTree("docs") {
                include("**/*.md")
            }
        )
    }
}
```

For more information about working with files, see the [Gradle docs](https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_trees).

</tab>
<tab title="Kotlin custom plugin" group-key="kotlin custom">

```kotlin
// CustomPlugin.kt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension

abstract class CustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.jetbrains.dokka")

        project.extensions.configure(DokkaExtension::class.java) { dokka ->
            
            dokka.moduleName.set(project.name)
            dokka.moduleVersion.set(project.version.toString())

            dokka.dokkaPublications.named("html") { publication ->
                // Standard output directory for HTML documentation
                publication.outputDirectory.set(project.layout.buildDirectory.dir("dokka/html"))
                publication.failOnWarning.set(true)
                publication.suppressInheritedMembers.set(true)
                publication.offlineMode.set(false)
                publication.suppressObviousFunctions.set(true)
                publication.includes.from("packages.md", "extra.md")

                // Output directory for additional files
                // Use this instead of the standard block when you 
                // want to change the output directory and include extra files
                html.outputDirectory.set(project.rootDir.resolve("docs/api/0.x"))
            }
        }
    }
}
```

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}

dokka {
    dokkaPublications {
        html {
            // Sets general module information
            moduleName.set(project.name)
            moduleVersion.set(project.version.toString())

            // Standard output directory for HTML documentation
            outputDirectory.set(layout.buildDirectory.dir("dokka/html"))

            // Core Dokka options
            failOnWarning.set(false)
            suppressInheritedMembers.set(false)
            suppressObviousFunctions.set(true)
            offlineMode.set(false)
            includes.from(files("packages.md", "extra.md"))

            // Output directory for additional files
            // Use this block instead of the standard when you want to 
            // change the output directory and include extra files
            outputDirectory.set(file("$rootDir/docs/api/0.x"))
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="moduleName">
        <p>
           The display name for the project’s documentation. It appears in the table of contents, navigation, 
           headers, and log messages. In multi-project builds, each subproject's <code>moduleName</code> is 
           used as its section title in aggregated documentation.
        </p>
        <p>Default: Gradle project name</p>
    </def>
    <def title="moduleVersion">
        <p>
            The subproject version displayed in the generated documentation. 
            In single-project builds, it is used as the project version.
            In multi-project builds, each subproject's <code>moduleVersion</code> 
            is used when aggregating documentation. 
        </p>
        <p>Default: Gradle project version</p>
    </def>
    <def title="outputDirectory">
        <p>The directory where the generated documentation is stored.</p>
        <p>This setting applies to all documentation formats (HTML, Javadoc, etc.) generated by the <code>dokkaGenerate</code> task.</p>
        <p>Default: <code>build/dokka/html</code></p>
        <p><b>Output directory for additional files</b></p>
        <p>You can specify the output directory and include additional files for both single and multi-project builds.
           For multi-project builds,
           set the output directory and include additional files in the configuration of the root project.
        </p>
    </def>
    <def title="failOnWarning">
        <p>
            Determines whether Dokka should fail the build when a warning occurs during documentation generation.
            The process waits until all errors and warnings have been emitted first.
        </p>
        <p>This setting works well with <code>reportUndocumented</code>.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="suppressInheritedMembers">
        <p>Whether to suppress inherited members that aren't explicitly overridden in a given class.</p>
        <p>
            Note: 
            This suppresses functions such as <code>equals</code>, <code>hashCode</code>, and <code>toString</code>, 
            but does not suppress synthetic functions such as <code>dataClass.componentN</code> and 
            <code>dataClass.copy</code>. Use <code>suppressObviousFunctions</code>
            for that.
        </p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="suppressObviousFunctions">
        <p>Whether to suppress obvious functions.</p>
        <p>
            A function is considered to be obvious if it is:</p>
            <list>
                <li>
                    Inherited from <code>kotlin.Any</code>, <code>Kotlin.Enum</code>, <code>java.lang.Object</code> or
                    <code>java.lang.Enum</code>, such as <code>equals</code>, <code>hashCode</code>, <code>toString</code>.
                </li>
                <li>
                    Synthetic (generated by the compiler) and does not have any documentation, such as
                    <code>dataClass.componentN</code> or <code>dataClass.copy</code>.
                </li>
            </list>
        <p>Default: <code>true</code></p>
    </def>
    <def title="offlineMode">
        <p>Whether to resolve remote files and links over your network.</p>
        <p>
            This includes package-lists used for generating links to external documentation.. 
            For example, this allows to make classes from the standard library clickable in your documentation. 
        </p>
        <p>
            Setting this to <code>true</code> can significantly speed up build times in certain cases,
            but can also worsen user experience. For example, by
            not resolving class and member links from your dependencies, including the standard library.
        </p>
        <p>Note: You can cache fetched files locally and provide them to Dokka as local paths. See 
           the <code><a href="#external-documentation-links-configuration">externalDocumentationLinks</a></code> section.</p>
        <p>Default: <code>false</code></p>
    </def>
     <def title="includes">
        <p>
            A list of Markdown files that contain
            <a href="dokka-module-and-package-docs.md">subproject and package documentation</a>. The Markdown files must
            match the <a href="dokka-module-and-package-docs.md#file-format">required format</a>.
        </p>
        <p>The contents of the specified files are parsed and embedded into documentation as subproject and package descriptions.</p>
        <p>
            See <a href="https://github.com/Kotlin/dokka/blob/master/examples/gradle-v2/basic-gradle-example/build.gradle.kts">Dokka Gradle example</a>
            for an example of what it looks like and how to use it.
        </p>
    </def>
</deflist>

### Source set configuration

Dokka allows configuring some options for
[Kotlin source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets):

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

dokka {
    // ..
    // General configuration section
    // ..

    // Source sets configuration
    dokkaSourceSets {
        // Example: Configuration exclusive to the 'linux' source set
        named("linux") {
            dependentSourceSets{named("native")}
            sourceRoots.from(file("linux/src"))
        }

        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set(setOf(VisibilityModifier.Public)) // OR documentedVisibilities(VisibilityModifier.Public)
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            sourceRoots.from(file("src"))
            classpath.from(file("libs/dependency.jar"))
            samples.from("samples/Basic.kt", "samples/Advanced.kt")
           
            sourceLink {
                // Source link section
            }
            perPackageOption {
                // Package options section
            }
            externalDocumentationLinks {
                // External documentation links section
            }
        }
    }
}
```

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

dokka {
    // ..
    // General configuration section
    // ..

    dokkaSourceSets {
        // Example: Configuration exclusive to the 'linux' source set
        named("linux") {
            dependentSourceSets { named("native") }
            sourceRoots.from(file("linux/src"))
        }

        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set([VisibilityModifier.Public] as Set) // OR documentedVisibilities(VisibilityModifier.Public)
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            sourceRoots.from(file("src"))
            classpath.from(file("libs/dependency.jar"))
            samples.from("samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                // Source link section
            }
            perPackageOption {
                // Package options section
            }
            externalDocumentationLinks {
                // External documentation links section
            }
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="suppress">
        <p>Whether this source set should be skipped when generating documentation.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="displayName">
        <p>The display name used to refer to this source set.</p>
        <p>
            The name is used both externally (for example, as the source set name visible to documentation readers) and 
            internally (for example, for logging messages of <code>reportUndocumented</code>).
        </p>
        <p>By default, the value is deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="documentedVisibilities">
        <p>Defines which visibility modifiers Dokka should include in the generated documentation.</p>
        <p>
            Use them if you want to document <code>protected</code>, <code>internal</code>, and <code>private</code> declarations,
            as well as if you want to exclude <code>public</code> declarations and only document internal API.
        </p>
        <p>
            Additionally, you can use Dokka's 
            <a href="https://github.com/Kotlin/dokka/blob/v2.1.0/dokka-runners/dokka-gradle-plugin/src/main/kotlin/engine/parameters/HasConfigurableVisibilityModifiers.kt"><code>documentedVisibilities()</code> function</a> 
            to add documented visibilities.
        </p>
        <p>This can be configured for each individual package.</p>
        <p>Default: <code>VisibilityModifier.Public</code></p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
            after they have been filtered by <code>documentedVisibilities</code> and other filters.
        </p>
        <p>This setting works well with <code>failOnWarning</code>.</p>
        <p>This can be configured for each individual package.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="skipEmptyPackages">
        <p>
            Whether to skip packages that contain no visible declarations after
            various filters have been applied.
        </p>
        <p>
            For example, if <code>skipDeprecated</code> is set to <code>true</code> and your package contains only
            deprecated declarations, it is considered to be empty.
        </p>
        <p>Default: <code>true</code></p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>This can be configured for each individual package.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="suppressGeneratedFiles">
        <p>Whether to document generated files.</p>
        <p>
            Generated files are expected to be present under the <code>{project}/{buildDir}/generated</code> directory.
        </p>
        <p>
            If set to <code>true</code>, it effectively adds all files from that directory to the
            <code>suppressedFiles</code> option, so you can configure it manually.
        </p>
        <p>Default: <code>true</code></p>
    </def>
    <def title="suppressedAnnotations">
        <p>A set of annotation fully qualified names (FQNs) to suppress declarations annotated with.</p>
        <p>
            Any declaration annotated with one of these annotations is excluded from the generated documentation.
        </p>
    </def>
    <def title="jdkVersion">
        <p>The JDK version to use when generating external documentation links for Java types.</p>
        <p>
            For example, if you use <code>java.util.UUID</code> in some public declaration signature,
            and this option is set to <code>8</code>, Dokka generates an external documentation link
            to <a href="https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html">JDK 8 Javadocs</a> for it.
        </p>
        <p>Default: `8`</p>
    </def>
    <def title="languageVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">The Kotlin language version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
        <p>By default, the latest language version available to Dokka's embedded compiler is used.</p>
    </def>
    <def title="apiVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">The Kotlin API version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
        <p>By default, it is deduced from <code>languageVersion</code>.</p>
    </def>
    <def title="sourceRoots">
        <p>
            The source code roots to be analyzed and documented.
            Acceptable inputs are directories and individual <code>.kt</code> and <code>.java</code> files.
        </p>
        <p>By default, source roots are deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="classpath">
        <p>The classpath for analysis and interactive samples.</p>
        <p>This is useful if some types that come from dependencies are not resolved or picked up automatically.</p>
        <p>This option accepts both <code>.jar</code> and <code>.klib</code> files.</p>
        <p>By default, the classpath is deduced from information provided by the Kotlin Gradle plugin.</p>
    </def>
    <def title="samples">
        <p>
            A list of directories or files that contain sample functions which are referenced via the
            <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a> KDoc tag.
        </p>
    </def>
</deflist>

### Source link configuration

Configure source links to help readers find the source for each declaration in a remote repository.
Use the `dokkaSourceSets.main {}` block for this configuration.

The `sourceLinks {}` configuration block allows you to add a `source` link to each signature
that leads to the `remoteUrl` with a specific line number.
The line number is configurable by setting `remoteLineSuffix`.


For an example, see the documentation for the
[`count()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/count.html)
function in `kotlinx.coroutines`.

The syntax of `build.gradle.kts` files differs from regular `.kt`
files (such as those used for custom Gradle plugins) because Gradle's Kotlin DSL uses type-safe accessors:

<tabs group="dokka-configuration">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

```kotlin
// build.gradle.kts

dokka {
    dokkaSourceSets.main {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/your-repo")
            remoteLineSuffix.set("#L")
        }
    }
}
```

</tab>
<tab title="Kotlin custom plugin" group-key="kotlin custom">

```kotlin
// CustomPlugin.kt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension

abstract class CustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.jetbrains.dokka")
        project.extensions.configure(DokkaExtension::class.java) { dokka ->
            dokka.dokkaSourceSets.named("main") { dss ->
                dss.includes.from("README.md")
                dss.sourceLink {
                    it.localDirectory.set(project.file("src/main/kotlin"))
                    it.remoteUrl("https://example.com/src")
                    it.remoteLineSuffix.set("#L")
                }
            }
        }
    }
}
```

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

```groovy
dokka {
    dokkaSourceSets {
        main {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(new URI("https://github.com/your-repo"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="localDirectory">
        <p>
            The path to the local source directory. The path must be relative to the root of 
            the current project.
        </p>
    </def>
    <def title="remoteUrl">
        <p>
            The URL of the source code hosting service that can be accessed by documentation readers,
            like GitHub, GitLab, Bitbucket, or any hosting service that provides stable URLs for source files. 
            This URL is used to generate
            source code links of declarations.
        </p>
    </def>
    <def title="remoteLineSuffix">
        <p>
            The suffix used to append the source code line number to the URL. This helps readers navigate
            not only to the file, but to the specific line number of the declaration.
        </p>
        <p>
            The number itself is appended to the specified suffix. For example,
            if this option is set to <code>#L</code> and the line number is 10, the resulting URL suffix
            is <code>#L10</code>.
        </p>
        <p>
            Suffixes used by popular services:</p>
            <list>
                <li>GitHub: <code>#L</code></li>
                <li>GitLab: <code>#L</code></li>
                <li>Bitbucket: <code>#lines-</code></li>
            </list>
        <p>Default: <code>#L</code></p>
    </def>
</deflist>

### Package options

The `perPackageOption` configuration block allows setting some options for specific packages matched by `matchingRegex`:

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

dokka {
    dokkaPublications.html {
        dokkaSourceSets.configureEach {
            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set(setOf(VisibilityModifier.Public)) // OR documentedVisibilities(VisibilityModifier.Public)
            }
        }
    }
}
```

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

dokka {
    dokkaPublications {
        html {
            dokkaSourceSets.configureEach {
                perPackageOption {
                    matchingRegex.set(".*api.*")
                    suppress.set(false)
                    skipDeprecated.set(false)
                    reportUndocumented.set(false)
                    documentedVisibilities.set([VisibilityModifier.Public] as Set)
                }
            }
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="matchingRegex">
        <p>The regular expression that is used to match the package.</p>
        <p>Default: <code>.*</code></p>
    </def>
    <def title="suppress">
        <p>Whether the package should be skipped when generating documentation.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>This can be configured on the source set level.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
            after they have been filtered by <code>documentedVisibilities</code> and other filters.
        </p>
        <p>This setting works well with <code>failOnWarning</code>.</p>
        <p>This can be configured on the source set level.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="documentedVisibilities">
        <p>Defines which visibility modifiers Dokka should include in the generated documentation.</p>
        <p>
            Use them if you want to document <code>protected</code>, <code>internal</code>, and <code>private</code> 
            declarations within this package,
            as well as if you want to exclude <code>public</code> declarations and only document internal API.
        </p>
        <p>
            Additionally, you can use Dokka's 
            <a href="https://github.com/Kotlin/dokka/blob/v2.0.0/dokka-runners/dokka-gradle-plugin/src/main/kotlin/engine/parameters/HasConfigurableVisibilityModifiers.kt#L14-L16"><code>documentedVisibilities()</code> function</a> 
            to add documented visibilities.
        </p>
        <p>This can be configured on the source set level.</p>
        <p>Default: <code>VisibilityModifier.Public</code></p>
    </def>
</deflist>

### External documentation links configuration

The `externalDocumentationLinks {}`
block allows the creation of links that lead to the externally hosted documentation of
your dependencies.

For example, if you are using types from `kotlinx.serialization`, by default they are not clickable in your
documentation, as if they are unresolved. However, since the API reference documentation for `kotlinx.serialization`
is built by Dokka and is [published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/), you can
configure external documentation links for it. This allows Dokka to generate links for types from the library, making
them resolve successfully and clickable.

By default, external documentation links for Kotlin standard library, JDK, Android SDK, and AndroidX are configured.

Register external documentation links using the `register()` method to define each link.
The `externalDocumentationLinks` API uses this method aligning with Gradle DSL conventions:

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

```kotlin
dokka {
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("example-docs") {
            url("https://example.com/docs/")
            packageListUrl("https://example.com/docs/package-list")
        }
    }
}
```

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

```groovy
dokka {
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("example-docs") {
            url.set(new URI("https://example.com/docs/"))
            packageListUrl.set(new URI("https://example.com/docs/package-list"))
        }
    }
}
```

</tab>
</tabs>

<deflist collapsible="true">
    <def title="url">
        <p>The root URL of documentation to link to. It <b>must</b> contain a trailing slash.</p>
        <p>
            Dokka does its best to automatically find <code>package-list</code> for the given URL, 
            and link declarations together.
        </p>
        <p>
            If the automatic resolution fails or if you want to use locally cached files instead, 
            consider setting the <code>packageListUrl</code> option.
        </p>
    </def>
    <def title="packageListUrl">
        <p>
            The exact location of a <code>package-list</code>. This is an alternative to relying on Dokka
            automatically resolving it.
        </p>
        <p>
            Package lists contain information about the documentation and the project itself, 
            such as subproject and package names.
        </p>
        <p>This can also be a locally cached file to avoid network calls.</p>
    </def>
</deflist>

### Complete configuration

Below you can see all possible configuration options applied at the same time:

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}

dokka {
    dokkaPublications.html {
        moduleName.set(project.name)
        moduleVersion.set(project.version.toString())
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        failOnWarning.set(false)
        suppressInheritedMembers.set(false)
        suppressObviousFunctions.set(true)
        offlineMode.set(false)
        includes.from("packages.md", "extra.md")
   }

    dokkaSourceSets {
        // Example: Configuration exclusive to the 'linux' source set
        named("linux") {
            dependentSourceSets{named("native")}
            sourceRoots.from(file("linux/src"))
        }

        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set(setOf(VisibilityModifier.Public)) // OR documentedVisibilities(VisibilityModifier.Public)
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            sourceRoots.from(file("src"))
            classpath.from(file("libs/dependency.jar"))
            samples.from("samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl("https://example.com/src")
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLinks {
                url = URL("https://example.com/docs/")
                packageListUrl = File("/path/to/package-list").toURI().toURL()
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set(
                    setOf(
                        VisibilityModifier.Public,
                        VisibilityModifier.Private,
                        VisibilityModifier.Protected,
                        VisibilityModifier.Internal,
                        VisibilityModifier.Package
                    )
                )
            }
        }
    }
}
```

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

```groovy
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}

dokka {
    dokkaPublications {
        html {
            moduleName.set(project.name)
            moduleVersion.set(project.version.toString())
            outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
            failOnWarning.set(false)
            suppressInheritedMembers.set(false)
            suppressObviousFunctions.set(true)
            offlineMode.set(false)
            includes.from("packages.md", "extra.md")
        }
    }

    dokkaSourceSets {
        // Example: Configuration exclusive to the 'linux' source set
        named("linux") {
            dependentSourceSets { named("native") }
            sourceRoots.from(file("linux/src"))
        }

        configureEach {
            suppress.set(false)
            displayName.set(name)
            documentedVisibilities.set([VisibilityModifier.Public] as Set)
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            suppressGeneratedFiles.set(true)
            jdkVersion.set(8)
            languageVersion.set("1.7")
            apiVersion.set("1.7")
            sourceRoots.from(file("src"))
            classpath.from(file("libs/dependency.jar"))
            samples.from("samples/Basic.kt", "samples/Advanced.kt")

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(new URI("https://example.com/src"))
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLinks {
                url.set(new URI("https://example.com/docs/"))
                packageListUrl.set(new File("/path/to/package-list").toURI().toURL())
            }

            perPackageOption {
                matchingRegex.set(".*api.*")
                suppress.set(false)
                skipDeprecated.set(false)
                reportUndocumented.set(false)
                documentedVisibilities.set([
                        VisibilityModifier.Public,
                        VisibilityModifier.Private,
                        VisibilityModifier.Protected,
                        VisibilityModifier.Internal,
                        VisibilityModifier.Package
                ] as Set)
            }
        }
    }
}
```

</tab>
</tabs>
