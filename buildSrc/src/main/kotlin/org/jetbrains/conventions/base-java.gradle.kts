package org.jetbrains.conventions

/**
 * Base configuration for Java projects.
 *
 * This convention plugin contains shared Java config for both the [KotlinJvmPlugin] convention plugin and
 * the Gradle Plugin subproject (which cannot have the `kotlin("jvm")` plugin applied).
 */

plugins {
    `java`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

plugins.withType<MavenPublishPlugin>().configureEach {
    // enable the sources JAR if this project will be published
    java {
        withSourcesJar()
    }
}
