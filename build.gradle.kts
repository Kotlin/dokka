import org.jetbrains.ValidatePublications
import org.jetbrains.publicationChannels

plugins {
    id("org.jetbrains.conventions.base") apply false
    id("org.jetbrains.dokka") version "1.8.10"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.12.1"
}

val dokka_version: String by project

group = "org.jetbrains.dokka"
version = dokka_version


logger.lifecycle("Publication version: $dokka_version")
tasks.register<ValidatePublications>("validatePublications")

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USER"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

val dokkaPublish by tasks.registering {
    if (publicationChannels.any { it.isMavenRepository() }) {
        finalizedBy(tasks.named("closeAndReleaseSonatypeStagingRepository"))
    }
}

apiValidation {
    // note that subprojects are ignored by their name, not their path https://github.com/Kotlin/binary-compatibility-validator/issues/16
    ignoredProjects += setOf(
        // NAME                    PATH
        "search-component",    // :plugins:search-component
        "frontend",            // :plugins:base:frontend

        "kotlin-analysis",     // :kotlin-analysis
        "compiler-dependency", // :kotlin-analysis:compiler-dependency
        "intellij-dependency", // :kotlin-analysis:intellij-dependency

        "integration-tests",   // :integration-tests
        "gradle",              // :integration-tests:gradle
        "cli",                 // :integration-tests:cli
        "maven",               // integration-tests:maven

        "test-utils",          // :test-utils
    )
}
