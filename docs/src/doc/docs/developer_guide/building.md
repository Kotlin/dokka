# Working with Dokka repository

## Building Dokka

Dokka is built with Gradle. To build it, use `./gradlew build`.
Alternatively, open the project directory in IntelliJ IDEA and use the IDE to build and run Dokka.

Here's how to import and configure Dokka in IntelliJ IDEA:

* Select "Open" from the IDEA welcome screen, or File > Open if a project is
  already open
* Select the directory with your clone of Dokka

IntelliJ should automatically detect Dokka as a gradle project, all you need to do is to click the import button.

## Repository structure

## Publishing locally

Dokka can be published locally using `./gradlew publishToMavenLocal`.
This builds all the artefacts and places them in your local maven repository.
It is important to check current development version of Dokka as locally published packages will have it suffixed with `-SNAPSHOT` e.g. `1.5.21-SNAPSHOT`.
Dokka version is placed in `gradle.properties` file under `dokka_version_base` key. 

After successfully publishing, Dokka can be used on a project by applying the plugin with **correct** version and adding maven local repository in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        // Your standard repositories goes here
        mavenLocal()
    }
}
```

and in `build.gradle.kts`:

```kotlin
repositories {
    // Your standard repositories goes here
    mavenLocal()
}
```

## Publishing releases

Each Dokka release is published to maven central 

## Testing

Dokka is tested using a collection of unit and integration tests

### Unit tests

Each test run contains a generation run that is very similar to what is happening when Dokka is running on a project.
To make it convenient Dokka allows writing Kotlin or Java code as strings that will later be available as parsed model.
If a test would like to use this mechanism it must extend `BaseAbstractTest` that has `testInline` method and in first 
line define a virtual path in which the file should be available. A valid test string can look like this:
```kotlin
"""
|/src/main/kotlin/basic/Test.kt
|package example
|
|class ThisShouldBePresent { }
""".trimMargin()
```

It is possible to define multiple files this way:
```kotlin
 """
|/src/main/kotlin/basic/Test.kt
|package example
|
|class ThisShouldBePresent { }
|/src/main/kotlin/basic/OtherTest.kt
|package example
|class ThisAlsoShouldBePresent { }
""".trimMargin()
```
Alternatively it is possible to analyse whole projects using `testFromData` method and passing a path to where it is stored.

After choosing your preferred method of defining tested code, you must define a configuration.
In most cases it is as simple as defining a single source root:

```kotlin
val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin")
                }
            }
        }
```




## Debugging