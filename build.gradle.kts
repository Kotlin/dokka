import org.jetbrains.configureDistMaven
import org.jetbrains.configureDokkaVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    id("com.jfrog.bintray") apply false
    id("java")
}

val dokka_version: String by project

allprojects {
    configureDokkaVersion()

    group = "org.jetbrains.dokka"
    version = dokka_version

    val language_version: String by project
    tasks.withType(KotlinCompile::class).all {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict -Xskip-metadata-version-check -Xopt-in=kotlin.RequiresOptIn."
            languageVersion = language_version
            apiVersion = language_version
            jvmTarget = "1.8"
        }
    }

    repositories {
        jcenter()
        mavenCentral()
        mavenLocal()
        maven(url = "https://dl.bintray.com/jetbrains/markdown/")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
    }

    configureDistMaven()
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("java")
    }

    // Gradle metadata
    java {
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

println("Publication version: $dokka_version")
