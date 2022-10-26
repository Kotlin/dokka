[//]: # (title: Versioning plugin)

Versioning plugin aims to provide the ability to host documentation for multiple versions of your library/application
with seamless switching between them. This, in turn, provides better experience for your users.

![Screenshot of documentation version dropdown](versioning-plugin-example.png){height=350}

> Versioning plugin only works with [HTML](html.md) format.
> 
{type="note"}

## Applying the plugin

You can apply Versioning plugin the same way as other [Dokka plugins](plugins_introduction.md#applying-dokka-plugins):

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:versioning-plugin:%dokkaVersion%")
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
dependencies {
    dokkaHtmlPlugin 'org.jetbrains.dokka:versioning-plugin:%dokkaVersion%'
}
```

</tab>
<tab title="Maven" group-key="mvn">

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>versioning-plugin</artifactId>
                <version>%dokkaVersion%</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

</tab>
<tab title="CLI" group-key="cli">

You can find versioning plugin's artifact on 
[mvnrepository](https://mvnrepository.com/artifact/org.jetbrains.dokka/versioning-plugin/%dokkaVersion%) or by browsing
[maven central repository](https://repo1.maven.org/maven2/org/jetbrains/dokka/versioning-plugin/%dokkaVersion%/) 
directly, and pass it to `pliginsClasspath`.

Via [command line arguments](cli.md#running-with-command-line-arguments):

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     -pluginsClasspath "./dokka-base-%dokkaVersion%.jar;...;./versioning-plugin-%dokkaVersion%.jar" \
     ...
```

Via [JSON configuration](cli.md#running-with-json-configuration):

```json
{
  ...
  "pluginsClasspath": [
    "./dokka-base-%dokkaVersion%.jar",
    "...",
    "./versioning-plugin-%dokkaVersion%.jar"
  ],
  ...
}
```

</tab>
</tabs>

## Configuration

### Configuration options

Versioning plugin has a number of optional configuration properties:

| **Task**                             | **Description**                                                                                                                                                                               |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `version`                            | Version of your application/library for which documentation is going to be generated. This will be the version in the dropdown menu.                                                          |
| `versionsOrdering`                   | Optional list of strings that represents the order in which versions should appear in the dropdown. Must match version string exactly. First item of the list is the topmost in the dropdown. |
| `olderVersionsDir`                   | Optional path to a parent folder that contains other documentation versions. Requires a certain [directory structure](#directory-structure).                                                  |
| `olderVersions`                      | Optional list of paths to other documentation versions. Must point to Dokka's outputs directly. Useful if different versions are scattered and cannot be put into a single directory.         |
| `renderVersionsNavigationOnAllPages` | Optional boolean indicating whether to render navigation dropdown on all pages. True by default.                                                                                              |

#### Directory structure

Note that the directory passed to `olderVersionsDir` requires a specific structure:

```text
.
└── olderVersionsDir
    └── 1.7.10
        ├── <dokka output>
    └── 1.7.20
        ├── <dokka output>
...
```

### Configuration example

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.dokka.versioning.VersioningPlugin
import org.jetbrains.dokka.versioning.VersioningConfiguration

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:%dokkaVersion%")
    }
}

tasks.dokkaHtml {
    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        version = "1.5"
        versionsOrdering = listOf("1.5", "1.4", "1.3", "1.2", "1.1", "alpha-2", "alpha-1")
        olderVersionsDir = file("documentation/version")
        olderVersions = listOf(file("documentation/alpha/alpha-2"), file("documentation/alpha/alpha-1"))
        renderVersionsNavigationOnAllPages = true
    }
}
```

Alternatively, you can configure it via JSON:

```kotlin
    val versioningConfiguration = """
    {
      "version": "1.5",
      "versionsOrdering": ["1.5", "1.4", "1.3", "1.2", "1.1", "alpha-2", "alpha-1"],
      "olderVersionsDir": "documentation/version",
      "olderVersions": ["documentation/alpha/alpha-2", "documentation/alpha/alpha-1"],
      "renderVersionsNavigationOnAllPages": true
    }
    """
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.versioning.VersioningPlugin" to versioningConfiguration
        )
    )
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
dokkaHtml {
    String versioningConfiguration = """
    {
      "version": "1.5",
      "versionsOrdering": ["1.5", "1.4", "1.3", "1.2", "1.1", "alpha-2", "alpha-1"],
      "olderVersionsDir": "documentation/version",
      "olderVersions": ["documentation/alpha/alpha-2", "documentation/alpha/alpha-1"],
      "renderVersionsNavigationOnAllPages": true
    }
    """
    pluginsMapConfiguration.set(
            ["org.jetbrains.dokka.versioning.VersioningPlugin": versioningConfiguration]
    )
}
```

</tab>
<tab title="Maven" group-key="mvn">

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <pluginsConfiguration>
            <org.jetbrains.dokka.versioning.VersioningPlugin>
                <version>1.5</version>
                <versionsOrdering>
                    <version>1.5</version>
                    <version>1.4</version>
                    <version>1.3</version>
                    <version>1.2</version>
                    <version>1.1</version>
                    <version>alpha-2</version>
                    <version>alpha-1</version>
                </versionsOrdering>
                <olderVersionsDir>${project.basedir}/documentation/version</olderVersionsDir>
                <olderVersions>
                    <version>${project.basedir}/documentation/alpha/alpha-2</version>
                    <version>${project.basedir}/documentation/alpha/alpha-1</version>
                </olderVersions>
                <renderVersionsNavigationOnAllPages>true</renderVersionsNavigationOnAllPages>
            </org.jetbrains.dokka.versioning.VersioningPlugin>
        </pluginsConfiguration>
    </configuration>
</plugin>
```

</tab>
<tab title="CLI" group-key="cli">

```Bash
java -jar dokka-cli-%dokkaVersion%.jar \
     ...
     -pluginsConfiguration "org.jetbrains.dokka.versioning.VersioningPlugin={\"version\": \"1.5\", \"versionsOrdering\": [\"1.5\", \"1.4\", \"1.3\", \"1.2\", \"1.1\", \"alpha-2\", \"alpha-1\"], \"olderVersionsDir\": \"documentation/version\", \"olderVersions\": [\"documentation/alpha/alpha-2\", \"documentation/alpha/alpha-1\"], \"renderVersionsNavigationOnAllPages\": true}"

```

Alternatively, via JSON configuration:
```json
{
  "moduleName": "Dokka Example",
  ...
  "pluginsConfiguration": [
    {
      "fqPluginName": "org.jetbrains.dokka.versioning.VersioningPlugin",
      "serializationFormat": "JSON",
      "values": "{\"version\": \"1.5\", \"versionsOrdering\": [\"1.5\", \"1.4\", \"1.3\", \"1.2\", \"1.1\", \"alpha-2\", \"alpha-1\"], \"olderVersionsDir\": \"documentation/version\", \"olderVersions\": [\"documentation/alpha/alpha-2\", \"documentation/alpha/alpha-1\"], \"renderVersionsNavigationOnAllPages\": true}"
    }
  ]
}
```

</tab>
</tabs>

## Generating versioned documentation

With versioning plugin applied and configured, no other steps are needed: documentation can be built the usual way.

Versioning plugin will add a `version.json` file to the output folder. This file will be used by the plugin to match
versions and generate version navigation. If your previously generated documentation does not have that file, you
may need to re-generate it.

Versioning plugin will also bundle all other documentation versions that have been passed through `olderVersionsDir` 
and `olderVersions` configuration properties by putting them inside `older` directory.

## Usage example

There is no single correct way to configure the plugin, it can be tailored to your liking and needs. However,
it can be a bit overwhelming when starting out. Below you will find one of the ways it can be configured so that you
can begin publishing versioned documentation right away.

The main idea behind it is the following:

1. One directory will contains all versions of your documentation. For instance, `documentation/version/{version_string}`.
   This is your archive, you will need to preserve it for future builds.
2. Output directory of all new builds will be set to that directory as well, under `documentation/version/{new_version}`
3. When new builds are executed, the plugin will look for previous versions of documentation in the archive directory.
4. Once new documentation has been generated, it needs to be **copied** to some place accessible by the user: 
   GitHub pages, nginx static directories, and so on. It needs to be copied and not moved because Dokka will still need
   this version for future builds, otherwise there will be a gap in the archive.
5. Once it has been safely copied away, you can remove `older` directory from the archived version. This helps reduce
   the overhead of each version bundling all previous versions as these files are effectively duplicate.

```kotlin
import org.jetbrains.dokka.versioning.VersioningPlugin
import org.jetbrains.dokka.versioning.VersioningConfiguration

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:%dokkaVersion%")
    }
}

dependencies {
    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:$dokkaVersion")
}

tasks.dokkaHtml {
    // can be any persistent folder where
    // you store documentation by version
    val docVersionsDir = projectDir.resolve("documentation/version")

    // version for which you are currently generating docs
    val currentVersion = "1.3"
    
    // set output to folder with all other versions
    // as you'll need current version for future builds
    val currentDocsDir = docVersionsDir.resolve(currentVersion)
    outputDirectory.set(currentDocsDir)

    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
        olderVersionsDir = docVersionsDir
        version = currentVersion
    }

    doLast {
        // this folder will contain latest documentation with all
        // previous versions included, so it's ready to be published.
        // make sure it's copied and not moved - you'll still need this
        // version for future builds
        currentDocsDir.copyTo(file("/my/hosting"))
       
        // only once current documentation has been safely moved,
        // remove previous versions bundled in it. They will not
        // be needed in future builds, it's just overhead
        currentDocsDir.resolve("older").deleteRecursively()
    }
}
```
