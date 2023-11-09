/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.overridePublicationArtifactId

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

    inputs.property("dokka_version", project.version)

    val pomTemplateFile = layout.projectDirectory.file("pom.template.xml")

    val mavenVersion = mavenCliSetup.mavenVersion.orNull
    val mavenPluginToolsVersion = mavenCliSetup.mavenPluginToolsVersion.orNull

    from(pomTemplateFile) {
        rename { it.replace(".template.xml", ".xml") }

        expand(
            "mavenVersion" to mavenVersion,
            "dokka_version" to project.version,
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
