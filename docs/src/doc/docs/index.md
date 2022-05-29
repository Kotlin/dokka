# Dokka  [![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![TeamCity (build status)](https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:Kotlin_Dokka_DokkaAntMavenGradle)/statusIcon)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_Dokka_DokkaAntMavenGradle&branch_KotlinTools_Dokka=%3Cdefault%3E&tab=buildTypeStatusDiv) 

Dokka is an API documentation engine for Kotlin that performs the same function as the Javadoc tool for Java,
but more modern and highly pluggable.

Just like Kotlin itself, Dokka supports mixed-language Kotlin/Java projects. It understands
[KDoc comments](https://kotlinlang.org/docs/reference/kotlin-doc.html) in Kotlin source files as well
as Javadoc comments in Java files, and can generate documentation in multiple formats including its
own HTML format, Java's Javadoc lookalike and Markdown.

Some libraries that use Dokka for API reference docs:

* [kotlinx.coroutines](https://kotlin.github.io/kotlinx.coroutines/)
* [kotlinx.serialization](https://kotlin.github.io/kotlinx.serialization/)
* [Ktor](https://api.ktor.io/)
* [Spring Framework](https://docs.spring.io/spring-framework/docs/current/kdoc-api/)

Dokka provides support for the following build systems:

* [Gradle](user_guide/applying/gradle.md) (preffered)
* [Maven](user_guide/applying/maven.md)
* [Command line](user_guide/applying/cli.md)

// TODO write about plugins and pluggability
