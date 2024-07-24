/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.shouldForAll
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContainIgnoringCase
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.gradle.utils.name
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject

class DokkaBaseTasksLinksTest : FunSpec({

    context("Verify that the base lifecycle tasks do not trigger Dokkatoo tasks") {
        val project = initMultiModuleProject("TaskLinks")

        withData(
            "assemble",
            "build",
            "check",
            "clean",
        ) { baseTask ->
            project.runner
                .addArguments(
                    baseTask,
                    "--quiet",
                )
                .build {
                    tasks.shouldForOne { it.path shouldBe ":${baseTask}" }
                    tasks.shouldForOne { it.path shouldBe ":subproject-goodbye:${baseTask}" }
                    tasks.shouldForOne { it.path shouldBe ":subproject-hello:${baseTask}" }

                    tasks.shouldForAll { task ->
                        task.name shouldNotContainIgnoringCase "dokka"
                    }
                }
        }
    }
})
