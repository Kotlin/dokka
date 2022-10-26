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

See [all command line arguments](#command-line-arguments) for more details.

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
        "scopeId": "html",
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

By default, `dokka-base` artifact contains [HTML](html.md) format only. 

All other output formats come as [Dokka plugins](plugins_introduction.md). In order to use them, you have to put it
on plugins classpath.

For example, if you want to generate documentation in [GFM](markdown.md#gfm) format, you have to download and
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

## Configuration

### Command line arguments

TODO paste output of `-help` after correcting in-code descriptions. 

### JSON configuration

TODO add descriptions for each setting as a separate entry in a table
TODO add a link to Dokka's api reference, to DokkaConfiguration class

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
        "scopeId": "html",
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
  ],
  "finalizeCoroutines": true
}
```
