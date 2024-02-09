/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import com.pswidersk.gradle.python.VenvTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

plugins {
    id("com.pswidersk.python-plugin") version "2.4.0"
}

val mkdocsSetup by tasks.registering(VenvTask::class) {
    venvExec = "pip"
    args = parseSpaceSeparatedArgs("install -r requirements.txt")
}

val pipPrintRequirements by tasks.registering(VenvTask::class) {
    description = "Log the current requirements"
    venvExec = "pip"
    args = parseSpaceSeparatedArgs("freeze")
}

val mkdocsBuild by tasks.registering(VenvTask::class) {
    dependsOn(mkdocsSetup)
    venvExec = "mkdocs"
    args = parseSpaceSeparatedArgs("build")

    inputs.dir("docs")
        .withPropertyName("docs")
        .withPathSensitivity(RELATIVE)
        .normalizeLineEndings()

    // output directory `site_dir` is specified in mkdocs.yml
    val mkdocsSiteDir = layout.buildDirectory.dir("mkdocs")
    outputs.dir(mkdocsSiteDir)
        .withPropertyName("site_dir")

    outputs.cacheIf("always cache - task has defined output directory") { true }

    // log a link to the generated docs using the build-in IJ server
    val ideaActive = System.getProperty("idea.active").toBoolean()
    val dokkaProjectDir = project.rootDir.parentFile

    doLast {
        if (ideaActive) {
            // The built-in IntelliJ server requires a path to index.html
            // that includes the root project name.
            val indexPath = mkdocsSiteDir.get().asFile.relativeTo(dokkaProjectDir)

            logger.quiet(
                """
                |
                |***************************************************************************************************
                |    
                |    Dokka Developer Docs: http://localhost:63342/$indexPath/index.html
                |
                |***************************************************************************************************
                |
                """.trimMargin()
            )
        }
    }
}
