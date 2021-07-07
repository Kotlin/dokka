# Dokka  [![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![TeamCity (build status)](https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:Kotlin_Dokka_DokkaAntMavenGradle)/statusIcon)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_Dokka_DokkaAntMavenGradle&branch_KotlinTools_Dokka=%3Cdefault%3E&tab=buildTypeStatusDiv) 

Dokka is a documentation engine for Kotlin, performing the same function as javadoc for Java.
Just like Kotlin itself, Dokka fully supports mixed-language Java/Kotlin projects. It understands
standard Javadoc comments in Java files and [KDoc comments](https://kotlinlang.org/docs/reference/kotlin-doc.html) in Kotlin files,
and can generate documentation in multiple formats including standard Javadoc, HTML and Markdown.

## Using Dokka

**Full documentation is available at [https://kotlin.github.io/dokka/1.5.0/](https://kotlin.github.io/dokka/1.5.0/)**

### Using the Gradle plugin
_Note: If you are upgrading from 0.10.x to a current release of Dokka, please have a look at our 
[migration guide](runners/gradle-plugin/MIGRATION.md)_

The preferred way is to use `plugins` block.
 
build.gradle.kts:
```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.5.0"
}

repositories {
    mavenCentral()
}
```

The plugin adds `dokkaHtml`, `dokkaJavadoc`, `dokkaGfm` and `dokkaJekyll` tasks to the project.
 
#### Applying plugins
Dokka plugin creates Gradle configuration for each output format in the form of `dokka${format}Plugin`:

```kotlin
dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.5.0")
}
``` 

You can also create a custom Dokka task and add plugins directly inside:

```kotlin
val customDokkaTask by creating(DokkaTask::class) {
    dependencies {
        plugins("org.jetbrains.dokka:kotlin-as-java-plugin:1.5.0")
    }
}
```

Please note that `dokkaJavadoc` task will properly document only single `jvm` source set

To generate the documentation, use the appropriate `dokka${format}` Gradle task:

```bash
./gradlew dokkaHtml
```

Please see the [Dokka Gradle example project](https://github.com/Kotlin/kotlin-examples/tree/master/gradle/dokka/dokka-gradle-example) for an example.

We encourage users to create their own plugins and share them with the community on [official plugins list](docs/src/doc/docs/community/plugins-list.md).

#### Android

Make sure you apply Dokka after `com.android.library` and `kotlin-android`.

```kotlin
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlin_version}")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}")
    }
}
repositories {
    mavenCentral()
}
apply(plugin= "com.android.library")
apply(plugin= "kotlin-android")
apply(plugin= "org.jetbrains.dokka")
```

```kotlin
dokkaHtml.configure {
    dokkaSourceSets {
        named("main") {
            noAndroidSdkLink.set(false)
        }   
    }
}
```

#### Multi-module projects
For documenting Gradle multi-module projects, you can use `dokka${format}Multimodule` tasks.

```kotlin
tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(buildDir.resolve("dokkaCustomMultiModuleOutput"))
}
```

`DokkaMultiModule` depends on all Dokka tasks in the subprojects, runs them, and creates a toplevel page
with links to all generated (sub)documentations

### Using the Maven plugin

The Maven plugin does not support multi-platform projects. 

Documentation is by default generated in `target/dokka`.

The following goals are provided by the plugin:

  * `dokka:dokka` - generate HTML documentation in Dokka format (showing declarations in Kotlin syntax)
  * `dokka:javadoc` - generate HTML documentation in Javadoc format (showing declarations in Java syntax)
  * `dokka:javadocJar` - generate a .jar file with Javadoc format documentation

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

To run Dokka from the command line, download the [Dokka CLI runner](https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-cli).
To generate documentation, run the following command:
```
java -jar dokka-cli.jar <arguments>
```

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
to the Dokka plugins classpath, eg. in Gradle:

```kotlin
dependencies{
    implementation("...")
    dokkaGfmPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${dokka-version}")
}
```

#### FAQ
If you encounter any problems, please see the [FAQ](https://github.com/Kotlin/dokka/wiki/faq).
