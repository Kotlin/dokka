[//]: # (title: CLI runner)

If for some reason you cannot use [Gradle](gradle.md) or [Maven](maven.md) plugins, Dokka has
a command line runner for generating documentation without any build tools.

In comparison, it has the same, if not more, capabilities as the Gradle plugin, although it is considerably more 
difficult to set up as there is no autoconfiguration, especially in multiplatform and multimodule environments.

CLI runner is published as a separate artifact to Maven Central under `org.jetbrains.dokka:dokka-cli`, so you can 
find it on [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-cli) or by browsing 
[maven central repository directories](https://repo1.maven.org/maven2/org/jetbrains/dokka/dokka-cli/) directly.

## Getting started

With `dokka-cli-%dokkaVersion%.jar` file on your computer, you can run it with `-help` to see available arguments and their description:

```Bash
java -jar dokka-cli-%dokkaVersion%.jar -help
```

It also works for some nested arguments, such as `-sourceSet`:

```Bash
java -jar dokka-cli-%dokkaVersion%.jar -sourceSet -help
```

## Generating documentation

### Pre-requisites

Since there is no build tool to manage dependencies, you will have to provide dependency `.jar` files yourself. 

Dependencies you will need for any format:

| **Group**             | **Artifact**               | **Version**    | **Link**                                                                                                        |
|-----------------------|----------------------------|----------------|-----------------------------------------------------------------------------------------------------------------|
| `org.jetbrains.dokka` | `dokka-base`               | %dokkaVersion% | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-base/%dokkaVersion%)               |
| `org.jetbrains.dokka` | `dokka-analysis`           | %dokkaVersion% | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-analysis/%dokkaVersion%)           |
| `org.jetbrains.dokka` | `kotlin-analysis-compiler` | %dokkaVersion% | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/kotlin-analysis-compiler/%dokkaVersion%) |
| `org.jetbrains.dokka` | `kotlin-analysis-intellij` | %dokkaVersion% | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/kotlin-analysis-intellij/%dokkaVersion%) |

Additional dependencies for [HTML](html.md) format:

| **Group**               | **Artifact**       | **Version** | **Link**                                                                                         |
|-------------------------|--------------------|-------------|--------------------------------------------------------------------------------------------------|
| `org.jetbrains.kotlinx` | `kotlinx-html-jvm` | 0.8.0       | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-html-jvm/0.8.0) |
| `org.freemarker`        | `freemarker`       | 2.3.31      | [mvnrepository](https://mvnrepository.com/artifact/org.freemarker/freemarker/2.3.31)             |


### Running with command line arguments

You can pass command line arguments to configure the CLI runner. 

At the very least you would need to provide the following arguments:
* `-pluginsClasspath` - a list of absolute/relative paths to downloaded dependencies, separated by semi-colons `;`
* `-sourceSet` - an absolute path to code sources for which to generate documentation
* `-outputDir` - an absolute/relative path of documentation output directory

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;./dokka-analysis-%dokkaVersion%.jar;./kotlin-analysis-intellij-%dokkaVersion%.jar;./kotlin-analysis-compiler-%dokkaVersion%.jar;./kotlinx-html-jvm-0.8.0.jar;./freemarker-2.3.31.jar" \
     -sourceSet "-src /home/myCoolProject/src/main/kotlin" \
     -outputDir "./dokka/html"
```

> Due to an internal class conflict, first pass `kotlin-analysis-intellij` and only then `kotlin-analysis-compiler`,
> otherwise it can lead to obscure exceptions such as `NoSuchFieldError`.
>
{type="note"}

Executing the given example should generate documentation in [HTML](html.md) format.

See [command line arguments](#command-line-arguments) for more configuration details.

### Running with JSON configuration

It is possible to configure the CLI runner with JSON. In this case, you will need to provide an
absolute/relative path to the JSON configuration file, and  all other configuration options will be parsed from it.

```Bash
java -jar dokka-cli-%dokkaVersion%.jar dokka-configuration.json
```

At the very least, you would need the following configuration:
```json
{
  "outputDir": "./dokka/html",
  "sourceSets": [
    {
      "sourceSetID": {
        "scopeId": "moduleName",
        "sourceSetName": "main"
      },
      "sourceRoots": [
        "/home/myCoolProject/src/main/kotlin"
      ]
    }
  ],
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "./kotlinx-html-jvm-0.8.0.jar",
    "./dokka-analysis-%dokkaVersion%.jar",
    "./kotlin-analysis-intellij-%dokkaVersion%.jar",
    "./kotlin-analysis-compiler-%dokkaVersion%.jar",
    "./freemarker-2.3.31.jar"
  ]
}
```

> Due to an internal class conflict, first pass `kotlin-analysis-intellij` and only then `kotlin-analysis-compiler`,
> otherwise it can lead to obscure exceptions such as `NoSuchFieldError`.
>
{type="note"}

See [JSON configuration options](#json-configuration) for more details.

### Other output formats

By default, `dokka-base` artifact contains stable [HTML](html.md) format only. 

All other output formats come as [Dokka plugins](plugins_introduction.md). In order to use them, you have to put it
on plugins classpath.

For example, if you want to generate documentation in experimental [GFM](markdown.md#gfm) format, you have to download and
pass [gfm-plugin's jar](https://mvnrepository.com/artifact/org.jetbrains.dokka/gfm-plugin/%dokkaVersion%) into 
`pluginsClasspath` configuration option.

Via command line arguments:

```Shell
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./gfm-plugin-%dokkaVersion%.jar" \
     ...
```

Via JSON configuration:

```json
{
  ...
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "...",
    "./gfm-plugin-%dokkaVersion%.jar"
  ],
  ...
}
```

With GFM plugin on classpath, CLI runner should generate documentation in GFM format, no extra actions 
needed.

For more information, see [Markdown](markdown.md) and [Javadoc](javadoc.md#geneatring-javadoc-documentation) topics.

## Command line arguments

You can see short descriptions for command line arguments by running:

```Bash
java -jar dokka-cli-%dokkaVersion%.jar -help
```

Summary:

- `-moduleName` -> Name of the project/module.
- `-moduleVersion` -> Documented version.
- `-outputDir` -> Output directory path, `./dokka` by default.
- `-sourceSet` -> Configuration for a Dokka source set. Contains nested configuration.
- `-pluginsConfiguration` -> Configuration for Dokka plugins.
- `-pluginsClasspath` -> List of jars with Dokka plugins and their dependencies. Accepts multiple paths separated by semicolons.
- `-offlineMode` -> Whether to resolve remote files/links over network.
- `-failOnWarning` -> Whether to fail documentation generation if Dokka has emitted a warning or an error.
- `-delayTemplateSubstitution` -> Delay substitution of some elements. Used in incremental builds of 
   multimodule projects.
- `-noSuppressObviousFunctions` -> Whether to suppress obvious functions such as inherited from `kotlin.Any` 
  and `java.lang.Object`.
- `-includes` -> Markdown files that contain module and package documentation. Accepts multiple values separated by 
  semicolons.
- `-suppressInheritedMembers` -> Whether to suppress inherited members that aren't explicitly overridden in a 
  given class.
- `-globalPackageOptions` -> Global list of package configurations in format 
  `"matchingRegex,-deprecated,-privateApi,+warnUndocumented,+suppress;+visibility:PUBLIC;..."`. 
  Accepts multiple values separated by semicolons.
- `-globalLinks` -> Global external documentation links in format `{url}^{packageListUrl}`. 
  Accepts multiple values separated by `^^`.
- `-globalSrcLink` -> Global mapping between a source directory and a Web service for browsing the code. 
  Accepts multiple paths separated by semicolons.
- `-helpSourceSet` -> Prints help for nested `-sourceSet` configuration.
- `-loggingLevel` -> Logging level, possible values: `DEBUG, PROGRESS, INFO, WARN, ERROR`.
- `-help, -h` -> Usage info.

#### Source set arguments

You can also see short descriptions for nested `-sourceSet` configuration:

```Bash
java -jar dokka-cli-%dokkaVersion%.jar -sourceSet -help
```

Summary:

- `-sourceSetName` -> Name of the source set.
- `-displayName` -> Display name of the source set, used both internally and externally.
- `-classpath` -> Classpath for analysis and interactive samples. Accepts multiple paths separated by semicolons.
- `-src` -> Source code roots to be analyzed and documented. Accepts multiple paths separated by semicolons.
- `-dependentSourceSets` -> Names of dependent source sets in format `moduleName/sourceSetName`. 
  Accepts multiple paths separated by semicolons.
- `-samples` -> List of directories or files that contain sample functions. Accepts multiple paths separated by semicolons.
- `-includes` -> Markdown files that contain module and package documentation. Accepts multiple paths separated by semicolons.
- `-includeNonPublic` -> Deprecated, use `documentedVisibilities`. 
  Possible values: `PUBLIC`, `PRIVATE`, `PROTECTED`, `INTERNAL`, `PACKAGE`.
- `-documentedVisibilities` -> Visibilities to be documented. Accepts multiple values separated by semicolons.
- `-reportUndocumented` -> Whether to report undocumented declarations. 
- `-noSkipEmptyPackages` -> Whether to create pages for empty packages. 
- `-skipDeprecated` -> Whether to skip deprecated declarations. 
- `-jdkVersion` -> Version of JDK to use for linking to JDK Javadocs.
- `-languageVersion` -> Language version used for setting up analysis and samples.
- `-apiVersion` -> Kotlin API version used for setting up analysis and samples.
- `-noStdlibLink` -> Whether to generate links to Standard library. 
- `-noJdkLink` -> Whether to generate links to JDK Javadocs. 
- `-suppressedFiles` -> Paths to files to be suppressed. Accepts multiple paths separated by semicolons.
- `-analysisPlatform` -> Platform used for setting up analysis.
- `-perPackageOptions` -> List of package source set configuration in format 
  `matchingRegexp,-deprecated,-privateApi,+warnUndocumented,+suppress;...`. Accepts multiple values separated by semicolons.
- `-externalDocumentationLinks` -> External documentation links in format `{url}^{packageListUrl}`. 
  Accepts multiple values separated by `^^`.
- `-srcLink` -> Mapping between a source directory and a Web service for browsing the code. 
  Accepts multiple paths separated by semicolons.
- `-help`, `-h` -> Usage info

## JSON configuration

Below you will find examples and detailed descriptions for each configuration section. You can also find an example 
with [all configuration options](#complete-configuration) applied at once at the very bottom.

### General configuration

```json
{
  "moduleName": "Dokka Example",
  "moduleVersion": null,
  "outputDir": "./build/dokka/html",
  "failOnWarning": false,
  "suppressObviousFunctions": true,
  "suppressInheritedMembers": false,
  "offlineMode": false,
  "includes": [
    "module.md"
  ],
  "sourceLinks":  [
    { "_comment": "Options are described in a separate section" }
  ],
  "perPackageOptions": [
    { "_comment": "Options are described in a separate section" }
  ],
  "externalDocumentationLinks":  [
    { "_comment": "Options are described in a separate section" }
  ],
  "sourceSets": [
    { "_comment": "Options are described in a separate section" }
  ],
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "./kotlinx-html-jvm-0.8.0.jar",
    "./dokka-analysis-%dokkaVersion%.jar",
    "./kotlin-analysis-intellij-%dokkaVersion%.jar",
    "./kotlin-analysis-compiler-%dokkaVersion%.jar",
    "./freemarker-2.3.31.jar"
  ]
}
```

<deflist>
    <def title="moduleName">
        <p>Display name used to refer to the module. Used for ToC, navigation, logging, etc.</p>
        <p>Default is <code>root</code>.</p>
    </def>
    <def title="moduleVersion">
        <p>Module version.</p>
        <p>Default is empty.</p>
    </def>
    <def title="outputDirectory">
        <p>Directory to which documentation will be generated, regardless of format.</p>
        <p>Default is <code>./dokka</code></p>
    </def>
    <def title="failOnWarning">
        <p>
            Whether to fail documentation generation if Dokka has emitted a warning or an error.
            Will wait until all errors and warnings have been emitted first.
        </p>
        <p>This setting works well with <code>reportUndocumented</code></p>
        <p>Default is <code>false</code>.</p>
    </def>
    <def title="suppressObviousFunctions">
        <p>Whether to suppress obvious functions.</p>
        <p>
            A function is considered to be obvious if it is:
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
        </p>
        <p>Default is <code>true</code>.</p>
    </def>
    <def title="suppressInheritedMembers">
        <p>Whether to suppress inherited members that aren't explicitly overridden in a given class.</p>
        <p>
            Note: this can suppress functions such as <code>equals</code> / <code>hashCode</code> / <code>toString</code>, 
            but cannot suppress synthetic functions such as <code>dataClass.componentN</code> and 
            <code>dataClass.copy</code>. Use <code>suppressObviousFunctions</code>
            for that.
        </p>
        <p>Default is <code>false</code>.</p>
    </def>
    <def title="offlineMode">
        <p>Whether to resolve remote files/links over network.</p>
        <p>
            This includes package-lists used for generating external documentation links:
            for instance, to make classes from standard library clickable.
        </p>
        <p>
            Setting this to <code>true</code> can significantly speed up build times in certain cases,
            but can also worsen documentation quality and user experience, for instance by
            not resolving some dependency's class/member links.
        </p>
        <p>
            Note: you can cache fetched files locally and provide them to
            Dokka as local paths. See <code>externalDocumentationLinks</code>.
        </p>
        <p>Default is <code>false</code>.</p>
    </def>
    <def title="includes">
        <p>
            List of Markdown files that contain
            <a href="https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation">module and package documentation</a>.
        </p>
        <p>Contents of specified files will be parsed and embedded into documentation as module and package descriptions.</p>
        <p>Can be configured on per-package basis.</p>
    </def>
    <def title="sourceSets">
        <p>
          Individual and additional configuration of Kotlin  
          <a href="https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets">source sets</a>.
        </p>
        <p>For a list of possible options, see <a href="#source-set-configuration">source set configuration</a>.</p>
    </def>
    <def title="sourceLinks">
        <p>Global configuration of source links that will be applied for all source sets.</p>
        <p>For a list of possible options, see <a href="#source-link-configuration">source link configuration</a>.</p>
    </def>
    <def title="perPackageOptions">
        <p>Global configuration of matched packages, regardless of the source set they are in.</p>
        <p>For a list of possible options, see <a href="#per-package-configuration">per-package configuration</a>.</p>
    </def>
    <def title="externalDocumentationLinks">
        <p>Global configuration of external documentation links, regardless of the source set they are used in.</p>
        <p>For a list of possible options, see <a href="#external-documentation-configuration">external documentation configuration</a>.</p>
    </def>
    <def title="pluginsClasspath">
        <p>List of jars with Dokka plugins and their dependencies.</p>
    </def>
</deflist>

### Source set configuration

Configuration of Kotlin
[source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets).

```json
{
  "sourceSets": [
    {
      "displayName": "jvm",
      "sourceSetID": {
        "scopeId": "moduleName",
        "sourceSetName": "main"
      },
      "dependentSourceSets": [
        {
          "scopeId": "dependentSourceSetScopeId",
          "sourceSetName": "dependentSourceSetName"
        }
      ],
      "documentedVisibilities": ["PUBLIC", "PRIVATE", "PROTECTED", "INTERNAL", "PACKAGE"],
      "reportUndocumented": false,
      "skipEmptyPackages": true,
      "skipDeprecated": false,
      "jdkVersion": 8,
      "languageVersion": "1.7",
      "apiVersion": "1.7",
      "noStdlibLink": false,
      "noJdkLink": false,
      "includes": [
        "module.md"
      ],
      "analysisPlatform": "jvm",
      "sourceRoots": [
        "/home/ignat/IdeaProjects/dokka-debug-mvn/src/main/kotlin"
      ],
      "classpath": [
        "libs/kotlin-stdlib-%kotlinVersion%.jar",
        "libs/kotlin-stdlib-common-%kotlinVersion%.jar"
      ],
      "samples": [
        "samples/basic.kt"
      ],
      "suppressedFiles": [
        "src/main/kotlin/org/jetbrains/dokka/Suppressed.kt"
      ],
      "sourceLinks":  [
        { "_comment": "Options are described in a separate section" }
      ],
      "perPackageOptions": [
        { "_comment": "Options are described in a separate section" }
      ],
      "externalDocumentationLinks":  [
        { "_comment": "Options are described in a separate section" }
      ]
    }
  ]
}
```

<deflist>
    <def title="displayName">
        <p>Display name used to refer to the source set.</p>
        <p>
            The name will be used both externally (for example, as source set name visible to documentation readers) and 
            internally (for example, for logging messages of <code>reportUndocumented</code>).
        </p>
        <p>Platform name could be used if you don't have a better alternative.</p>
    </def>
    <def title="sourceSetID">
        <p>Technical ID of the source set</p>
    </def>
    <def title="documentedVisibilities">
        <p>Set of visibility modifiers that should be documented.</p>
        <p>
            This can be used if you want to document protected/internal/private declarations,
            as well as if you want to exclude public declarations and only document internal API.
        </p>
        <p>Can be configured on per-package basis.</p>
        <p>
            Possible values:
            <list>
                <li><code>PUBLIC</code></li>
                <li><code>PRIVATE</code></li>
                <li><code>PROTECTED</code></li>
                <li><code>INTERNAL</code></li>
                <li><code>PACKAGE</code></li>
            </list>
        </p>
        <p>Default is <code>PUBLIC</code>.</p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
            after they have been filtered by <code>documentedVisibilities</code>.
        </p>
        <p>This setting works well with <code>failOnWarning</code>. Can be overridden for a specific package</p>
        <p>Default is <code>false</code>.</p>
    </def>
    <def title="skipEmptyPackages">
        <p>
            Whether to skip packages that contain no visible declarations after
            various filters have been applied.
        </p>
        <p>
            For instance, if <code>skipDeprecated</code> is set to <code>true</code> and your package contains only
            deprecated declarations, it will be considered to be empty.
        </p>
        <p>Default for CLI runner is <code>false</code>.</p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>Can be overridden on package level.</p>
        <p>Default is <code>false</code>.</p>
    </def>
    <def title="jdkVersion">
        <p>JDK version to use when generating external documentation links for Java types.</p>
        <p>
            For instance, if you use <code>java.util.UUID</code> from JDK in some public declaration signature,
            and this property is set to <code>8</code>, Dokka will generate an external documentation link
            to <a href="https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html">JDK 8 Javadocs</a> for it.
        </p>
    </def>
    <def title="languageVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">Kotlin language version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
    </def>
    <def title="apiVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">Kotlin API version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
    </def>
    <def title="noStdlibLink">
        <p>
            Whether to generate external documentation links that lead to API reference
            documentation for Kotlin's standard library when declarations from it are used.
        </p>
        <p>Default is <code>false</code>, meaning links will be generated.</p>
    </def>
    <def title="noJdkLink">
        <p>Whether to generate external documentation links to JDK's Javadocs when declarations from it are used.</p>
        <p>The version of JDK Javadocs is determined by <code>jdkVersion</code> property.</p>
        <p>Default is <code>false</code>, meaning links will be generated.</p>
    </def>
    <def title="includes">
        <p>
            List of Markdown files that contain
            <a href="https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation">module and package documentation</a>.
        </p>
        <p>Contents of specified files will be parsed and embedded into documentation as module and package descriptions.</p>
    </def>
    <def title="analysisPlatform">
        <p>
            Platform to be used for setting up code analysis and 
            <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a> environment.
        </p>
        <p>The default value is deduced from information provided by the Kotlin Gradle plugin.</p>
        <p>
            Possible values:
            <list>
                <li><code>jvm</code></li>
                <li><code>common</code></li>
                <li><code>js</code></li>
                <li><code>native</code></li>
            </list>
        </p>
    </def>
    <def title="sourceRoots">
        <p>
            Source code roots to be analyzed and documented.
            Accepts directories and individual <code>.kt</code> / <code>.java</code> files.
        </p>
    </def>
    <def title="classpath">
        <p>
            Classpath for analysis and interactive samples. If you use a declaration from a dependency, 
            it should be present on the classpath to be resolved.
        </p>
        <p>Property accepts both <code>.jar</code> and <code>.klib</code> files.</p>
    </def>
    <def title="samples">
        <p>
            List of directories or files that contain sample functions which are referenced via
            <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a> KDoc tag.
        </p>
    </def>
    <def title="suppressedFiles">
        <p>Files to be suppressed when generating documentation.</p>
    </def>
    <def title="sourceLinks">
        <p>Configuration of source links that will be applied only for this source set.</p>
        <p>For a list of possible options, see <a href="#source-link-configuration">source link configuration</a>.</p>
    </def>
    <def title="perPackageOptions">
        <p>Configuration specific to matched packages within this source set.</p>
        <p>For a list of possible options, see <a href="#per-package-configuration">per-package configuration</a>.</p>
    </def>
    <def title="externalDocumentationLinks">
        <p>Configuration of external documentation links that will be applied only for this source set.</p>
        <p>For a list of possible options, see <a href="#external-documentation-configuration">external documentation configuration</a>.</p>
    </def>
</deflist>

### Source link configuration

Configuration block that allows adding a `source` link to each signature
which leads to `remoteUrl` with a specific line number (configurable by setting `remoteLineSuffix`),
letting documentation readers find source code for each declaration.

For an example, see documentation for
[count](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/count.html)
function from `kotlinx.coroutines`.

Configurable for all source sets at once, or [for each source set individually](#source-set-configuration)

```json
{
  "sourceLinks": [
    {
      "localDirectory": "src/main/kotlin",
      "remoteUrl": "https://github.com/Kotlin/dokka/tree/master/src/main/kotlin",
      "remoteLineSuffix": "#L"
    }
  ]
}
```

<deflist>
    <def title="localDirectory">
        <p>Path to the local source directory.</p>
    </def>
    <def title="remoteUrl">
        <p>
            URL of source code hosting service that can be accessed by documentation readers,
            like GitHub, GitLab, Bitbucket, etc. This URL will be used to generate
            source code links of declarations.
        </p>
    </def>
    <def title="remoteLineSuffix">
        <p>
            Suffix used to append source code line number to the URL. This will help readers navigate
            not only to the file, but to the specific line number of the declaration.
        </p>
        <p>
            The number itself will be appended to the specified suffix. For instance,
            if this property is set to <code>#L</code> and the line number is 10, resulting URL suffix
            will be <code>#L10</code>.
        </p>
        <p>
            Suffixes used by popular services:
            <list>
                <li>GitHub: <code>#L</code></li>
                <li>GitLab: <code>#L</code></li>
                <li>Bitbucket: <code>#lines-</code></li>
            </list>
        </p>
        <p>Default is empty (no suffix).</p>
    </def>
</deflist>

### Per-package configuration

Configuration block that allows setting some options for specific packages matched by `matchingRegex`.

Configurable for all source sets at once, or [for each source set individually](#source-set-configuration)

```json
{
  "perPackageOptions": [
    {
      "matchingRegex": ".*internal.*",
      "suppress": false,
      "skipDeprecated": false,
      "reportUndocumented": false,
      "documentedVisibilities": ["PUBLIC", "PRIVATE", "PROTECTED", "INTERNAL", "PACKAGE"]
    }
  ]
}
```

<deflist>
    <def title="matchingRegex">
        <p>Regular expression that is used to match the package.</p>
    </def>
    <def title="suppress">
        <p>Whether this package should be skipped when generating documentation.</p>
        <p>Default is <code>false</code>.</p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>Can be set on project/module level.</p>
        <p>Default is <code>false</code>.</p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations from
            this package and without KDocs, after they have been filtered by <code>documentedVisibilities</code>.
        </p>
        <p>This setting works well with <code>failOnWarning</code>.</p>
        <p>Can be configured on source set level.</p>
        <p>Default is <code>false</code>.</p>
    </def>
    <def title="documentedVisibilities">
        <p>Set of visibility modifiers that should be documented.</p>
        <p>
            This can be used if you want to document protected/internal/private declarations within a
            specific package, as well as if you want to exclude public declarations and only document internal API.
        </p>
        <p>Can be configured on source set level.</p>
        <p>Default is <code>PUBLIC</code>.</p>
    </def>
</deflist>

### External documentation configuration

Configuration block that allows creating links leading to externally hosted documentation of your dependencies.

For instance, if you are using types from `kotlinx.serialization`, by default they will be unclickable in your
documentation, as if unresolved. However, since API reference for `kotlinx.serialization` is also built by Dokka and is
[published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/), you can configure external
documentation links for it, allowing Dokka to generate links for used types, making them clickable
and appear resolved.

Configurable for all source sets at once, or [for each source set individually](#source-set-configuration)

```json
{
  "externalDocumentationLinks": [
    {
      "url": "https://kotlinlang.org/api/kotlinx.serialization/",
      "packageListUrl": "https://kotlinlang.org/api/kotlinx.serialization/package-list"
    }
  ]
}
```

<deflist>
    <def title="url">
        <p>Root URL of documentation to link with. Must contain a trailing slash.</p>
        <p>
            Dokka will do its best to automatically find <code>package-list</code> for the given URL, 
            and link declarations together.
        </p>
        <p>
            If automatic resolution fails or if you want to use locally cached files instead, 
            consider providing <code>packageListUrl</code>.
        </p>
    </def>
    <def title="packageListUrl">
        <p>
            Specifies the exact location of a <code>package-list</code> instead of relying on Dokka
            automatically resolving it. Can also be a locally cached file to avoid network calls.
        </p>
    </def>
</deflist>

### Complete configuration

Below you can see all possible configuration options applied at once.

```json
{
  "moduleName": "Dokka Example",
  "moduleVersion": null,
  "outputDir": "./build/dokka/html",
  "failOnWarning": false,
  "suppressObviousFunctions": true,
  "suppressInheritedMembers": false,
  "offlineMode": false,
  "sourceLinks": [
    {
      "localDirectory": "src/main/kotlin",
      "remoteUrl": "https://github.com/Kotlin/dokka/tree/master/src/main/kotlin",
      "remoteLineSuffix": "#L"
    }
  ],
  "externalDocumentationLinks": [
    {
      "url": "https://docs.oracle.com/javase/8/docs/api/",
      "packageListUrl": "https://docs.oracle.com/javase/8/docs/api/package-list"
    },
    {
      "url": "https://kotlinlang.org/api/latest/jvm/stdlib/",
      "packageListUrl": "https://kotlinlang.org/api/latest/jvm/stdlib/package-list"
    }
  ],
  "perPackageOptions": [
    {
      "matchingRegex": ".*internal.*",
      "suppress": false,
      "reportUndocumented": false,
      "skipDeprecated": false,
      "documentedVisibilities": ["PUBLIC", "PRIVATE", "PROTECTED", "INTERNAL", "PACKAGE"]
    }
  ],
  "sourceSets": [
    {
      "displayName": "jvm",
      "sourceSetID": {
        "scopeId": "moduleName",
        "sourceSetName": "main"
      },
      "dependentSourceSets": [
        {
          "scopeId": "dependentSourceSetScopeId",
          "sourceSetName": "dependentSourceSetName"
        }
      ],
      "documentedVisibilities": ["PUBLIC", "PRIVATE", "PROTECTED", "INTERNAL", "PACKAGE"],
      "reportUndocumented": false,
      "skipEmptyPackages": true,
      "skipDeprecated": false,
      "jdkVersion": 8,
      "languageVersion": "1.7",
      "apiVersion": "1.7",
      "noStdlibLink": false,
      "noJdkLink": false,
      "includes": [
        "module.md"
      ],
      "analysisPlatform": "jvm",
      "sourceRoots": [
        "/home/ignat/IdeaProjects/dokka-debug-mvn/src/main/kotlin"
      ],
      "classpath": [
        "libs/kotlin-stdlib-%kotlinVersion%.jar",
        "libs/kotlin-stdlib-common-%kotlinVersion%.jar"
      ],
      "samples": [
        "samples/basic.kt"
      ],
      "suppressedFiles": [
        "src/main/kotlin/org/jetbrains/dokka/Suppressed.kt"
      ],
      "sourceLinks": [
        {
          "localDirectory": "src/main/kotlin",
          "remoteUrl": "https://github.com/Kotlin/dokka/tree/master/src/main/kotlin",
          "remoteLineSuffix": "#L"
        }
      ],
      "externalDocumentationLinks": [
        {
          "url": "https://docs.oracle.com/javase/8/docs/api/",
          "packageListUrl": "https://docs.oracle.com/javase/8/docs/api/package-list"
        },
        {
          "url": "https://kotlinlang.org/api/latest/jvm/stdlib/",
          "packageListUrl": "https://kotlinlang.org/api/latest/jvm/stdlib/package-list"
        }
      ],
      "perPackageOptions": [
        {
          "matchingRegex": ".*internal.*",
          "suppress": false,
          "reportUndocumented": false,
          "skipDeprecated": false,
          "documentedVisibilities": ["PUBLIC", "PRIVATE", "PROTECTED", "INTERNAL", "PACKAGE"]
        }
      ]
    }
  ],
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "./kotlinx-html-jvm-0.8.0.jar",
    "./dokka-analysis-%dokkaVersion%.jar",
    "./kotlin-analysis-intellij-%dokkaVersion%.jar",
    "./kotlin-analysis-compiler-%dokkaVersion%.jar",
    "./freemarker-2.3.31.jar"
  ],
  "pluginsConfiguration": [
    {
      "fqPluginName": "org.jetbrains.dokka.base.DokkaBase",
      "serializationFormat": "JSON",
      "values": "{\"separateInheritedMembers\":false,\"footerMessage\":\"© 2021 pretty good Copyright\"}"
    }
  ],
  "includes": [
    "module.md"
  ]
}
```
