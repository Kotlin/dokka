/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject
import kotlin.io.path.readText

class ModuleDocumentationCrossModuleLinksTest : FunSpec({

    context("cross-module links from Module.md") {
        val project = initMultiModuleProject("module-documentation-links") {
            dir("subproject-hello") {
                createKotlinFile(
                    "src/main/kotlin/TopLevel.kt",
                    """
                    |package com.project.hello
                    |
                    |fun readBytes(): String = ""
                    |val SystemFileSystem: String = ""
                    |""".trimMargin()
                )
            }
            dir("subproject-goodbye") {
                buildGradleKts += """
                    |dependencies {
                    |    implementation(project(":subproject-hello"))
                    |}
                    |
                    |dokka {
                    |    dokkaSourceSets.configureEach {
                    |        includes.from("Module.md")
                    |    }
                    |}
                    |""".trimMargin()
                createFile(
                    "Module.md",
                    """
                    |# Module subproject-goodbye
                    |
                    |See [com.project.hello.readBytes], [com.project.hello.SystemFileSystem], and [com.project.hello.Hello].
                    |
                    |# Package com.project.goodbye
                    |
                    |See [com.project.hello.readBytes], [com.project.hello.SystemFileSystem], and [com.project.hello.Hello].
                    |""".trimMargin()
                )
            }
        }

        project.runner
            .addArguments(":dokkaGeneratePublicationHtml", "--stacktrace")
            .build {
                val outputDir = project.projectDir.resolve("build/dokka/html")
                val targets = mapOf(
                    "com.project.hello.readBytes" to "read-bytes.html",
                    "com.project.hello.SystemFileSystem" to "-system-file-system.html",
                    "com.project.hello.Hello" to "-hello/index.html",
                )
                mapOf(
                    "module" to "subproject-goodbye/index.html",
                    "package" to "subproject-goodbye/com.project.goodbye/index.html",
                ).forEach { (description, path) ->
                    targets.forEach { (label, declarationPath) ->
                        // https://github.com/Kotlin/dokka/issues/3891
                        test("$description description links to $label in the other module") {
                            val page = outputDir.resolve(path)
                            val target = outputDir.resolve("subproject-hello/com.project.hello/$declarationPath")
                            page.shouldBeAFile()
                            target.shouldBeAFile()
                            val href = page.parent.relativize(target).toString().replace('\\', '/')
                            page.readText() shouldContain """<a href="$href">$label</a>"""
                        }
                    }
                }
            }
    }
})
