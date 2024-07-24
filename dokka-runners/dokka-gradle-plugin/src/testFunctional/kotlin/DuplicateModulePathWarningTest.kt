/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject

class DuplicateModulePathWarningTest : FunSpec({

    context("when subprojects have duplicate modulePaths") {
        val project = initMultiModuleProject("DuplicateModulePath")

        project.dir("subproject-hello") {
            buildGradleKts += """
                |
                |dokkatoo {
                |  modulePath = "dupe"
                |}
                |
                """.trimMargin()
        }

        project.dir("subproject-goodbye") {
            buildGradleKts += """
                |
                |dokkatoo {
                |  modulePath = "dupe"
                |}
                |
                """.trimMargin()
        }

        context("generate HTML publication") {
            project.runner
                .addArguments(
                    ":dokkatooGeneratePublicationHtml",
                    "--rerun-tasks",
                    "--stacktrace",
                    "--warn",
                )
                .forwardOutput()
                .build {
                    test("expect duplicate module path warning") {
                        output.shouldContainAll(
                            "[:dokkatooGeneratePublicationHtml] Duplicate `modulePath`s in Dokka Generator parameters.",
                            "- 'subproject-hello', 'subproject-goodbye' have modulePath 'dupe'",
                        )
                    }
                }
        }
    }
})
