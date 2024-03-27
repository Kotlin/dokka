/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId
import dokkabuild.tasks.MvnExec

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.publish-jvm")
    id("dokkabuild.setup-maven-cli")
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

overridePublicationArtifactId("dokka-maven-plugin")

dependencies {
    // this version is required, so that it will be available in the POM of plugin
    implementation("org.jetbrains.dokka:dokka-core:$version")

    implementation(libs.apacheMaven.core)
    implementation(libs.apacheMaven.pluginApi)
    implementation(libs.apacheMaven.pluginAnnotations)
    implementation(libs.apacheMaven.archiver)
}

val mavenPluginTaskGroup = "maven plugin"

val generatePom by tasks.registering(Sync::class) {
    description = "Generate pom.xml for Maven Plugin Plugin"
    group = mavenPluginTaskGroup

    val dokkaVersion = dokkaBuild.projectVersion
    inputs.property("dokkaVersion", dokkaVersion)

    val mavenVersion = mavenCliSetup.mavenVersion
    inputs.property("mavenVersion", mavenVersion)

    val mavenPluginToolsVersion = mavenCliSetup.mavenPluginToolsVersion
    inputs.property("mavenPluginToolsVersion", mavenPluginToolsVersion)

    val pomTemplateFile = layout.projectDirectory.file("pom.template.xml")

    from(pomTemplateFile) {
        rename { it.replace(".template.xml", ".xml") }

        expand(
            "mavenVersion" to mavenVersion.get(),
            "dokka_version" to dokkaVersion.get(),
            "mavenPluginToolsVersion" to mavenPluginToolsVersion.get(),
        )
    }

    into(temporaryDir)
}

val generateHelpMojo by tasks.registering(MvnExec::class) {
    description = "Generate the Maven Plugin HelpMojo"
    group = mavenPluginTaskGroup

    resources.from(generatePom)
    arguments.addAll(
        "org.apache.maven.plugins:maven-plugin-plugin:helpmojo"
    )
}

val helpMojoSources by tasks.registering(Sync::class) {
    description = "Sync the HelpMojo source files into a SourceSet SrcDir"
    group = mavenPluginTaskGroup

    from(generateHelpMojo) {
        eachFile {
            // Maven generates sources into `generated-sources/plugin/`,
            // so drop 2 leading directories:
            relativePath = RelativePath(true, *relativePath.segments.drop(2).toTypedArray())
        }
    }
    includeEmptyDirs = false

    into(temporaryDir)

    // this task prepares generated helpmojo _sources_, so only include source files
    include("**/*.java")
}

val helpMojoResources by tasks.registering(Sync::class) {
    description = "Sync the HelpMojo resource files into a SourceSet SrcDir"
    group = mavenPluginTaskGroup

    from(generateHelpMojo)

    into(temporaryDir)

    // this task prepares generated helpmojo _resources_, so...
    include("**/**")       // include everything by default
    exclude("**/*.java")   // don't include source files
    // `maven-plugin-help.properties` contains an absolute path: destinationDirectory.
    // Exclude it, so that Build Cache is relocatable.
    exclude("**/maven-plugin-help.properties")

    includeEmptyDirs = false
}

sourceSets.main {
    // use the generated HelpMojo tasks as compilation input: Gradle will automatically trigger the tasks when required
    java.srcDirs(helpMojoSources)
    resources.srcDirs(helpMojoResources)
}

val generatePluginDescriptor by tasks.registering(MvnExec::class) {
    description = "Generate the Maven Plugin descriptor"
    group = mavenPluginTaskGroup

    classes.from(tasks.compileKotlin)
    classes.from(tasks.compileJava)

    resources.from(helpMojoResources)

    arguments.addAll(
        "org.apache.maven.plugins:maven-plugin-plugin:descriptor"
    )
}

val pluginDescriptorMetaInf: Provider<RegularFile> =
    generatePluginDescriptor.flatMap {
        it.workDirectory.file("classes/java/main/META-INF/maven")
    }

tasks.jar {
    metaInf {
        from(pluginDescriptorMetaInf) {
            into("maven")
        }
    }
    manifest {
        attributes("Class-Path" to configurations.runtimeClasspath.map { configuration ->
            configuration.resolve().joinToString(" ") { it.name }
        })
    }
}
