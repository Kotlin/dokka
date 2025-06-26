/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jetbrains.dokka.gradle.utils.addArguments
import org.jetbrains.dokka.gradle.utils.build
import org.jetbrains.dokka.gradle.utils.buildGradleKts
import org.jetbrains.dokka.gradle.utils.createKotlinFile
import org.jetbrains.dokka.gradle.utils.projects.initMultiModuleProject
import kotlin.io.path.readText

class MultiModuleAggregationTest : FunSpec({

    context("when aggregating in root project") {
        val project = initMultiModuleProject("root-aggregate") {
            buildGradleKts = buildGradleKts
                .replace(
                    """kotlin("jvm") version embeddedKotlinVersion apply false""",
                    """kotlin("jvm") version embeddedKotlinVersion""",
                )

            buildGradleKts += """
                |dependencies {
                |  dokka(rootProject)
                |}
                |""".trimMargin()

            createKotlinFile(
                "src/main/kotlin/RootProjectClass.kt",
                """
                |class RootProjectClass {
                |  fun thisClassIsInTheRootProject() {}
                |}
                |""".trimMargin()
            )
        }

        project.runner
            .addArguments(
                ":dokkaGeneratePublicationHtml",
                "--stacktrace",
            )
            .forwardOutput()
            .build {
                test("expect build is successful") {
                    output shouldContain "BUILD SUCCESSFUL"
                }

                test("expect HTML site is generated") { // TODO better test name
                    val navigationHtml = project.projectDir.resolve("build/dokka/html/navigation.html")

                    navigationHtml.readText() shouldNotContain "..//[root]/-root-project-class/index.html"
                    navigationHtml.readText() shouldContain "..//[root]/-root-project-class/index.html"
                }
            }
    }
})
