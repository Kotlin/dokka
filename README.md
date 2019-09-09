# dokka  [![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![TeamCity (build status)](https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:Kotlin_Dokka_DokkaAntMavenGradle)/statusIcon)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_Dokka_DokkaAntMavenGradle&branch_KotlinTools_Dokka=%3Cdefault%3E&tab=buildTypeStatusDiv) [![Download](https://api.bintray.com/packages/kotlin/dokka/dokka/images/download.svg)](https://bintray.com/kotlin/dokka/dokka/_latestVersion)

Dokka is a documentation engine for Kotlin, performing the same function as javadoc for Java.
Just like Kotlin itself, Dokka fully supports mixed-language Java/Kotlin projects. It understands
standard Javadoc comments in Java files and [KDoc comments](https://kotlinlang.org/docs/reference/kotlin-doc.html) in Kotlin files,
and can generate documentation in multiple formats including standard Javadoc, HTML and Markdown.

## Using dokka

### Using the Gradle plugin

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}"
    }
}
repositories {
    jcenter() // or maven { url 'https://dl.bintray.com/kotlin/dokka' }
}

apply plugin: 'org.jetbrains.dokka'
```

or using the plugins block:

```groovy
plugins {
    id 'org.jetbrains.dokka' version '0.10.0'
}
repositories {
    jcenter() // or maven { url 'https://dl.bintray.com/kotlin/dokka' }
}
```

The plugin adds a task named `dokka` to the project.

If you encounter any problems when migrating from older versions of Dokka, please see the [FAQ](https://github.com/Kotlin/dokka/wiki/faq).

Minimal dokka configuration:

Groovy
```groovy
dokka {
    outputFormat = 'html' 
    outputDirectory = "$buildDir/dokka"
}
```

Kotlin
```kotlin
tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }
}
```

[Output formats](#output_formats)

The available configuration options for single platform are shown below:

```groovy
dokka {
    outputFormat = 'html'
    outputDirectory = "$buildDir/javadoc"
    
    // In case of a Gradle multiproject build, you can include subprojects here to get merged documentation
    // Note however, that you have to have the Kotlin plugin available in the root project and in the subprojects
    subProjects = ["subproject1", "subproject2"]
        
    // Used for disabling auto extraction of sources and platforms in both multi-platform and single-platform modes
    // When set to true, subProject and kotlinTasks are also omitted
    disableAutoconfiguration = false 

    // Use default or set to custom path to cache directory
    // to enable package-list caching
    // When this is set to default, caches are stored in $USER_HOME/.cache/dokka
    cacheRoot = 'default' 
    
    configuration {
        moduleName = 'data'

        // Use to include or exclude non public members.
        includeNonPublic = false
        
        // Do not output deprecated members. Applies globally, can be overridden by packageOptions
        skipDeprecated = false 
       
        // Emit warnings about not documented members. Applies globally, also can be overridden by packageOptions
        reportUndocumented = true 
        
        // Do not create index pages for empty packages
        skipEmptyPackages = true 
     
        // See the "Platforms" section of this readme
        targets = ["JVM"]  

        // Platform used for code analysis. See the "Platforms" section of this readme
        platform = "JVM"  
        
        // Property used for manual addition of files to the classpath
        // This property does not override the classpath collected automatically but appends to it
        classpath = [new File("$buildDir/other.jar")]
    
        // By default, sourceRoots are taken from Kotlin Plugin, subProjects and kotlinTasks, following roots will be appended to them
        sourceRoots = [files('src/main/kotlin')]
        
        // List of files with module and package documentation
        // https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation
        includes = ['packages.md', 'extra.md']
    
        // List of files or directories containing sample code (referenced with @sample tags)
        samples = ['samples/basic.kt', 'samples/advanced.kt']

        // By default, sourceRoots are taken from Kotlin Plugin, subProjects and kotlinTasks, following roots will be appended to them
        // Full form sourceRoot declaration
        // Repeat for multiple sourceRoots
        sourceRoot {
            // Path to a source root
            path = "src" 
        }
        
        // These tasks will be used to determine source directories and classpath
        kotlinTasks {
            defaultKotlinTasks() + [':some:otherCompileKotlin', project("another").compileKotlin]
        }

        // Specifies the location of the project source code on the Web.
        // If provided, Dokka generates "source" links for each declaration.
        // Repeat for multiple mappings
        sourceLink {
            // Unix based directory relative path to the root of the project (where you execute gradle respectively). 
            path = "src/main/kotlin" // or simply "./"
             
            // URL showing where the source code can be accessed through the web browser
            url = "https://github.com/cy6erGn0m/vertx3-lang-kotlin/blob/master/src/main/kotlin" //remove src/main/kotlin if you use "./" above
            
            // Suffix which is used to append the line number to the URL. Use #L for GitHub
            lineSuffix = "#L"
        }

        // Used for linking to JDK documentation
        jdkVersion = 6 

        // Disable linking to online kotlin-stdlib documentation
        noStdlibLink = false
        
        // Disable linking to online JDK documentation
        noJdkLink = false 
        
        // Allows linking to documentation of the project's dependencies (generated with Javadoc or Dokka)
        // Repeat for multiple links
        externalDocumentationLink {
            // Root URL of the generated documentation to link with. The trailing slash is required!
            url = new URL("https://example.com/docs/")
            
            // If package-list file is located in non-standard location
            // packageListUrl = new URL("file:///home/user/localdocs/package-list") 
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
Since version 0.10.0 dokka supports multiplatform projects. For a general understanding how a multiplatform documentation is generated, please consult the [FAQ](https://github.com/Kotlin/dokka/wiki/faq).
In the multiplatform mode, the `configuration` block is replaced by a `multiplatform` block which has inner blocks for each platform. The inner blocks can be named arbitrarly, however if you want to use source roots and classpath provided by Kotlin Multiplatform plugin, they must have the same names. See an example below:

Groovy
```groovy
kotlin { // Kotlin Multiplatform plugin configuration
    jvm() 
    js("customName") // Define a js platform named "customName" If you want to generate docs for it, you need to have this name in dokka configuration below 
}

dokka {
    outputDirectory = "$buildDir/dokka"
    outputFormat = "html"

    multiplatform {
        customName {} // The same name as in Kotlin Multiplatform plugin, so the sources are fetched automatically
        
        differentName { // Different name, so source roots, classpath and platform must be passed explicitly.
            targets = ["JVM"]
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

Kotlin
```kotlin
kotlin {  // Kotlin Multiplatform plugin configuration
    jvm()
    js("customName")
}

val dokka by getting(DokkaTask::class) {
        outputDirectory = "$buildDir/dokka"
        outputFormat = "html"

        multiplatform { 
            val customName by creating {} // The same name as in Kotlin Multiplatform plugin, so the sources are fetched automatically

            register("differentName") { // Different name, so source roots must be passed explicitly
                targets = listOf("JVM")
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

For convenience, there is also a reserved block called `global`, which is a top-level configuration of `perPackageOptions`, `externalDocumentationLinks`, and `sourceLinks` shared by every platform. Eg.

```groovy
dokka {
    multiplatform {
        global { // perPackageOptions, sourceLinks and externalDocumentationLinks from here will be copied to every other platform (jvm and js in eg.)
            perPackageOption {
                prefix = "com.somePackage"
                suppress = true
            }
            perPackageOption {
                prefix = "kotlin" 
                skipDeprecated = false
                reportUndocumented = true
                includeNonPublic = false
            }
            sourceLink {
                path = "src/main/kotlin" 
                url = "https://github.com/cy6erGn0m/vertx3-lang-kotlin/blob/master/src/main/kotlin" 
                lineSuffix = "#L"
            }
            externalDocumentationLink {
                url = new URL("https://example.com/docs/")
            }
       }
       js {}
       jvm {}
    }
}
```



Note that `javadoc` output format cannot be used with multiplatform. 

To generate the documentation, use the `dokka` Gradle task:

```bash
./gradlew dokka
```

More dokka tasks can be added to a project like this:

```groovy
task dokkaJavadoc(type: org.jetbrains.dokka.gradle.DokkaTask) {
    outputFormat = 'javadoc'
    outputDirectory = "$buildDir/javadoc"
}
```

Please see the [Dokka Gradle example project](https://github.com/JetBrains/kotlin-examples/tree/master/gradle/dokka-gradle-example) for an example.

#### Dokka Runtime
If you are using Gradle plugin and you want to use a custom version of dokka, you can do it by setting `dokkaRuntime` configuration:

```groovy
buildscript {
    ...
}

apply plugin: 'org.jetbrains.dokka'

repositories {
    jcenter()
}

dependencies {
    dokkaRuntime "org.jetbrains.dokka:dokka-fatjar:0.10.0"
}

dokka {
    outputFormat = 'html'
    outputDirectory = "$buildDir/dokkaHtml"
}

```
To use your Fat Jar, just set the path to it:

 ```groovy
 buildscript {
     ...
 }
 
 apply plugin: 'org.jetbrains.dokka'
 
 repositories {
     jcenter()
 }
 
 dependencies {
     dokkaRuntime files("/path/to/fatjar/dokka-fatjar-0.10.0.jar")
 }
 
 dokka {
     outputFormat = 'html'
     outputDirectory = "$buildDir/dokkaHtml"
 }
 
 ```

#### FAQ
If you encounter any problems, please see the [FAQ](https://github.com/Kotlin/dokka/wiki/faq).

#### Android

Since version 0.10.0 the separate Android plugin is merged with the default one. 
Just make sure you apply the plugin after
`com.android.library` and `kotlin-android`.

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}"
    }
}
repositories {
    jcenter()
}
apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'
apply plugin: 'org.jetbrains.dokka'
```

There is also a `noAndroidSdkLink` configuration parameter that works similar to `noJdkLink` and `noStdlibLink`

### Using the Maven plugin

The Maven plugin does not support multiplatform projects.

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

  * `dokka:dokka` - generate HTML documentation in Dokka format (showing declarations in Kotlin syntax);
  * `dokka:javadoc` - generate HTML documentation in JavaDoc format (showing declarations in Java syntax);
  * `dokka:javadocJar` - generate a .jar file with JavaDoc format documentation.

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
        <moduleName>data</moduleName>
        <!-- See list of possible formats below -->
        <outputFormat>html</outputFormat>
        <!-- Default: ${project.basedir}/target/dokka -->
        <outputDir>some/out/dir</outputDir>
        
        <!-- Use default or set to custom path to cache directory to enable package-list caching. -->
        <!-- When set to default, caches stored in $USER_HOME/.cache/dokka -->
        <cacheRoot>default</cacheRoot>

        <!-- List of '.md' files with package and module docs -->
        <!-- https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation -->
        <includes>
            <file>packages.md</file>
            <file>extra.md</file>
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
    </configuration>
</plugin>
```

Please see the [Dokka Maven example project](https://github.com/JetBrains/kotlin-examples/tree/master/maven/dokka-maven-example) for an example.

[Output formats](#output_formats)

### Using the Ant task

The Ant task definition is also contained in the dokka-fatjar.jar referenced above. Here's an example usage:

```xml
<project name="Dokka" default="document">
  <typedef resource="dokka-antlib.xml" classpath="dokka-fatjar.jar"/>

  <target name="document">
    <dokka format="html" outputDir="doc"/>
  </target>
</project>
```

The Ant task supports the following attributes:

  * `outputDir` - the output directory where the documentation is generated
  * `format` - the output format (see the list of supported formats below)
  * `cacheRoot` - Use `default` or set to custom path to cache directory to enable package-list caching. When set to `default`, caches stored in $USER_HOME/.cache/dokka

Inside the `dokka` tag you can create another tags named `<passconfig/>` that support the following attributes: 

  * `classpath` - list of directories or .jar files to include in the classpath (used for resolving references)
  * `samples` - list of directories containing sample code (documentation for those directories is not generated but declarations from them can be referenced using the `@sample` tag)
  * `moduleName` - the name of the module being documented (used as the root directory of the generated documentation)
  * `include` - names of files containing the documentation for the module and individual packages
  * `skipDeprecated` - if set, deprecated elements are not included in the generated documentation
  * `jdkVersion` - version for linking to JDK
  * `analysisPlatform="jvm"` - platform used for analysing sourceRoots, see the [platforms](#platforms) section
  * `<sourceRoot path="src" />` - source root
  * `<packageOptions prefix="kotlin" includeNonPublic="false" reportUndocumented="true" skipDeprecated="false"/>` - 
    Per package options for package `kotlin` and sub-packages of it
  * `noStdlibLink` - disable linking to online kotlin-stdlib documentation
  * `noJdkLink` - disable linking to online JDK documentation
  * `<externalDocumentationLink url="https://example.com/docs/" packageListUrl="file:///home/user/localdocs/package-list"/>` -
    linking to external documentation, packageListUrl should be used if package-list located not in standard location
  * `<target value="JVM"/>` - see the [platforms](#platforms) section 


### Using the Command Line

To run Dokka from the command line, download the [Dokka jar](https://github.com/Kotlin/dokka/releases/download/0.10.0/dokka-fatjar.jar).
To generate documentation, run the following command:

    java -jar dokka-fatjar.jar <arguments>

Dokka supports the following command line arguments:

  * `-output` - the output directory where the documentation is generated
  * `-format` - the [output format](#output-formats):
  * `-cacheRoot` - use `default` or set to custom path to cache directory to enable package-list caching. When set to `default`, caches stored in $USER_HOME/.cache/dokka
  * `-pass` - (repeatable) - configuration for single analyser pass. Following this argument, you can pass other arguments:
    * `-src` - (repeatable) - source file or directory (allows many paths separated by the system path separator)
    * `-classpath` - (repeatable) - directory or .jar file to include in the classpath (used for resolving references)
    * `-sample` - (repeatable) - directory containing a sample code (documentation for those directories is not generated but declarations from them can be referenced using the `@sample` tag)
    * `-module` - the name of the module being documented (used as the root directory of the generated documentation)
    * `-include` - (repeatable) - names of files containing the documentation for the module and individual packages
    * `-skipDeprecated` - if set, deprecated elements are not included in the generated documentation
    * `-reportUndocumented` - warn about undocumented members
    * `-skipEmptyPackages` - do not create index pages for empty packages
    * `-packageOptions` - list of package options in format `prefix,-deprecated,-privateApi,+warnUndocumented;prefix, ...` 
    * `-links` - external documentation links in format `url^packageListUrl^^url2...`
    * `-srcLink` - (repeatable) - mapping between a source directory and a Web site for browsing the code in format `<path>=<url>[#lineSuffix]`
    * `-noStdlibLink` - disable linking to online kotlin-stdlib documentation
    * `-noJdkLink` - disable linking to online JDK documentation
    * `-jdkVersion` - version of JDK to use for linking to JDK JavaDoc
    * `-analysisPlatform` - platform used for analysis, see the [Platforms](#platforms) section
    * `-target` - (repeatable) - generation target


### Output formats<a name="output_formats"></a>

  * `html` - minimalistic html format used by default, Java classes are translated to Kotlin
  * `javadoc` - looks like normal Javadoc, Kotlin classes are translated to Java
  * `html-as-java` - looks like `html`, but Kotlin classes are translated to Java
  * `markdown` - markdown structured as `html`, Java classes are translated to Kotlin
    * `gfm` - GitHub flavored markdown
    * `jekyll` - Jekyll compatible markdown 
  * `kotlin-website*` - internal format used for documentation on [kotlinlang.org](https://kotlinlang.org)

### Platforms<a name="platforms"></a>

Dokka can annotate elements with special `platform` block with platform requirements 
Example result and usage can be found on [kotlinlang.org](https://kotlinlang.org/api/latest/jvm/stdlib/)

Each multiplatform closure has two properties: `platform` and `targets`. If you use autoconfiguration, those are filled automatically.

`targets` property is a list of platform names that will be shown in the final result. 

`platform` property is used for the analysis of source roots. Available values are: 
  * `jvm`
  * `js`
  * `native`
  * `common` 

## Building dokka

Dokka is built with Gradle. To build it, use `./gradlew build`.
Alternatively, open the project directory in IntelliJ IDEA and use the IDE to build and run dokka.
