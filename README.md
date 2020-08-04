# dokka  [![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![TeamCity (build status)](https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:Kotlin_Dokka_DokkaAntMavenGradle)/statusIcon)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_Dokka_DokkaAntMavenGradle&branch_KotlinTools_Dokka=%3Cdefault%3E&tab=buildTypeStatusDiv) 

Dokka is a documentation engine for Kotlin, performing the same function as javadoc for Java.
Just like Kotlin itself, Dokka fully supports mixed-language Java/Kotlin projects. It understands
standard Javadoc comments in Java files and [KDoc comments](https://kotlinlang.org/docs/reference/kotlin-doc.html) in Kotlin files,
and can generate documentation in multiple formats including standard Javadoc, HTML and Markdown.

## Using dokka

### Plugins 
Dokka can be customized with plugins. Each output format is internally a plugin.
Additionally, `kotlin-as-java` plugin can be used to generate documentation as seen from Java perspective. 
Currently maintained plugins are:
* `dokka-base` - the main plugin needed to run dokka, contains html format
* `gfm-plugin` - configures `GFM` output format
* `jekyll-plugin` - configures `Jekyll` output format
* `javadoc-plugin` - configures `Javadoc` output format, automatically applies `kotlin-as-java-plugin` 
* `kotlin-as-java-plugin` - translates Kotlin definitions to Java 

Please see the usage instructions below for how to add plugins to dokka. 

### Source sets 
Dokka generates documentation based on source sets. 

For single-platform projects, there is almost always only one source set - `main`.

For multi-platform projects, source sets are the same as in Kotlin plugin:

 * One source set for each platform, eg. `jvmMain` or `jsMain`;
 * One source set for each common source set, eg. the default `commonMain` and custom ones like `jsAndJvmMain`.

When configuring multi-platform projects manually (eg. in the CLI or in Gradle without autoconfiguration)
source sets must declare their dependent source sets. 
Eg. in the following Kotlin plugin configuration:

* `jsMain` and `jvmMain` both depend on `commonMain` (by default and transitively) and `jsAndJvmMain`;
* `linuxX64Main` only depends on `commonMain`. 

```kotlin
kotlin { // Kotlin plugin configuration
    jvm()
    js()
    linuxX64()

    sourceSets {
        val commonMain by getting {}
        val jvmAndJsSecondCommonMain by creating { dependsOn(commonMain) }
        val jvmMain by getting { dependsOn(jvmAndJsSecondCommonMain) }
        val jsMain by getting { dependsOn(jvmAndJsSecondCommonMain) }
        val linuxX64Main by getting { dependsOn(commonMain) }
    }
}
```

### Using the Gradle plugin

The preferred way is to use `plugins` block. Since Kotlin compiler used by dokka is still in EAP, 
you not only need to add `dokka` to the `build.gradle.kts` file, but you also need to modify the `settings.gradle.kts` file: 
 
build.gradle.kts:
```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.4.0-rc"
}

repositories {
    jcenter() // or maven(url="https://dl.bintray.com/kotlin/dokka")
}
```

settings.gradle.kts:
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
    }
}
```

You can also use the legacy plugin application method with `buildscript` block.
Note that by using the `buildscript` way type-safe accessors are not available in Gradle Kotlin DSL,
eg. you'll have to use `named<DokkaTask>("dokkaHtml")` instead of `dokkaHtml`:

```kotlin
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}")
    }
}
repositories {
    jcenter() // or maven(url="https://dl.bintray.com/kotlin/dokka")
}

apply(plugin="org.jetbrains.dokka")
```

The plugin adds `dokkaHtml`, `dokkaJavadoc`, `dokkaGfm` and `dokkaJekyll` tasks to the project.
 
Each task corresponds to one output format, so you should run `dokkaGfm` when you want to have a documentation in `GFM` format.
Output formats are explained [there](#output_formats)

If you encounter any problems when migrating from older versions of dokka, please see the [FAQ](https://github.com/Kotlin/dokka/wiki/faq).

Minimal dokka configuration:

Kotlin
(single-platform project)
```kotlin
tasks.dokkaHtml {
    outputDirectory = "$buildDir/dokka"
}
```
(mutli-platform project)
```kotlin
tasks.dokkaHtml {
    outputDirectory = "$buildDir/dokka"
    dokkaSourceSets {
        create("jvmMain")
        create("jsMain") // or other names, identical to those in Kotlin-plugin
    }
}
```

Groovy
(single-platform project)
```kotlin
dokkaHtml {
    outputDirectory = "$buildDir/dokka"
}
```
(mutli-platform project)
```kotlin
dokkaHtml {
    outputDirectory = "$buildDir/dokka"
    dokkaSourceSets {
        create("jvmMain") {}
        create("jsMain") {} // or other names, identical to those in Kotlin-plugin
    }
}
```

Dokka documents single-platform as well as multi-platform projects. 
Most of the configuration options are set per one source set.
The available configuration options for are shown below:

```kotlin
dokkaHtml {
    outputDirectory = "$buildDir/docs"

    // Used for disabling auto extraction of sources and platforms
    // When set to true kotlinTasks are also omitted
    disableAutoconfiguration = false

    // Use default or set to custom path to cache directory
    // to enable package-list caching
    // When this is set to default, caches are stored in $USER_HOME/.cache/dokka
    cacheRoot = "default"
    dokkaSourceSets {
        configureEach { // Or source set name, for single-platform the default source sets are `main` and `test`
            moduleDisplayName = "data"

            // Used when configuring source sets manually for declaring which source sets this one depends on
            dependsOn("otherSourceSetName")

            // Use to include or exclude non public members
            includeNonPublic = false

            // Do not output deprecated members. Applies globally, can be overridden by packageOptions
            skipDeprecated = false

            // Emit warnings about not documented members. Applies globally, also can be overridden by packageOptions
            reportUndocumented = true

            // Do not create index pages for empty packages
            skipEmptyPackages = true

            // This name will be shown in the final output
            displayName = "JVM"

            // Platform used for code analysis. See the "Platforms" section of this readme
            platform = "JVM"

            // Property used for manual addition of files to the classpath
            // This property does not override the classpath collected automatically but appends to it
            classpath = listOf("$buildDir/other.jar")

            // List of files with module and package documentation
            // https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation
            includes = listOf("packages.md", "extra.md")

            // List of files or directories containing sample code (referenced with @sample tags)
            samples = listOf("samples/basic.kt", "samples/advanced.kt")

            // By default, sourceRoots are taken from Kotlin Plugin and kotlinTasks, following roots will be appended to them
            // Repeat for multiple sourceRoots
            sourceRoot {
                // Path to a source root
                path = "src"
            }

            // These tasks will be used to determine source directories and classpath
            kotlinTasks {
                defaultKotlinTasks() + listOf(
                    ":some:otherCompileKotlin",
                    project("another").tasks.getByName("compileKotlin")
                )
            }

            // Specifies the location of the project source code on the Web.
            // If provided, Dokka generates "source" links for each declaration.
            // Repeat for multiple mappings
            sourceLink {
                // Unix based directory relative path to the root of the project (where you execute gradle respectively). 
                path = "src/main/kotlin" // or simply "./"

                // URL showing where the source code can be accessed through the web browser
                url =
                    "https://github.com/cy6erGn0m/vertx3-lang-kotlin/blob/master/src/main/kotlin" //remove src/main/kotlin if you use "./" above

                // Suffix which is used to append the line number to the URL. Use #L for GitHub
                lineSuffix = "#L"
            }

            // Used for linking to JDK documentation
            jdkVersion = 8

            // Disable linking to online kotlin-stdlib documentation
            noStdlibLink = false

            // Disable linking to online JDK documentation
            noJdkLink = false

            // Disable linking to online Android documentation (only applicable for Android projects)
            noAndroidSdkLink = false

            // Allows linking to documentation of the project"s dependencies (generated with Javadoc or Dokka)
            // Repeat for multiple links
            externalDocumentationLink {
                // Root URL of the generated documentation to link with. The trailing slash is required!
                url = URL("https://example.com/docs/")

                // If package-list file is located in non-standard location
                // packageListUrl = URL("file:///home/user/localdocs/package-list")
            }

            // Allows to customize documentation generation options on a per-package basis
            // Repeat for multiple packageOptions
            perPackageOption {
                prefix = "kotlin" // will match kotlin and all sub-packages of it
                // All options are optional, default values are below:
                skipDeprecated = false
                reportUndocumented = true // Emit warnings about not documented members 
                includeNonPublic = false
            }
            // Suppress a package
            perPackageOption {
                prefix = "kotlin.internal" // will match kotlin.internal and all sub-packages of it
                suppress = true
            }
        }
    }
```

#### Multiplatform
Dokka supports single-platform and multi-platform projects using source sets abstraction. For most mutli-platform projects
you should assume that dokka's source sets correspond to Kotlin plugin's source sets. 
Source sets can be named arbitrarily, however in order for autoconfiguration (extraction of source roots and classpath from Kotlin plugin) to work, 
they must have the same names as source sets in the Kotlin Multiplatform plugin.
See an example below:

Kotlin
```kotlin
kotlin {  // Kotlin Multiplatform plugin configuration
    jvm()
    js("customName")
}

dokkaHtml {
        outputDirectory = "$buildDir/dokka"

        dokkaSourceSets { 
            val customNameMain by creating { // The same name as in Kotlin Multiplatform plugin, so the sources are fetched automatically
                includes = listOf("packages.md", "extra.md")
                samples = listOf("samples/basic.kt", "samples/advanced.kt")
            }

            register("differentName") { // Different name, so source roots must be passed explicitly
                displayName = "JVM"
                platform = "jvm"
                sourceRoot {
                    path = kotlin.sourceSets.getByName("jvmMain").kotlin.srcDirs.first().toString()
                }
                sourceRoot {
                    path = kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs.first().toString()
                }
            }
        }
    }
```

Groovy
```groovy
kotlin { // Kotlin Multiplatform plugin configuration
    jvm() 
    js("customName") // Define a js platform named "customName" If you want to generate docs for it, you need to have this name followed by "Main" in the dokka configuration below 
    
    // Note: Kotlin plugin creates `main` and `test` source sets for the platforms above automatically, eg. in this project there will be:
    // `jvmMain`, `jvmTest`, `customNameMain` and `customNameTest`  
    // Those names can be used in the dokka tasks, as shown below:
}

dokkaHtml {
    outputDirectory = "$buildDir/dokka"

    dokkaSourceSets {
        customNameMain { // The same name as Kotlin Multiplatform plugin source set for `customName` platform, so the sources are fetched automatically
            includes = ['packages.md', 'extra.md']
            samples = ['samples/basic.kt', 'samples/advanced.kt']
        } 
        
        differentName { // Different name, so source roots, classpath and platform must be passed explicitly.
            displayName = "JVM"
            platform = "jvm"
            sourceRoot {
                path = kotlin.sourceSets.jvmMain.kotlin.srcDirs[0]
            }
            sourceRoot {
                path = kotlin.sourceSets.commonMain.kotlin.srcDirs[0]
            }
        }
    }
}
```

If you want to share the configuration between source sets, you can use Gradle's `configureEach`

#### Applying plugins
Dokka plugin creates Gradle configuration for each output format in the form of `dokka${format}Plugin`:

```kotlin
dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.0-rc")
}
``` 

You can also create a custom dokka task and add plugins directly inside:

```kotlin
val customDokkaTask by creating(DokkaTask::class) {
    dependencies {
        plugins("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.0-rc")
    }
}
```

Please note that `dokkaJavadoc` task will properly document only single `jvm` source set

To generate the documentation, use the appropriate `dokka${format}` Gradle task:

```bash
./gradlew dokkaHtml
```

Please see the [Dokka Gradle example project](https://github.com/JetBrains/kotlin-examples/tree/master/gradle/dokka-gradle-example) for an example.

#### FAQ
If you encounter any problems, please see the [FAQ](https://github.com/Kotlin/dokka/wiki/faq).

#### Android

Make sure you apply dokka after `com.android.library` and `kotlin-android`.

```kotlin
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlin_version}")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}")
    }
}
repositories {
    jcenter()
}
apply(plugin= "com.android.library")
apply(plugin= "kotlin-android")
apply(plugin= "org.jetbrains.dokka")
```

```kotlin
dokkaHtml {
    dokkaSourceSets {
        create("main") {
            noAndroidSdkLink = true
        }   
    }
}
```

#### Multi-module projects
For documenting Gradle multi-module projects, you can use `dokka${format}Multimodule` tasks.

```kotlin
tasks.dokkaHtmlMultimodule {
    outputDirectory = "$buildDir/multimodule"
    documentationFileName = "README.md"
}
```

`DokkaMultimodule` depends on all dokka tasks in the subprojects, runs them, and creates a toplevel page (based on the `documentationFile`)
with links to all generated (sub)documentations

### Using the Maven plugin

The Maven plugin does not support multi-platform projects.

The Maven plugin is available in JCenter. You need to add the JCenter repository to the list of plugin repositories if it's not there:

```xml
<pluginRepositories>
    <pluginRepository>
        <id>jcenter</id>
        <name>JCenter</name>
        <url>https://jcenter.bintray.com/</url>
    </pluginRepository>
</pluginRepositories>
```

Minimal Maven configuration is

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    <version>${dokka.version}</version>
    <executions>
        <execution>
            <phase>pre-site</phase>
            <goals>
                <goal>dokka</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

By default files will be generated in `target/dokka`.

The following goals are provided by the plugin:

  * `dokka:dokka` - generate HTML documentation in Dokka format (showing declarations in Kotlin syntax)
  * `dokka:javadoc` - generate HTML documentation in Javadoc format (showing declarations in Java syntax)
  * `dokka:javadocJar` - generate a .jar file with Javadoc format documentation

The available configuration options are shown below:

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    <version>${dokka.version}</version>
    <executions>
        <execution>
            <phase>pre-site</phase>
            <goals>
                <goal>dokka</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
    
        <!-- Set to true to skip dokka task, default: false -->
        <skip>false</skip>
    
        <!-- Default: ${project.artifactId} -->
        <moduleDisplayName>data</moduleDisplayName>

        <!-- Default: ${project.basedir}/target/dokka -->
        <outputDir>some/out/dir</outputDir>
        
        <!-- Use default or set to custom path to cache directory to enable package-list caching. -->
        <!-- When set to default, caches stored in $USER_HOME/.cache/dokka -->
        <cacheRoot>default</cacheRoot>

        <!-- List of '.md' files with package and module docs -->
        <!-- https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation -->
        <includes>
            <include>packages.md</include>
            <include>extra.md</include>
        </includes>
        
        <!-- List of sample roots -->
        <samples>
            <dir>src/test/samples</dir>
        </samples>
        
        <!-- Used for linking to JDK, default: 6 -->
        <jdkVersion>6</jdkVersion>

        <!-- Do not output deprecated members, applies globally, can be overridden by packageOptions -->
        <skipDeprecated>false</skipDeprecated> 
        <!-- Emit warnings about not documented members, applies globally, also can be overridden by packageOptions -->
        <reportUndocumented>true</reportUndocumented>             
        <!-- Do not create index pages for empty packages -->
        <skipEmptyPackages>true</skipEmptyPackages> 
        
        <!-- Short form list of sourceRoots, by default, set to ${project.compileSourceRoots} -->
        <sourceDirectories>
            <dir>src/main/kotlin</dir>
        </sourceDirectories>
        
        <!-- Full form list of sourceRoots -->
        <sourceRoots>
            <root>
                <path>src/main/kotlin</path>
                <!-- See platforms section of documentation -->
                <platforms>JVM</platforms>
            </root>
        </sourceRoots>
        
        <!-- Specifies the location of the project source code on the Web. If provided, Dokka generates "source" links
             for each declaration. -->
        <sourceLinks>
            <link>
                <!-- Source directory -->
                <path>${project.basedir}/src/main/kotlin</path>
                <!-- URL showing where the source code can be accessed through the web browser -->
                <url>https://github.com/cy6erGn0m/vertx3-lang-kotlin/blob/master/src/main/kotlin</url> <!-- //remove src/main/kotlin if you use "./" above -->
                <!--Suffix which is used to append the line number to the URL. Use #L for GitHub -->
                <lineSuffix>#L</lineSuffix>
            </link>
        </sourceLinks>
        
        <!-- Disable linking to online kotlin-stdlib documentation  -->
        <noStdlibLink>false</noStdlibLink>
        
        <!-- Disable linking to online JDK documentation -->
        <noJdkLink>false</noJdkLink>
        
        <!-- Allows linking to documentation of the project's dependencies (generated with Javadoc or Dokka) -->
        <externalDocumentationLinks>
            <link>
                <!-- Root URL of the generated documentation to link with. The trailing slash is required! -->
                <url>https://example.com/docs/</url>
                <!-- If package-list file located in non-standard location -->
                <!-- <packageListUrl>file:///home/user/localdocs/package-list</packageListUrl> -->
            </link>
        </externalDocumentationLinks>

        <!-- Allows to customize documentation generation options on a per-package basis -->
        <perPackageOptions>
            <packageOptions>
                <!-- Will match kotlin and all sub-packages of it -->
                <prefix>kotlin</prefix>
                
                <!-- All options are optional, default values are below: -->
                <skipDeprecated>false</skipDeprecated>
                <!-- Emit warnings about not documented members  -->
                <reportUndocumented>true</reportUndocumented>
                <includeNonPublic>false</includeNonPublic>
            </packageOptions>
        </perPackageOptions>
        
        <!-- Allows to use any dokka plugin, eg. GFM format   -->
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>gfm-plugin</artifactId>
                <version>${dokka.version}</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

#### Applying plugins
You can add plugins inside the `dokkaPlugins` block:

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    <version>${dokka.version}</version>
    <executions>
        <execution>
            <phase>pre-site</phase>
            <goals>
                <goal>dokka</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>kotlin-as-java-plugin</artifactId>
                <version>${dokka.version}</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

Please see the [Dokka Maven example project](https://github.com/JetBrains/kotlin-examples/tree/master/maven/dokka-maven-example) for an example.

### Using the Command Line

To run Dokka from the command line, download the [Dokka CLI runner](https://github.com/Kotlin/dokka/releases/download/1.4.0-rc/dokka-cli-1.4.0-rc.jar).
To generate documentation, run the following command:
```
java -jar dokka-cli-1.4.0-rc.jar <arguments>
```
Dokka supports the following command line arguments:

  * `-outputDir` - the output directory where the documentation is generated
  * `-cacheRoot` - cache directory to enable package-list caching
  * `-pluginsClasspath` - artifacts with dokka plugins, separated by `;`. At least dokka base and all its dependencies must be added there 
  * `-offlineMode` - do not resolve package-lists online
  * `-failOnWarning` - throw an exception instead of a warning
  * `-globalPackageOptions` - per package options added to all source sets
  * `-globalLinks` - external documentation links added to all source sets
  * `-globalSrcLink` - source links added to all source sets
  * `-sourceSet` - (repeatable) - configuration for a single source set. Following this argument, you can pass other arguments:
    * `-moduleName` - (required) - module name used as a part of source set ID when declaring dependent source sets
    * `-moduleDisplayName` - displayed module name
    * `-sourceSetName` - source set name as a part of source set ID when declaring dependent source sets
    * `-displayName` - source set name displayed in the generated documentation
    * `-src` - list of source files or directories separated by `;`
    * `-classpath` - list of directories or .jar files to include in the classpath (used for resolving references) separated by `;`
    * `-samples` - list of directories containing sample code (documentation for those directories is not generated but declarations from them can be referenced using the `@sample` tag) separated by `;`
    * `-includes` - list of files containing the documentation for the module and individual packages separated by `;`
    * `-includeNonPublic` - include protected and private code   
    * `-skipDeprecated` - if set, deprecated elements are not included in the generated documentation
    * `-reportUndocumented` - warn about undocumented members
    * `-skipEmptyPackages` - do not create index pages for empty packages
    * `-packageOptions` - list of package options in format `prefix,-deprecated,-privateApi,+reportUndocumented;prefix, ...`, separated by `;`
    * `-links` - list of external documentation links in format `url^packageListUrl^^url2...`, separated by `;`
    * `-srcLink` - mapping between a source directory and a Web site for browsing the code in format `<path>=<url>[#lineSuffix]`
    * `-noStdlibLink` - disable linking to online kotlin-stdlib documentation
    * `-noJdkLink` - disable linking to online JDK documentation
    * `-jdkVersion` - version of JDK to use for linking to JDK JavaDoc
    * `-analysisPlatform` - platform used for analysis, see the [Platforms](#platforms) section
    * `-dependentSourceSets` - list of dependent source sets in format `moduleName/sourceSetName`, separated by `;`

You can also use a JSON file with dokka configuration:
 ```
 java -jar <dokka_cli.jar> <path_to_config.json>
 ```

### Output formats<a name="output_formats"></a>
  Dokka documents Java classes as seen in Kotlin by default, with javadoc format being the only exception.

  * `html` - HTML format used by default
  * `javadoc` - looks like JDK's Javadoc, Kotlin classes are translated to Java
  * `gfm` - GitHub flavored markdown
  * `jekyll` - Jekyll compatible markdown

If you want to generate the documentation as seen from Java perspective, you can add the `kotlin-as-java` plugin
to the dokka plugins classpath, eg. in Gradle:

```kotlin
dependencies{
    implementation("...")
    dokkaGfmPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${dokka-version}")
}
```

### Platforms<a name="platforms"></a>

Each dokka source set is analyzed for a specific platform. The platform should be extracted automatically from the Kotlin plugin.
In case of a manual source set configuration, you have to select one of the following:
  * `jvm`
  * `js`
  * `native`
  * `common` 

## Building dokka

Dokka is built with Gradle. To build it, use `./gradlew build`.
Alternatively, open the project directory in IntelliJ IDEA and use the IDE to build and run dokka.

Here's how to import and configure Dokka in IntelliJ IDEA 2019.3:
 * Select "Open" from the IDEA welcome screen, or File > Open if a project is
  already open
* Select the directory with your clone of Dokka
  * Note: IDEA may have an error after the project is initally opened; it is OK
    to ignore this as the next step will address this error
* After IDEA opens the project, select File > New > Module from existing sources
  and select the `build.gradle` file from the root directory of your Dokka clone
* After Dokka is loaded into IDEA, open the Gradle tool window (View > Tool
  Windows > Gradle) and click on the top left "Refresh all Gradle projects"
  button
