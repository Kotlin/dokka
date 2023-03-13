package org.jetbrains.conventions

/**
 * Base configuration for Java projects.
 *
 * This convention plugin contains shared Java config for both the [KotlinJvmPlugin] convention plugin and
 * the Gradle Plugin subproject (which cannot have the `kotlin("jvm")` plugin applied).
 */

plugins {
    id("org.jetbrains.conventions.base")
    `java`
}

java {
    toolchain {
        languageVersion.set(dokkaBuild.mainJavaVersion)
    }
}

java {
    withSourcesJar()
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(dokkaBuild.testJavaLauncherVersion)
    })
}
