# Dokka  [![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![TeamCity (build status)](https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:Kotlin_Dokka_DokkaAntMavenGradle)/statusIcon)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_Dokka_DokkaAntMavenGradle&branch_KotlinTools_Dokka=%3Cdefault%3E&tab=buildTypeStatusDiv) 

Dokka is a documentation engine for Kotlin, performing the same function as javadoc for Java.
Just like Kotlin itself, Dokka fully supports mixed-language Java/Kotlin projects. It understands
standard Javadoc comments in Java files and [KDoc comments](https://kotlinlang.org/docs/reference/kotlin-doc.html) in Kotlin files,
and can generate documentation in multiple formats including standard Javadoc, HTML and Markdown.

## Integrations
Dokka provides support for the following build systems:

* [Gradle](user_guide/gradle/usage.md)
* [Maven](user_guide/maven/usage.md)
* [Command line](user_guide/cli/usage.md)

!!! note
    The Gradle plugin is the preferred way to use Dokka
