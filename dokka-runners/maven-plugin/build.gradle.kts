/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("org.jetbrains.conventions.maven-cli-setup")
}

dependencies {
    implementation(projects.core)

    implementation(libs.apacheMaven.core)
    implementation(libs.apacheMaven.pluginApi)
    implementation(libs.apacheMaven.pluginAnnotations)
    implementation(libs.apacheMaven.archiver)
}

val mavenPluginTaskGroup = "maven plugin"

val generatePom by tasks.registering(Sync::class) {
    description = "Generate pom.xml for Maven Plugin Plugin"
    group = mavenPluginTaskGroup

    val dokka_version: String by project
    inputs.property("dokka_version", dokka_version)

    val pomTemplateFile = layout.projectDirectory.file("pom.template.xml")

    val mavenVersion = mavenCliSetup.mavenVersion.orNull
    val mavenPluginToolsVersion = mavenCliSetup.mavenPluginToolsVersion.orNull

    from(pomTemplateFile) {
        rename { it.replace(".template.xml", ".xml") }

        expand(
            "mavenVersion" to mavenVersion,
            "dokka_version" to dokka_version,
            "mavenPluginToolsVersion" to mavenPluginToolsVersion,
        )
    }

    into(temporaryDir)
}

val prepareHelpMojoDir by tasks.registering(Sync::class) {
    description = "Prepare files for generating the Maven Plugin HelpMojo"
    group = mavenPluginTaskGroup

    into(layout.buildDirectory.dir("maven-help-mojo"))
    from(generatePom)
}

val helpMojo by tasks.registering(Exec::class) {
    description = "Generate the Maven Plugin HelpMojo"
    group = mavenPluginTaskGroup

    dependsOn(tasks.installMavenBinary, prepareHelpMojoDir)

    workingDir(prepareHelpMojoDir.map { it.destinationDir })
    executable(mavenCliSetup.mvn.get())
    args("-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:helpmojo")

    outputs.dir(workingDir)
}

val helpMojoSources by tasks.registering(Sync::class) {
    description = "Sync the HelpMojo source files into a SourceSet SrcDir"
    group = mavenPluginTaskGroup
    from(helpMojo) {
        eachFile {
            // drop 2 leading directories
            relativePath = RelativePath(true, *relativePath.segments.drop(2).toTypedArray())
        }
    }
    includeEmptyDirs = false
    into(temporaryDir)
    include("**/*.java")
}

val helpMojoResources by tasks.registering(Sync::class) {
    description = "Sync the HelpMojo resource files into a SourceSet SrcDir"
    group = mavenPluginTaskGroup
    from(helpMojo)
    into(temporaryDir)
    include("**/**")
    exclude("**/*.java")
}

sourceSets.main {
    // use the generated HelpMojo as compilation input, so Gradle will automatically generate the mojo
    java.srcDirs(helpMojoSources)
    resources.srcDirs(helpMojoResources)
}

val preparePluginDescriptorDir by tasks.registering(Sync::class) {
    description = "Prepare files for generating the Maven Plugin descriptor"
    group = mavenPluginTaskGroup

    into(layout.buildDirectory.dir("maven-plugin-descriptor"))

    from(tasks.compileKotlin) { into("classes/java/main") }
    from(tasks.compileJava) { into("classes/java/main") }
    from(helpMojoResources)
}

val pluginDescriptor by tasks.registering(Exec::class) {
    description = "Generate the Maven Plugin descriptor"
    group = mavenPluginTaskGroup

    dependsOn(tasks.installMavenBinary, preparePluginDescriptorDir)

    workingDir(preparePluginDescriptorDir.map { it.destinationDir })
    executable(mavenCliSetup.mvn.get())
    args("-e", "-B", "org.apache.maven.plugins:maven-plugin-plugin:descriptor")

    outputs.dir("$workingDir/classes/java/main/META-INF/maven")
}

tasks.jar {
    metaInf {
        from(pluginDescriptor) {
            into("maven")
        }
    }
    manifest {
        attributes("Class-Path" to configurations.runtimeClasspath.map { configuration ->
            configuration.resolve().joinToString(" ") { it.name }
        })
    }
}

registerDokkaArtifactPublication("dokkaMavenPlugin") {
    artifactId = "dokka-maven-plugin"
}
