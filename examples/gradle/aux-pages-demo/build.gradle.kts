import org.jetbrains.dokka.auxiliaryDocs.AuxiliaryConfiguration
import org.jetbrains.dokka.auxiliaryDocs.AuxiliaryDocsPlugin
import org.jetbrains.dokka.gradle.DokkaTask

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        val dokka_version: String by project
        classpath("org.jetbrains.dokka:auxiliary-docs:$dokka_version")
    }
}

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64()
    jvmToolchain(11)
}

dependencies {
    val dokka_version: String by project
    dokkaPlugin("org.jetbrains.dokka:auxiliary-docs:$dokka_version")
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        moduleName.set("OkHttp")
        moduleVersion.set("4.X")
    }

    pluginConfiguration<AuxiliaryDocsPlugin, AuxiliaryConfiguration> {
        nodesDir = projectDir.resolve("docs")
        entryPointNode = projectDir.resolve("README.md")
        apiReferenceNodeName = "API Reference"
    }
}
