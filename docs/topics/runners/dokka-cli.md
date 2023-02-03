[//]: # (title: CLI)

If for some reason you cannot use [Gradle](dokka-gradle.md) or [Maven](dokka-maven.md) build tools, Dokka has
a command line (CLI) runner for generating documentation.

In comparison, it has the same, if not more, capabilities as the Gradle plugin for Dokka. Although it is considerably more
difficult to set up as there is no autoconfiguration, especially in multiplatform and multi-module environments.

## Get started

The CLI runner is published to Maven Central as a separate runnable artifact.

You can find it on [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-cli/%dokkaVersion%) or by browsing
[maven central repository directories](https://repo1.maven.org/maven2/org/jetbrains/dokka/dokka-cli/%dokkaVersion%) directly.

With the `dokka-cli-%dokkaVersion%.jar` file saved on your computer, run it with the `-help` option to see all 
available configuration options and their description:

```Bash
java -jar dokka-cli-%dokkaVersion%.jar -help
```

It also works for some nested options, such as `-sourceSet`:

```Bash
java -jar dokka-cli-%dokkaVersion%.jar -sourceSet -help
```

## Generate documentation

### Prerequisites

Since there is no build tool to manage dependencies, you have to provide dependency `.jar` files yourself.

Listed below are the dependencies that you need for any output format:

| **Group**             | **Artifact**               | **Version**    | **Link**                                                                                                        |
|-----------------------|----------------------------|----------------|-----------------------------------------------------------------------------------------------------------------|
| `org.jetbrains.dokka` | `dokka-base`               | %dokkaVersion% | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-base/%dokkaVersion%)               |
| `org.jetbrains.dokka` | `dokka-analysis`           | %dokkaVersion% | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-analysis/%dokkaVersion%)           |
| `org.jetbrains.dokka` | `kotlin-analysis-compiler` | %dokkaVersion% | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/kotlin-analysis-compiler/%dokkaVersion%) |
| `org.jetbrains.dokka` | `kotlin-analysis-intellij` | %dokkaVersion% | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/kotlin-analysis-intellij/%dokkaVersion%) |

Below are the additional dependencies that you need for [HTML](dokka-html.md) output format:

| **Group**               | **Artifact**       | **Version** | **Link**                                                                                         |
|-------------------------|--------------------|-------------|--------------------------------------------------------------------------------------------------|
| `org.jetbrains.kotlinx` | `kotlinx-html-jvm` | 0.8.0       | [mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-html-jvm/0.8.0) |
| `org.freemarker`        | `freemarker`       | 2.3.31      | [mvnrepository](https://mvnrepository.com/artifact/org.freemarker/freemarker/2.3.31)             |


### Run with command line options

You can pass command line options to configure the CLI runner. 

At the very least you need to provide the following options:

* `-pluginsClasspath` - a list of absolute/relative paths to downloaded dependencies, separated by semi-colons `;`
* `-sourceSet` - an absolute path to code sources to generate documentation for
* `-outputDir` - an absolute/relative path of the documentation output directory

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;./dokka-analysis-%dokkaVersion%.jar;./kotlin-analysis-intellij-%dokkaVersion%.jar;./kotlin-analysis-compiler-%dokkaVersion%.jar;./kotlinx-html-jvm-0.8.0.jar;./freemarker-2.3.31.jar" \
     -sourceSet "-src /home/myCoolProject/src/main/kotlin" \
     -outputDir "./dokka/html"
```

> Due to an internal class conflict, first pass `kotlin-analysis-intellij` and only then `kotlin-analysis-compiler`.
> Otherwise you may see obscure exceptions, such as `NoSuchFieldError`.
>
{type="note"}

Executing the given example generates documentation in [HTML](dokka-html.md) output format.

See [Command line options](#command-line-options) for more configuration details.

### Run with JSON configuration

It's possible to configure the CLI runner with JSON. In this case, you need to provide an
absolute/relative path to the JSON configuration file as the first and only argument. 
All other configuration options are parsed from it.

```Bash
java -jar dokka-cli-%dokkaVersion%.jar dokka-configuration.json
```

At the very least, you need the following JSON configuration file:

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

> Due to an internal class conflict, first pass `kotlin-analysis-intellij` and only then `kotlin-analysis-compiler`.
> Otherwise you may see obscure exceptions, such as `NoSuchFieldError`.
>
{type="note"}

See [JSON configuration options](#json-configuration) for more details.

### Other output formats

By default, the `dokka-base` artifact contains the [HTML](dokka-html.md) output format only.

All other output formats are implemented as [Dokka plugins](dokka-plugins.md). In order to use them, you have to put them
on the plugins classpath.

For example, if you want to generate documentation in the experimental [GFM](dokka-markdown.md#gfm) output format, you need to download and
pass [gfm-plugin's JAR](https://mvnrepository.com/artifact/org.jetbrains.dokka/gfm-plugin/%dokkaVersion%) into 
the `pluginsClasspath` configuration option.

Via command line options:

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

With the GFM plugin passed to `pluginsClasspath`, the CLI runner generates documentation in the GFM output format.

For more information, see [Markdown](dokka-markdown.md) and [Javadoc](dokka-javadoc.md#generate-javadoc-documentation) pages.

## Command line options

To see the list of all possible command line options and their detailed description, run:

```Bash
java -jar dokka-cli-%dokkaVersion%.jar -help
```

Short summary:

| Option                       | Description                                                                                                                                                                                           |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `moduleName`                 | Name of the project/module.                                                                                                                                                                           |
| `moduleVersion`              | Documented version.                                                                                                                                                                                   |
| `outputDir`                  | Output directory path, `./dokka` by default.                                                                                                                                                          |
| `sourceSet`                  | Configuration for a Dokka source set. Contains nested configuration options.                                                                                                                          |
| `pluginsConfiguration`       | Configuration for Dokka plugins.                                                                                                                                                                      |
| `pluginsClasspath`           | List of jars with Dokka plugins and their dependencies. Accepts multiple paths separated by semicolons.                                                                                               |
| `offlineMode`                | Whether to resolve remote files/links over network.                                                                                                                                                   |
| `failOnWarning`              | Whether to fail documentation generation if Dokka has emitted a warning or an error.                                                                                                                  |
| `delayTemplateSubstitution`  | Whether to delay substitution of some elements. Used in incremental builds of multi-module projects.                                                                                                  |
| `noSuppressObviousFunctions` | Whether to suppress obvious functions such as those inherited from `kotlin.Any` and `java.lang.Object`.                                                                                               |
| `includes`                   | Markdown files that contain module and package documentation. Accepts multiple values separated by semicolons.                                                                                        |
| `suppressInheritedMembers`   | Whether to suppress inherited members that aren't explicitly overridden in a given class.                                                                                                             |
| `globalPackageOptions`       | Global list of package configuration options in format `"matchingRegex,-deprecated,-privateApi,+warnUndocumented,+suppress;+visibility:PUBLIC;..."`. Accepts multiple values separated by semicolons. |
| `globalLinks`                | Global external documentation links in format `{url}^{packageListUrl}`. Accepts multiple values separated by `^^`.                                                                                    |
| `globalSrcLink`              | Global mapping between a source directory and a Web service for browsing the code. Accepts multiple paths separated by semicolons.                                                                    |
| `helpSourceSet`              | Prints help for the nested `-sourceSet` configuration.                                                                                                                                                |
| `loggingLevel`               | Logging level, possible values: `DEBUG, PROGRESS, INFO, WARN, ERROR`.                                                                                                                                 |
| `help, h`                    | Usage info.                                                                                                                                                                                           |

#### Source set options

To see the list of command line options for the nested `-sourceSet` configuration, run:

```Bash
java -jar dokka-cli-%dokkaVersion%.jar -sourceSet -help
```

Short summary:

| Option                       | Description                                                                                                                                                                    |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `sourceSetName`              | Name of the source set.                                                                                                                                                        |
| `displayName`                | Display name of the source set, used both internally and externally.                                                                                                           |
| `classpath`                  | Classpath for analysis and interactive samples. Accepts multiple paths separated by semicolons.                                                                                |
| `src`                        | Source code roots to be analyzed and documented. Accepts multiple paths separated by semicolons.                                                                               |
| `dependentSourceSets`        | Names of the dependent source sets in format `moduleName/sourceSetName`. Accepts multiple paths separated by semicolons.                                                       |
| `samples`                    | List of directories or files that contain sample functions. Accepts multiple paths separated by semicolons. <anchor name="includes-cli"/>                                      |
| `includes`                   | Markdown files that contain [module and package documentation](dokka-module-and-package-docs.md). Accepts multiple paths separated by semicolons.                              |
| `documentedVisibilities`     | Visibilities to be documented. Accepts multiple values separated by semicolons. Possible values: `PUBLIC`, `PRIVATE`, `PROTECTED`, `INTERNAL`, `PACKAGE`.                      |
| `reportUndocumented`         | Whether to report undocumented declarations.                                                                                                                                   | 
| `noSkipEmptyPackages`        | Whether to create pages for empty packages.                                                                                                                                    | 
| `skipDeprecated`             | Whether to skip deprecated declarations.                                                                                                                                       | 
| `jdkVersion`                 | Version of JDK to use for linking to JDK Javadocs.                                                                                                                             |
| `languageVersion`            | Language version used for setting up analysis and samples.                                                                                                                     |
| `apiVersion`                 | Kotlin API version used for setting up analysis and samples.                                                                                                                   |
| `noStdlibLink`               | Whether to generate links to the Kotlin standard library.                                                                                                                      | 
| `noJdkLink`                  | Whether to generate links to JDK Javadocs.                                                                                                                                     | 
| `suppressedFiles`            | Paths to files to be suppressed. Accepts multiple paths separated by semicolons.                                                                                               |
| `analysisPlatform`           | Platform used for setting up analysis.                                                                                                                                         |
| `perPackageOptions`          | List of package source set configurations in format `matchingRegexp,-deprecated,-privateApi,+warnUndocumented,+suppress;...`. Accepts multiple values separated by semicolons. |
| `externalDocumentationLinks` | External documentation links in format `{url}^{packageListUrl}`. Accepts multiple values separated by `^^`.                                                                    |
| `srcLink`                    | Mapping between a source directory and a Web service for browsing the code. Accepts multiple paths separated by semicolons.                                                    |

## JSON configuration

Below are some examples and detailed descriptions for each configuration section. You can also find an example
with [all configuration options](#complete-configuration) applied at the bottom of the page.

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

<deflist collapsible="true">
    <def title="moduleName">
        <p>The display name used to refer to the module. It is used for the table of contents, navigation, logging, etc.</p>
        <p>Default: <code>root</code></p>
    </def>
    <def title="moduleVersion">
        <p>The module version.</p>
        <p>Default: empty</p>
    </def>
    <def title="outputDirectory">
        <p>The directory to where documentation is generated, regardless of output format.</p>
        <p>Default: <code>./dokka</code></p>
    </def>
    <def title="failOnWarning">
        <p>
            Whether to fail documentation generation if Dokka has emitted a warning or an error.
            The process waits until all errors and warnings have been emitted first.
        </p>
        <p>This setting works well with <code>reportUndocumented</code></p>
        <p>Default: <code>false</code></p>
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
        <p>Default: <code>true</code></p>
    </def>
    <def title="suppressInheritedMembers">
        <p>Whether to suppress inherited members that aren't explicitly overridden in a given class.</p>
        <p>
            Note: This can suppress functions such as <code>equals</code> / <code>hashCode</code> / <code>toString</code>, 
            but cannot suppress synthetic functions such as <code>dataClass.componentN</code> and 
            <code>dataClass.copy</code>. Use <code>suppressObviousFunctions</code>
            for that.
        </p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="offlineMode">
        <anchor name="includes-json"/>
        <p>Whether to resolve remote files/links over your network.</p>
        <p>
            This includes package-lists used for generating external documentation links. 
            For example, to make classes from the standard library clickable. 
        </p>
        <p>
            Setting this to <code>true</code> can significantly speed up build times in certain cases,
            but can also worsen documentation quality and user experience. For example, by
            not resolving class/member links from your dependencies, including the standard library.
        </p>
        <p>
            Note: You can cache fetched files locally and provide them to
            Dokka as local paths. See <code>externalDocumentationLinks</code> section.
        </p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="includes">
        <p>
            A list of Markdown files that contain
            <a href="dokka-module-and-package-docs.md">module and package documentation</a>.
        </p>
        <p>The contents of specified files are parsed and embedded into documentation as module and package descriptions.</p>
        <p>This can be configured on per-package basis.</p>
    </def>
    <def title="sourceSets">
        <p>
          Individual and additional configuration of Kotlin  
          <a href="https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets">source sets</a>.
        </p>
        <p>For a list of possible options, see <a href="#source-set-configuration">source set configuration</a>.</p>
    </def>
    <def title="sourceLinks">
        <p>The global configuration of source links that is applied for all source sets.</p>
        <p>For a list of possible options, see <a href="#source-link-configuration">source link configuration</a>.</p>
    </def>
    <def title="perPackageOptions">
        <p>The global configuration of matched packages, regardless of the source set they are in.</p>
        <p>For a list of possible options, see <a href="#per-package-configuration">per-package configuration</a>.</p>
    </def>
    <def title="externalDocumentationLinks">
        <p>The global configuration of external documentation links, regardless of the source set they are used in.</p>
        <p>For a list of possible options, see <a href="#external-documentation-configuration">external documentation configuration</a>.</p>
    </def>
    <def title="pluginsClasspath">
        <p>A list of JAR files with Dokka plugins and their dependencies.</p>
    </def>
</deflist>

### Source set configuration

How to configure Kotlin
[source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets):

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

<deflist collapsible="true">
    <def title="displayName">
        <p>The display name used to refer to this source set.</p>
        <p>
            The name is used both externally (for example, the source set name is visible to documentation readers) and 
            internally (for example, for logging messages of <code>reportUndocumented</code>).
        </p>
        <p>The platform name can be used if you don't have a better alternative.</p>
    </def>
    <def title="sourceSetID">
        <p>The technical ID of the source set</p>
    </def>
    <def title="documentedVisibilities">
        <p>The set of visibility modifiers that should be documented.</p>
        <p>
            This can be used if you want to document protected/internal/private declarations,
            as well as if you want to exclude public declarations and only document internal API.
        </p>
        <p>This can be configured on per-package basis.</p>
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
        <p>Default: <code>PUBLIC</code></p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
            after they have been filtered by <code>documentedVisibilities</code> and other filters.
        </p>
        <p>This setting works well with <code>failOnWarning</code>.</p>
        <p>This can be configured on per-package basis.</p>
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
        <p>Default for CLI runner is <code>false</code>.</p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>This can be configured on per-package basis.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="jdkVersion">
        <p>The JDK version to use when generating external documentation links for Java types.</p>
        <p>
            For example, if you use <code>java.util.UUID</code> in some public declaration signature,
            and this option is set to <code>8</code>, Dokka generates an external documentation link
            to <a href="https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html">JDK 8 Javadocs</a> for it.
        </p>
    </def>
    <def title="languageVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">The Kotlin language version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
    </def>
    <def title="apiVersion">
        <p>
            <a href="https://kotlinlang.org/docs/compatibility-modes.html">The Kotlin API version</a>
            used for setting up analysis and <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a>
            environment.
        </p>
    </def>
    <def title="noStdlibLink">
        <p>
            Whether to generate external documentation links that lead to the API reference
            documentation of Kotlin's standard library.
        </p>
        <p>Note: Links <b>are</b> generated when <code>noStdLibLink</code> is set to <code>false</code>.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="noJdkLink">
        <p>Whether to generate external documentation links to JDK's Javadocs.</p>
        <p>The version of JDK Javadocs is determined by the <code>jdkVersion</code> option.</p>
        <p>Note: Links <b>are</b> generated when <code>noJdkLink</code> is set to <code>false</code>.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="includes">
        <p>
            A list of Markdown files that contain
            <a href="dokka-module-and-package-docs.md">module and package documentation</a>.
        </p>
        <p>The contents of the specified files are parsed and embedded into documentation as module and package descriptions.</p>
    </def>
    <def title="analysisPlatform">
        <p>
            Platform to be used for setting up code analysis and 
            <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a> environment.
        </p>
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
            The source code roots to be analyzed and documented.
            Acceptable inputs are directories and individual <code>.kt</code> / <code>.java</code> files.
        </p>
    </def>
    <def title="classpath">
        <p>The classpath for analysis and interactive samples.</p>
        <p>This is useful if some types that come from dependencies are not resolved/picked up automatically.</p>
        <p>This option accepts both <code>.jar</code> and <code>.klib</code> files.</p>
    </def>
    <def title="samples">
        <p>
            A list of directories or files that contain sample functions which are referenced via the
            <a href="https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier">@sample</a> KDoc tag.
        </p>
    </def>
    <def title="suppressedFiles">
        <p>The files to be suppressed when generating documentation.</p>
    </def>
    <def title="sourceLinks">
        <p>A set of parameters for source links that is applied only for this source set.</p>
        <p>For a list of possible options, see <a href="#source-link-configuration">source link configuration</a>.</p>
    </def>
    <def title="perPackageOptions">
        <p>A set of parameters specific to matched packages within this source set.</p>
        <p>For a list of possible options, see <a href="#per-package-configuration">per-package configuration</a>.</p>
    </def>
    <def title="externalDocumentationLinks">
        <p>A set of parameters for external documentation links that is applied only for this source set.</p>
        <p>For a list of possible options, see <a href="#external-documentation-configuration">external documentation configuration</a>.</p>
    </def>
</deflist>

### Source link configuration

The `sourceLinks` configuration block allows you to add a `source` link to each signature
that leads to the `remoteUrl` with a specific line number. (The line number is configurable by setting `remoteLineSuffix`).

This helps readers to find the source code for each declaration.

For an example, see the documentation for the
[`count()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/count.html)
function in `kotlinx.coroutines`.

You can configure source links for all source sets together at the same time, or 
[individually](#source-set-configuration):

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

<deflist collapsible="true">
    <def title="localDirectory">
        <p>The path to the local source directory.</p>
    </def>
    <def title="remoteUrl">
        <p>
            The URL of the source code hosting service that can be accessed by documentation readers,
            like GitHub, GitLab, Bitbucket, etc. This URL is used to generate
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
            Suffixes used by popular services:
            <list>
                <li>GitHub: <code>#L</code></li>
                <li>GitLab: <code>#L</code></li>
                <li>Bitbucket: <code>#lines-</code></li>
            </list>
        </p>
        <p>Default: empty (no suffix)</p>
    </def>
</deflist>

### Per-package configuration

The `perPackageOptions` configuration block allows setting some options for specific packages matched by `matchingRegex`.

You can add package configurations for all source sets together at the same time, or 
[individually](#source-set-configuration):

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

<deflist collapsible="true">
    <def title="matchingRegex">
        <p>The regular expression that is used to match the package.</p>
    </def>
    <def title="suppress">
        <p>Whether this package should be skipped when generating documentation.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="skipDeprecated">
        <p>Whether to document declarations annotated with <code>@Deprecated</code>.</p>
        <p>This can be set on project/module level.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="reportUndocumented">
        <p>
            Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
            after they have been filtered by <code>documentedVisibilities</code> and other filters.
        </p>
        <p>This setting works well with <code>failOnWarning</code>.</p>
        <p>This can be configured on source set level.</p>
        <p>Default: <code>false</code></p>
    </def>
    <def title="documentedVisibilities">
        <p>The set of visibility modifiers that should be documented.</p>
        <p>
            This can be used if you want to document protected/internal/private declarations within this package,
            as well as if you want to exclude public declarations and only document internal API.
        </p>
        <p>Can be configured on source set level.</p>
        <p>Default: <code>PUBLIC</code></p>
    </def>
</deflist>

### External documentation configuration

The `externalDocumentationLink` block allows the creation of links that lead to the externally hosted documentation of
your dependencies.

For example, if you are using types from `kotlinx.serialization`, by default they are unclickable in your
documentation, as if they are unresolved. However, since the API reference documentation for `kotlinx.serialization`
is built by Dokka and is [published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/), you can
configure external documentation links for it. Thus allowing Dokka to generate links for types from the library, making
them resolve successfully and clickable.

You can configure external documentation links for all source sets together at the same time, or 
[individually](#source-set-configuration):

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

<deflist collapsible="true">
    <def title="url">
        <p>The root URL of documentation to link to. It <b>must</b> contain a trailing slash.</p>
        <p>
            Dokka does its best to automatically find <code>package-list</code> for the given URL, 
            and link declarations together.
        </p>
        <p>
            If automatic resolution fails or if you want to use locally cached files instead, 
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
            such as module and package names.
        </p>
        <p>This can also be a locally cached file to avoid network calls.</p>
    </def>
</deflist>

### Complete configuration

Below you can see all possible configuration options applied at the same time.

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
