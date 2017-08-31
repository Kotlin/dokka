dokka  [![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity (build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/Kotlin_Dokka_DokkaAntMavenGradle.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_Dokka_DokkaAntMavenGradle&branch_KotlinTools_Dokka=%3Cdefault%3E&tab=buildTypeStatusDiv) [ ![Download](https://api.bintray.com/packages/kotlin/dokka/dokka/images/download.svg) ](https://bintray.com/kotlin/dokka/dokka/_latestVersion)
=====

Dokka is a documentation engine for Kotlin, performing the same function as javadoc for Java.
Just like Kotlin itself, Dokka fully supports mixed-language Java/Kotlin projects. It understands
standard Javadoc comments in Java files and [KDoc comments](https://kotlinlang.org/docs/reference/kotlin-doc.html) in Kotlin files,
and can generate documentation in multiple formats including standard Javadoc, HTML and Markdown.

## Using Dokka

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

apply plugin: 'org.jetbrains.dokka'
```

The plugin adds a task named "dokka" to the project.
 
Minimal dokka configuration:

```groovy
dokka {
    outputFormat = 'html' 
    outputDirectory = "$buildDir/javadoc"
}
```

[Output formats](#output_formats)
 
The available configuration options are shown below:

```groovy
dokka {
    moduleName = 'data'
    outputFormat = 'html'
    outputDirectory = "$buildDir/javadoc"
    
    // These tasks will be used to determine source directories and classpath 
    kotlinTasks {
        defaultKotlinTasks() + [':some:otherCompileKotlin', project("another").compileKotlin]
    }
    
    // List of files with module and package documentation
    // http://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation
    includes = ['packages.md', 'extra.md']
    
    // The list of files or directories containing sample code (referenced with @sample tags)
    samples = ['samples/basic.kt', 'samples/advanced.kt']
    
    jdkVersion = 6 // Used for linking to JDK

    // Use default or set to custom path to cache directory
    // to enable package-list caching
    // When set to default, caches stored in $USER_HOME/.cache/dokka
    cacheRoot = 'default' 
    
    // Use to include or exclude non public members.
    includeNonPublic = false
    
    // Do not output deprecated members. Applies globally, can be overridden by packageOptions
    skipDeprecated = false 
   
    // Emit warnings about not documented members. Applies globally, also can be overridden by packageOptions
    reportNotDocumented = true 
    
    skipEmptyPackages = true // Do not create index pages for empty packages
 
    impliedPlatforms = ["JVM"] // See platforms section of documentation 
    
    // Manual adding files to classpath
    // This property not overrides classpath collected from kotlinTasks but appends to it
    classpath = [new File("$buildDir/other.jar")]

    // By default, sourceRoots is taken from kotlinTasks, following roots will be appended to it
    // Short form sourceRoots
    sourceDirs = files('src/main/kotlin')
    
    // By default, sourceRoots is taken from kotlinTasks, following roots will be appended to it
    // Full form sourceRoot declaration
    // Repeat for multiple sourceRoots
    sourceRoot {
        // Path to source root
        path = "src" 
        // See platforms section of documentation 
        platforms = ["JVM"] 
    }
    
    // Specifies the location of the project source code on the Web.
    // If provided, Dokka generates "source" links for each declaration.
    // Repeat for multiple mappings
    linkMapping {
        // Source directory
        dir = "src/main/kotlin"
         
        // URL showing where the source code can be accessed through the web browser
        url = "https://github.com/cy6erGn0m/vertx3-lang-kotlin/blob/master/src/main/kotlin"
        
        // Suffix which is used to append the line number to the URL. Use #L for GitHub
        suffix = "#L"
    }
    
    // No default documentation link to kotlin-stdlib
    noStdlibLink = false
    
    // Allows linking to documentation of the project's dependencies (generated with Javadoc or Dokka)
    // Repeat for multiple links
    externalDocumentationLink {
        // Root URL of the generated documentation to link with. The trailing slash is required!
        url = new URL("https://example.com/docs/")
        
        // If package-list file located in non-standard location
        // packageListUrl = new URL("file:///home/user/localdocs/package-list") 
    }
    
    // Allows to customize documentation generation options on a per-package basis
    // Repeat for multiple packageOptions
    packageOptions {
        prefix = "kotlin" // will match kotlin and all sub-packages of it
        // All options are optional, default values are below:
        skipDeprecated = false
        reportUndocumented = true // Emit warnings about not documented members 
        includeNonPublic = false
    }
}
```

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

#### Android

If you are using Android there is a separate Gradle plugin. Just make sure you apply the plugin after
`com.android.library` and `kotlin-android`.

```groovy
buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath "org.jetbrains.dokka:dokka-android-gradle-plugin:${dokka_version}"
    }
}

apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'
apply plugin: 'org.jetbrains.dokka-android'
```

### Using the Maven plugin

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
        <outFormat>html</outFormat>
        <!-- Default: ${project.basedir}/target/dokka -->
        <outputDir>some/out/dir</outputDir>
        
        <!-- Use default or set to custom path to cache directory to enable package-list caching. -->
        <!-- When set to default, caches stored in $USER_HOME/.cache/dokka -->
        <cacheRoot>default</cacheRoot>

        <!-- List of '.md' files with package and module docs -->
        <!-- http://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation -->
        <includes>
            <file>packages.md</file>
            <file>extra.md</file>
        </includes>
        
        <!-- List of sample roots -->
        <samplesDirs>
            <dir>src/test/samples</dir>
        </samplesDirs>
        
        <!-- Used for linking to JDK, default: 6 -->
        <jdkVersion>6</jdkVersion>
        
        <!-- Do not output deprecated members, applies globally, can be overridden by packageOptions -->
        <skipDeprecated>false</skipDeprecated> 
        <!-- Emit warnings about not documented members, applies globally, also can be overridden by packageOptions -->
        <reportNotDocumented>true</reportNotDocumented>             
        <!-- Do not create index pages for empty packages -->
        <skipEmptyPackages>true</skipEmptyPackages> 
        
        <!-- See platforms section of documentation -->
        <impliedPlatforms>
            <platform>JVM</platform>
        </impliedPlatforms>
        
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
                <dir>${project.basedir}/src/main/kotlin</dir>
                <!-- URL showing where the source code can be accessed through the web browser -->
                <url>http://github.com/me/myrepo</url>
                <!--Suffix which is used to append the line number to the URL. Use #L for GitHub -->
                <urlSuffix>#L</urlSuffix>
            </link>
        </sourceLinks>
        
        <!-- No default documentation link to kotlin-stdlib -->
        <noStdlibLink>false</noStdlibLink>
        
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

The Ant task definition is also contained in the dokka-fatjar.jar referenced above. Here's an example of using it:

```xml
<project name="Dokka" default="document">
    <typedef resource="dokka-antlib.xml" classpath="dokka-fatjar.jar"/>

    <target name="document">
        <dokka src="src" outputdir="doc" modulename="myproject"/>
    </target>
</project>
```

The Ant task supports the following attributes:

  * `outputDir` - the output directory where the documentation is generated
  * `outputFormat` - the output format (see the list of supported formats above)
  * `classpath` - list of directories or .jar files to include in the classpath (used for resolving references)
  * `samples` - list of directories containing sample code (documentation for those directories is not generated but declarations from them can be referenced using the `@sample` tag)
  * `moduleName` - the name of the module being documented (used as the root directory of the generated documentation)
  * `include` - names of files containing the documentation for the module and individual packages
  * `skipDeprecated` - if set, deprecated elements are not included in the generated documentation
  * `jdkVersion` - version for linking to JDK
  * `impliedPlatforms` - See [platforms](#platforms) section
  * `<sourceRoot path="src" platforms="JVM" />` - analogue of src, but allows to specify [platforms](#platforms) 
  * `<packageOptions prefix="kotlin" includeNonPublic="false" reportUndocumented="true" skipDeprecated="false"/>` - 
    Per package options for package `kotlin` and sub-packages of it
  * `noStdlibLink` - No default documentation link to kotlin-stdlib
  * `<externalDocumentationLink url="https://example.com/docs/" packageListUrl="file:///home/user/localdocs/package-list"/>` -
    linking to external documentation, packageListUrl should be used if package-list located not in standard location
  * `cacheRoot` - Use `default` or set to custom path to cache directory to enable package-list caching. When set to `default`, caches stored in $USER_HOME/.cache/dokka
    

### Using the Command Line

To run Dokka from the command line, download the [Dokka jar](https://github.com/Kotlin/dokka/releases/download/0.9.10/dokka-fatjar.jar).
To generate documentation, run the following command:

    java -jar dokka-fatjar.jar <source directories> <arguments>

Dokka supports the following command line arguments:

  * `-output` - the output directory where the documentation is generated
  * `-format` - the [output format](#output-formats):
  * `-classpath` - list of directories or .jar files to include in the classpath (used for resolving references)
  * `-samples` - list of directories containing sample code (documentation for those directories is not generated but declarations from them can be referenced using the `@sample` tag)
  * `-module` - the name of the module being documented (used as the root directory of the generated documentation)
  * `-include` - names of files containing the documentation for the module and individual packages
  * `-nodeprecated` - if set, deprecated elements are not included in the generated documentation
  * `-impliedPlatforms` - List of implied platforms (comma-separated)
  * `-packageOptions` - List of package options in format `prefix,-deprecated,-privateApi,+warnUndocumented;...` 
  * `-links` - External documentation links in format `url^packageListUrl^^url2...`
  * `-noStdlibLink` - Disable documentation link to stdlib
  * `-cacheRoot` - Use `default` or set to custom path to cache directory to enable package-list caching. When set to `default`, caches stored in $USER_HOME/.cache/dokka


### Output formats<a name="output_formats"></a>

  * `html` - minimalistic html format used by default
  * `javadoc` - Dokka mimic to javadoc
  * `html-as-java` - as `html` but using java syntax
  * `markdown` - Markdown structured as `html`
    * `gfm` - GitHub flavored markdown  
    * `jekyll` - Jekyll compatible markdown 
  * `kotlin-website*` - internal format used for documentation on [kotlinlang.org](https://kotlinlang.org)

### Platforms<a name="platforms"></a>

Dokka can annotate elements with special `platform` block with platform requirements 

Example of usage can be found on [kotlinlang.org](https://kotlinlang.org/api/latest/jvm/stdlib/)

Each source root has a list of platforms for which members are suitable. 
Also, the list of 'implied' platforms is passed to Dokka.
If a member is not available for all platforms in the implied platforms set, its documentation will show
the list of platforms for which it's available.

## Dokka Internals

### Documentation Model

Dokka uses Kotlin-as-a-service technology to build `code model`, then processes it into `documentation model`.
`Documentation model` is graph of items describing code elements such as classes, packages, functions, etc.

Each node has semantic attached, e.g. Value:name -> Type:String means that some value `name` is of type `String`.

Each reference between nodes also has semantic attached, and there are three of them:

1. Member - reference means that target is member of the source, form tree.
2. Detail - reference means that target describes source in more details, form tree.
3. Link - any link to any other node, free form.

Member & Detail has reverse Owner reference, while Link's back reference is also Link.

Nodes that are Details of other nodes cannot have Members.

### Rendering Docs

When we have documentation model, we can render docs in various formats, languages and layouts. We have some core services:

* FormatService -- represents output format
* LocationService -- represents folder and file layout
* SignatureGenerator -- represents target language by generating class/function/package signatures from model

Basically, given the `documentation` as a model, we do this:

```kotlin
    val signatureGenerator = KotlinSignatureGenerator()
    val locationService = FoldersLocationService(arguments.outputDir)
    val markdown = JekyllFormatService(locationService, signatureGenerator)
    val generator = FileGenerator(signatureGenerator, locationService, markdown)
    generator.generate(documentation)
```

## Building Dokka

Dokka is built with Gradle. To build it, use `./gradlew build`.
Alternatively, open the project directory in IntelliJ IDEA and use the IDE to build and run Dokka.
