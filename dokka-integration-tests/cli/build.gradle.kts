/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("dokkabuild.test-integration")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(kotlin("test-junit5"))
    implementation(libs.junit.jupiterApi)
    implementation(projects.utilities)
}

/* Create a fat base plugin jar for cli tests */
val basePluginShadow: Configuration by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
    }
}

val cliConfiguration: Configuration by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
        attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.SHADOWED))
    }
    // we should have single artifact here
    isTransitive = false
}

dependencies {
    cliConfiguration("org.jetbrains.dokka:runner-cli")

    basePluginShadow("org.jetbrains.dokka:plugin-base")

    // TODO [beresnev] analysis switcher
    basePluginShadow("org.jetbrains.dokka:analysis-kotlin-descriptors") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.SHADOWED))
        }
    }
}

val basePluginShadowJar by tasks.register("basePluginShadowJar", ShadowJar::class) {
    configurations = listOf(basePluginShadow)
    archiveFileName.set("fat-base-plugin-${project.version}.jar")
    archiveClassifier.set("")

    // service files are merged to make sure all Dokka plugins
    // from the dependencies are loaded, and not just a single one.
    mergeServiceFiles()
}

tasks.integrationTest {
    dependsOn(cliConfiguration)
    dependsOn(basePluginShadowJar)

    inputs.dir(file("projects"))
    environment("CLI_JAR_PATH", cliConfiguration.singleFile)
    environment("BASE_PLUGIN_JAR_PATH", basePluginShadowJar.archiveFile.get())
}
