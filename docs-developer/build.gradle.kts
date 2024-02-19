/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import com.pswidersk.gradle.python.VenvTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

plugins {
    id("com.pswidersk.python-plugin") version "2.4.0"
}

val mkDocsSiteDir: Provider<Directory> = layout.buildDirectory.dir("mkdocs")

val mkDocsSetup: TaskProvider<VenvTask> by tasks.registering(VenvTask::class) {
    description = "Install required Python libraries"
    group = project.name
    venvExec = "pip"
    args = parseSpaceSeparatedArgs("install -r requirements.txt")

    inputs.file("requirements.txt")
        .withPropertyName("requirements.txt")
        .withPathSensitivity(RELATIVE)
        .normalizeLineEndings()
}

val pipPrintRequirements by tasks.registering(VenvTask::class) {
    description = "Log the current requirements (useful util for manually updating dependencies & requirements.txt)"
    group = project.name
    venvExec = "pip"
    args = parseSpaceSeparatedArgs("freeze")
}

val mkDocsBuild: TaskProvider<VenvTask> by tasks.registering(VenvTask::class) {
    description = "Compile Dokka Developer Documentation site using MkDocs"
    group = project.name

    dependsOn(mkDocsSetup)
    finalizedBy(logMkDocsLink)

    venvExec = "mkdocs"
    args = parseSpaceSeparatedArgs("build")

    inputs.dir("docs")
        .withPropertyName("docs")
        .withPathSensitivity(RELATIVE)
        .normalizeLineEndings()

    inputs.file("mkdocs.yml")
        .withPropertyName("mkdocs.yml")
        .withPathSensitivity(RELATIVE)
        .normalizeLineEndings()

    // output directory `site_dir` is specified in mkdocs.yml
    outputs.dir(mkDocsSiteDir)
        .withPropertyName("site_dir")

    outputs.cacheIf("always cache - task has defined output directory") { true }
}

val logMkDocsLink: TaskProvider<Task> by tasks.registering {
    description = "Prints a link to the generated docs"
    group = project.name

    dependsOn(mkDocsBuild)
    doNotTrackState("this task performs no work and should always log the link")

    // redefine mkdocsSiteDir for config-cache compliance
    val mkDocsSiteDir = mkDocsSiteDir

    val ideaActive = System.getProperty("idea.active").toBoolean()

    val dokkaProjectDir = project.rootDir.parentFile

    doLast {
        val indexHtml = mkDocsSiteDir.get().asFile.resolve("index.html")
        val link =
            if (ideaActive) {
                // The built-in IntelliJ server requires a specific path to index.html:
                // a _relative_ path starting with the project's directory (dokka)
                val relativeOutputPath = indexHtml.relativeTo(dokkaProjectDir).invariantSeparatorsPath
                "http://localhost:63342/$relativeOutputPath"
            } else {
                indexHtml.invariantSeparatorsPath
            }

        logger.quiet(
            """
            |
            |***************************************************************************************************
            |    
            |    Dokka Developer Docs: $link
            |
            |***************************************************************************************************
            |
            """.trimMargin()
        )
    }
}
