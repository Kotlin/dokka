import org.jetbrains.configureDistMaven
import org.jetbrains.configureDokkaVersion

plugins {
    kotlin("jvm") apply false
    id("com.jfrog.bintray") apply false
}

allprojects {
    configureDokkaVersion()
    val dokka_version: String by this

    if (this == rootProject) {
        println("Publication version: $dokka_version")
    }
    group = "org.jetbrains.dokka"
    version = dokka_version

    val language_version: String by project
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
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
    }

    configureDistMaven()
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
    }
}