/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.SHADOWED
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE

plugins {
    id("dokkabuild.test-integration")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)
    implementation(projects.utilities)
}

val basePluginShadow: Configuration by configurations.creating {
    description = "Create a fat base plugin jar for cli tests"
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
    }
}

val dokkaCliResolver: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
        attribute(BUNDLING_ATTRIBUTE, objects.named(SHADOWED))
    }
    // we should have single artifact here
    isTransitive = false
}

dependencies {
    dokkaCliResolver("org.jetbrains.dokka:runner-cli")

    basePluginShadow("org.jetbrains.dokka:plugin-base")

    // TODO [beresnev] analysis switcher
    basePluginShadow("org.jetbrains.dokka:analysis-kotlin-descriptors") {
        attributes {
            attribute(BUNDLING_ATTRIBUTE, objects.named(SHADOWED))
        }
    }
}

val basePluginShadowJar by tasks.registering(ShadowJar::class) {
    configurations = listOf(basePluginShadow)
    archiveFileName.set("fat-base-plugin-${project.version}.jar")
    archiveClassifier.set("")

    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}

tasks.integrationTest {
    inputs.dir(file("projects"))

    val basePluginShadowJar = basePluginShadowJar.flatMap { it.archiveFile }
    inputs.file(basePluginShadowJar).withPropertyName("basePluginShadowJar")

    val dokkaCli = dokkaCliResolver.incoming.artifacts.resolvedArtifacts.map { it.first().file }
    inputs.file(dokkaCli).withPropertyName("dokkaCli")

    doFirst("workaround for https://github.com/gradle/gradle/issues/24267") {
        environment("CLI_JAR_PATH", dokkaCli.get().invariantSeparatorsPath)
        environment("BASE_PLUGIN_JAR_PATH", basePluginShadowJar.get().asFile.invariantSeparatorsPath)
    }
}
