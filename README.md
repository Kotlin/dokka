dokka  [![TeamCity (build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/Kotlin_Dokka_DokkaAntMavenGradle.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_Dokka_DokkaAntMavenGradle&branch_KotlinTools_Dokka=%3Cdefault%3E&tab=buildTypeStatusDiv) [ ![Download](https://api.bintray.com/packages/kotlin/dokka/dokka/images/download.svg) ](https://bintray.com/kotlin/dokka/dokka/_latestVersion)
=====

Dokka is a documentation engine for Kotlin, performing the same function as javadoc for Java.
Just like Kotlin itself, Dokka fully supports mixed-language Java/Kotlin projects. It understands
standard Javadoc comments in Java files and [KDoc comments](https://kotlinlang.org/docs/reference/kotlin-doc.html) in Kotlin files,
and can generate documentation in multiple formats including standard Javadoc, HTML and Markdown.

## Using Dokka

### Using the Command Line

To run Dokka from the command line, download the [Dokka jar](https://github.com/Kotlin/dokka/releases/download/0.9.9/dokka-fatjar.jar).
To generate documentation, run the following command:

    java -jar dokka-fatjar.jar <source directories> <arguments>

Dokka supports the following command line arguments:

  * `-output` - the output directory where the documentation is generated
  * `-format` - the output format:
    * `html` - HTML (default)
    * `markdown` - Markdown
    * `gfm` - GitHub-Flavored Markdown
    * `jekyll` - Markdown adapted for Jekyll sites
    * `javadoc` - Javadoc (showing how the project can be accessed from Java)
  * `-classpath` - list of directories or .jar files to include in the classpath (used for resolving references)
  * `-samples` - list of directories containing sample code (documentation for those directories is not generated but declarations from them can be referenced using the `@sample` tag)
  * `-module` - the name of the module being documented (used as the root directory of the generated documentation)
  * `-include` - names of files containing the documentation for the module and individual packages
  * `-nodeprecated` - if set, deprecated elements are not included in the generated documentation


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

  * `outputdir` - the output directory where the documentation is generated
  * `outputformat` - the output format (see the list of supported formats above)
  * `classpath` - list of directories or .jar files to include in the classpath (used for resolving references)
  * `samples` - list of directories containing sample code (documentation for those directories is not generated but declarations from them can be referenced using the `@sample` tag)
  * `modulename` - the name of the module being documented (used as the root directory of the generated documentation)
  * `include` - names of files containing the documentation for the module and individual packages
  * `skipdeprecated` - if set, deprecated elements are not included in the generated documentation

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

Minimal maven configuration is

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

Configuring source links mapping:

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
        <sourceLinks>
            <link>
                <dir>${project.basedir}/src/main/kotlin</dir>
                <url>http://github.com/me/myrepo</url>
            </link>
        </sourceLinks>
    </configuration>
</plugin>
```

Please see the [Dokka Maven example project](https://github.com/JetBrains/kotlin-examples/tree/master/maven/dokka-maven-example) for an example.

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

The plugin adds a task named "dokka" to the project. The available configuration
options are shown below:

```groovy
dokka {
    moduleName = 'data'
    outputFormat = 'javadoc'
    outputDirectory = "$buildDir/javadoc"
    processConfigurations = ['compile', 'extra']
    includes = ['packages.md', 'extra.md']
    samples = ['samples/basic.kt', 'samples/advanced.kt']
    linkMapping {
        dir = "src/main/kotlin"
        url = "https://github.com/cy6erGn0m/vertx3-lang-kotlin/blob/master/src/main/kotlin"
        suffix = "#L"
    }
    sourceDirs = files('src/main/kotlin')
}
```

To get it generated use gradle `dokka` task

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

If you are using Android there is a separate gradle plugin. Just make sure you apply the plugin after
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
