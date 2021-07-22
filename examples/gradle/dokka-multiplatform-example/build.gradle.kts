@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.Platform

plugins {
    kotlin("multiplatform") version "1.4.32"
    id("org.jetbrains.dokka") version "1.4.32"
}

repositories {
    mavenCentral()
}

group = "org.test"
version = "1.0-SNAPSHOT"

kotlin {
    jvm() // Create a JVM target with the default name "jvm"
    linuxX64("linux")
    macosX64("macos")
    js()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
            }
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        /*
        Create custom source set (not known to the Kotlin Gradle Plugin)
         */
        register("customSourceSet") {
            this.jdkVersion.set(9)
            this.displayName.set("custom")
            this.sourceRoots.from(file("src/customJdk9/kotlin"))
        }
    }
}
