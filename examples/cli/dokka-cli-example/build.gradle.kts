/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm") version "2.3.21"
}

@CacheableTask
abstract class DokkaRunner : DefaultTask() {
    @get:Inject abstract val execOperations: ExecOperations
    @get:Classpath abstract val dokkaClasspath: ConfigurableFileCollection
    @get:Classpath abstract val pluginsClasspath: ConfigurableFileCollection
    @get:[InputFiles PathSensitive(PathSensitivity.NONE)] abstract val sourceDirectory: DirectoryProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun exec() {
        execOperations.javaexec {
            classpath = dokkaClasspath
            args = listOf(
                "-pluginsClasspath",
                pluginsClasspath.files.joinToString(";"),
                "-sourceSet",
                "-src ${sourceDirectory.get().asFile.absolutePath}",
                "-outputDir",
                outputDirectory.get().asFile.absolutePath,
            )
        }
    }
}

val runTask = tasks.register<DokkaRunner>("run") {
    dokkaClasspath.from(configurations.create("runnerJar") {
        dependencies.add(
            project.dependencies.create("org.jetbrains.dokka:dokka-cli:2.3.0-SNAPSHOT")
        )
    })
    pluginsClasspath.from(
        configurations.create("pluginsJars") {
            dependencies.add(
                project.dependencies.create("org.jetbrains.dokka:dokka-base:2.3.0-SNAPSHOT")
            )
            dependencies.add(
                project.dependencies.create("org.jetbrains.dokka:analysis-kotlin-symbols:2.3.0-SNAPSHOT")
            )
        }
    )
    sourceDirectory.set(project.layout.projectDirectory.dir("src/main/kotlin"))
    outputDirectory.set(project.layout.buildDirectory.dir("docs"))
}
